package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.*;

public record S3CPacketUpdateScore(String owner,int action,String objective,int value) implements Packet<ClientPlayPacketListener> {
    public static S3CPacketUpdateScore read(PacketReader in)throws IOException { String owner=in.string(40);int action=in.unsignedByte();String objective=in.string(16);return new S3CPacketUpdateScore(owner,action,objective,action==1?0:in.varInt()); }
    @Override public void write(PacketWriter out)throws IOException { out.string(owner,40).byteValue(action).string(objective,16);if(action!=1)out.varInt(value); }
    @Override public void handle(ClientPlayPacketListener listener)throws IOException { listener.onUpdateScore(this); }
}
