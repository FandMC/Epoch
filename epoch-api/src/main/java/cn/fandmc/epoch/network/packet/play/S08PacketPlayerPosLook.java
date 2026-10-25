package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S08PacketPlayerPosLook(double x, double y, double z, float yaw, float pitch, int flags)
        implements Packet<ClientPlayPacketListener> {
    public static final int RELATIVE_X = 1, RELATIVE_Y = 2, RELATIVE_Z = 4, RELATIVE_YAW = 8, RELATIVE_PITCH = 16;
    public S08PacketPlayerPosLook {
        PacketFields.position(x, y, z);
        PacketFields.rotation(yaw, pitch);
        if ((flags & ~31) != 0) throw new IllegalArgumentException("Invalid relative position flags");
    }
    public static S08PacketPlayerPosLook read(PacketReader input) throws IOException {
        return new S08PacketPlayerPosLook(input.doubleValue(), input.doubleValue(), input.doubleValue(),
                input.floatValue(), input.floatValue(), input.unsignedByte());
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.doubleValue(x).doubleValue(y).doubleValue(z).floatValue(yaw).floatValue(pitch).byteValue(flags);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onPlayerPosition(this); }
}
