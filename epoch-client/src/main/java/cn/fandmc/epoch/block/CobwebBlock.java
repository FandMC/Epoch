package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;

public final class CobwebBlock extends Block {
    public CobwebBlock(ResourceId id) { super(id); }
    @Override public void onInside(BlockContact contact) { contact.markInWeb(); }
}
