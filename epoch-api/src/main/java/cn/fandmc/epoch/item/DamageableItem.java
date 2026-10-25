package cn.fandmc.epoch.item;

import cn.fandmc.epoch.asset.ResourceId;

/** An item type whose legacy stack damage is durability, rather than a variant. */
public final class DamageableItem extends Item {
    private final int maximumDamage;

    public DamageableItem(ResourceId id, int maximumDamage) {
        super(id, 1);
        if (maximumDamage < 1) throw new IllegalArgumentException("Invalid item durability");
        this.maximumDamage = maximumDamage;
    }

    public int maximumDamage() { return maximumDamage; }
}
