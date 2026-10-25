package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S48PacketResourcePackSend(String url, String hash) implements Packet<ClientPlayPacketListener> {
    public static S48PacketResourcePackSend read(PacketReader input) throws IOException { return new S48PacketResourcePackSend(input.string(32767), input.string(40)); }
    @Override public void write(PacketWriter output) throws IOException { output.string(url, 32767).string(hash, 40); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onResourcePackSend(this); }
}
