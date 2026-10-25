package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S3BPacketScoreboardObjective(String name,int mode,String title,String type) implements Packet<ClientPlayPacketListener> {
    public static S3BPacketScoreboardObjective read(PacketReader in)throws IOException { String name=in.string(16);int mode=in.unsignedByte();if(mode>2)throw new IOException("Invalid objective mode");return new S3BPacketScoreboardObjective(name,mode,mode==1?"":in.string(32),mode==1?"":in.string(16)); }
    @Override public void write(PacketWriter out)throws IOException { out.string(name,16).byteValue(mode);if(mode!=1)out.string(title,32).string(type,16); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onScoreboardObjective(this); }
}
