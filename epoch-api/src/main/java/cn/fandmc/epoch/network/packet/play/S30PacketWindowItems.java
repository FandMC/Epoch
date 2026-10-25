package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S30PacketWindowItems(int windowId,List<SlotData> items) implements Packet<ClientPlayPacketListener> {
    public S30PacketWindowItems {items=List.copyOf(items);}
    public static S30PacketWindowItems read(PacketReader in)throws IOException { int id=in.unsignedByte(),count=in.shortValue();if(count<0||count>1024)throw new IOException("Invalid window size");List<SlotData> items=new ArrayList<>(count);for(int i=0;i<count;i++)items.add(SlotData.read(in));return new S30PacketWindowItems(id,List.copyOf(items)); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(windowId).shortValue(items.size());for(var item:items)item.write(out); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onWindowItems(this); }
}
