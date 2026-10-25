package cn.fandmc.epoch.network.handler;

import cn.fandmc.epoch.network.*;
import cn.fandmc.epoch.network.listener.ClientLoginPacketListener;
import cn.fandmc.epoch.network.packet.login.*;
import java.io.IOException;

public final class LoginPacketHandler implements ClientLoginPacketListener {
    private final GameSession session;

    public LoginPacketHandler(GameSession session) { this.session = session; }

    @Override public void onDisconnect(S00PacketDisconnect packet) throws IOException {
        throw new ProtocolException("Login rejected: " + ChatText.plain(packet.reasonJson()));
    }

    @Override public void onEncryptionRequest(S01PacketEncryptionRequest packet) throws IOException {
        throw new ProtocolException("Server requires authenticated encryption. Microsoft session authentication is not implemented yet.");
    }

    @Override public void onLoginSuccess(S02PacketLoginSuccess packet) throws IOException { session.loginSucceeded(packet); }
    @Override public void onEnableCompression(S03PacketEnableCompression packet) throws IOException { session.compression(packet.threshold()); }
}
