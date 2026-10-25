package cn.fandmc.epoch.block.entity;

import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.nbt.NbtCompound;

public final class SignBlockEntity extends BlockEntity {
    private final boolean wall;
    public SignBlockEntity(ResourceId id,boolean wall){super(id);this.wall=wall;}
    public boolean wall(){return wall;}
    @Override public BlockEntityData update(BlockEntityData current,BlockEntityUpdate update){
        return update instanceof BlockEntityUpdate.SignText text
                ?new BlockEntityData(text.lines(),NbtCompound.EMPTY):current;
    }
}
