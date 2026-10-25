package cn.fandmc.epoch.network.packet.status;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerStatusPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C01PacketPing(long payload) implements Packet<ServerStatusPacketListener> {
    public static C01PacketPing read(PacketReader input) throws IOException { return new C01PacketPing(input.longValue()); }
    @Override public void write(PacketWriter output) throws IOException { output.longValue(payload); }
    @Override public void handle(ServerStatusPacketListener listener) throws IOException { listener.onPing(this); }
}
