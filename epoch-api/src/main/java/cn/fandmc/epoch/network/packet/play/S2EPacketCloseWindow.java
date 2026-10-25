package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S2EPacketCloseWindow(int windowId) implements Packet<ClientPlayPacketListener> {
    public static S2EPacketCloseWindow read(PacketReader in)throws IOException { return new S2EPacketCloseWindow(in.unsignedByte()); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(windowId); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onCloseWindow(this); }
}
