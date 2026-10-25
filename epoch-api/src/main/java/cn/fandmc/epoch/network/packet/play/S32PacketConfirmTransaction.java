package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S32PacketConfirmTransaction(int windowId, short actionNumber, boolean accepted) implements Packet<ClientPlayPacketListener> {
    public S32PacketConfirmTransaction { if (windowId < 0 || windowId > 255) throw new IllegalArgumentException("Invalid window id"); }
    public static S32PacketConfirmTransaction read(PacketReader input) throws IOException {
        return new S32PacketConfirmTransaction(input.unsignedByte(), input.shortValue(), input.bool());
    }
    @Override public void write(PacketWriter output) throws IOException { output.byteValue(windowId).shortValue(actionNumber).bool(accepted); }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onConfirmTransaction(this); }
}
