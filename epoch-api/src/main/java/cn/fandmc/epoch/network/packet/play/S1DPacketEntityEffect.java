package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S1DPacketEntityEffect(int entityId, int effectId, int amplifier, int duration, boolean particles) implements Packet<ClientPlayPacketListener> {
    public S1DPacketEntityEffect {
        if (effectId < 0 || effectId > 255 || amplifier < 0 || amplifier > 255 || duration < 0)
            throw new IllegalArgumentException("Invalid entity effect");
    }
    public static S1DPacketEntityEffect read(PacketReader input) throws IOException {
        return new S1DPacketEntityEffect(input.varInt(), input.unsignedByte(), input.unsignedByte(), input.varInt(), input.bool());
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.varInt(entityId).byteValue(effectId).byteValue(amplifier).varInt(duration).bool(particles);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onEntityEffect(this); }
}
