package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C01PacketChatMessage(String message) implements Packet<ServerPlayPacketListener> {
    public C01PacketChatMessage {
        if (message == null || message.length() > 100) throw new IllegalArgumentException("Chat exceeds 1.8 character limit");
    }
    public static C01PacketChatMessage read(PacketReader input) throws IOException { return new C01PacketChatMessage(input.string(100)); }
    @Override public void write(PacketWriter output) throws IOException { output.string(message, 100); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onChat(this); }
}
