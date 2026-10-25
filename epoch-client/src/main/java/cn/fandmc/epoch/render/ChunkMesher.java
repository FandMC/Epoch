package cn.fandmc.epoch.render;

import cn.fandmc.epoch.asset.BlockModels;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.block.BlockStateResolver;
import java.util.*;
import java.util.concurrent.CancellationException;

/** CPU-only meshing. Prebaked model/UV arrays are immutable and shared between workers. */
public final class ChunkMesher {
    public record MeshData(float[] vertices,float[] translucentVertices,int minY,int maxY,long buildNanos) {}
    private record CullShape(int direction,float minA,float minB,float maxA,float maxB) {
        boolean covers(CullShape other){return minA<=other.minA+.0001f&&minB<=other.minB+.0001f&&maxA>=other.maxA-.0001f&&maxB>=other.maxB-.0001f;}
        boolean full(){return minA<=.0001f&&minB<=.0001f&&maxA>=.9999f&&maxB>=.9999f;}
    }
    private record Face(float[] vertices,FaceLighting.Geometry lighting,boolean translucent,float red,float green,float blue,CullShape cullShape) {}
    private record Model(Face[] faces,int weight,boolean smoothLight) {}
    private record Material(Model[] models,int totalWeight,boolean occludes,boolean opaque) {}
    private static final int[] DX={0,0,0,0,-1,1},DY={-1,1,0,0,0,0},DZ={0,0,-1,1,0,0};
    private static final int[] TRIANGLES={0,1,2,0,2,3};
    private final Material[] materials;
    private final cn.fandmc.epoch.block.BlockState[] states;
    private final BlockStateResolver stateResolver;

    public ChunkMesher(BlockRegistry registry,BlockModels models,TextureAtlas atlas){
        stateResolver=new BlockStateResolver(registry);
        materials=new Material[registry.size()];
        states=new cn.fandmc.epoch.block.BlockState[registry.size()];
        for(int handle=0;handle<materials.length;handle++){
            var definition=registry.definition(handle);
            states[handle]=definition.state();
            var choices=models.models(definition.state());
            List<Model> baked=new ArrayList<>();boolean occludes=definition.opaque()&&!choices.isEmpty();int totalWeight=0;
            for(var choice:choices){
                List<Face> faces=new ArrayList<>();int fullSides=0;
                for(var face:choice.faces()){
                    int normal=direction(face.normal()),cull=face.cullFace()==null?-1:direction(face.cullFace());
                    var uv=atlas.region(face.texture());float[] vertices=new float[20];
                    boolean full=true;int index=0;
                    for(var v:face.vertices()){
                        vertices[index++]=v.x();vertices[index++]=v.y();vertices[index++]=v.z();
                        vertices[index++]=uv.u0()+v.u()*(uv.u1()-uv.u0());vertices[index++]=uv.v0()+v.v()*(uv.v1()-uv.v0());
                        full&=(nearEdge(v.x())&&nearEdge(v.y())&&nearEdge(v.z()));
                    }
                    CullShape cullShape=cull>=0?cullShape(cull,vertices):null;
                    if(full&&cullShape!=null&&cullShape.full())fullSides|=1<<cull;
                    float shade=face.shade();
                    float[] tint=BlockColors.tint(definition.state(),face.tinted());
                    faces.add(new Face(vertices,FaceLighting.geometry(normal,vertices,definition.lighting().fullCube()),atlas.translucent(face.texture()),shade*tint[0],shade*tint[1],shade*tint[2],cullShape));
                }
                occludes&=fullSides==63;
                baked.add(new Model(faces.toArray(Face[]::new),choice.weight(),choice.ambientOcclusion()&&definition.lighting().emission()==0));totalWeight+=choice.weight();
            }
            materials[handle]=new Material(baked.toArray(Model[]::new),totalWeight,occludes,definition.opaque());
        }
    }

