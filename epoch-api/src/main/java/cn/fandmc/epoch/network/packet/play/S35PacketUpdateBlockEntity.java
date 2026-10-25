package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.nbt.NbtCompound;
import cn.fandmc.epoch.nbt.parser.NbtParser;
import cn.fandmc.epoch.network.codec.PacketReader;
import cn.fandmc.epoch.network.codec.PacketWriter;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

/** Preserves the exact bounded NBT payload for symmetric packet serialization. */
public record S35PacketUpdateBlockEntity(int x,int y,int z,int action,byte[] payload,NbtCompound data) implements Packet<ClientPlayPacketListener> {
    public S35PacketUpdateBlockEntity {payload=payload.clone();}
    @Override public byte[] payload(){return payload.clone();}
    public static S35PacketUpdateBlockEntity read(PacketReader input) throws IOException {
        long p=input.longValue();int action=input.unsignedByte();byte[] bytes=input.bytes(input.remaining());
        NbtCompound data=NbtParser.parseCompound(bytes);
        return new S35PacketUpdateBlockEntity((int)(p>>38),(int)(p>>26&4095),(int)(p<<38>>38),action,bytes,data);
    }
    @Override public void write(PacketWriter out) throws IOException {out.longValue(((long)x&0x3ffffff)<<38|((long)y&4095)<<26|((long)z&0x3ffffff)).byteValue(action).bytes(payload);}
    @Override public void handle(ClientPlayPacketListener listener) throws IOException {listener.onUpdateBlockEntity(this);}
}
