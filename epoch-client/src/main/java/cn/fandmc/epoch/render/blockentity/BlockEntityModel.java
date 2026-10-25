package cn.fandmc.epoch.render.blockentity;

import cn.fandmc.epoch.asset.BlockModels.Face;
import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.block.entity.*;
import cn.fandmc.epoch.render.BlockEntityTextures;
import cn.fandmc.epoch.world.ClientWorld;
import java.util.*;
import java.util.function.Supplier;

/** Typed CPU model provider. GPU resources remain owned by the common renderer. */
public abstract class BlockEntityModel<T extends BlockEntity> {
    private final Class<T> blockType;
    private final Map<Object,List<Face>> geometry=new HashMap<>();

    protected BlockEntityModel(Class<T> blockType){this.blockType=blockType;}
    public final Class<T> blockType(){return blockType;}
    public final List<Face> faces(BlockEntity block,BlockState state,BlockEntityData data,
                                  ClientWorld world,int x,int y,int z,BlockEntityTextures textures){
        return build(blockType.cast(block),state,data,world,x,y,z,textures);
    }
    protected abstract List<Face> build(T block,BlockState state,BlockEntityData data,
                                       ClientWorld world,int x,int y,int z,BlockEntityTextures textures);
    protected final List<Face> cached(Object key,Supplier<List<Face>> factory){
        return geometry.computeIfAbsent(key,k->List.copyOf(factory.get()));
    }
    public final void trim(){if(geometry.size()>4096)geometry.clear();}
    public final void reset(){geometry.clear();}
}
