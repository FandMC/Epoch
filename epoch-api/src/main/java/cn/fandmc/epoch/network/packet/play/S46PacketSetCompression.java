package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S46PacketSetCompression(int threshold) implements Packet<ClientPlayPacketListener> {
    public S46PacketSetCompression { if (threshold < -1) throw new IllegalArgumentException("Invalid compression threshold"); }
    public static S46PacketSetCompression read(PacketReader input) throws IOException { return new S46PacketSetCompression(input.varInt()); }
    @Override public void write(PacketWriter output) throws IOException { output.varInt(threshold); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onSetCompression(this); }
}
