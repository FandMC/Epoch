package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.JsonResources;
import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.physics.Box;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.*;

/** Runtime handles index interned flattened states; handle values are NOT protocol IDs. */
public final class BlockRegistry {
    public record Definition(BlockState state, Block block, ResourceId resource, Map<String, String> resourceProperties,
                             boolean opaque, boolean collidable, BlockLighting lighting, boolean recognized, List<Box> collision,
                             boolean blocksMovement, boolean solidForFluid,boolean normalCube,boolean providesPower) {
        public Definition { collision = List.copyOf(collision); }
    }
    private final List<Definition> definitions = new ArrayList<>();
    private final Map<BlockState, Integer> handles = new HashMap<>();
    private final Map<String, BlockState> byName = new HashMap<>();
    private final Map<ResourceId,Block> blocks=new HashMap<>();

    public static BlockRegistry load() throws IOException {
        BlockRegistry registry = new BlockRegistry();
        try {
            JsonObject data = JsonResources.bundled("/epoch/block-states.json").getAsJsonObject("states");
            List<String> identities = new ArrayList<>(data.keySet());
            identities.remove("minecraft:air");
            Collections.sort(identities);
            identities.addFirst("minecraft:air");
            for (String identity : identities) {
                JsonObject entry = data.getAsJsonObject(identity);
                BlockState state = new BlockState(ResourceId.parse(entry.get("name").getAsString()), strings(entry.getAsJsonObject("properties")));
                if (!state.toString().equals(identity)) throw new IOException("Noncanonical block identity: " + identity);
                List<Box> boxes = new ArrayList<>();
                for (var raw : entry.getAsJsonArray("collision")) {
                    var box = raw.getAsJsonArray();
                    boxes.add(new Box(box.get(0).getAsDouble(),box.get(1).getAsDouble(),box.get(2).getAsDouble(),
                            box.get(3).getAsDouble(),box.get(4).getAsDouble(),box.get(5).getAsDouble()));
                }
                Block block=registry.blocks.computeIfAbsent(state.block(),BlockTypes::create);
                Definition definition = new Definition(state, block, ResourceId.parse(entry.get("resource").getAsString()),
                        strings(entry.getAsJsonObject("resourceProperties")), entry.get("opaque").getAsBoolean(),
                        entry.get("collidable").getAsBoolean(), new BlockLighting(entry.get("emission").getAsInt(),
                        entry.get("lightOpacity").getAsInt(), entry.get("neighborBrightness").getAsBoolean(),
                        entry.get("opaqueCube").getAsBoolean(), entry.get("fullCube").getAsBoolean(), entry.get("translucent").getAsBoolean(),
                        entry.get("ambientOcclusion").getAsFloat()), entry.get("recognized").getAsBoolean(), boxes,
                        entry.get("blocksMovement").getAsBoolean(), entry.get("solidForFluid").getAsBoolean(),
                        entry.get("normalCube").getAsBoolean(),entry.get("providesPower").getAsBoolean());
                registry.handles.put(state, registry.definitions.size());
                registry.byName.put(identity, state);
                registry.definitions.add(definition);
            }
        } catch (RuntimeException e) { throw new IOException("Invalid bundled flattened block registry", e); }
        return registry;
    }
    private static Map<String, String> strings(JsonObject object) {
        Map<String, String> result = new TreeMap<>();
        object.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsString()));
        return Collections.unmodifiableMap(result);
    }
    public int size() { return definitions.size(); }
    public BlockState air() { return state(0); }
    public BlockState state(int handle) { return definitions.get(handle).state(); }
    public BlockState named(String identity) {
        BlockState state = byName.get(identity);
        if (state == null) throw new IllegalArgumentException("Unregistered block state: " + identity);
        return state;
    }
    public int handle(BlockState state) {
        Integer handle = handles.get(state);
        if (handle == null) throw new IllegalArgumentException("Unregistered state: " + state);
        return handle;
    }
    public Definition definition(BlockState state) { return definitions.get(handle(state)); }
    public Definition definition(int handle) { return definitions.get(handle); }
    public Block block(BlockState state) { return definition(state).block(); }
    public Block block(int handle) { return definition(handle).block(); }
    public Collection<Block> blocks() { return Collections.unmodifiableCollection(blocks.values()); }
    public List<Definition> definitions() { return Collections.unmodifiableList(definitions); }
}
