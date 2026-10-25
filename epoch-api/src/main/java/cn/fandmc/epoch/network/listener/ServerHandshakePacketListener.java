package cn.fandmc.epoch.network.listener;

import cn.fandmc.epoch.network.packet.handshake.C00Handshake;
import java.io.IOException;

public interface ServerHandshakePacketListener extends PacketListener {
    void onHandshake(C00Handshake packet) throws IOException;
}
