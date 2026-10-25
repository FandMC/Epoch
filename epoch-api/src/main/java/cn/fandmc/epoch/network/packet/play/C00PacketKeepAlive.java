package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C00PacketKeepAlive(int id) implements Packet<ServerPlayPacketListener> {
    public static C00PacketKeepAlive read(PacketReader input) throws IOException { return new C00PacketKeepAlive(input.varInt()); }
    @Override public void write(PacketWriter output) throws IOException { output.varInt(id); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onKeepAlive(this); }
}
