package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C0BPacketEntityAction(int entityId,Action action,int parameter) implements Packet<ServerPlayPacketListener> {
    public enum Action{START_SNEAKING,STOP_SNEAKING,LEAVE_BED,START_SPRINTING,STOP_SPRINTING,HORSE_JUMP,OPEN_HORSE_INVENTORY}
    public C0BPacketEntityAction{if(action==null||parameter<0||parameter>100)throw new IllegalArgumentException("Invalid entity action");}
    public static C0BPacketEntityAction read(PacketReader input)throws IOException{
        int entity=input.varInt(),action=input.varInt(),parameter=input.varInt();
        if(action<0||action>=Action.values().length)throw new IOException("Invalid entity action ID");
        return new C0BPacketEntityAction(entity,Action.values()[action],parameter);
    }
    @Override public void write(PacketWriter output)throws IOException{output.varInt(entityId).varInt(action.ordinal()).varInt(parameter);}
    @Override public void handle(ServerPlayPacketListener listener)throws IOException{listener.onEntityAction(this);}
}