    public MeshData build(MeshSnapshot snapshot){
        long start=System.nanoTime();Floats data=new Floats(),translucent=new Floats();
        FaceLighting lighting=new FaceLighting(snapshot);
        BlockStateResolver.Lookup baseStates=snapshot::state;
        var column=snapshot.center();int baseX=column.x()*16,baseZ=column.z()*16,minY=256,maxY=0;
        for(int section=0;section<16;section++)if(column.hasSection(section)){
            if(Thread.currentThread().isInterrupted())throw new CancellationException();
            for(int y=section*16;y<section*16+16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                int state=column.stateHandle(x,y,z);if(state==0)continue;
                state=stateResolver.resolve(state,x,y,z,baseStates);
                Material material=materials[state];if(material.totalWeight()==0)continue;
                Model chosen=selectModel(material,baseX+x,y,baseZ+z);
                for(Face face:chosen.faces()){
                    if(shouldCull(state,material,face,x,y,z,baseX,baseZ,snapshot))continue;
                    lighting.calculate(x,y,z,face.lighting(),chosen.smoothLight());
                    Floats target=face.translucent()?translucent:data;
                    for(int triangle:TRIANGLES){int offset=triangle*5;float[] v=face.vertices();
                        int light=lighting.light(triangle);float ao=lighting.occlusion(triangle);
                        target.vertex(x+v[offset],y+v[offset+1],z+v[offset+2],v[offset+3],v[offset+4],face.red()*ao,face.green()*ao,face.blue()*ao,LightSampler.block(light),LightSampler.sky(light));
                    }
                    minY=Math.min(minY,y);maxY=Math.max(maxY,y+2);
                }
            }
        }
        return new MeshData(data.finish(),translucent.finish(),minY==256?0:minY,maxY,System.nanoTime()-start);
    }
    private boolean shouldCull(int state,Material material,Face face,int x,int y,int z,int baseX,int baseZ,MeshSnapshot snapshot){
        CullShape shape=face.cullShape();if(shape==null)return false;
        int direction=shape.direction(),neighbor=snapshot.state(x+DX[direction],y+DY[direction],z+DZ[direction]);
        if(materials[neighbor].occludes())return true;
        neighbor=stateResolver.resolve(neighbor,x+DX[direction],y+DY[direction],z+DZ[direction],snapshot::state);
        if(!cullsMatchingFace(state,neighbor,material))return false;
        Material adjacent=materials[neighbor];
        if(adjacent.totalWeight()==0)return false;
        Model adjacentModel=selectModel(adjacent,baseX+x+DX[direction],y+DY[direction],baseZ+z+DZ[direction]);
        int opposite=direction^1;
        for(Face candidate:adjacentModel.faces()){
            CullShape other=candidate.cullShape();
            if(other!=null&&other.direction()==opposite&&other.covers(shape))return true;
        }
        return false;
    }
    private static Model selectModel(Material material,int x,int y,int z){
        int pick=Math.floorMod(x*3129871^z*116129781^y,material.totalWeight());
        for(Model model:material.models()){if(pick<model.weight())return model;pick-=model.weight();}
        throw new IllegalStateException("Model weight selection failed");
    }
    private static CullShape cullShape(int direction,float[] vertices){
        int axis=direction<2?1:direction<4?2:0;
        int first=direction<2?0:direction<4?0:2;
        int second=direction<2?2:1;
        float plane=(direction&1)==0?0:1,minA=Float.POSITIVE_INFINITY,minB=Float.POSITIVE_INFINITY;
        float maxA=Float.NEGATIVE_INFINITY,maxB=Float.NEGATIVE_INFINITY;
        for(int i=0;i<4;i++){
            int offset=i*5;float a=vertices[offset+first],b=vertices[offset+second],normal=vertices[offset+axis];
            if(Math.abs(normal-plane)>.0001f)return null;
            minA=Math.min(minA,a);maxA=Math.max(maxA,a);minB=Math.min(minB,b);maxB=Math.max(maxB,b);
        }
        if(maxA-minA<.0001f||maxB-minB<.0001f)return null;
        // Bounding rectangles of rotated quads can cover empty corners.
        for(int i=0;i<4;i++){
            float a=vertices[i*5+first],b=vertices[i*5+second];
            if((Math.abs(a-minA)>.0001f&&Math.abs(a-maxA)>.0001f)
                    ||(Math.abs(b-minB)>.0001f&&Math.abs(b-maxB)>.0001f))return null;
        }
        return new CullShape(direction,minA,minB,maxA,maxB);
    }
    private boolean cullsMatchingFace(int state,int neighbor,Material material){
        if(material.opaque())return false;
        if(states[state].block().equals(states[neighbor].block()))return true;
        String block=states[state].block().path();
        // Connected panes have different resolved handles at each end. Only glass
        // shares boundary faces by block identity; partial blocks need their shape.
        return (block.equals("glass")||block.equals("glass_pane")||block.endsWith("_stained_glass")||block.endsWith("_stained_glass_pane")||block.equals("iron_bars"))
                &&states[state].block().equals(states[neighbor].block());
    }
    private static boolean nearEdge(float value){return Math.abs(value)<.0001f||Math.abs(value-1)<.0001f;}
    private static int direction(String name){return switch(name){case "down"->0;case "up"->1;case "north"->2;case "south"->3;case "west"->4;case "east"->5;default->throw new IllegalArgumentException("Invalid direction: "+name);};}
    private static final class Floats{
        private float[] values=new float[16384];private int size;
        void vertex(float x,float y,float z,float u,float v,float r,float g,float b,float block,float sky){
            if(size+10>values.length)values=Arrays.copyOf(values,Math.max(values.length*2,size+10));
            values[size++]=x;values[size++]=y;values[size++]=z;values[size++]=u;values[size++]=v;
            values[size++]=r;values[size++]=g;values[size++]=b;values[size++]=block;values[size++]=sky;
        }
        float[] finish(){return Arrays.copyOf(values,size);}
    }
}
