package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S3FPacketCustomPayload(String channel, byte[] data) implements Packet<ClientPlayPacketListener> {
    public S3FPacketCustomPayload {
        if (channel == null || channel.length() > 20 || data.length > 1048576) throw new IllegalArgumentException("Invalid custom payload");
        data = data.clone();
    }
    @Override public byte[] data() { return data.clone(); }
    public static S3FPacketCustomPayload read(PacketReader input) throws IOException {
        String channel = input.string(20);
        return new S3FPacketCustomPayload(channel, input.bytes(input.remaining()));
    }
    @Override public void write(PacketWriter output) throws IOException { output.string(channel, 20).bytes(data); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onCustomPayload(this); }
}
