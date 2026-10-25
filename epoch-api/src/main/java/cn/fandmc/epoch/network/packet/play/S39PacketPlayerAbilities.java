package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S39PacketPlayerAbilities(int flags,float flySpeed,float walkSpeed) implements Packet<ClientPlayPacketListener> {
    public S39PacketPlayerAbilities{
        if((flags&~15)!=0||!Float.isFinite(flySpeed)||!Float.isFinite(walkSpeed)||flySpeed<0||walkSpeed<0)
            throw new IllegalArgumentException("Invalid player abilities");
    }
    public static S39PacketPlayerAbilities read(PacketReader input)throws IOException{return new S39PacketPlayerAbilities(input.unsignedByte(),input.floatValue(),input.floatValue());}
    @Override public void write(PacketWriter output)throws IOException{output.byteValue(flags).floatValue(flySpeed).floatValue(walkSpeed);}
    @Override public void handle(ClientPlayPacketListener listener)throws IOException{listener.onPlayerAbilities(this);}
}
