package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S09PacketHeldItemChange(int slot) implements Packet<ClientPlayPacketListener> {
    public static S09PacketHeldItemChange read(PacketReader in)throws IOException { return new S09PacketHeldItemChange(in.unsignedByte()); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(slot); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onHeldItemChange(this); }
}
