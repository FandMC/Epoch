package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S02PacketChat(String json, int position) implements Packet<ClientPlayPacketListener> {
    public S02PacketChat {
        if (json == null || json.length() > 32767 || position < 0 || position > 2) throw new IllegalArgumentException("Invalid chat packet");
    }
    public static S02PacketChat read(PacketReader input) throws IOException { return new S02PacketChat(input.string(32767), input.unsignedByte()); }
    @Override public void write(PacketWriter output) throws IOException { output.string(json, 32767).byteValue(position); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onChat(this); }
}
