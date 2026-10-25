package cn.fandmc.epoch.network.mapping;

import cn.fandmc.epoch.asset.JsonResources;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.network.ProtocolException;
import java.io.IOException;
import java.util.*;

/** The only bridge between legacy (blockId << 4 | metadata) and named states. */
public final class LegacyBlockMapping {
    private final BlockState[] incoming = new BlockState[65536];
    private final Map<BlockState, List<Integer>> outgoing = new HashMap<>();

    public LegacyBlockMapping(BlockRegistry registry) throws IOException {
        try {
            var mappings = JsonResources.bundled("/epoch/network/1.8.9/blocks.json").getAsJsonObject("states");
            for (var entry : mappings.entrySet()) {
                int packed = Integer.parseInt(entry.getKey());
                BlockState state = registry.named(entry.getValue().getAsString());
                incoming[packed] = state;
                outgoing.computeIfAbsent(state, ignored -> new ArrayList<>()).add(packed);
            }
            outgoing.values().forEach(Collections::sort);
        } catch (RuntimeException e) { throw new IOException("Invalid Protocol 47 block mapping", e); }
    }

    public BlockState decode(int packed) throws ProtocolException {
        if (packed < 0 || packed >= incoming.length || incoming[packed] == null)
            throw new ProtocolException("Unsupported Protocol 47 block state: " + packed);
        return incoming[packed];
    }

    /** Canonical encoding where several legacy values describe the same semantic state. */
    public int encode(BlockState state) throws ProtocolException {
        List<Integer> values = outgoing.get(wireState(state));
        if (values == null) throw new ProtocolException("State has no Protocol 47 encoding: " + state);
        return values.getFirst();
    }

    /** Preserve an original wire alias when exact round-trip identity is required. */
    public int encode(BlockState state, int preferredWireValue) throws ProtocolException {
        return wireState(state).equals(decode(preferredWireValue)) ? preferredWireValue : encode(state);
    }

    private static BlockState wireState(BlockState state) {
        String name=state.block().path();
        if(name.equals("redstone_wire")||name.equals("repeater")) {
            var properties=new TreeMap<>(state.properties());
            if(name.equals("repeater"))properties.put("locked","false");
            else for(String direction:List.of("east","south","west","north"))properties.put(direction,"none");
            return new BlockState(state.block(),properties);
        }
        if (name.endsWith("_door")) {
            var properties=new TreeMap<>(state.properties());
            if (state.property("half","lower").equals("upper")) {
                properties.put("facing","north");properties.put("open","false");
            } else {
                properties.put("hinge","left");properties.put("powered","false");
            }
            return new BlockState(state.block(),properties);
        }
        boolean stair=name.endsWith("_stairs"),gate=name.endsWith("_fence_gate");
        boolean connected=name.endsWith("_fence")||name.endsWith("_wall")||name.endsWith("_pane")||name.equals("iron_bars");
        if (!stair&&!gate&&!connected) return state;
        var properties=new TreeMap<>(state.properties());
        if(stair)properties.put("shape","straight");
        if(gate)properties.put("in_wall","false");
        if(connected){
            for(String direction:List.of("north","east","south","west"))properties.put(direction,"false");
            if(name.endsWith("_wall"))properties.put("up","false");
        }
        return new BlockState(state.block(),properties);
    }
}
