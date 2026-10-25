package cn.fandmc.epoch.network;

import cn.fandmc.epoch.network.packet.handshake.C00Handshake;
import cn.fandmc.epoch.network.packet.status.*;
import cn.fandmc.epoch.network.transport.Connection;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;

public final class StatusClient {
    public record Status(String version, int protocol, int online, int maximum, String description, long pingMillis) {}

    public Status query(String host, int port) throws IOException {
        try (Connection connection = new Connection()) {
            connection.connect(host, port, 4000);
            connection.send(new C00Handshake(ProtocolVersion.CURRENT.wireVersion(), host, port, ProtocolState.STATUS));
            connection.send(new C00PacketServerQuery());
            var response = connection.receive(PacketCatalog.STATUS_CLIENTBOUND).orElseThrow();
            if (!(response instanceof S00PacketServerInfo info)) throw new ProtocolException("Expected status response");
            long start = System.nanoTime();
            connection.send(new C01PacketPing(start));
            var reply = connection.receive(PacketCatalog.STATUS_CLIENTBOUND).orElseThrow();
            if (!(reply instanceof S01PacketPong pong) || pong.payload() != start) throw new ProtocolException("Invalid pong payload");
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            try {
                JsonObject status = JsonParser.parseString(info.json()).getAsJsonObject();
                JsonObject version = status.getAsJsonObject("version");
                JsonObject players = status.getAsJsonObject("players");
                return new Status(version.get("name").getAsString(), version.get("protocol").getAsInt(),
                        players.get("online").getAsInt(), players.get("max").getAsInt(),
                        ChatText.plain(status.get("description").toString()), elapsed);
            } catch (RuntimeException e) { throw new ProtocolException("Malformed status JSON", e); }
        }
    }
}
