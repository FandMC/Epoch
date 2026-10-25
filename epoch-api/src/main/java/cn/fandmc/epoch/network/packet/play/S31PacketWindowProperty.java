package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S31PacketWindowProperty(int windowId,int property,int value) implements Packet<ClientPlayPacketListener> {
    public static S31PacketWindowProperty read(PacketReader in)throws IOException { return new S31PacketWindowProperty(in.unsignedByte(),in.shortValue(),in.shortValue()); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(windowId).shortValue(property).shortValue(value); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onWindowProperty(this); }
}
