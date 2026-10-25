package cn.fandmc.epoch.render;

import cn.fandmc.epoch.asset.*;
import cn.fandmc.epoch.asset.BlockModels.Face;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.block.entity.*;
import cn.fandmc.epoch.render.blockentity.*;
import cn.fandmc.epoch.world.*;
import org.joml.FrustumIntersection;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;

/** Tile geometry is cached; visible instances are batched by full-size texture. */
public final class BlockEntityRenderer implements AutoCloseable {
    private static final int[] TRIANGLES={0,1,2,0,2,3};
    private final Map<Class<? extends BlockEntity>,BlockEntityModel<?>> models=new HashMap<>();
    private final Map<ResourceId,Vertices> batches=new HashMap<>();
    private final BlockEntityTextures textures;
    private final int vao,buffer;
    private int draws,vertices;
    public BlockEntityRenderer(AssetDirectory assets,BlockRegistry registry) {
        register(new ChestBlockEntityModel());register(new SignBlockEntityModel());register(new SkullBlockEntityModel());
        for(var block:registry.blocks())if(block instanceof BlockEntity&&!models.containsKey(block.getClass()))
            throw new IllegalArgumentException("No block entity model for "+block.id());
        textures=new BlockEntityTextures(assets);vao=glGenVertexArrays();buffer=glGenBuffers();
        glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,buffer);
        glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,false,40,0L);
        glEnableVertexAttribArray(1);glVertexAttribPointer(1,2,GL_FLOAT,false,40,12L);
        glEnableVertexAttribArray(2);glVertexAttribPointer(2,3,GL_FLOAT,false,40,20L);
        glEnableVertexAttribArray(3);glVertexAttribPointer(3,2,GL_FLOAT,false,40,32L);glBindVertexArray(0);
    }
    public int draws(){return draws;}public int vertices(){return vertices;}
    public void render(ClientWorld world,double cameraX,double cameraY,double cameraZ,int distance,FrustumIntersection frustum,int offsetLocation) {
        batches.values().forEach(v->v.size=0);draws=vertices=0;
        double limit=Math.min(distance*16d,96);limit*=limit;
        glActiveTexture(GL_TEXTURE0);glBindSampler(0,0);
        LightSampler light=new LightSampler(world,false);
        for(ChunkColumn column:world.columns()) {
            double cx=column.x()*16d+8-cameraX,cz=column.z()*16d+8-cameraZ;
            if(cx*cx+cz*cz>limit+512)continue;
            for(var entry:column.blockEntities().entrySet()) {
                int index=entry.getKey(),x=column.x()*16+(index&15),y=index>>8,z=column.z()*16+(index>>4&15);
                float ox=(float)(x-cameraX),oy=(float)(y-cameraY),oz=(float)(z-cameraZ);
                if(ox*ox+oy*oy+oz*oz>limit||!frustum.testAab(ox-.1f,oy,oz-.1f,ox+2,oy+1.6f,oz+2))continue;
                var state=entry.getValue();
                if(!(world.registry().block(state) instanceof BlockEntity type))continue;
                BlockEntityModel<?> model=models.get(type.getClass());
                if(model==null)throw new IllegalStateException("No block entity model for "+type.id());
                List<Face> faces=model.faces(type,state,world.blockEntity(x,y,z),world,x,y,z,textures);
                int brightness=light.packed(x,y,z);
                float block=LightSampler.block(brightness),sky=LightSampler.sky(brightness);
                for(Face face:faces){if(face.texture().namespace().equals("minecraft"))textures.asset(face.texture());append(face,ox,oy,oz,block,sky);}
            }
        }
        glUniform3f(offsetLocation,0,0,0);glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,buffer);
        glEnable(GL_BLEND);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);glDepthMask(true);
        for(var entry:batches.entrySet()) {
            Vertices batch=entry.getValue();if(batch.size==0)continue;
            glBindTexture(GL_TEXTURE_2D,textures.get(entry.getKey()).id());glBufferData(GL_ARRAY_BUFFER,Arrays.copyOf(batch.data,batch.size),GL_STREAM_DRAW);
            glDrawArrays(GL_TRIANGLES,0,batch.size/10);draws++;vertices+=batch.size/10;
        }
        glDisable(GL_BLEND);textures.trim();batches.entrySet().removeIf(e->e.getValue().size==0);
        models.values().forEach(BlockEntityModel::trim);
    }
    private void register(BlockEntityModel<?> model){
        if(models.putIfAbsent(model.blockType(),model)!=null)throw new IllegalArgumentException("Duplicate block entity model");
    }
    private void append(Face face,float x,float y,float z,float block,float sky) {
        Vertices batch=batches.computeIfAbsent(face.texture(),ignored->new Vertices());
        for(int i:TRIANGLES){var v=face.vertices().get(i);batch.add(x+v.x(),y+v.y(),z+v.z(),v.u(),v.v(),face.shade(),block,sky);}
    }
    private static final class Vertices {
        float[] data=new float[1024];int size;
        void add(float x,float y,float z,float u,float v,float shade,float block,float sky) {
            if(size+10>data.length)data=Arrays.copyOf(data,data.length*2);
            data[size++]=x;data[size++]=y;data[size++]=z;data[size++]=u;data[size++]=v;
            data[size++]=shade;data[size++]=shade;data[size++]=shade;data[size++]=block;data[size++]=sky;
        }
    }
    public void reset(){models.values().forEach(BlockEntityModel::reset);batches.clear();textures.reset();}
    @Override public void close(){textures.close();glDeleteBuffers(buffer);glDeleteVertexArrays(vao);}
}
