package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C17PacketCustomPayload(String channel, byte[] data) implements Packet<ServerPlayPacketListener> {
    public C17PacketCustomPayload {
        if (channel == null || channel.length() > 20 || data.length > 32767) throw new IllegalArgumentException("Invalid custom payload");
        data = data.clone();
    }
    @Override public byte[] data() { return data.clone(); }
    public static C17PacketCustomPayload read(PacketReader input) throws IOException {
        String channel = input.string(20);
        return new C17PacketCustomPayload(channel, input.bytes(input.remaining()));
    }
    public static C17PacketCustomPayload brand(String brand) throws IOException {
        return new C17PacketCustomPayload("MC|Brand", new PacketWriter().string(brand, 32767).toByteArray());
    }
    @Override public void write(PacketWriter output) throws IOException { output.string(channel, 20).bytes(data); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onCustomPayload(this); }
}
