package cn.fandmc.epoch.network.codec;

import cn.fandmc.epoch.nbt.*;
import cn.fandmc.epoch.nbt.parser.NbtWriter;
import java.io.IOException;

/** Wire-only representation. Converted to a flattened ItemStack before entering gameplay. */
public record SlotData(int id,int count,int metadata,NbtCompound tag) {
    public static final SlotData EMPTY=new SlotData(-1,0,0,NbtCompound.EMPTY);
    public static SlotData read(PacketReader input)throws IOException {
        int id=input.shortValue();
        if(id==-1)return EMPTY;
        if(id<0)throw new IOException("Negative item ID");
        int count=input.unsignedByte(),meta=input.unsignedShort();var tag=input.compound();
        return count==0?EMPTY:new SlotData(id,count,meta,tag);
    }
    public void write(PacketWriter out)throws IOException {
        out.shortValue(id);if(id==-1)return;
        out.byteValue(count).shortValue(metadata);
        out.bytes(NbtWriter.write(new NbtDocument("",tag.equals(NbtCompound.EMPTY)?null:tag)));
    }
}
