package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record C0DPacketCloseWindow(int windowId) implements Packet<ServerPlayPacketListener> {
    public static C0DPacketCloseWindow read(PacketReader in)throws IOException { return new C0DPacketCloseWindow(in.unsignedByte()); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(windowId); }
    @Override public void handle(ServerPlayPacketListener listener)throws IOException { listener.onCloseWindow(this); }
}
