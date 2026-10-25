package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C0FPacketConfirmTransaction(int windowId, short actionNumber, boolean accepted) implements Packet<ServerPlayPacketListener> {
    public C0FPacketConfirmTransaction { if (windowId < 0 || windowId > 255) throw new IllegalArgumentException("Invalid window id"); }
    public static C0FPacketConfirmTransaction read(PacketReader input) throws IOException {
        return new C0FPacketConfirmTransaction(input.unsignedByte(), input.shortValue(), input.bool());
    }
    @Override public void write(PacketWriter output) throws IOException { output.byteValue(windowId).shortValue(actionNumber).bool(accepted); }
    @Override public void handle(ServerPlayPacketListener listener) throws IOException { listener.onConfirmTransaction(this); }
}
