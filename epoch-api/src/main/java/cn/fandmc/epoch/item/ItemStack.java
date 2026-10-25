package cn.fandmc.epoch.item;

import cn.fandmc.epoch.nbt.NbtCompound;
import java.util.Objects;

/** Immutable data for one inventory slot; never stored on the shared Item type. */
public record ItemStack(Item item, int count, int damage, NbtCompound tag) {
    public static final ItemStack EMPTY = new ItemStack(null, 0, 0, NbtCompound.EMPTY);

    public ItemStack {
        Objects.requireNonNull(tag);
        if (item == null) {
            if (count != 0 || damage != 0 || !tag.equals(NbtCompound.EMPTY))
                throw new IllegalArgumentException("An empty stack cannot contain item data");
        } else {
            if (count < 1 || count > 127) throw new IllegalArgumentException("Invalid item stack count");
            if (item instanceof DamageableItem durable) {
                if (damage < 0 || damage > 65535) throw new IllegalArgumentException("Invalid item durability value");
            } else if (damage != 0) throw new IllegalArgumentException("Only damageable items have durability");
        }
    }

    public boolean isEmpty() { return item == null; }
    public ItemStack withCount(int value) { return value == 0 ? EMPTY : new ItemStack(item, value, damage, tag); }
    public ItemStack withDamage(int value) { return new ItemStack(item, count, value, tag); }
    public boolean sameItemAndData(ItemStack other) {
        return other != null && !isEmpty() && !other.isEmpty()
                && item.id().equals(other.item.id()) && damage == other.damage && tag.equals(other.tag);
    }
}
