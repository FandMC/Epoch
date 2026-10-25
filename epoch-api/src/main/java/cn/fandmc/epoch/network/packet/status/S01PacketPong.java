package cn.fandmc.epoch.network.packet.status;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientStatusPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S01PacketPong(long payload) implements Packet<ClientStatusPacketListener> {
    public static S01PacketPong read(PacketReader input) throws IOException { return new S01PacketPong(input.longValue()); }
    @Override public void write(PacketWriter output) throws IOException { output.longValue(payload); }
    @Override public void handle(ClientStatusPacketListener listener) throws IOException { listener.onPong(this); }
}
