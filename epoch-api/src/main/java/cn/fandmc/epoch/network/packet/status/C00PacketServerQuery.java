package cn.fandmc.epoch.network.packet.status;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerStatusPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C00PacketServerQuery() implements Packet<ServerStatusPacketListener> {
    public static C00PacketServerQuery read(PacketReader input) { return new C00PacketServerQuery(); }
    // Status Request 在 Protocol 47 中没有负载。
    @Override public void write(PacketWriter output) {}
    @Override public void handle(ServerStatusPacketListener listener) throws IOException { listener.onQuery(this); }
}
