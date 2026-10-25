package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;

public final class SoulSandBlock extends Block {
    public SoulSandBlock(ResourceId id) { super(id); }
    @Override public void onInside(BlockContact contact) { contact.scaleVelocity(.4D,1,.4D); }
}
