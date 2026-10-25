package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;

public class ClimbableBlock extends Block {
    public ClimbableBlock(ResourceId id) { super(id); }
    @Override public final boolean climbable() { return true; }
}
