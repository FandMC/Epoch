package cn.fandmc.epoch.render;

import cn.fandmc.epoch.asset.*;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.player.PlayerState;
import cn.fandmc.epoch.world.ClientWorld;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import static org.lwjgl.opengl.GL33C.*;

/** Snapshot-based CPU workers; bounded uploads and OpenGL ownership stay on the game thread. */
public final class WorldRenderer implements AutoCloseable {
    private record Layer(int vao,int buffer,int indexBuffer,int count,TranslucentOrder order) implements AutoCloseable {
        public void close(){if(indexBuffer!=0)glDeleteBuffers(indexBuffer);glDeleteBuffers(buffer);glDeleteVertexArrays(vao);}
    }
    private record Mesh(Layer solid,Layer translucent,int minY,int maxY) implements AutoCloseable {
        public void close(){if(solid!=null)solid.close();if(translucent!=null)translucent.close();}
    }
    private record Visible(long key,Mesh mesh,float x,float y,float z,double distance) {}
    private record Job(long revision,Future<ChunkMesher.MeshData> future) {}
    private final BlockModels models;
    private final TextureAtlas atlas;
    private final ChunkMesher mesher;
    private final BlockEntityRenderer blockEntities;
    private final Map<Long,Mesh> meshes=new HashMap<>();
    private final Set<Long> dirty=new HashSet<>();
    private final Map<Long,Long> revisions=new HashMap<>();
    private final Map<Long,Job> jobs=new HashMap<>();
    private final int workerCount=Math.max(1,Math.min(2,Runtime.getRuntime().availableProcessors()-1));
    private final ExecutorService workers=Executors.newFixedThreadPool(workerCount,Thread.ofPlatform().daemon().name("epoch-mesh-",0).factory());
    private final int program,matrixLocation,offsetLocation,atlasLocation,daylightLocation,ambientLocation;
    private final Matrix4f matrix=new Matrix4f();
    private final FrustumIntersection frustum=new FrustumIntersection();
    private final float[] matrixValues=new float[16];
    private final List<Visible> visible=new ArrayList<>();
    private int drawCalls,vertices;
    private double lastMeshMillis;
    private ClientWorld currentWorld;
    private long worldEpoch=-1;

