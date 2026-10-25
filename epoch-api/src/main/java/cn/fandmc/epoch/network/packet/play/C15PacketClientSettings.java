package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C15PacketClientSettings(String locale, int viewDistance, int chatMode, boolean chatColors, int skinParts)
        implements Packet<ServerPlayPacketListener> {
    public C15PacketClientSettings {
        if (locale == null || locale.length() > 7 || viewDistance < 2 || viewDistance > 32
                || chatMode < 0 || chatMode > 2 || skinParts < 0 || skinParts > 127)
            throw new IllegalArgumentException("Invalid client settings");
    }
    public static C15PacketClientSettings read(PacketReader input) throws IOException {
        return new C15PacketClientSettings(input.string(7), input.byteValue(), input.unsignedByte(), input.bool(), input.unsignedByte());
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.string(locale, 7).byteValue(viewDistance).byteValue(chatMode).bool(chatColors).byteValue(skinParts);
    }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onClientSettings(this); }
}
