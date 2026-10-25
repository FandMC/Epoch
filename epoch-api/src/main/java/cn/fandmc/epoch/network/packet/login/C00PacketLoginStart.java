package cn.fandmc.epoch.network.packet.login;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerLoginPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C00PacketLoginStart(String username) implements Packet<ServerLoginPacketListener> {
    public C00PacketLoginStart {
        if (username == null || !username.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Invalid username");
    }
    public static C00PacketLoginStart read(PacketReader input) throws IOException { return new C00PacketLoginStart(input.string(16)); }
    @Override public void write(PacketWriter output) throws IOException { output.string(username, 16); }
    @Override public void handle(ServerLoginPacketListener listener) throws IOException { listener.onLoginStart(this); }
}
