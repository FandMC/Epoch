package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S23PacketBlockChange(int x, int y, int z, int blockState) implements Packet<ClientPlayPacketListener> {
    public S23PacketBlockChange {
        if (x < -33554432 || x > 33554431 || z < -33554432 || z > 33554431 || (y & ~255) != 0 || (blockState & ~65535) != 0)
            throw new IllegalArgumentException("Invalid block change position/state");
    }
    public static S23PacketBlockChange read(PacketReader input) throws IOException {
        long position = input.longValue();
        return new S23PacketBlockChange((int) (position >> 38), (int) ((position >> 26) & 0xfff), (int) (position << 38 >> 38), input.varInt());
    }
    @Override public void write(PacketWriter output) throws IOException {
        long position = ((long) x & 0x3ffffffL) << 38 | ((long) y & 0xfffL) << 26 | ((long) z & 0x3ffffffL);
        output.longValue(position).varInt(blockState);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onBlockChange(this); }
}
