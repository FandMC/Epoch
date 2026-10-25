package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C19PacketResourcePackStatus(String hash, Result result) implements Packet<ServerPlayPacketListener> {
    public enum Result { SUCCESSFULLY_LOADED, DECLINED, FAILED_DOWNLOAD, ACCEPTED }
    public C19PacketResourcePackStatus {
        if (hash == null || hash.length() > 40 || result == null) throw new IllegalArgumentException("Invalid resource pack status");
    }
    public static C19PacketResourcePackStatus read(PacketReader input) throws IOException {
        String hash = input.string(40);
        int value = input.varInt();
        if (value < 0 || value >= Result.values().length) throw new IllegalArgumentException("Invalid resource pack result");
        return new C19PacketResourcePackStatus(hash, Result.values()[value]);
    }
    @Override public void write(PacketWriter output) throws IOException { output.string(hash, 40).varInt(result.ordinal()); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onResourcePackStatus(this); }
}
