package cn.fandmc.epoch.item;

import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.block.BlockState;
import java.util.Objects;

/** Inventory item that places a flattened block state. */
public final class BlockItem extends Item {
    private final BlockState placedState;

    public BlockItem(ResourceId id, BlockState placedState, int maximumStackSize) {
        super(id, maximumStackSize);
        this.placedState = Objects.requireNonNull(placedState);
    }

    public BlockState placedState() { return placedState; }
}
