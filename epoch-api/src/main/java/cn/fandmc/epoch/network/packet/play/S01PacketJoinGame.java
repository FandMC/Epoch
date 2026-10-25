package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S01PacketJoinGame(int entityId, int gameMode, int dimension, int difficulty,
                              int maxPlayers, String levelType, boolean reducedDebugInfo) implements Packet<ClientPlayPacketListener> {
    public S01PacketJoinGame {
        PacketFields.dimension(dimension);
        if ((gameMode & ~11) != 0 || difficulty < 0 || difficulty > 3 || maxPlayers < 0 || maxPlayers > 255
                || levelType == null || levelType.length() > 16) throw new IllegalArgumentException("Invalid join game fields");
    }
    public static S01PacketJoinGame read(PacketReader input) throws IOException {
        return new S01PacketJoinGame(input.intValue(), input.unsignedByte(), input.byteValue(), input.unsignedByte(),
                input.unsignedByte(), input.string(16), input.bool());
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.intValue(entityId).byteValue(gameMode).byteValue(dimension).byteValue(difficulty)
                .byteValue(maxPlayers).string(levelType, 16).bool(reducedDebugInfo);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onJoinGame(this); }
}
