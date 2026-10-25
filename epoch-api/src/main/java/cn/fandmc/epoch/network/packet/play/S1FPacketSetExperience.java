package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S1FPacketSetExperience(float progress,int level,int total) implements Packet<ClientPlayPacketListener> {
    public static S1FPacketSetExperience read(PacketReader in)throws IOException { return new S1FPacketSetExperience(in.floatValue(),in.varInt(),in.varInt()); }
    @Override public void write(PacketWriter out)throws IOException { out.floatValue(progress).varInt(level).varInt(total); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onExperience(this); }
}
