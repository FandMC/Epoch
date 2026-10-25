package cn.fandmc.epoch.render;

import cn.fandmc.epoch.asset.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;

/** Nearest-filtered block atlas, including frame sequences from .png.mcmeta. */
public final class TextureAtlas implements AutoCloseable {
    public record Region(float u0,float v0,float u1,float v1) {}
    private record Animated(int x,int y,byte[] pixels,int width,TextureAnimation metadata) {}
    private static final int SIZE=1024, TILE=16, CELL=18;
    private final Map<ResourceId,Region> regions=new HashMap<>();
    private final Set<ResourceId> translucent=new HashSet<>();
    private final List<Animated> animations=new ArrayList<>();
    private final int texture;
    private long lastTick=-1;

    public TextureAtlas(AssetDirectory assets,Set<ResourceId> resources) throws IOException {
        texture=glGenTextures();
        glBindTexture(GL_TEXTURE_2D,texture);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,SIZE,SIZE,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        try {
            int slot=0;
            for(ResourceId id:resources){
                int x=(slot%(SIZE/CELL))*CELL+1,y=(slot/(SIZE/CELL))*CELL+1;
                if(y+TILE>=SIZE)throw new IOException("Block texture atlas capacity exceeded");
                String path=id.assetPath("textures",".png");
                byte[] encoded=assets.read(path);
                ByteBuffer input=MemoryUtil.memAlloc(encoded.length);
                try(MemoryStack stack=MemoryStack.stackPush()){
                    input.put(encoded).flip();
                    var w=stack.mallocInt(1);var h=stack.mallocInt(1);var channels=stack.mallocInt(1);
                    ByteBuffer pixels=stbi_load_from_memory(input,w,h,channels,4);
                    if(pixels==null)throw new IOException("Invalid PNG: "+path+": "+stbi_failure_reason());
                    try{
                        int width=w.get(0),height=h.get(0);
                        if(width<1||height<width||height%width!=0)throw new IOException("Unsupported texture dimensions: "+path);
                        byte[] all=new byte[pixels.remaining()];
                        // STB frees memAddress(pixels), which includes the buffer position.
                        // An absolute read leaves the owning buffer at its allocation base.
                        pixels.get(pixels.position(),all);
                        for(int index=3;index<all.length;index+=4){
                            int alpha=all[index]&255;
                            if(alpha>0&&alpha<255){translucent.add(id);break;}
                        }
                        byte[] meta=assets.find(path+".mcmeta").orElse(null);
                        TextureAnimation animation=meta==null?new TextureAnimation(List.of(new TextureAnimation.Frame(0,1)),false):TextureAnimation.parse(meta,height/width,path+".mcmeta");
                        upload(x,y,all,width,animation.frame(0));
                        if(animation.frames().size()>1)animations.add(new Animated(x,y,all,width,animation));
                    }finally{stbi_image_free(pixels);}
                }finally{MemoryUtil.memFree(input);}
                regions.put(id,new Region(x/(float)SIZE,y/(float)SIZE,(x+TILE)/(float)SIZE,(y+TILE)/(float)SIZE));
                slot++;
            }
        }catch(IOException|RuntimeException e){glDeleteTextures(texture);throw e;}
    }
    private void upload(int x,int y,byte[] pixels,int width,int frame){
        ByteBuffer tile=MemoryUtil.memAlloc(CELL*CELL*4);
        try{
            for(int row=-1;row<=TILE;row++)for(int col=-1;col<=TILE;col++){
                int sx=Math.clamp(col,0,TILE-1)*width/TILE,sy=Math.clamp(row,0,TILE-1)*width/TILE;
                int index=((frame*width+sy)*width+sx)*4;
                tile.put(pixels,index,4);
            }
            tile.flip();glTexSubImage2D(GL_TEXTURE_2D,0,x-1,y-1,CELL,CELL,GL_RGBA,GL_UNSIGNED_BYTE,tile);
        }finally{MemoryUtil.memFree(tile);}
    }
    public void bind(long tick){
        glActiveTexture(GL_TEXTURE0);glBindSampler(0,0);glBindTexture(GL_TEXTURE_2D,texture);
        if(tick!=lastTick){for(Animated a:animations)if(lastTick<0||a.metadata().frame(tick)!=a.metadata().frame(lastTick))upload(a.x(),a.y(),a.pixels(),a.width(),a.metadata().frame(tick));lastTick=tick;}
    }
    public Region region(ResourceId id){return Objects.requireNonNull(regions.get(id),"Texture was not loaded: "+id);}
    public boolean translucent(ResourceId id){return translucent.contains(id);}
    public int size(){return regions.size();}
    @Override public void close(){glDeleteTextures(texture);}
}
