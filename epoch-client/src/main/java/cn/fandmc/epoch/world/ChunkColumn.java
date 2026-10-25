package cn.fandmc.epoch.world;

import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.block.entity.BlockEntity;

public final class ChunkColumn {
    private final int x;
    private final int z;
    private final BlockRegistry registry;
    private final int[][] states = new int[16][];
    private final byte[][] blockLight = new byte[16][];
    private final byte[][] skyLight = new byte[16][];
    private final int[] lightHeight = new int[256];
    private boolean heightsDirty;
    private byte[] biomes;
    private final java.util.Map<Integer,BlockState> blockEntities=new java.util.HashMap<>();
    public java.util.Map<Integer,BlockState> blockEntities(){return java.util.Collections.unmodifiableMap(blockEntities);}

    public ChunkColumn(int x, int z, BlockRegistry registry) { this.x = x; this.z = z; this.registry = registry; }
    public int x() { return x; }
    public BlockRegistry registry(){return registry;}
    public int z() { return z; }
    public BlockState state(int x, int y, int z) {
        return registry.state(stateHandle(x,y,z));
    }
    public int stateHandle(int x,int y,int z) {
        if(y<0 || y>255 || states[y>>4]==null)return 0;
        return states[y>>4][((y&15)<<8)|((z&15)<<4)|(x&15)];
    }

    /** Snapshot published to a mesh worker; subsequent game-thread mutations cannot affect it. */
    public ChunkColumn snapshot() {
        ensureLightHeights();
        ChunkColumn copy=new ChunkColumn(x,z,registry);
        for(int i=0;i<16;i++){
            copy.states[i]=states[i]==null?null:states[i].clone();
            copy.blockLight[i]=blockLight[i]==null?null:blockLight[i].clone();
            copy.skyLight[i]=skyLight[i]==null?null:skyLight[i].clone();
        }
        copy.biomes=biomes==null?null:biomes.clone();
        copy.blockEntities.putAll(blockEntities);
        System.arraycopy(lightHeight,0,copy.lightHeight,0,256);
        return copy;
    }

    public void setState(int x, int y, int z, BlockState state) {
        if (y < 0 || y > 255) throw new IllegalArgumentException("Invalid block height");
        if (states[y >> 4] == null) states[y >> 4] = new int[4096];
        states[y >> 4][((y & 15) << 8) | ((z & 15) << 4) | (x & 15)] = registry.handle(state);
        int position=(y<<8)|((z&15)<<4)|(x&15);
        if(registry.block(state) instanceof BlockEntity)blockEntities.put(position,state);else blockEntities.remove(position);
        if(!heightsDirty)updateLightHeight(x,z);
    }

    public void merge(ChunkColumn update) {
        if (registry != update.registry) throw new IllegalArgumentException("Different block registries");
        for (int section = 0; section < 16; section++) {
            if (update.states[section] != null) {
                int targetSection=section;blockEntities.keySet().removeIf(p->(p>>12)==targetSection);
                states[section] = update.states[section];
                blockLight[section] = update.blockLight[section];
                skyLight[section] = update.skyLight[section];
            }
        }
        if (update.biomes != null) biomes = update.biomes;
        blockEntities.putAll(update.blockEntities);
        heightsDirty=true;
    }

    public int sectionCount() {
        int count = 0;
        for (int[] section : states) if (section != null) count++;
        return count;
    }

    public boolean hasSection(int section) { return states[section] != null; }

    public void section(int section, int[] handles, byte[] block, byte[] sky) {
        if (section < 0 || section > 15 || handles.length != 4096 || block.length != 2048
                || (sky != null && sky.length != 2048)) throw new IllegalArgumentException("Invalid section");
        for (int handle : handles) registry.state(handle);
        states[section] = handles.clone(); blockLight[section] = block.clone(); skyLight[section] = sky == null ? null : sky.clone();
        blockEntities.keySet().removeIf(p->(p>>12)==section);
        for(int i=0;i<handles.length;i++){BlockState state=registry.state(handles[i]);if(registry.block(handles[i]) instanceof BlockEntity)blockEntities.put(section*4096+i,state);}
        heightsDirty=true;
    }

    public void biomes(byte[] value) {
        if (value.length != 256) throw new IllegalArgumentException("Invalid biome data");
        biomes = value.clone();
    }

    public int light(int x, int y, int z, boolean sky) {
        if (y < 0) return 0;
        if (y > 255) return sky ? 15 : 0;
        byte[] data = (sky ? skyLight : blockLight)[y >> 4];
        if (data == null) return sky && y>=lightHeight(x,z) ? 15 : 0;
        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        return (data[index >> 1] >>> ((index & 1) * 4)) & 15;
    }

    /** First cell exposed to direct sky; optical opacity is independent of collision/mesh opacity. */
    public int lightHeight(int x,int z) {
        ensureLightHeights();
        return lightHeight[((z&15)<<4)|(x&15)];
    }
    private void ensureLightHeights() {
        if(!heightsDirty)return;
        for(int z=0;z<16;z++)for(int x=0;x<16;x++)updateLightHeight(x,z);
        heightsDirty=false;
    }
    private void updateLightHeight(int x,int z) {
        int y=255;
        while(y>=0&&registry.definition(stateHandle(x,y,z)).lighting().opacity()==0)y--;
        lightHeight[((z&15)<<4)|(x&15)]=y+1;
    }
    boolean setLight(int x,int y,int z,boolean sky,int value) {
        if(y<0||y>255||light(x,y,z,sky)==value)return false;
        byte[][] channel=sky?skyLight:blockLight;
        int section=y>>4;
        if(channel[section]==null){
            byte[] data=new byte[2048];
            if(sky)for(int i=0;i<4096;i++){
                int level=(section*16+(i>>8))>=lightHeight(i&15,(i>>4)&15)?15:0;
                data[i>>1]|=(byte)(level<<((i&1)*4));
            }
            channel[section]=data;
        }
        int index=((y&15)<<8)|((z&15)<<4)|(x&15),shift=(index&1)*4;
        channel[section][index>>1]=(byte)((channel[section][index>>1]&~(15<<shift))|(value<<shift));
        return true;
    }
}
