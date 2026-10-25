package cn.fandmc.epoch.network.packet.login;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientLoginPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S00PacketDisconnect(String reasonJson) implements Packet<ClientLoginPacketListener> {
    public static S00PacketDisconnect read(PacketReader input) throws IOException { return new S00PacketDisconnect(input.string(32767)); }
    @Override public void write(PacketWriter output) throws IOException { output.string(reasonJson, 32767); }
    @Override public void handle(ClientLoginPacketListener listener) throws IOException { listener.onDisconnect(this); }
}
