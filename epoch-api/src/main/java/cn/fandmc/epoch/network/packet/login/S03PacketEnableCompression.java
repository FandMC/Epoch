package cn.fandmc.epoch.network.packet.login;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientLoginPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S03PacketEnableCompression(int threshold) implements Packet<ClientLoginPacketListener> {
    public S03PacketEnableCompression { if (threshold < -1) throw new IllegalArgumentException("Invalid compression threshold"); }
    public static S03PacketEnableCompression read(PacketReader input) throws IOException { return new S03PacketEnableCompression(input.varInt()); }
    @Override public void write(PacketWriter output) throws IOException { output.varInt(threshold); }
    @Override public void handle(ClientLoginPacketListener listener) throws IOException { listener.onEnableCompression(this); }
}
