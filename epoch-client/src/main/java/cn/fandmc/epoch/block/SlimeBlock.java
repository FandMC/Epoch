package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;

public final class SlimeBlock extends SlipperyBlock {
    public SlimeBlock(ResourceId id) { super(id,.8f); }
    @Override public double landingVelocity(double verticalVelocity,boolean sneaking) {
        return !sneaking&&verticalVelocity<0?-verticalVelocity:0;
    }
}
