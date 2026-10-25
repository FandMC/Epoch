package cn.fandmc.epoch.render;

import cn.fandmc.epoch.asset.*;
import cn.fandmc.epoch.font.FontBook;
import cn.fandmc.epoch.nbt.NbtCompound;
import cn.fandmc.epoch.nbt.NbtTag;
import com.google.gson.JsonParser;
import io.github.humbleui.skija.*;
import org.lwjgl.system.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.stb.STBImage.*;

/** Full-size entity PNGs. All GL/Skija ownership stays on the game thread. */
public final class BlockEntityTextures implements AutoCloseable {
    public record Texture(int id,int width,int height) {}
    public record Skin(ResourceId resource,Texture texture) {}
    private final AssetDirectory assets;
    private final FontBook fonts=new FontBook();
    private final Map<ResourceId,Texture> textures=new LinkedHashMap<>(32,.75f,true);
    private final Map<String,CompletableFuture<byte[]>> downloads=new HashMap<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("epoch-skin-io").factory());
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final Path cache=Path.of("run/cache/skins");
    public BlockEntityTextures(AssetDirectory assets){this.assets=assets;}
    public Texture asset(ResourceId resource) {
        Texture texture=textures.get(resource);if(texture!=null)return texture;
        try {texture=upload(assets.read(resource.assetPath("textures",".png")),false);textures.put(resource,texture);return texture;}
        catch(IOException e){throw new IllegalStateException("Cannot load block entity texture: "+resource,e);}
    }
    public Texture get(ResourceId resource){return Objects.requireNonNull(textures.get(resource),"Unprepared texture "+resource);}
    public ResourceId sign(List<String> lines) {
        String value=String.join("\n",lines);ResourceId id=new ResourceId("epoch","sign/"+UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)));
        if(textures.containsKey(id)){textures.get(id);return id;}
        try(var surface=Surface.makeRasterN32Premul(360,160);var paint=new Paint().setColor(0xff000000).setAntiAlias(true)) {
            Canvas canvas=surface.getCanvas();canvas.clear(0);
            for(int i=0;i<Math.min(4,lines.size());i++) {
                String line=lines.get(i);if(line.length()>384)line=line.substring(0,384);
                float width=fonts.width(line,32),scale=Math.min(1,352/Math.max(1,width));
                canvas.save();canvas.translate(180,30+i*40);canvas.scale(scale,1);fonts.draw(canvas,line,-width/2,0,32,paint);canvas.restore();
            }
            try(var image=surface.makeImageSnapshot();var data=image.encodeToData()) {textures.put(id,upload(data.getBytes(),false));}
        }catch(IOException e){throw new IllegalStateException("Cannot rasterize sign",e);}
        return id;
    }
    public Skin skull(int type,NbtCompound tag) {
        String path=switch(type){case 1->"skeleton/wither_skeleton";case 2->"zombie/zombie";case 3->"steve";case 4->"creeper/creeper";default->"skeleton/skeleton";};
        ResourceId fallback=ResourceId.parse("minecraft:entity/"+path);
        if(type==3) {
            // Block-entity packets use Owner; SkullOwner is the item-stack form.
            NbtCompound owner=tag.compound("Owner");if(owner.values().isEmpty())owner=tag.compound("SkullOwner");
            String hash=skinHash(owner);
            if(hash!=null) {
                ResourceId id=new ResourceId("epoch","skin/"+hash);Texture found=textures.get(id);if(found!=null)return new Skin(id,found);
                CompletableFuture<byte[]> future=downloads.get(hash);
                if(future==null&&downloads.size()<256){future=CompletableFuture.supplyAsync(()->download(hash),worker);downloads.put(hash,future);}
                if(future!=null&&future.isDone()) {
                    byte[] bytes=future.getNow(null);
                    if(bytes!=null)try {Texture loaded=upload(bytes,true);textures.put(id,loaded);return new Skin(id,loaded);}
                    catch(IOException e){downloads.put(hash,CompletableFuture.completedFuture(null));}
                }
            }
        }
        return new Skin(fallback,asset(fallback));
    }
    private static String skinHash(NbtCompound owner) {
        for(NbtTag raw:owner.compound("Properties").list("textures"))if(raw instanceof NbtCompound property) {
            String value=property.string("Value","");if(value.length()>65536)return null;
            try {
                var json=JsonParser.parseString(new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8)).getAsJsonObject();
                URI uri=URI.create(json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString());
                if(!"textures.minecraft.net".equalsIgnoreCase(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1||uri.getQuery()!=null||uri.getFragment()!=null)return null;
                if(!"http".equals(uri.getScheme())&&!"https".equals(uri.getScheme()))return null;
                String path=uri.getPath();if(!path.matches("/texture/[0-9a-fA-F]{32,64}"))return null;
                return path.substring(9).toLowerCase(Locale.ROOT);
            }catch(RuntimeException ignored){return null;}
        }
        return null;
    }
    private byte[] download(String hash) {
        try {
            Path file=cache.resolve(hash+".png");
            if(Files.isRegularFile(file)&&Files.size(file)<=1048576)return Files.readAllBytes(file);
            var request=HttpRequest.newBuilder(URI.create("https://textures.minecraft.net/texture/"+hash)).timeout(Duration.ofSeconds(10)).GET().build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
            byte[] bytes;
            try(var input=response.body()){if(response.statusCode()!=200)return null;bytes=input.readNBytes(1048577);}
            if(bytes.length>1048576)return null;
            if(bytes.length<24||bytes[0]!=(byte)137||bytes[1]!=80||bytes[2]!=78||bytes[3]!=71)return null;
            ByteBuffer header=ByteBuffer.wrap(bytes);int width=header.getInt(16),height=header.getInt(20);
            if(width!=64||(height!=32&&height!=64))return null;
            Files.createDirectories(cache);Files.write(file,bytes);return bytes;
        }catch(InterruptedException e){Thread.currentThread().interrupt();return null;}
        catch(IOException|RuntimeException e){return null;}
    }
    private static Texture upload(byte[] bytes,boolean skin) throws IOException {
        ByteBuffer input=MemoryUtil.memAlloc(bytes.length);
        try(MemoryStack stack=MemoryStack.stackPush()) {
            input.put(bytes).flip();var w=stack.mallocInt(1);var h=stack.mallocInt(1);var channels=stack.mallocInt(1);
            if(!stbi_info_from_memory(input,w,h,channels)||w.get(0)<1||h.get(0)<1||w.get(0)>1024||h.get(0)>1024)throw new IOException("Invalid entity PNG dimensions");
            if(skin&&(w.get(0)!=64||(h.get(0)!=32&&h.get(0)!=64)))throw new IOException("Invalid skin dimensions");
            ByteBuffer pixels=stbi_load_from_memory(input,w,h,channels,4);if(pixels==null)throw new IOException("Invalid entity PNG");
            try {
                int texture=glGenTextures();glBindTexture(GL_TEXTURE_2D,texture);
                glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,w.get(0),h.get(0),0,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
                glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
                return new Texture(texture,w.get(0),h.get(0));
            }finally{stbi_image_free(pixels);}
        }finally{MemoryUtil.memFree(input);}
    }
    public void trim(){while(textures.size()>512){var iterator=textures.entrySet().iterator();glDeleteTextures(iterator.next().getValue().id());iterator.remove();}}
    public void reset(){textures.values().forEach(t->glDeleteTextures(t.id()));textures.clear();downloads.clear();}
    @Override public void close(){worker.shutdownNow();http.close();reset();fonts.close();}
}
