package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.List;

public record S33PacketUpdateSign(int x,int y,int z,List<String> lines) implements Packet<ClientPlayPacketListener> {
    public S33PacketUpdateSign { lines=List.copyOf(lines);if(lines.size()!=4)throw new IllegalArgumentException("Sign needs four lines"); }
    public static S33PacketUpdateSign read(PacketReader input) throws IOException {
        long p=input.longValue();
        return new S33PacketUpdateSign((int)(p>>38),(int)(p>>26&4095),(int)(p<<38>>38),
                List.of(input.string(32767),input.string(32767),input.string(32767),input.string(32767)));
    }
    @Override public void write(PacketWriter out) throws IOException {
        out.longValue(((long)x&0x3ffffff)<<38|((long)y&4095)<<26|((long)z&0x3ffffff));
        for(String line:lines)out.string(line,32767);
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException {listener.onUpdateSign(this);}
}
