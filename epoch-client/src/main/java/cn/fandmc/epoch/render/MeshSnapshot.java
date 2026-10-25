package cn.fandmc.epoch.render;

import cn.fandmc.epoch.world.ChunkColumn;
import cn.fandmc.epoch.world.ClientWorld;
import cn.fandmc.epoch.world.LightView;
import cn.fandmc.epoch.block.BlockRegistry;

/** A column and its eight neighbors, copied before worker publication, including corner light samples. */
public record MeshSnapshot(ChunkColumn center, ChunkColumn west, ChunkColumn east,
                           ChunkColumn north, ChunkColumn south, ChunkColumn northWest, ChunkColumn northEast,
                           ChunkColumn southWest, ChunkColumn southEast, boolean skylight) implements LightView {
    public static MeshSnapshot capture(ClientWorld world,int x,int z) {
        ChunkColumn center=world.column(x,z);
        if(center==null)return null;
        return new MeshSnapshot(center.snapshot(),copy(world.column(x-1,z)),copy(world.column(x+1,z)),
                copy(world.column(x,z-1)),copy(world.column(x,z+1)),copy(world.column(x-1,z-1)),
                copy(world.column(x+1,z-1)),copy(world.column(x-1,z+1)),copy(world.column(x+1,z+1)),world.dimension()==0);
    }
    private static ChunkColumn copy(ChunkColumn column){return column==null?null:column.snapshot();}
    private ChunkColumn column(int x,int z){
        if(x<0)return z<0?northWest:z>15?southWest:west;
        if(x>15)return z<0?northEast:z>15?southEast:east;
        if(z<0)return north;if(z>15)return south;return center;
    }
    public int state(int x,int y,int z){ChunkColumn c=column(x,z);return c==null?0:c.stateHandle(x,y,z);}
    @Override public BlockRegistry.Definition lightDefinition(int x,int y,int z){return center.registry().definition(state(x,y,z));}
    @Override public boolean hasSkylight(){return skylight;}
    @Override public int rawLight(int x,int y,int z,boolean sky){
        if(sky&&!skylight)return 0;
        ChunkColumn c=column(x,z);
        return c==null?(sky?15:0):c.light(x,y,z,sky);
    }
}
