package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record C0EPacketClickWindow(int windowId,int slot,int button,short action,int mode,SlotData clicked) implements Packet<ServerPlayPacketListener> {
    public static C0EPacketClickWindow read(PacketReader in)throws IOException { return new C0EPacketClickWindow(in.unsignedByte(),in.shortValue(),in.byteValue(),in.shortValue(),in.byteValue(),SlotData.read(in)); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(windowId).shortValue(slot).byteValue(button).shortValue(action).byteValue(mode);clicked.write(out); }
    @Override public void handle(ServerPlayPacketListener listener)throws IOException { listener.onClickWindow(this); }
}
