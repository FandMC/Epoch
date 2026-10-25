package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S07PacketRespawn(int dimension, int difficulty, int gameMode, String levelType) implements Packet<ClientPlayPacketListener> {
    public S07PacketRespawn {
        PacketFields.dimension(dimension);
        if (difficulty < 0 || difficulty > 3 || gameMode < 0 || gameMode > 3 || levelType == null || levelType.length() > 16)
            throw new IllegalArgumentException("Invalid respawn fields");
    }
    public static S07PacketRespawn read(PacketReader input) throws IOException {
        return new S07PacketRespawn(input.intValue(), input.unsignedByte(), input.unsignedByte(), input.string(16));
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.intValue(dimension).byteValue(difficulty).byteValue(gameMode).string(levelType, 16);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onRespawn(this); }
}
