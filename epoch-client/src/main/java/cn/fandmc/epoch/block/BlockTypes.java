package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.block.entity.*;

/** One responsibility-based implementation per namespaced block identity. */
final class BlockTypes {
    private BlockTypes(){ }
    static Block create(ResourceId id){
        if(!id.namespace().equals("minecraft"))return new Block(id);
        return switch(id.path()){
            case "cobweb"->new CobwebBlock(id);
            case "soul_sand"->new SoulSandBlock(id);
            case "ice","packed_ice"->new SlipperyBlock(id,.98f);
            case "slime_block"->new SlimeBlock(id);
            case "ladder"->new LadderBlock(id);
            case "vine"->new ClimbableBlock(id);
            case "chest"->new ChestBlockEntity(id,ChestBlockEntity.Kind.NORMAL);
            case "trapped_chest"->new ChestBlockEntity(id,ChestBlockEntity.Kind.TRAPPED);
            case "ender_chest"->new ChestBlockEntity(id,ChestBlockEntity.Kind.ENDER);
            case "sign"->new SignBlockEntity(id,false);
            case "wall_sign"->new SignBlockEntity(id,true);
            case "skeleton_skull"->new SkullBlockEntity(id,false);
            case "skeleton_wall_skull"->new SkullBlockEntity(id,true);
            default->new Block(id);
        };
    }
}
