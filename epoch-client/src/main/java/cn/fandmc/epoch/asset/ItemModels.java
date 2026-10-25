package cn.fandmc.epoch.asset;

import cn.fandmc.epoch.asset.BlockModels.*;
import com.google.gson.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;

/** Actual item JSON inheritance, generated layers and GUI display transforms. */
public final class ItemModels {
    public record Model(List<Face> faces, List<ResourceId> layers, Matrix4f transform, boolean entity) {}
    private record Source(Map<String,String> textures,JsonArray elements,JsonObject gui,String builtin) {}
    private final AssetDirectory assets;
    private final Map<ResourceId,Source> sources=new HashMap<>();
    private final Map<ResourceId,Model> models=new HashMap<>();
    public ItemModels(AssetDirectory assets){this.assets=assets;}
    public Model get(ResourceId id)throws IOException {
        Model found=models.get(id);if(found!=null)return found;
        ResourceId path=id.path().startsWith("item/")?id:new ResourceId(id.namespace(),"item/"+id.path());
        Source source=source(path,new HashSet<>());
        List<ResourceId> layers=new ArrayList<>();
        if(source.builtin().equals("generated"))for(int i=0;i<5;i++){
            String value=source.textures().get("layer"+i);if(value==null)break;
            Set<String> seen=new HashSet<>();
            while(value.startsWith("#")){if(!seen.add(value))throw new IOException("Cyclic item texture");value=source.textures().get(value.substring(1));if(value==null)throw new IOException("Missing item texture");}
            layers.add(ResourceId.parse(value));
        }
        Matrix4f transform=new Matrix4f();
        boolean flat=!layers.isEmpty(),entity=source.builtin().equals("entity");
        // The 1.8 item renderer applies its fixed block view before JSON GUI transforms.
        if(!flat)transform.rotateX((float)Math.toRadians(30)).rotateY((float)Math.toRadians(225)).scale(.625f);
        if(source.gui()!=null){
            var gui=source.gui();Vector3f rotation=vector(gui,"rotation",0),translation=vector(gui,"translation",0),scale=vector(gui,"scale",1);
            transform.translate(translation.mul(1/16f)).rotateY((float)Math.toRadians(rotation.y))
                    .rotateX((float)Math.toRadians(rotation.x)).rotateZ((float)Math.toRadians(rotation.z)).scale(scale);
        }
        Model result=new Model(ModelGeometry.bake(source.elements(),source.textures(),0,0,false),List.copyOf(layers),transform,entity);
        models.put(id,result);return result;
    }
    private Source source(ResourceId id,Set<ResourceId> visiting)throws IOException {
        if(id.path().equals("builtin/generated")||id.path().equals("builtin/entity"))return new Source(Map.of(),new JsonArray(),null,id.path().substring(8));
        if(id.path().equals("builtin/compass")||id.path().equals("builtin/clock"))return new Source(Map.of(),new JsonArray(),null,"generated");
        Source found=sources.get(id);if(found!=null)return found;
        if(visiting.size()>32||!visiting.add(id))throw new IOException("Cyclic/deep item parent: "+id);
        String path=id.assetPath("models",".json");JsonObject json=JsonResources.object(assets.read(path),path);
        Source parent=json.has("parent")?source(ResourceId.parse(json.get("parent").getAsString()),visiting):new Source(Map.of(),new JsonArray(),null,"");
        Map<String,String> textures=new HashMap<>(parent.textures());
        if(json.has("textures"))json.getAsJsonObject("textures").entrySet().forEach(e->textures.put(e.getKey(),e.getValue().getAsString()));
        JsonObject gui=parent.gui();if(json.has("display")&&json.getAsJsonObject("display").has("gui"))gui=json.getAsJsonObject("display").getAsJsonObject("gui");
        Source result=new Source(Map.copyOf(textures),json.has("elements")?json.getAsJsonArray("elements"):parent.elements(),gui,parent.builtin());
        visiting.remove(id);sources.put(id,result);return result;
    }
    private static Vector3f vector(JsonObject object,String key,float fallback){
        if(!object.has(key))return new Vector3f(fallback);JsonArray a=object.getAsJsonArray(key);
        return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());
    }
}
