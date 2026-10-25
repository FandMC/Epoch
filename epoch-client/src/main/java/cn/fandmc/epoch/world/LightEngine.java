package cn.fandmc.epoch.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Game-thread incremental relighting. Received chunk light remains the initial authority. */
final class LightEngine {
    private record Cell(int x,int y,int z) {}
    private static final int[] DX={-1,1,0,0,0,0},DY={0,0,-1,1,0,0},DZ={0,0,0,0,-1,1};
    private final ClientWorld world;
    private final ArrayDeque<Cell> queue=new ArrayDeque<>();
    private final Set<Cell> pending=new HashSet<>();

    LightEngine(ClientWorld world){this.world=world;}
    void clear(){queue.clear();pending.clear();}
    void changed(int x,int y,int z,int oldHeight,int newHeight){
        around(x,y,z);
        if(oldHeight!=newHeight)for(int h=Math.min(oldHeight,newHeight);h<=Math.max(oldHeight,newHeight);h++)around(x,h,z);
    }
    private void enqueue(int x,int y,int z){
        if(y<0||y>255||!world.hasChunk(x>>4,z>>4))return;
        Cell cell=new Cell(x,y,z);
        if(pending.add(cell))queue.addLast(cell);
    }
    private void around(int x,int y,int z){
        enqueue(x,y,z);
        for(int d=0;d<6;d++)enqueue(x+DX[d],y+DY[d],z+DZ[d]);
    }
    void update(int budget){
        while(budget-->0&&!queue.isEmpty()){
            Cell cell=queue.removeFirst();pending.remove(cell);
            ChunkColumn column=world.column(cell.x>>4,cell.z>>4);
            if(column==null)continue;
            var definition=world.registry().definition(column.stateHandle(cell.x,cell.y,cell.z)).lighting();
            int opacity=definition.opacity();
            // Vanilla allows an opaque emitter to hold and spread its own light.
            int attenuation=Math.max(1,opacity>=15&&definition.emission()>0?1:opacity);
            int block=definition.emission();
            int sky=world.dimension()==0&&cell.y>=column.lightHeight(cell.x,cell.z)?15:0;
            for(int d=0;d<6;d++){
                int x=cell.x+DX[d],y=cell.y+DY[d],z=cell.z+DZ[d];
                ChunkColumn neighbor=world.column(x>>4,z>>4);
                // Unloaded chunks do not manufacture daylight or act as light sources.
                if(neighbor==null)continue;
                block=Math.max(block,neighbor.light(x,y,z,false)-attenuation);
                if(world.dimension()==0)sky=Math.max(sky,neighbor.light(x,y,z,true)-attenuation);
            }
            boolean changed=column.setLight(cell.x,cell.y,cell.z,false,block);
            if(world.dimension()==0)changed|=column.setLight(cell.x,cell.y,cell.z,true,sky);
            if(changed){world.lightChanged(cell.x,cell.z);around(cell.x,cell.y,cell.z);}
        }
    }
}
