package cn.fandmc.epoch.render.blockentity;

import cn.fandmc.epoch.asset.BlockModels.*;
import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.block.entity.*;
import cn.fandmc.epoch.render.BlockEntityTextures;
import cn.fandmc.epoch.world.ClientWorld;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
import static cn.fandmc.epoch.render.blockentity.ModelBoxGeometry.box;

public final class SignBlockEntityModel extends BlockEntityModel<SignBlockEntity> {
    private record Key(BlockState state,ResourceId text) { }
    public SignBlockEntityModel(){super(SignBlockEntity.class);}
    @Override protected List<Face> build(SignBlockEntity block,BlockState state,BlockEntityData data,
                                         ClientWorld world,int x,int y,int z,BlockEntityTextures textures){
        ResourceId text=data.signLines().isEmpty()?null:textures.sign(data.signLines());
        return cached(new Key(state,text),()->{
            List<Face> faces=new ArrayList<>(sign(block,state));
            if(text!=null)faces.add(signText(block,state,text));
            return faces;
        });
    }
    private static Matrix4f signTransform(SignBlockEntity block,BlockState state) {
        boolean wall=block.wall();
        float angle=wall?switch(state.property("facing","north")){case "north"->180;case "west"->90;case "east"->-90;default->0;}
                :Integer.parseInt(state.property("rotation","0"))*22.5f;
        Matrix4f result=new Matrix4f().translation(.5f,.5f,.5f).rotateY((float)Math.toRadians(-angle));
        if(wall)result.translate(0,-.3125f,-.4375f);
        return result;
    }
    private static List<Face> sign(SignBlockEntity block,BlockState state) {
        List<Face> faces=new ArrayList<>();ResourceId texture=ResourceId.parse("minecraft:entity/sign");
        Matrix4f transform=signTransform(block,state).scale(2/3f,-2/3f,-2/3f).scale(1/16f);
        box(faces,texture,transform,-12,-14,-1,24,12,2,0,0,64,32,0);
        if(!block.wall())box(faces,texture,transform,-1,-2,-1,2,14,2,0,14,64,32,0);
        return List.copyOf(faces);
    }
    private static Face signText(SignBlockEntity block,BlockState state,ResourceId texture) {
        Matrix4f transform=signTransform(block,state).translate(0,1/3f,.047f);
        float w=.9375f,h=40/96f;
        List<Vertex> vertices=new ArrayList<>();
        for(float[] p:new float[][]{{-w/2,h/2,0,0},{-w/2,-h/2,0,1},{w/2,-h/2,1,1},{w/2,h/2,1,0}}) {
            Vector3f v=transform.transformPosition(new Vector3f(p[0],p[1],0));vertices.add(new Vertex(v.x,v.y,v.z,p[2],p[3]));
        }
        return new Face(List.copyOf(vertices),texture,null,"south",false,1);
    }
}
