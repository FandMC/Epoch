package cn.fandmc.epoch.block.entity;

import cn.fandmc.epoch.asset.ResourceId;
import java.util.List;

public final class SkullBlockEntity extends BlockEntity {
    private final boolean wall;
    public SkullBlockEntity(ResourceId id,boolean wall){super(id);this.wall=wall;}
    public boolean wall(){return wall;}
    public int type(BlockEntityData data){return Math.clamp(data.tag().integer("SkullType",0),0,4);}
    public int rotation(BlockEntityData data){return data.tag().integer("Rot",0)&15;}
    @Override public BlockEntityData update(BlockEntityData current,BlockEntityUpdate update){
        return update instanceof BlockEntityUpdate.Skull skull?new BlockEntityData(List.of(),skull.tag()):current;
    }
}
