package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S2FPacketSetSlot(int windowId,int slot,SlotData item) implements Packet<ClientPlayPacketListener> {
    public static S2FPacketSetSlot read(PacketReader in)throws IOException { return new S2FPacketSetSlot(in.byteValue(),in.shortValue(),SlotData.read(in)); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(windowId).shortValue(slot);item.write(out); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onSetSlot(this); }
}
