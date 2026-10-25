package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;

public class SlipperyBlock extends Block {
    private final float friction;
    public SlipperyBlock(ResourceId id,float friction) { super(id);this.friction=friction; }
    @Override public final float slipperiness() { return friction; }
}
