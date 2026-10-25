package cn.fandmc.epoch.network.packet.handshake;

import cn.fandmc.epoch.network.ProtocolState;
import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerHandshakePacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C00Handshake(int protocolVersion, String host, int port, ProtocolState nextState)
        implements Packet<ServerHandshakePacketListener> {
    public C00Handshake {
        if (host == null || host.isBlank() || host.length() > 255 || host.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Invalid handshake host");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid handshake port");
        if (nextState != ProtocolState.STATUS && nextState != ProtocolState.LOGIN)
            throw new IllegalArgumentException("Handshake can only enter Status or Login");
    }

    public static C00Handshake read(PacketReader input) throws IOException {
        int version = input.varInt();
        String host = input.string(255);
        int port = input.unsignedShort();
        ProtocolState state = switch (input.varInt()) {
            case 1 -> ProtocolState.STATUS;
            case 2 -> ProtocolState.LOGIN;
            default -> throw new IllegalArgumentException("Invalid handshake next state");
        };
        return new C00Handshake(version, host, port, state);
    }

    @Override public void write(PacketWriter output) throws IOException {
        output.varInt(protocolVersion).string(host, 255).shortValue(port).varInt(nextState == ProtocolState.STATUS ? 1 : 2);
    }
    @Override public void handle(ServerHandshakePacketListener listener) throws IOException { listener.onHandshake(this); }
}
