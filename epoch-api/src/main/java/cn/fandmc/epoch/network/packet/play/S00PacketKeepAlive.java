package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S00PacketKeepAlive(int id) implements Packet<ClientPlayPacketListener> {
    public static S00PacketKeepAlive read(PacketReader input) throws IOException { return new S00PacketKeepAlive(input.varInt()); }
    @Override public void write(PacketWriter output) throws IOException { output.varInt(id); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onKeepAlive(this); }
}
