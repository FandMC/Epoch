package cn.fandmc.epoch.block.entity;

import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.block.Block;

/** Base for blocks with per-position data and a dedicated renderer. Instances are shared types. */
public abstract class BlockEntity extends Block {
    protected BlockEntity(ResourceId id) { super(id); }

    /** Unsupported semantic updates must preserve the existing immutable position data. */
    public abstract BlockEntityData update(BlockEntityData current, BlockEntityUpdate update);
}
