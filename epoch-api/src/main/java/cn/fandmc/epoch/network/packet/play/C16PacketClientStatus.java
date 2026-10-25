package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.PacketReader;
import cn.fandmc.epoch.network.codec.PacketWriter;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C16PacketClientStatus(Action action) implements Packet<ServerPlayPacketListener> {
    public enum Action { PERFORM_RESPAWN, REQUEST_STATS, OPEN_INVENTORY }
    public C16PacketClientStatus {
        if (action==null) throw new IllegalArgumentException("Missing client status action");
    }
    public static C16PacketClientStatus read(PacketReader input) throws IOException {
        int id=input.varInt();
        if (id<0||id>=Action.values().length) throw new IOException("Invalid client status action");
        return new C16PacketClientStatus(Action.values()[id]);
    }
    @Override public void write(PacketWriter output) throws IOException { output.varInt(action.ordinal()); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onClientStatus(this); }
}
