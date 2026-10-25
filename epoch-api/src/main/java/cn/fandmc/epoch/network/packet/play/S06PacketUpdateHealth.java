package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S06PacketUpdateHealth(float health, int food, float saturation) implements Packet<ClientPlayPacketListener> {
    public S06PacketUpdateHealth {
        if (!Float.isFinite(health) || !Float.isFinite(saturation) || food < 0 || food > 20)
            throw new IllegalArgumentException("Invalid health update");
    }
    public static S06PacketUpdateHealth read(PacketReader input) throws IOException {
        return new S06PacketUpdateHealth(input.floatValue(), input.varInt(), input.floatValue());
    }
    @Override public void write(PacketWriter output) throws IOException { output.floatValue(health).varInt(food).floatValue(saturation); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onUpdateHealth(this); }
}
