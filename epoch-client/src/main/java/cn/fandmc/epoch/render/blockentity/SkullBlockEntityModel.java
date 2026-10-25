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

public final class SkullBlockEntityModel extends BlockEntityModel<SkullBlockEntity> {
    private record Key(BlockState state,int type,int rotation,ResourceId skin,int textureHeight) { }
    public SkullBlockEntityModel(){super(SkullBlockEntity.class);}
    @Override protected List<Face> build(SkullBlockEntity block,BlockState state,BlockEntityData data,
                                         ClientWorld world,int x,int y,int z,BlockEntityTextures textures){
        int type=block.type(data),rotation=block.rotation(data);var skin=textures.skull(type,data.tag());
        return cached(new Key(state,type,rotation,skin.resource(),skin.texture().height()),
                ()->skull(block,state,rotation,type,skin.resource(),skin.texture().height()));
    }
    private static List<Face> skull(SkullBlockEntity block,BlockState state,int rotation,int type,ResourceId texture,int textureHeight) {
        float x=.5f,y=0,z=.5f,yaw=rotation*22.5f;
        if(block.wall()) {
            y=.25f;
            switch(state.property("facing","north")) {
                case "north"->{z=.74f;yaw=0;}case "south"->{z=.26f;yaw=180;}
                case "west"->{x=.74f;yaw=270;}case "east"->{x=.26f;yaw=90;}
            }
        }
        Matrix4f transform=new Matrix4f().translation(x,y,z).scale(-1,-1,1).rotateY((float)Math.toRadians(yaw)).scale(1/16f);
        List<Face> faces=new ArrayList<>();
        box(faces,texture,transform,-4,-8,-4,8,8,8,0,0,64,textureHeight,0);
        if(type==2||type==3)box(faces,texture,transform,-4,-8,-4,8,8,8,32,0,64,textureHeight,.25f);
        return List.copyOf(faces);
    }
}
