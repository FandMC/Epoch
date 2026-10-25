package cn.fandmc.epoch.network.packet.status;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientStatusPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S00PacketServerInfo(String json) implements Packet<ClientStatusPacketListener> {
    public static S00PacketServerInfo read(PacketReader input) throws IOException { return new S00PacketServerInfo(input.string(32767)); }
    @Override public void write(PacketWriter output) throws IOException { output.string(json, 32767); }
    @Override public void handle(ClientStatusPacketListener listener) throws IOException { listener.onServerInfo(this); }
}