    public WorldRenderer(AssetDirectory assets,BlockRegistry registry) throws IOException {
        models=new BlockModels(assets,registry);
        Set<ResourceId> textures=new TreeSet<>(Comparator.comparing(ResourceId::toString));
        for(var definition:registry.definitions())if(definition.recognized())
            for(var model:models.models(definition.state()))for(var face:model.faces())textures.add(face.texture());
        atlas=new TextureAtlas(assets,textures);
        int linked=0;
        try {mesher=new ChunkMesher(registry,models,atlas);linked=createProgram();program=linked;}
        catch(RuntimeException e){atlas.close();if(linked!=0)glDeleteProgram(linked);workers.shutdownNow();throw e;}
        matrixLocation=glGetUniformLocation(program,"matrix");offsetLocation=glGetUniformLocation(program,"offset");
        atlasLocation=glGetUniformLocation(program,"atlas");daylightLocation=glGetUniformLocation(program,"daylight");
        ambientLocation=glGetUniformLocation(program,"ambient");
        blockEntities=new BlockEntityRenderer(assets,registry);
        System.out.printf("Loaded %d named states; %d textures; %d unresolved model states; %d mesh workers%n",registry.size(),atlas.size(),models.unresolvedCount(),workerCount);
    }
    public int drawCalls(){return drawCalls;}
    public int vertices(){return vertices;}
    public int pendingMeshes(){return dirty.size()+jobs.size();}
    public double lastMeshMillis(){return lastMeshMillis;}
    public int unresolvedModels(){return models.unresolvedCount();}
    public Set<String> unresolved(){return models.unresolved();}
    public void reset(){
        blockEntities.reset();
        jobs.values().forEach(job->job.future().cancel(true));jobs.clear();
        meshes.values().forEach(Mesh::close);meshes.clear();dirty.clear();revisions.clear();
        currentWorld=null;worldEpoch=-1;
    }
    public void render(ClientWorld world,PlayerState player,int width,int height,int distance,long tick,float alpha){
        if(currentWorld!=world||worldEpoch!=world.epoch()){reset();currentWorld=world;worldEpoch=world.epoch();}
        for(long key:world.drainDirty()){
            revisions.merge(key,1L,Long::sum);
            if(world.column((int)(key>>32),(int)key)==null){
                dirty.remove(key);Mesh mesh=meshes.remove(key);if(mesh!=null)mesh.close();
                Job job=jobs.remove(key);if(job!=null)job.future().cancel(true);
            }else dirty.add(key);
        }
        publishMeshes();scheduleMeshes(world,player,distance);
        drawCalls=vertices=0;
        if(!player.positioned)return;
        double cameraX=player.renderX(alpha),cameraY=player.renderY(alpha)+player.eyeHeight(),cameraZ=player.renderZ(alpha);
        matrix.identity().perspective((float)Math.toRadians(70),width/(float)height,.05f,(distance+2)*16f)
                .rotateX((float)Math.toRadians(player.pitch)).rotateY((float)Math.toRadians(player.yaw+180));
        frustum.set(matrix);
        glUseProgram(program);glUniformMatrix4fv(matrixLocation,false,matrix.get(matrixValues));
        glUniform1i(atlasLocation,0);glUniform1f(daylightLocation,world.daylight());glUniform1f(ambientLocation,world.dimension()==-1?.1f:.035f);
        glEnable(GL_DEPTH_TEST);glDepthMask(true);glEnable(GL_CULL_FACE);glCullFace(GL_BACK);glFrontFace(GL_CCW);glDisable(GL_BLEND);
        atlas.bind(tick);
        visible.clear();
        for(var entry:meshes.entrySet()){
            if(distance(entry.getKey(),player)>(distance+1d)*(distance+1d)*256)continue;
            Mesh mesh=entry.getValue();
            int x=(int)(entry.getKey()>>32),z=(int)(long)entry.getKey();
            float ox=(float)(x*16d-cameraX),oy=(float)-cameraY,oz=(float)(z*16d-cameraZ);
            if(!frustum.testAab(ox,oy+mesh.minY(),oz,ox+16,oy+mesh.maxY(),oz+16))continue;
            visible.add(new Visible(entry.getKey(),mesh,ox,oy,oz,(ox+8d)*(ox+8d)+(oz+8d)*(oz+8d)));
            Layer solid=mesh.solid();
            if(solid!=null){
                glUniform3f(offsetLocation,ox,oy,oz);glBindVertexArray(solid.vao());
                glDrawArrays(GL_TRIANGLES,0,solid.count());drawCalls++;vertices+=solid.count();
            }
        }
        blockEntities.render(world,cameraX,cameraY,cameraZ,distance,frustum,offsetLocation);
        drawCalls+=blockEntities.draws();vertices+=blockEntities.vertices();atlas.bind(tick);
        visible.sort(Comparator.comparingDouble(Visible::distance).reversed());
        glEnable(GL_BLEND);glBlendEquation(GL_FUNC_ADD);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);glDepthMask(false);
        for(Visible entry:visible){
            Layer layer=entry.mesh().translucent();if(layer==null)continue;
            glUniform3f(offsetLocation,entry.x(),entry.y(),entry.z());glBindVertexArray(layer.vao());
            int[] indices=layer.order().update(cameraX-(int)(entry.key()>>32)*16d,cameraY,cameraZ-(int)entry.key()*16d);
            if(indices!=null){glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,layer.indexBuffer());glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_DYNAMIC_DRAW);}
            glDrawElements(GL_TRIANGLES,layer.count(),GL_UNSIGNED_INT,0L);drawCalls++;vertices+=layer.count();
        }
        glDepthMask(true);glDisable(GL_BLEND);
        glBindVertexArray(0);glUseProgram(0);
    }
    private void scheduleMeshes(ClientWorld world,PlayerState player,int distance){
        if(!player.positioned)return;
        int slots=workerCount-jobs.size();
        while(slots-->0){
            Long closest=null;double best=(distance+2d)*(distance+2d)*256;
            for(long key:dirty)if(!jobs.containsKey(key)){
                double value=distance(key,player);if(value<best){best=value;closest=key;}
            }
            if(closest==null)break;
            long key=closest;dirty.remove(key);
            MeshSnapshot snapshot=MeshSnapshot.capture(world,(int)(key>>32),(int)key);
            if(snapshot==null)continue;
            jobs.put(key,new Job(revisions.getOrDefault(key,0L),workers.submit(()->mesher.build(snapshot))));
        }
    }
    private void publishMeshes(){
        long deadline=System.nanoTime()+2_000_000L;int uploaded=0;
        for(var iterator=jobs.entrySet().iterator();iterator.hasNext();){
            var entry=iterator.next();Job job=entry.getValue();if(!job.future().isDone())continue;
            iterator.remove();
            try{
                ChunkMesher.MeshData data=job.future().get();
                if(job.revision()!=revisions.getOrDefault(entry.getKey(),0L))continue;
                Mesh replacement=upload(data);Mesh old=meshes.put(entry.getKey(),replacement);if(old!=null)old.close();
                lastMeshMillis=data.buildNanos()/1_000_000d;uploaded++;
            }catch(CancellationException ignored){
                // A world reset or unload invalidated this snapshot before publication.
            }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Mesh publication interrupted",e);}
            catch(ExecutionException e){throw new IllegalStateException("Chunk meshing failed",e.getCause());}
            if(uploaded>=2||System.nanoTime()>deadline)break;
        }
    }
    private static Mesh upload(ChunkMesher.MeshData data){
        Layer solid=uploadLayer(data.vertices(),false);
        try{return new Mesh(solid,uploadLayer(data.translucentVertices(),true),data.minY(),data.maxY());}
        catch(RuntimeException e){if(solid!=null)solid.close();throw e;}
    }
    private static Layer uploadLayer(float[] vertices,boolean translucent){
        if(vertices.length==0)return null;
        int vao=glGenVertexArrays(),buffer=glGenBuffers();
        glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,buffer);glBufferData(GL_ARRAY_BUFFER,vertices,GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,false,40,0L);
        glEnableVertexAttribArray(1);glVertexAttribPointer(1,2,GL_FLOAT,false,40,12L);
        glEnableVertexAttribArray(2);glVertexAttribPointer(2,3,GL_FLOAT,false,40,20L);
        glEnableVertexAttribArray(3);glVertexAttribPointer(3,2,GL_FLOAT,false,40,32L);
        int indices=translucent?glGenBuffers():0;
        if(translucent)glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,indices);
        glBindVertexArray(0);return new Layer(vao,buffer,indices,vertices.length/10,translucent?new TranslucentOrder(vertices):null);
    }
    private static double distance(long key,PlayerState player){double dx=(int)(key>>32)*16d+8-player.x,dz=(int)key*16d+8-player.z;return dx*dx+dz*dz;}
    private static int createProgram(){
        int vertex=shader(GL_VERTEX_SHADER,"""
                #version 330 core
                layout(location=0) in vec3 position;
                layout(location=1) in vec2 uv;
                layout(location=2) in vec3 color;
                layout(location=3) in vec2 light;
                uniform mat4 matrix;
                uniform vec3 offset;
                out vec2 texCoord;
                out vec3 tint;
                out vec2 illumination;
                void main(){
                    gl_Position=matrix*vec4(position+offset,1);
                    texCoord=uv;tint=color;illumination=light;
                }
                """);
        int fragment=0,program=0;
        try{
            fragment=shader(GL_FRAGMENT_SHADER,"""
                    #version 330 core
                    in vec2 texCoord;
                    in vec3 tint;
                    in vec2 illumination;
                    uniform sampler2D atlas;
                    uniform float daylight;
                    uniform float ambient;
                    out vec4 outColor;
                    float lightCurve(float level){return level/(4.0-3.0*level);}
                    void main(){
                        vec4 c=texture(atlas,texCoord);
                        if(c.a<0.1)discard;
                        float sky=lightCurve(max(0.0,illumination.y-(1.0-daylight)*11.0/15.0));
                        float block=lightCurve(illumination.x);
                        vec3 lighting=max(vec3(ambient),max(block*vec3(1.0,.88,.72),sky*mix(vec3(.70,.78,1.0),vec3(1.0),daylight)));
                        outColor=vec4(c.rgb*tint*lighting,c.a);
                    }
                    """);
            program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program);
            if(glGetProgrami(program,GL_LINK_STATUS)==GL_FALSE)throw new IllegalStateException(glGetProgramInfoLog(program));
            return program;
        }catch(RuntimeException e){if(program!=0)glDeleteProgram(program);throw e;}
        finally{glDeleteShader(vertex);if(fragment!=0)glDeleteShader(fragment);}
    }
    private static int shader(int type,String source){
        int id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);
        if(glGetShaderi(id,GL_COMPILE_STATUS)==GL_FALSE){String error=glGetShaderInfoLog(id);glDeleteShader(id);throw new IllegalStateException(error);}return id;
    }
    @Override public void close(){reset();workers.shutdownNow();blockEntities.close();atlas.close();glDeleteProgram(program);}
}
