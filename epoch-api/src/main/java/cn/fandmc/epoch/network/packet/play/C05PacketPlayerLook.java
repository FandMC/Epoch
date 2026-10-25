package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C05PacketPlayerLook(float yaw, float pitch, boolean onGround) implements Packet<ServerPlayPacketListener> {
    public C05PacketPlayerLook { PacketFields.rotation(yaw, pitch); }
    public static C05PacketPlayerLook read(PacketReader input) throws IOException {
        return new C05PacketPlayerLook(input.floatValue(), input.floatValue(), input.bool());
    }
    @Override public void write(PacketWriter output) throws IOException { output.floatValue(yaw).floatValue(pitch).bool(onGround); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onPlayerLook(this); }
}
