package cn.fandmc.epoch.render.blockentity;

import cn.fandmc.epoch.asset.BlockModels.*;
import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.block.entity.*;
import cn.fandmc.epoch.render.BlockEntityTextures;
import cn.fandmc.epoch.world.ClientWorld;
import org.joml.Matrix4f;
import java.util.*;
import static cn.fandmc.epoch.render.blockentity.ModelBoxGeometry.box;

public final class ChestBlockEntityModel extends BlockEntityModel<ChestBlockEntity> {
    private record Key(BlockState state,boolean large) { }
    public ChestBlockEntityModel(){super(ChestBlockEntity.class);}
    @Override protected List<Face> build(ChestBlockEntity block,BlockState state,BlockEntityData data,
                                         ClientWorld world,int x,int y,int z,BlockEntityTextures textures){
        boolean large=false;
        if(block.pairable()){
            String facing=state.property("facing","south");
            boolean alongX=facing.equals("north")||facing.equals("south");int dx=alongX?1:0,dz=alongX?0:1;
            if(block.connects(state,world.state(x-dx,y,z-dz)))return List.of();
            large=block.connects(state,world.state(x+dx,y,z+dz));
        }
        boolean doubleChest=large;
        return cached(new Key(state,large),()->chest(block,state,doubleChest));
    }
    private static List<Face> chest(ChestBlockEntity block,BlockState state,boolean large) {
        ResourceId texture=block.texture(large);
        float angle=switch(state.property("facing","south")){case "north"->180;case "west"->90;case "east"->-90;default->0;};
        Matrix4f transform=new Matrix4f().translation(large?1:.5f,1,.5f).scale(1,-1,-1).rotateY((float)Math.toRadians(angle))
                .translate(large?-1:-.5f,0,-.5f).scale(1/16f);
        // Double chest center follows its world axis.
        if(large&&(state.property("facing","").equals("east")||state.property("facing","").equals("west")))
            transform=new Matrix4f().translation(.5f,1,1).scale(1,-1,-1).rotateY((float)Math.toRadians(angle)).translate(-1,0,-.5f).scale(1/16f);
        List<Face> faces=new ArrayList<>();int width=large?30:14,tw=large?128:64;
        box(faces,texture,transform,1,6,1,width,10,14,0,19,tw,64,0);
        box(faces,texture,transform,1,2,1,width,5,14,0,0,tw,64,0);
        box(faces,texture,transform,large?15:7,5,0,2,4,1,0,0,tw,64,0);
        return List.copyOf(faces);
    }
}
