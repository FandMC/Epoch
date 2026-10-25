package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record S26PacketMapChunkBulk(boolean skylight, List<Column> columns) implements Packet<ClientPlayPacketListener> {
    public record Column(int x, int z, int sectionMask, byte[] data) {
        public Column {
            if ((sectionMask & ~65535) != 0) throw new IllegalArgumentException("Invalid section mask");
            data = data.clone();
        }
        @Override public byte[] data() { return data.clone(); }
    }
    public S26PacketMapChunkBulk {
        if (columns.size() > 1024) throw new IllegalArgumentException("Too many bulk chunks");
        columns = List.copyOf(columns);
        for (Column column : columns)
            if (column.data.length != byteSize(column.sectionMask(), skylight)) throw new IllegalArgumentException("Bulk chunk data size mismatch");
    }
    private static int byteSize(int mask, boolean skylight) {
        return Integer.bitCount(mask) * (10240 + (skylight ? 2048 : 0)) + 256;
    }
    public static S26PacketMapChunkBulk read(PacketReader input) throws IOException {
        boolean skylight = input.bool();
        int count = input.varInt();
        if (count < 0 || count > 1024 || count > input.remaining() / 10) throw new IllegalArgumentException("Invalid bulk chunk count");
        int[] xs = new int[count], zs = new int[count], masks = new int[count];
        for (int i = 0; i < count; i++) {
            xs[i] = input.intValue(); zs[i] = input.intValue(); masks[i] = input.unsignedShort();
        }
        List<Column> columns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) columns.add(new Column(xs[i], zs[i], masks[i], input.bytes(byteSize(masks[i], skylight))));
        return new S26PacketMapChunkBulk(skylight, columns);
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.bool(skylight).varInt(columns.size());
        for (Column column : columns) output.intValue(column.x()).intValue(column.z()).shortValue(column.sectionMask());
        for (Column column : columns) output.bytes(column.data);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onMapChunkBulk(this); }
}
