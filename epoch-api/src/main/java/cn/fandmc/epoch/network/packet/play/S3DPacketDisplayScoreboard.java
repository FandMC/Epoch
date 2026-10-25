package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S3DPacketDisplayScoreboard(int position,String objective) implements Packet<ClientPlayPacketListener> {
    public static S3DPacketDisplayScoreboard read(PacketReader in)throws IOException { return new S3DPacketDisplayScoreboard(in.unsignedByte(),in.string(16)); }
    @Override public void write(PacketWriter out)throws IOException { out.byteValue(position).string(objective,16); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onDisplayScoreboard(this); }
}
