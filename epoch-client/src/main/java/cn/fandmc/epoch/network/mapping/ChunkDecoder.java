package cn.fandmc.epoch.network.mapping;

import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.network.ProtocolException;
import cn.fandmc.epoch.network.codec.PacketReader;
import cn.fandmc.epoch.world.ChunkColumn;
import java.io.IOException;

/** Decodes legacy wire sections into protocol-independent runtime state handles. */
public final class ChunkDecoder {
    private final BlockRegistry registry;
    private final LegacyBlockMapping mapping;
    public ChunkDecoder(BlockRegistry registry, LegacyBlockMapping mapping) { this.registry = registry; this.mapping = mapping; }
    public ChunkColumn decode(int x, int z, int mask, boolean groundUp, boolean skylight, byte[] data) throws IOException {
        int count = Integer.bitCount(mask & 0xffff);
        if (data.length != count * (8192 + 2048 + (skylight ? 2048 : 0)) + (groundUp ? 256 : 0))
            throw new ProtocolException("Unexpected chunk size: " + data.length);
        PacketReader input = new PacketReader(data);
        int[][] handles = new int[16][];
        byte[][] block = new byte[16][], sky = new byte[16][];
        for (int section = 0; section < 16; section++) if ((mask & (1 << section)) != 0) {
            handles[section] = new int[4096];
            for (int i = 0; i < 4096; i++) handles[section][i] = registry.handle(mapping.decode(input.unsignedByte() | (input.unsignedByte() << 8)));
        }
        for (int section = 0; section < 16; section++) if (handles[section] != null) block[section] = input.bytes(2048);
        if (skylight) for (int section = 0; section < 16; section++) if (handles[section] != null) sky[section] = input.bytes(2048);
        ChunkColumn column = new ChunkColumn(x, z, registry);
        for (int section = 0; section < 16; section++) if (handles[section] != null) column.section(section, handles[section], block[section], sky[section]);
        if (groundUp) column.biomes(input.bytes(256));
        input.requireEnd();
        return column;
    }
}
