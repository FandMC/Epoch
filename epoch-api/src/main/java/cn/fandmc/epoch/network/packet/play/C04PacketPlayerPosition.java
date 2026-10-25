package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C04PacketPlayerPosition(double x, double y, double z, boolean onGround) implements Packet<ServerPlayPacketListener> {
    public C04PacketPlayerPosition { PacketFields.position(x, y, z); }
    public static C04PacketPlayerPosition read(PacketReader input) throws IOException {
        return new C04PacketPlayerPosition(input.doubleValue(), input.doubleValue(), input.doubleValue(), input.bool());
    }
    @Override public void write(PacketWriter output) throws IOException { output.doubleValue(x).doubleValue(y).doubleValue(z).bool(onGround); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onPlayerPosition(this); }
}
