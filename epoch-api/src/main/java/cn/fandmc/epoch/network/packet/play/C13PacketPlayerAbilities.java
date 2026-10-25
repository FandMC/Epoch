package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C13PacketPlayerAbilities(int flags,float flySpeed,float walkSpeed) implements Packet<ServerPlayPacketListener> {
    public C13PacketPlayerAbilities{
        if((flags&~15)!=0||!Float.isFinite(flySpeed)||!Float.isFinite(walkSpeed)||flySpeed<0||walkSpeed<0)
            throw new IllegalArgumentException("Invalid player abilities");
    }
    public static C13PacketPlayerAbilities read(PacketReader input)throws IOException{return new C13PacketPlayerAbilities(input.unsignedByte(),input.floatValue(),input.floatValue());}
    @Override public void write(PacketWriter output)throws IOException{output.byteValue(flags).floatValue(flySpeed).floatValue(walkSpeed);}
    @Override public void handle(ServerPlayPacketListener listener)throws IOException{listener.onPlayerAbilities(this);}
}
