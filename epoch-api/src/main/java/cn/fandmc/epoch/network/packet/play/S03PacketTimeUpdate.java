package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S03PacketTimeUpdate(long worldAge,long timeOfDay) implements Packet<ClientPlayPacketListener> {
    public static S03PacketTimeUpdate read(PacketReader input)throws IOException{return new S03PacketTimeUpdate(input.longValue(),input.longValue());}
    @Override public void write(PacketWriter output)throws IOException{output.longValue(worldAge).longValue(timeOfDay);}
    @Override public void handle(ClientPlayPacketListener listener)throws IOException{listener.onTimeUpdate(this);}
}
