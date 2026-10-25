package cn.fandmc.epoch.item;

import cn.fandmc.epoch.asset.ResourceId;
import java.util.Objects;

/** Immutable item type shared by every inventory slot containing that identity. */
public class Item {
    private final ResourceId id;
    private final int maximumStackSize;

    public Item(ResourceId id, int maximumStackSize) {
        this.id = Objects.requireNonNull(id);
        if (maximumStackSize < 1 || maximumStackSize > 127) throw new IllegalArgumentException("Invalid item stack size");
        this.maximumStackSize = maximumStackSize;
    }

    public final ResourceId id() { return id; }
    public int maximumStackSize() { return maximumStackSize; }
}
