package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C06PacketPlayerPosLook(double x, double y, double z, float yaw, float pitch, boolean onGround)
        implements Packet<ServerPlayPacketListener> {
    public C06PacketPlayerPosLook {
        PacketFields.position(x, y, z);
        PacketFields.rotation(yaw, pitch);
    }
    public static C06PacketPlayerPosLook read(PacketReader input) throws IOException {
        return new C06PacketPlayerPosLook(input.doubleValue(), input.doubleValue(), input.doubleValue(), input.floatValue(), input.floatValue(), input.bool());
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.doubleValue(x).doubleValue(y).doubleValue(z).floatValue(yaw).floatValue(pitch).bool(onGround);
    }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onPlayerPositionAndLook(this); }
}
