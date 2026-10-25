package cn.fandmc.epoch.network.listener;

import cn.fandmc.epoch.network.packet.login.*;
import java.io.IOException;

public interface ClientLoginPacketListener extends PacketListener {
    void onDisconnect(S00PacketDisconnect packet) throws IOException;
    void onEncryptionRequest(S01PacketEncryptionRequest packet) throws IOException;
    void onLoginSuccess(S02PacketLoginSuccess packet) throws IOException;
    void onEnableCompression(S03PacketEnableCompression packet) throws IOException;
}
