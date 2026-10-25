package cn.fandmc.epoch.block.entity;

import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.block.BlockState;

public final class ChestBlockEntity extends BlockEntity {
    public enum Kind {
        NORMAL("normal",true),TRAPPED("trapped",true),ENDER("ender",false);
        private final String texture;private final boolean pairable;
        Kind(String texture,boolean pairable){this.texture=texture;this.pairable=pairable;}
    }
    private final Kind kind;
    public ChestBlockEntity(ResourceId id,Kind kind){super(id);this.kind=kind;}
    public boolean pairable(){return kind.pairable;}
    public boolean connects(BlockState state,BlockState neighbor) {
        return kind.pairable&&state.block().equals(neighbor.block())
                &&state.property("facing","").equals(neighbor.property("facing",""));
    }
    public ResourceId texture(boolean large){return new ResourceId("minecraft","entity/chest/"+kind.texture+(large?"_double":""));}
    @Override public BlockEntityData update(BlockEntityData current,BlockEntityUpdate update){return current;}
}
