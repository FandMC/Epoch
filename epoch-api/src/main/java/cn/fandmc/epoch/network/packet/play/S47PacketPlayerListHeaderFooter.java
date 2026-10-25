package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S47PacketPlayerListHeaderFooter(String header,String footer) implements Packet<ClientPlayPacketListener> {
    public static S47PacketPlayerListHeaderFooter read(PacketReader in)throws IOException { return new S47PacketPlayerListHeaderFooter(in.string(32767),in.string(32767)); }
    @Override public void write(PacketWriter out)throws IOException { out.string(header,32767).string(footer,32767); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onPlayerListHeaderFooter(this); }
}
