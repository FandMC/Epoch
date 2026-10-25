package cn.fandmc.epoch.network.listener;

import cn.fandmc.epoch.network.packet.status.*;
import java.io.IOException;

public interface ClientStatusPacketListener extends PacketListener {
    void onServerInfo(S00PacketServerInfo packet) throws IOException;
    void onPong(S01PacketPong packet) throws IOException;
}
