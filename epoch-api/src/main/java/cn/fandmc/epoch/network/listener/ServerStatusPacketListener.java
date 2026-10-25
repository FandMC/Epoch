package cn.fandmc.epoch.network.listener;

import cn.fandmc.epoch.network.packet.status.*;
import java.io.IOException;

public interface ServerStatusPacketListener extends PacketListener {
    void onQuery(C00PacketServerQuery packet) throws IOException;
    void onPing(C01PacketPing packet) throws IOException;
}
