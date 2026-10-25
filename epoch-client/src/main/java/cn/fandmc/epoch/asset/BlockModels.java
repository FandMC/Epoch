package cn.fandmc.epoch.asset;

import cn.fandmc.epoch.block.entity.BlockEntity;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.block.BlockState;
import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** Parses actual 1.8 blockstates, inherited model JSON, texture variables and element rotations. */
public final class BlockModels {
    public record Vertex(float x, float y, float z, float u, float v) {}
    public record Face(List<Vertex> vertices, ResourceId texture, String cullFace, String normal, boolean tinted, float shade) {}
    public record Model(List<Face> faces, int weight, boolean ambientOcclusion) {}
    private record Source(Map<String, String> textures, JsonArray elements, boolean ambientOcclusion) {}
    private final AssetDirectory assets;
    private final BlockRegistry registry;
    private final Map<ResourceId, Source> sources = new HashMap<>();
    private final Map<BlockState, List<Model>> cache = new HashMap<>();
    private final Set<String> unresolved = new TreeSet<>();

    public BlockModels(AssetDirectory assets, BlockRegistry registry) { this.assets = assets; this.registry = registry; }
    public int unresolvedCount() { return unresolved.size(); }
    public Set<String> unresolved() { return Collections.unmodifiableSet(unresolved); }

    public List<Model> models(BlockState state) {
        return cache.computeIfAbsent(state, value -> {
            if (value.isAir()) return List.of();
            if(registry.block(value) instanceof BlockEntity)return List.of();
            try { return load(value); }
            catch (IOException | RuntimeException e) {
                if (unresolved.add(value + ": " + e.getMessage()))
                    System.getLogger(BlockModels.class.getName()).log(System.Logger.Level.DEBUG, "Unresolved model: " + value, e);
                return List.of();
            }
        });
    }

    private List<Model> load(BlockState state) throws IOException {
        var definition = registry.definition(state);
        if (!definition.recognized()) throw new IOException("Unmapped legacy state");
        String path = definition.resource().assetPath("blockstates", ".json");
        JsonObject variants = JsonResources.object(assets.read(path), path).getAsJsonObject("variants");
        if (variants == null) throw new IOException("Missing variants");
        JsonElement selected = null;
        for (var entry : variants.entrySet()) {
            if (entry.getKey().equals("normal")) { if (selected == null) selected = entry.getValue(); continue; }
            if (!entry.getKey().contains("=")) {
                if (entry.getKey().equals(definition.resourceProperties().get("variant"))) selected = entry.getValue();
                continue;
            }
            boolean matches = true;
            for (String property : entry.getKey().split(",")) {
                String[] parts = property.split("=", 2);
                if (parts.length != 2 || !parts[1].equals(definition.resourceProperties().get(parts[0]))) { matches = false; break; }
            }
            if (matches) { selected = entry.getValue(); break; }
        }
        if (selected == null) throw new IOException("No resource variant for " + definition.resourceProperties());
        JsonArray choices = selected.isJsonArray() ? selected.getAsJsonArray() : new JsonArray();
        if (!selected.isJsonArray()) choices.add(selected);
        List<Model> result = new ArrayList<>();
        for (JsonElement choice : choices) {
            JsonObject variant = choice.getAsJsonObject();
            ResourceId id = ResourceId.parse(variant.get("model").getAsString());
            if (!id.path().startsWith("block/")) id = new ResourceId(id.namespace(), "block/" + id.path());
            Source source = source(id, new HashSet<>());
            int rx = integer(variant, "x", 0), ry = integer(variant, "y", 0), weight = integer(variant, "weight", 1);
            if (rx % 90 != 0 || ry % 90 != 0 || weight < 1) throw new IOException("Invalid variant transform");
            boolean uvlock = variant.has("uvlock") && variant.get("uvlock").getAsBoolean();
            List<Face> faces = ModelGeometry.bake(source.elements(), source.textures(), rx, ry, uvlock);
            result.add(new Model(List.copyOf(faces), weight, source.ambientOcclusion()));
        }
        return List.copyOf(result);
    }

    private Source source(ResourceId id, Set<ResourceId> visiting) throws IOException {
        Source cached = sources.get(id);
        if (cached != null) return cached;
        if (visiting.size() > 32 || !visiting.add(id)) throw new IOException("Cyclic/deep model parent: " + id);
        String path = id.assetPath("models", ".json");
        JsonObject json = JsonResources.object(assets.read(path), path);
        Map<String,String> textures = new HashMap<>();
        JsonArray elements = new JsonArray();
        boolean ambientOcclusion = true;
        if (json.has("parent")) {
            Source parent = source(ResourceId.parse(json.get("parent").getAsString()), visiting);
            textures.putAll(parent.textures()); elements = parent.elements();
            ambientOcclusion = parent.ambientOcclusion();
        }
        if (json.has("textures")) json.getAsJsonObject("textures").entrySet().forEach(e -> textures.put(e.getKey(), e.getValue().getAsString()));
        if (json.has("elements")) elements = json.getAsJsonArray("elements");
        if (json.has("ambientocclusion")) ambientOcclusion = json.get("ambientocclusion").getAsBoolean();
        Source result = new Source(Map.copyOf(textures), elements, ambientOcclusion);
        visiting.remove(id); sources.put(id, result); return result;
    }
    private static int integer(JsonObject object, String key, int fallback) { return object.has(key) ? object.get(key).getAsInt() : fallback; }
}
