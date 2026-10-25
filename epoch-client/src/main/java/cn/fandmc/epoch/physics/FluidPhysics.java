package cn.fandmc.epoch.physics;

import cn.fandmc.epoch.block.BlockState;
import cn.fandmc.epoch.player.PlayerState;
import cn.fandmc.epoch.world.ClientWorld;

/** Body contact, water currents and liquid clearance for player movement. */
public final class FluidPhysics {
    private static final int[][] HORIZONTAL = {{0,-1},{1,0},{0,1},{-1,0}};
    private record Flow(double x, double y, double z) {
        private static final Flow ZERO = new Flow(0,0,0);
        Flow normalized() {
            // The original vector math rounds the square root to float.
            double length=(float)Math.sqrt(x*x+y*y+z*z);
            return length<1.0e-4D?ZERO:new Flow(x/length,y/length,z/length);
        }
    }

    private FluidPhysics() {}

    /** Applies the sampled current once per call; the controller selects the vanilla tick phases. */
    public static boolean applyWater(ClientWorld world, PlayerState player) {
        Box body=player.bounds();
        Box box=new Box(body.minX()+.001D,body.minY()+(double).4f+.001D,body.minZ()+.001D,
                body.maxX()-.001D,body.maxY()-(double).4f-.001D,body.maxZ()-.001D);
        int minX=floor(box.minX()),maxX=floor(box.maxX()+1);
        int minY=floor(box.minY()),maxY=floor(box.maxY()+1);
        int minZ=floor(box.minZ()),maxZ=floor(box.maxZ()+1);
        // Match the inclusive area-loaded check, including its far boundary chunk.
        if (maxY<0||minY>=256) return false;
        for (int x=minX>>4;x<=maxX>>4;x++) for (int z=minZ>>4;z<=maxZ>>4;z++)
            if (!world.hasChunk(x,z)) return false;
        boolean contact=false;
        double flowX=0,flowY=0,flowZ=0;
        for (int x=minX;x<maxX;x++) for (int y=minY;y<maxY;y++) for (int z=minZ;z<maxZ;z++) {
            BlockState state=world.state(x,y,z);
            if (!is(state,"water")) continue;
            float surface=(float)(y+1)-(depth(state)+1)/9f;
            if (maxY<surface) continue;
            contact=true;
            if (!player.flying) {
                Flow flow=waterFlow(world,x,y,z,state);
                flowX+=flow.x();flowY+=flow.y();flowZ+=flow.z();
            }
        }
        if (!player.flying) {
            Flow push=new Flow(flowX,flowY,flowZ).normalized();
            player.velocityX+=push.x()*.014D;
            player.velocityY+=push.y()*.014D;
            player.velocityZ+=push.z()*.014D;
        }
        return contact;
    }

    public static boolean touchesLava(ClientWorld world, Box body) {
        Box box=new Box(body.minX()+(double).1f,body.minY()+(double).4f,body.minZ()+(double).1f,
                body.maxX()-(double).1f,body.maxY()-(double).4f,body.maxZ()-(double).1f);
        for (int x=floor(box.minX());x<floor(box.maxX()+1);x++)
            for (int y=floor(box.minY());y<floor(box.maxY()+1);y++)
                for (int z=floor(box.minZ());z<floor(box.maxZ()+1);z++)
                    if (is(world.state(x,y,z),"lava")) return true;
        return false;
    }

    public static boolean canExit(ClientWorld world, Box box) {
        if (!CollisionWorld.boxes(world,box).isEmpty()) return false;
        // Clearance rejects any liquid block, including contact at an integer maximum.
        for (int x=floor(box.minX());x<=floor(box.maxX());x++)
            for (int y=floor(box.minY());y<=floor(box.maxY());y++)
                for (int z=floor(box.minZ());z<=floor(box.maxZ());z++) {
                    BlockState state=world.state(x,y,z);
                    if (is(state,"water")||is(state,"lava")) return false;
                }
        return true;
    }

    private static Flow waterFlow(ClientWorld world, int x, int y, int z, BlockState state) {
        int level=depth(state);
        double flowX=0,flowZ=0;
        for (int[] direction:HORIZONTAL) {
            int nx=x+direction[0],nz=z+direction[1];
            BlockState neighbor=world.state(nx,y,nz);
            int adjacent=is(neighbor,"water")?depth(neighbor):-1;
            int delta;
            if (adjacent>=0) delta=adjacent-level;
            else {
                if (world.registry().definition(neighbor).blocksMovement()) continue;
                BlockState below=world.state(nx,y-1,nz);
                if (!is(below,"water")) continue;
                delta=depth(below)-(level-8);
            }
            flowX+=direction[0]*delta;flowZ+=direction[1]*delta;
        }
        Flow flow=new Flow(flowX,0,flowZ);
        if (Integer.parseInt(state.property("level","0"))>=8) {
            for (int[] direction:HORIZONTAL) {
                int nx=x+direction[0],nz=z+direction[1];
                if (solidFace(world,nx,y,nz)||solidFace(world,nx,y+1,nz)) {
                    flow=flow.normalized();
                    flow=new Flow(flow.x(),flow.y()-6,flow.z());
                    break;
                }
            }
        }
        return flow.normalized();
    }

    private static boolean solidFace(ClientWorld world, int x, int y, int z) {
        BlockState state=world.state(x,y,z);
        return !is(state,"water")&&world.registry().definition(state).solidForFluid();
    }
    private static int depth(BlockState state) {
        int level=Integer.parseInt(state.property("level","0"));
        return level>=8?0:level;
    }
    private static boolean is(BlockState state, String fluid) {
        return state.block().namespace().equals("minecraft")&&state.block().path().equals(fluid);
    }
    private static int floor(double value) { return (int)Math.floor(value); }
}
