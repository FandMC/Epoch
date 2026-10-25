package cn.fandmc.epoch.network.packet.login;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientLoginPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.UUID;
import java.util.Objects;

public record S02PacketLoginSuccess(UUID uuid, String username) implements Packet<ClientLoginPacketListener> {
    public S02PacketLoginSuccess {
        Objects.requireNonNull(uuid);
        if (username == null || username.isEmpty() || username.length() > 16) throw new IllegalArgumentException("Invalid login username");
    }
    public static S02PacketLoginSuccess read(PacketReader input) throws IOException {
        String uuid = input.string(36);
        if (!uuid.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new IllegalArgumentException("Invalid login UUID");
        return new S02PacketLoginSuccess(UUID.fromString(uuid), input.string(16));
    }
    @Override public void write(PacketWriter output) throws IOException { output.string(uuid.toString(), 36).string(username, 16); }
    @Override public void handle(ClientLoginPacketListener listener) throws IOException { listener.onLoginSuccess(this); }
}
