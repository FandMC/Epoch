package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.physics.Box;
import java.util.List;

public final class LadderBlock extends ClimbableBlock {
    private static final List<Box> NORTH=List.of(new Box(0,0,.8125,1,1,1));
    private static final List<Box> SOUTH=List.of(new Box(0,0,0,1,1,.1875));
    private static final List<Box> WEST=List.of(new Box(.8125,0,0,1,1,1));
    private static final List<Box> EAST=List.of(new Box(0,0,0,.1875,1,1));

    public LadderBlock(ResourceId id) { super(id); }
    @Override public List<Box> collision(BlockState state,List<Box> shape,BlockCollisionContext context) {
        if(!context.modernLadders())return shape;
        return switch(state.property("facing","north")) {
            case "north"->NORTH;case "south"->SOUTH;case "west"->WEST;default->EAST;
        };
    }
}
