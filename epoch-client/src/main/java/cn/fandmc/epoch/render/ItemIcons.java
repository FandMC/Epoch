package cn.fandmc.epoch.render;

import cn.fandmc.epoch.asset.*;
import cn.fandmc.epoch.asset.BlockModels.*;
import cn.fandmc.epoch.item.ItemStack;
import cn.fandmc.epoch.network.mapping.LegacyItemMapping;
import cn.fandmc.epoch.nbt.NbtCompound;
import io.github.humbleui.skija.*;
import org.joml.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;
import static cn.fandmc.epoch.render.blockentity.ModelBoxGeometry.box;

/** Bounded GPU icon cache. Baking is performed before the world/Skija passes. */
public final class ItemIcons implements AutoCloseable {
    private static final int SIZE=64,LIMIT=256;
    private record Key(ResourceId model,int color0,int color1,int color2,NbtCompound skin) {}
    private record Icon(int texture,BackendTexture backend,Image image) implements AutoCloseable {
        public void close(){image.close();backend.close();glDeleteTextures(texture);}
    }
    private final ItemModels models;
    private final BlockEntityTextures textures;
    private final LinkedHashMap<Key,Icon> cache=new LinkedHashMap<>(64,.75f,true);
    private final Set<ResourceId> failures=new HashSet<>();
    private final int framebuffer,depth,vao,vbo,program,matrixLocation,colorLocation;
    private final float[] matrix=new float[16];
    public ItemIcons(AssetDirectory assets){
        models=new ItemModels(assets);textures=new BlockEntityTextures(assets);
        framebuffer=glGenFramebuffers();depth=glGenRenderbuffers();glBindRenderbuffer(GL_RENDERBUFFER,depth);
        glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,SIZE,SIZE);
        vao=glGenVertexArrays();vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
        glEnableVertexAttribArray(0);glVertexAttribPointer(0,3,GL_FLOAT,false,20,0);
        glEnableVertexAttribArray(1);glVertexAttribPointer(1,2,GL_FLOAT,false,20,12);
        int vertex=shader(GL_VERTEX_SHADER,"#version 330 core\nlayout(location=0) in vec3 position; layout(location=1) in vec2 uv; uniform mat4 matrix; out vec2 tex; void main(){tex=uv;gl_Position=matrix*vec4(position,1);}");
        int fragment=shader(GL_FRAGMENT_SHADER,"#version 330 core\nin vec2 tex; uniform sampler2D image; uniform vec3 tint; out vec4 color; void main(){vec4 c=texture(image,tex);if(c.a<0.01)discard;color=vec4(c.rgb*tint,c.a);}");
        program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);
        if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(program));
        matrixLocation=glGetUniformLocation(program,"matrix");colorLocation=glGetUniformLocation(program,"tint");
    }
    private Key key(ItemStack stack,LegacyItemMapping mapping){
        var visual=mapping.visual(stack);if(visual==null||visual.model()==null)return null;
        int[] colors=visual.colors();int base=colors[0];
        if(stack.item().id().path().startsWith("leather_"))base=stack.tag().compound("display").integer("color",base);
        return new Key(visual.model(),base,colors[1],colors[2],stack.tag().compound("SkullOwner"));
    }
    public void prepare(Collection<ItemStack> stacks,LegacyItemMapping mapping,DirectContext context){
        int budget=12;
        for(ItemStack stack:stacks){if(stack.isEmpty())continue;Key key=key(stack,mapping);if(key==null||cache.containsKey(key)||failures.contains(key.model()))continue;
            if(budget--==0)break;
            try{cache.put(key,bake(key,stack,context));}
            catch(IOException|IllegalArgumentException e){if(failures.add(key.model()))System.getLogger(ItemIcons.class.getName()).log(System.Logger.Level.WARNING,"Item model unavailable: "+key.model()+": "+e.getMessage());}
            while(cache.size()>LIMIT){var it=cache.entrySet().iterator();it.next().getValue().close();it.remove();}
        }
        glBindFramebuffer(GL_FRAMEBUFFER,0);
    }
    public Image image(ItemStack stack,LegacyItemMapping mapping){Key key=key(stack,mapping);Icon icon=key==null?null:cache.get(key);return icon==null?null:icon.image();}
    public int cachedCount(){return cache.size();}
    public Set<ResourceId> unresolved(){return Set.copyOf(failures);}
    private Icon bake(Key key,ItemStack stack,DirectContext context)throws IOException {
        ItemModels.Model model=models.get(key.model());
        List<Face> faces=model.entity()?entity(stack):model.faces();
        if(faces.isEmpty()&&model.layers().isEmpty())throw new IOException("Model has no inventory geometry");
        int texture=glGenTextures();BackendTexture backend=null;Image image=null;
        try{
            glActiveTexture(GL_TEXTURE0);glBindSampler(0,0);glBindTexture(GL_TEXTURE_2D,texture);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,SIZE,SIZE,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
            glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,texture,0);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);
            if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Incomplete item framebuffer");
            glViewport(0,0,SIZE,SIZE);glDisable(GL_SCISSOR_TEST);glDisable(GL_STENCIL_TEST);glDisable(GL_CULL_FACE);glDisable(GL_FRAMEBUFFER_SRGB);
            glColorMask(true,true,true,true);glDepthMask(true);glClearColor(0,0,0,0);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
            glEnable(GL_BLEND);glBlendEquation(GL_FUNC_ADD);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            glUseProgram(program);glUniform1i(glGetUniformLocation(program,"image"),0);glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
            Matrix4f transform=new Matrix4f().ortho(-.65f,.65f,-.65f,.65f,-10,10).mul(model.transform()).translate(-.5f,-.5f,-.5f);
            glUniformMatrix4fv(matrixLocation,false,transform.get(matrix));
            if(!model.layers().isEmpty()){
                glDisable(GL_DEPTH_TEST);
                for(int i=0;i<model.layers().size();i++){
                    ResourceId layer=model.layers().get(i);var tex=textures.asset(layer);
                    float end=tex.width()/(float)tex.height();
                    draw(List.of(new Vertex(0,1,.5f,0,0),new Vertex(0,0,.5f,0,end),new Vertex(1,0,.5f,1,end),new Vertex(1,1,.5f,1,0)),tex.id(),color(key,i),1);
                }
            }else{
                glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);
                for(Face face:faces){var tex=face.texture().namespace().equals("epoch")?textures.get(face.texture()):textures.asset(face.texture());draw(face.vertices(),tex.id(),face.tinted()?key.color0():0xffffff,face.shade());}
            }
            context.resetAll();
            backend=BackendTexture.makeGL(SIZE,SIZE,false,new GLTextureInfo(GL_TEXTURE_2D,texture,GL_RGBA8));
            image=Image.borrowTextureFrom(context,backend,SurfaceOrigin.BOTTOM_LEFT,ColorType.RGBA_8888,ColorAlphaType.PREMUL,null,()->{});
            if(image==null)throw new IllegalStateException("Cannot share item texture with Skija");
            return new Icon(texture,backend,image);
        }catch(RuntimeException|Error e){if(image!=null)image.close();if(backend!=null)backend.close();glDeleteTextures(texture);throw e;}
    }
    private static int color(Key key,int layer){return switch(layer){case 0->key.color0();case 1->key.color1();default->key.color2();};}
    private void draw(List<Vertex> vertices,int texture,int color,float shade){
        glBindTexture(GL_TEXTURE_2D,texture);glUniform3f(colorLocation,((color>>16)&255)/255f*shade,((color>>8)&255)/255f*shade,(color&255)/255f*shade);
        float[] data=new float[30];int at=0;for(int index:new int[]{0,1,2,0,2,3}){Vertex v=vertices.get(index);data[at++]=v.x();data[at++]=v.y();data[at++]=v.z();data[at++]=v.u();data[at++]=v.v();}
        glBufferData(GL_ARRAY_BUFFER,data,GL_STREAM_DRAW);glDrawArrays(GL_TRIANGLES,0,6);
    }
    private List<Face> entity(ItemStack stack)throws IOException {
        String name=stack.item().id().path();List<Face> faces=new ArrayList<>();
        if(name.equals("chest")||name.equals("trapped_chest")||name.equals("ender_chest")){
            ResourceId texture=ResourceId.parse("minecraft:entity/chest/"+(name.equals("ender_chest")?"ender":name.equals("trapped_chest")?"trapped":"normal"));
            Matrix4f t=new Matrix4f().translation(0,1,1).scale(1,-1,-1).scale(1/16f);
            box(faces,texture,t,1,6,1,14,10,14,0,19,64,64,0);box(faces,texture,t,1,2,1,14,5,14,0,0,64,64,0);box(faces,texture,t,7,5,0,2,4,1,0,0,64,64,0);
        }else if(name.endsWith("_head")||name.endsWith("_skull")){
            int type=switch(name){case "wither_skeleton_skull"->1;case "zombie_head"->2;case "player_head"->3;case "creeper_head"->4;default->0;};
            var skin=textures.skull(type,stack.tag());Matrix4f t=new Matrix4f().translation(.5f,.75f,.5f).scale(-1,-1,1).scale(1/16f);
            box(faces,skin.resource(),t,-4,-4,-4,8,8,8,0,0,64,skin.texture().height(),0);
            if(type==2||type==3)box(faces,skin.resource(),t,-4,-4,-4,8,8,8,32,0,64,skin.texture().height(),.25f);
        }else throw new IOException("Unsupported built-in item renderer: "+name);
        return faces;
    }
    private static int shader(int type,String source){int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);if(glGetShaderi(shader,GL_COMPILE_STATUS)==0)throw new IllegalStateException(glGetShaderInfoLog(shader));return shader;}
    @Override public void close(){cache.values().forEach(Icon::close);cache.clear();textures.close();glDeleteProgram(program);glDeleteBuffers(vbo);glDeleteVertexArrays(vao);glDeleteRenderbuffers(depth);glDeleteFramebuffers(framebuffer);}
}
