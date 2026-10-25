package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S40PacketDisconnect(String reasonJson) implements Packet<ClientPlayPacketListener> {
    public static S40PacketDisconnect read(PacketReader input) throws IOException { return new S40PacketDisconnect(input.string(32767)); }
    @Override public void write(PacketWriter output) throws IOException { output.string(reasonJson, 32767); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onDisconnect(this); }
}
