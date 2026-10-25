package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record S22PacketMultiBlockChange(int chunkX, int chunkZ, List<Change> changes) implements Packet<ClientPlayPacketListener> {
    public record Change(int x, int y, int z, int blockState) {
        public Change {
            if ((x & ~15) != 0 || (z & ~15) != 0 || (y & ~255) != 0 || (blockState & ~65535) != 0)
                throw new IllegalArgumentException("Invalid block change");
        }
    }
    public S22PacketMultiBlockChange {
        if (changes.size() > 65536) throw new IllegalArgumentException("Too many block changes");
        changes = List.copyOf(changes);
    }
    public static S22PacketMultiBlockChange read(PacketReader input) throws IOException {
        int x = input.intValue(), z = input.intValue(), count = input.varInt();
        if (count < 0 || count > 65536 || count > input.remaining() / 3) throw new IllegalArgumentException("Invalid block change count");
        List<Change> changes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int horizontal = input.unsignedByte();
            changes.add(new Change(horizontal >> 4, input.unsignedByte(), horizontal & 15, input.varInt()));
        }
        return new S22PacketMultiBlockChange(x, z, changes);
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.intValue(chunkX).intValue(chunkZ).varInt(changes.size());
        for (Change change : changes) output.byteValue((change.x() << 4) | change.z()).byteValue(change.y()).varInt(change.blockState());
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onMultiBlockChange(this); }
}
