package cn.fandmc.epoch.asset;

import cn.fandmc.epoch.asset.BlockModels.*;
import com.google.gson.*;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;

/** Shared JSON element geometry for world blocks and inventory models. */
public final class ModelGeometry {
    private ModelGeometry() {}
    public static List<Face> bake(JsonArray elements, Map<String,String> textures, int rx, int ry, boolean uvlock) throws IOException {
        List<Face> faces = new ArrayList<>();
        for (JsonElement raw : elements) {
            JsonObject element = raw.getAsJsonObject();
            float[] from = vector(element.getAsJsonArray("from")), to = vector(element.getAsJsonArray("to"));
            JsonObject rotation = element.has("rotation") ? element.getAsJsonObject("rotation") : null;
            for (var entry : element.getAsJsonObject("faces").entrySet()) {
                String direction = entry.getKey();
                JsonObject face = entry.getValue().getAsJsonObject();
                ResourceId texture = texture(textures, face.get("texture").getAsString());
                float[][] corners = corners(direction, from, to);
                float[] uv = face.has("uv") ? vector4(face.getAsJsonArray("uv")) : defaultUv(direction, from, to);
                int turn = integer(face, "rotation", 0);
                if (turn % 90 != 0) throw new IOException("Invalid face UV rotation");
                float[][] tex = {{uv[0],uv[1]}, {uv[0],uv[3]}, {uv[2],uv[3]}, {uv[2],uv[1]}};
                List<Vertex> vertices = new ArrayList<>();
                String cull = face.has("cullface") ? rotatedDirection(face.get("cullface").getAsString(), rx, ry) : null;
                for (int i = 0; i < 4; i++) {
                    Vector3f point = new Vector3f(corners[i]);
                    if (rotation != null) rotateElement(point, rotation);
                    rotateVariant(point.sub(8,8,8), rx, ry).add(8,8,8);
                    int index = Math.floorMod(i + turn / 90, 4);
                    float u = tex[index][0], v = tex[index][1];
                    // Lock top/bottom UVs against a horizontal blockstate rotation.
                    if (uvlock && rx == 0 && (direction.equals("up") || direction.equals("down"))) {
                        float angle = (float)Math.toRadians(direction.equals("up") ? -ry : ry);
                        float du=u-8, dv=v-8;
                        u=8+du*(float)Math.cos(angle)-dv*(float)Math.sin(angle);
                        v=8+du*(float)Math.sin(angle)+dv*(float)Math.cos(angle);
                    }
                    vertices.add(new Vertex(point.x/16, point.y/16, point.z/16, u/16, v/16));
                }
                String normal = rotatedDirection(direction, rx, ry);
                float shade = switch(normal) { case "down" -> .5f; case "up" -> 1f; case "east", "west" -> .6f; default -> .8f; };
                if (element.has("shade") && !element.get("shade").getAsBoolean()) shade = 1;
                faces.add(new Face(List.copyOf(vertices), texture, cull, normal, face.has("tintindex"), shade));
            }
        }
        return List.copyOf(faces);
    }
    private static ResourceId texture(Map<String,String> textures, String value) throws IOException {
        Set<String> seen = new HashSet<>();
        while (value.startsWith("#")) {
            if (!seen.add(value)) throw new IOException("Cyclic texture variable");
            value = textures.get(value.substring(1));
            if (value == null) throw new IOException("Unresolved texture variable");
        }
        return ResourceId.parse(value);
    }
    private static int integer(JsonObject object, String key, int fallback) { return object.has(key) ? object.get(key).getAsInt() : fallback; }
    private static float[] vector(JsonArray a) { return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()}; }
    private static float[] vector4(JsonArray a) { return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat()}; }
    private static Vector3f rotateVariant(Vector3f point, int x, int y) { return point.rotateX((float)Math.toRadians(-x)).rotateY((float)Math.toRadians(-y)); }
    private static void rotateElement(Vector3f point, JsonObject rotation) {
        float[] o=vector(rotation.getAsJsonArray("origin"));
        float angle=(float)Math.toRadians(rotation.get("angle").getAsFloat());
        String axis=rotation.get("axis").getAsString();
        point.sub(o[0],o[1],o[2]);
        if (rotation.has("rescale") && rotation.get("rescale").getAsBoolean()) {
            float scale=1/(float)Math.cos(angle);
            point.mul(axis.equals("x")?1:scale,axis.equals("y")?1:scale,axis.equals("z")?1:scale);
        }
        switch(axis) { case "x" -> point.rotateX(angle); case "y" -> point.rotateY(angle); case "z" -> point.rotateZ(angle); default -> throw new IllegalArgumentException("Invalid rotation axis"); }
        point.add(o[0],o[1],o[2]);
    }
    private static String rotatedDirection(String direction,int x,int y) {
        Vector3f n=switch(direction){case "up"->new Vector3f(0,1,0);case "down"->new Vector3f(0,-1,0);case "north"->new Vector3f(0,0,-1);case "south"->new Vector3f(0,0,1);case "east"->new Vector3f(1,0,0);case "west"->new Vector3f(-1,0,0);default->throw new IllegalArgumentException("Invalid face");};
        rotateVariant(n,x,y);
        if(n.y>.5)return "up";if(n.y<-.5)return "down";if(n.x>.5)return "east";if(n.x<-.5)return "west";return n.z>.5?"south":"north";
    }
    private static float[] defaultUv(String d,float[] a,float[] b) {
        return switch(d){case "down"->new float[]{a[0],16-b[2],b[0],16-a[2]};case "up"->new float[]{a[0],a[2],b[0],b[2]};case "north"->new float[]{16-b[0],16-b[1],16-a[0],16-a[1]};case "south"->new float[]{a[0],16-b[1],b[0],16-a[1]};case "west"->new float[]{a[2],16-b[1],b[2],16-a[1]};default->new float[]{16-b[2],16-b[1],16-a[2],16-a[1]};};
    }
    private static float[][] corners(String d,float[] a,float[] b) {
        float x=a[0],y=a[1],z=a[2],X=b[0],Y=b[1],Z=b[2];
        return switch(d){case "north"->new float[][]{{X,Y,z},{X,y,z},{x,y,z},{x,Y,z}};case "south"->new float[][]{{x,Y,Z},{x,y,Z},{X,y,Z},{X,Y,Z}};case "west"->new float[][]{{x,Y,z},{x,y,z},{x,y,Z},{x,Y,Z}};case "east"->new float[][]{{X,Y,Z},{X,y,Z},{X,y,z},{X,Y,z}};case "up"->new float[][]{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}};case "down"->new float[][]{{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}};default->throw new IllegalArgumentException("Invalid face: "+d);};
    }
}
