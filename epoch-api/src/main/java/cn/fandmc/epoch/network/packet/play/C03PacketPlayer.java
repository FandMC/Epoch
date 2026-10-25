package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C03PacketPlayer(boolean onGround) implements Packet<ServerPlayPacketListener> {
    public static C03PacketPlayer read(PacketReader input) throws IOException { return new C03PacketPlayer(input.bool()); }
    @Override public void write(PacketWriter output) throws IOException { output.bool(onGround); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onPlayer(this); }
}
