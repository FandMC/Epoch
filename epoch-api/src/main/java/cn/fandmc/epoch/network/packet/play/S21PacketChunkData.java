package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S21PacketChunkData(int chunkX, int chunkZ, boolean groundUp, int sectionMask, byte[] data)
        implements Packet<ClientPlayPacketListener> {
    public S21PacketChunkData {
        if ((sectionMask & ~65535) != 0 || data.length > 196864) throw new IllegalArgumentException("Invalid chunk payload");
        data = data.clone();
    }
    @Override public byte[] data() { return data.clone(); }
    public boolean unload() { return groundUp && sectionMask == 0; }
    public static S21PacketChunkData read(PacketReader input) throws IOException {
        return new S21PacketChunkData(input.intValue(), input.intValue(), input.bool(), input.unsignedShort(),
                PacketFields.byteArray(input, 196864));
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.intValue(chunkX).intValue(chunkZ).bool(groundUp).shortValue(sectionMask);
        PacketFields.byteArray(output, data);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onChunkData(this); }
}
