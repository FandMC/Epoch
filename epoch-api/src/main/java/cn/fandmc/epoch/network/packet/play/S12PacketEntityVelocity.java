package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S12PacketEntityVelocity(int entityId,short x,short y,short z) implements Packet<ClientPlayPacketListener> {
    public static S12PacketEntityVelocity read(PacketReader input)throws IOException{return new S12PacketEntityVelocity(input.varInt(),input.shortValue(),input.shortValue(),input.shortValue());}
    @Override public void write(PacketWriter output)throws IOException{output.varInt(entityId).shortValue(x).shortValue(y).shortValue(z);}
    @Override public void handle(ClientPlayPacketListener listener)throws IOException{listener.onEntityVelocity(this);}
}
