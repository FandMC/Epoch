package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S1EPacketRemoveEntityEffect(int entityId, int effectId) implements Packet<ClientPlayPacketListener> {
    public S1EPacketRemoveEntityEffect { if (effectId < 0 || effectId > 255) throw new IllegalArgumentException("Invalid effect id"); }
    public static S1EPacketRemoveEntityEffect read(PacketReader input) throws IOException {
        return new S1EPacketRemoveEntityEffect(input.varInt(), input.unsignedByte());
    }
    @Override public void write(PacketWriter output) throws IOException { output.varInt(entityId).byteValue(effectId); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onRemoveEntityEffect(this); }
}
