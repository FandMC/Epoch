package cn.fandmc.epoch.network.packet;

import cn.fandmc.epoch.network.codec.PacketWriter;
import cn.fandmc.epoch.network.listener.PacketListener;
import java.io.IOException;

public interface Packet<L extends PacketListener> {
    void write(PacketWriter output) throws IOException;
    void handle(L listener) throws IOException;
}
