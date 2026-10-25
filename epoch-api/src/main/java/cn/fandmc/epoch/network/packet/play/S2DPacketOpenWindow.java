package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S2DPacketOpenWindow(int windowId,String type,String title,int slots,int entityId) implements Packet<ClientPlayPacketListener> {
    public static S2DPacketOpenWindow read(PacketReader in)throws IOException { int id=in.unsignedByte();String type=in.string(32),title=in.string(32767);int slots=in.unsignedByte();return new S2DPacketOpenWindow(id,type,title,slots,type.equals("EntityHorse")?in.intValue():0); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(windowId).string(type,32).string(title,32767).byteValue(slots);if(type.equals("EntityHorse"))out.intValue(entityId); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onOpenWindow(this); }
}
