package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record C09PacketHeldItemChange(int slot) implements Packet<ServerPlayPacketListener> {
    public static C09PacketHeldItemChange read(PacketReader in)throws IOException { return new C09PacketHeldItemChange(in.shortValue()); }
    @Override public void write(PacketWriter out)throws IOException { out.shortValue(slot); }
    @Override public void handle(ServerPlayPacketListener listener)throws IOException { listener.onHeldItemChange(this); }
}
