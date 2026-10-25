package cn.fandmc.epoch.physics;

import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.block.BlockCollisionContext;
import cn.fandmc.epoch.world.ClientWorld;
import java.util.ArrayList;
import java.util.List;

public final class CollisionWorld {
    private CollisionWorld() {}

    public static List<Box> boxes(ClientWorld world,Box query){
        List<Box> result=new ArrayList<>();
        BlockCollisionContext context=world.modernLadderCollision()
                ?BlockCollisionContext.MODERN_LADDERS:BlockCollisionContext.LEGACY;
        int minX=(int)Math.floor(query.minX()),maxX=(int)Math.floor(query.maxX());
        int minZ=(int)Math.floor(query.minZ()),maxZ=(int)Math.floor(query.maxZ());
        int minY=Math.max(0,(int)Math.floor(query.minY())-1),maxY=Math.min(255,(int)Math.floor(query.maxY()));
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            if(!world.hasChunk(x>>4,z>>4)){
                Box boundary=new Box(x,-4096,z,x+1,4096,z+1);
                if(boundary.intersects(query))result.add(boundary);
                continue;
            }
            for(int y=minY;y<=maxY;y++){
                BlockState state=world.resolvedState(x,y,z);
                if(state.isAir())continue;
                var definition=world.registry().definition(state);
                List<Box> shapes=definition.block().collision(state,definition.collision(),context);
                for(Box shape:shapes){
                    Box moved=shape.move(x,y,z);
                    if(moved.intersects(query))result.add(moved);
                }
            }
        }
        return result;
    }
}
