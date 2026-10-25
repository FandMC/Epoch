package cn.fandmc.epoch.network.listener;

import cn.fandmc.epoch.network.packet.login.*;
import java.io.IOException;

public interface ServerLoginPacketListener extends PacketListener {
    void onLoginStart(C00PacketLoginStart packet) throws IOException;
    void onEncryptionResponse(C01PacketEncryptionResponse packet) throws IOException;
}
