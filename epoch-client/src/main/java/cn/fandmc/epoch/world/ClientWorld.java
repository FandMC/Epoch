package cn.fandmc.epoch.world;

import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.block.BlockStateResolver;
import cn.fandmc.epoch.block.entity.BlockEntity;
import cn.fandmc.epoch.block.entity.BlockEntityData;
import cn.fandmc.epoch.block.entity.BlockEntityUpdate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ClientWorld implements LightView {
    private final BlockRegistry registry;
    private final BlockStateResolver stateResolver;
    private final LightEngine lighting=new LightEngine(this);
    private final BlockStateResolver.Lookup baseStates=this::stateHandle;
    private final java.util.Set<Long> dirty = new java.util.LinkedHashSet<>();
    public ClientWorld(BlockRegistry registry) { this.registry = registry;stateResolver=new BlockStateResolver(registry); }
    public BlockRegistry registry() { return registry; }
    @Override public BlockRegistry.Definition lightDefinition(int x,int y,int z){return registry.definition(stateHandle(x,y,z));}
    @Override public boolean hasSkylight(){return dimension==0;}
    @Override public int rawLight(int x,int y,int z,boolean sky){
        if(sky&&!hasSkylight())return 0;
        ChunkColumn column=column(x>>4,z>>4);
        return column==null?(sky?15:0):column.light(x,y,z,sky);
    }
    public ChunkColumn column(int x, int z) { return columns.get(key(x, z)); }
    public java.util.Set<Long> drainDirty() { var result = java.util.Set.copyOf(dirty); dirty.clear(); return result; }
    private final Map<Long, ChunkColumn> columns = new HashMap<>();
    private final Map<BlockEntityData.Position,BlockEntityData> blockEntities=new HashMap<>();
    public BlockEntityData blockEntity(int x,int y,int z) {return blockEntities.getOrDefault(new BlockEntityData.Position(x,y,z),BlockEntityData.EMPTY);}
    public void updateBlockEntity(int x,int y,int z,BlockEntityUpdate update) {
        if(!(registry.block(state(x,y,z)) instanceof BlockEntity type))return;
        BlockEntityData.Position position=new BlockEntityData.Position(x,y,z);
        BlockEntityData current=blockEntities.getOrDefault(position,BlockEntityData.EMPTY);
        BlockEntityData next=type.update(current,update);
        if(!current.equals(next)){blockEntities.put(position,next);revision++;}
    }
    private void clearBlockEntities(int x,int z) {blockEntities.keySet().removeIf(p->(p.x()>>4)==x&&(p.z()>>4)==z);}
    private long revision;
    private int dimension;
    private long epoch;
    private long worldTime=6000;
    private boolean daylightCycle=true;
    private boolean modernLadderCollision;
    public boolean modernLadderCollision(){return modernLadderCollision;}
    public void modernLadderCollision(boolean enabled){modernLadderCollision=enabled;}

    public void reset(int dimension) { dirty.addAll(columns.keySet()); columns.clear();blockEntities.clear();lighting.clear(); this.dimension = dimension; revision++; epoch++;worldTime=6000;daylightCycle=true; }
    public long epoch(){return epoch;}
    public void time(long time){worldTime=time==Long.MIN_VALUE?0:Math.abs(time);daylightCycle=time>=0;}
    public void tick(){if(daylightCycle)worldTime++;lighting.update(16384);}
    public float daylight(){
        if(dimension!=0)return 0;
        double angle=Math.floorMod(worldTime,24000L)/24000d-.25;
        if(angle<0)angle++;
        double smooth=1-(Math.cos(angle*Math.PI)+1)/2;
        angle+=(smooth-angle)/3;
        return (float)Math.clamp(Math.cos(angle*Math.PI*2)*2+.5,0,1);
    }
    public int dimension() { return dimension; }
    public long revision() { return revision; }
    public int chunkCount() { return columns.size(); }
    public Collection<ChunkColumn> columns() { return columns.values(); }
    public boolean hasChunk(int x, int z) { return columns.containsKey(key(x, z)); }

    public void apply(ChunkColumn column, boolean groundUp) {
        long key = key(column.x(), column.z());
        ChunkColumn previous = columns.get(key);
        if (groundUp || previous == null) {
            clearBlockEntities(column.x(), column.z());
            columns.put(key, column);
        } else {
            // Compare identities before merging; properties alone must not erase position data.
            blockEntities.keySet().removeIf(p -> (p.x() >> 4) == column.x() && (p.z() >> 4) == column.z()
                    && column.hasSection(p.y() >> 4)
                    && !previous.state(p.x(), p.y(), p.z()).block().equals(column.state(p.x(), p.y(), p.z()).block()));
            previous.merge(column);
        }
        dirtyAround(column.x(), column.z());
        revision++;
    }

    public void unload(int x, int z) { columns.remove(key(x, z));clearBlockEntities(x,z); dirtyAround(x, z); revision++; }

    public BlockState state(int x, int y, int z) {
        ChunkColumn chunk = columns.get(key(x >> 4, z >> 4));
        return chunk == null ? registry.air() : chunk.state(x, y, z);
    }
    public int stateHandle(int x,int y,int z){
        ChunkColumn chunk=columns.get(key(x>>4,z>>4));
        return chunk==null?0:chunk.stateHandle(x,y,z);
    }
    public BlockState resolvedState(int x,int y,int z) {
        return registry.state(stateResolver.resolve(stateHandle(x,y,z),x,y,z,baseStates));
    }

    public void setState(int x, int y, int z, BlockState state) {
        ChunkColumn chunk = columns.get(key(x >> 4, z >> 4));
        if (chunk != null) {
            if(chunk.state(x,y,z).equals(state))return;
            if(!chunk.state(x,y,z).block().equals(state.block()))blockEntities.remove(new BlockEntityData.Position(x,y,z));
            int oldHeight=chunk.lightHeight(x,z);
            chunk.setState(x, y, z, state);
            lighting.changed(x,y,z,oldHeight,chunk.lightHeight(x,z));
            dirtyAround(x>>4,z>>4);
            revision++;
        }
    }

    private void dirtyAround(int x, int z) {
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)dirty.add(key(x+dx,z+dz));
    }

    void lightChanged(int x,int z){dirtyAround(x>>4,z>>4);revision++;}

    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xffff_ffffL); }
}
