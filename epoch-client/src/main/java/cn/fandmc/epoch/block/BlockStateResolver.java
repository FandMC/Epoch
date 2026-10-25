package cn.fandmc.epoch.block;

import java.util.TreeMap;
import cn.fandmc.epoch.physics.Box;

/** Derives neighborhood properties without replacing the server's base state. Immutable after loading. */
public final class BlockStateResolver {
    @FunctionalInterface public interface Lookup { int state(int x, int y, int z); }
    private record Stair(int direction, boolean top, int[] shapes) {}
    private record Door(String type, boolean upper, int properties, int[] variants) {}
    private record Connections(Kind kind,int direction,int[] variants) {}
    private enum Kind { NONE,WOOD_FENCE,NETHER_FENCE,WALL,PANE,GATE }
    private static final int[] DX={1,0,-1,0}, DZ={0,1,0,-1};
    private static final String[] SHAPES={"straight","inner_left","inner_right","outer_left","outer_right"};
    private static final String[] DIRECTIONS={"east","south","west","north"};
    private final Stair[] stairs;
    private final Door[] doors;
    private final Connections[] connections;
    private final Kind[] kinds;
    private final boolean[] solidConnections,paneConnections,air;
    private final BlockState[] states;
    private final boolean[] normalCubes,providesPower;
    private final int[][] wires,repeaters;

    public BlockStateResolver(BlockRegistry registry) {
        states=new BlockState[registry.size()];normalCubes=new boolean[registry.size()];
        providesPower=new boolean[registry.size()];
        wires=new int[registry.size()][];repeaters=new int[registry.size()][];
        stairs=new Stair[registry.size()];
        doors=new Door[registry.size()];
        connections=new Connections[registry.size()];kinds=new Kind[registry.size()];
        solidConnections=new boolean[registry.size()];paneConnections=new boolean[registry.size()];air=new boolean[registry.size()];
        for (int handle=0;handle<stairs.length;handle++) {
            BlockState state=registry.state(handle);
            String name=state.block().path();
            Kind kind=kind(name);kinds[handle]=kind;air[handle]=state.isAir();
            var definition=registry.definition(handle);
            states[handle]=state;normalCubes[handle]=definition.normalCube();providesPower[handle]=definition.providesPower();
            if(name.equals("redstone_wire")) {
                int[] variants=new int[81];
                for(int v=0;v<81;v++) {
                    var properties=new TreeMap<>(state.properties());int index=v;
                    for(String d:DIRECTIONS){properties.put(d,new String[]{"none","side","up"}[index%3]);index/=3;}
                    variants[v]=registry.handle(new BlockState(state.block(),properties));
                }
                wires[handle]=variants;
            }
            if(name.equals("repeater")) {
                var properties=new TreeMap<>(state.properties());properties.put("locked","false");int unlocked=registry.handle(new BlockState(state.block(),properties));
                properties.put("locked","true");repeaters[handle]=new int[]{unlocked,registry.handle(new BlockState(state.block(),properties))};
            }
            if (name.endsWith("_door")) {
                int[] variants=new int[32];
                for (int variant=0;variant<variants.length;variant++) {
                    var properties=new TreeMap<>(state.properties());
                    properties.put("facing",DIRECTIONS[variant&3]);
                    properties.put("open",Boolean.toString((variant&4)!=0));
                    properties.put("hinge",(variant&8)!=0?"right":"left");
                    properties.put("powered",Boolean.toString((variant&16)!=0));
                    variants[variant]=registry.handle(new BlockState(state.block(),properties));
                }
                int properties=direction(state)|(state.property("open","false").equals("true")?4:0)
                        |(state.property("hinge","left").equals("right")?8:0)|(state.property("powered","false").equals("true")?16:0);
                doors[handle]=new Door(state.block().toString(),state.property("half","lower").equals("upper"),properties,variants);
            }
            boolean full=definition.collision().contains(new Box(0,0,0,1,1,1));
            boolean forbidden=name.equals("barrier")||name.equals("tnt");
            solidConnections[handle]=full&&definition.opaque()&&!forbidden
                    &&!name.equals("pumpkin")&&!name.equals("carved_pumpkin")&&!name.equals("jack_o_lantern")&&!name.equals("melon");
            paneConnections[handle]=!forbidden&&(full&&definition.opaque()||name.equals("glass")
                    ||name.endsWith("_stained_glass")||name.endsWith("_leaves"));
            if (kind!=Kind.NONE) {
                int[] variants=new int[kind==Kind.GATE?2:kind==Kind.WALL?32:16];
                for (int variant=0;variant<variants.length;variant++) {
                    var properties=new TreeMap<>(state.properties());
                    if (kind==Kind.GATE) properties.put("in_wall",Boolean.toString(variant!=0));
                    else {
                        for (int direction=0;direction<4;direction++) properties.put(DIRECTIONS[direction],Boolean.toString((variant&(1<<direction))!=0));
                        if (kind==Kind.WALL) properties.put("up",Boolean.toString((variant&16)!=0));
                    }
                    variants[variant]=registry.handle(new BlockState(state.block(),properties));
                }
                connections[handle]=new Connections(kind,direction(state),variants);
            }
            if (!state.block().path().endsWith("_stairs")) continue;
            int direction=direction(state);
            int[] variants=new int[SHAPES.length];
            for (int index=0;index<SHAPES.length;index++) {
                var properties=new TreeMap<>(state.properties());properties.put("shape",SHAPES[index]);
                variants[index]=registry.handle(new BlockState(state.block(),properties));
            }
            stairs[handle]=new Stair(direction,state.property("half","bottom").equals("top"),variants);
        }
    }

    public int resolve(int handle,int x,int y,int z,Lookup world) {
        if(wires[handle]!=null) {
            int mask=0,multiplier=1;boolean clearAbove=!normalCubes[world.state(x,y+1,z)];
            for(int d=0;d<4;d++) {
                int nx=x+DX[d],nz=z+DZ[d],neighbor=world.state(nx,y,nz),connection=0;
                if(redstoneConnects(neighbor,d)||!normalCubes[neighbor]&&wires[world.state(nx,y-1,nz)]!=null)connection=1;
                else if(clearAbove&&normalCubes[neighbor]&&wires[world.state(nx,y+1,nz)]!=null)connection=2;
                mask+=connection*multiplier;multiplier*=3;
            }
            return wires[handle][mask];
        }
        if(repeaters[handle]!=null) {
            int facing=direction(states[handle]);boolean locked=false;
            for(int d:new int[]{(facing+1)&3,(facing+3)&3}) {
                BlockState neighbor=states[world.state(x+DX[d],y,z+DZ[d])];
                String n=neighbor.block().path();
                // Diode FACING points towards its input; its output reaches the wire opposite it.
                if((n.equals("repeater")||n.equals("comparator"))&&direction(neighbor)==d&&neighbor.property("powered","false").equals("true"))locked=true;
            }
            return repeaters[handle][locked?1:0];
        }
        Door door=doors[handle];
        if (door!=null) {
            Door other=doors[world.state(x,y+(door.upper()?-1:1),z)];
            if (other==null||!door.type().equals(other.type())) return handle;
            int properties=door.upper()?(door.properties()&24)|(other.properties()&7)
                    :(door.properties()&7)|(other.properties()&24);
            return door.variants()[properties];
        }
        Connections connected=connections[handle];
        if (connected!=null) {
            if (connected.kind()==Kind.GATE) {
                int side=(connected.direction()+1)&3;
                boolean inWall=kinds[world.state(x+DX[side],y,z+DZ[side])]==Kind.WALL
                        ||kinds[world.state(x-DX[side],y,z-DZ[side])]==Kind.WALL;
                return connected.variants()[inWall?1:0];
            }
            int mask=0;
            for (int direction=0;direction<4;direction++) {
                int neighbor=world.state(x+DX[direction],y,z+DZ[direction]);
                if (connects(connected.kind(),neighbor)) mask|=1<<direction;
            }
            if (connected.kind()==Kind.WALL&&(mask!=5&&mask!=10||!air[world.state(x,y+1,z)])) mask|=16;
            return connected.variants()[mask];
        }
        Stair current=stairs[handle];
        if (current==null) return handle;
        int facing=current.direction();
        Stair front=stairs[world.state(x+DX[facing],y,z+DZ[facing])];
        if (corner(current,front)) {
            int side=(front.direction()+2)&3;
            if (!continues(current,stairs[world.state(x+DX[side],y,z+DZ[side])]))
                return current.shapes()[front.direction()==((facing+3)&3)?3:4];
        }
        Stair back=stairs[world.state(x-DX[facing],y,z-DZ[facing])];
        if (corner(current,back)) {
            int side=back.direction();
            if (!continues(current,stairs[world.state(x+DX[side],y,z+DZ[side])]))
                return current.shapes()[back.direction()==((facing+3)&3)?1:2];
        }
        return current.shapes()[0];
    }
    private static boolean corner(Stair a,Stair b) { return b!=null&&a.top()==b.top()&&(a.direction()&1)!=(b.direction()&1); }
    private boolean redstoneConnects(int handle,int direction) {
        String name=states[handle].block().path();
        if(name.equals("redstone_wire"))return true;
        if(name.equals("repeater"))return (direction(states[handle])&1)==(direction&1);
        return providesPower[handle];
    }
    private static boolean continues(Stair a,Stair b) { return b!=null&&a.top()==b.top()&&a.direction()==b.direction(); }
    private boolean connects(Kind current,int neighbor) {
        Kind other=kinds[neighbor];
        if (current==Kind.PANE) return other==Kind.PANE||paneConnections[neighbor];
        if (other==Kind.GATE) return true;
        return current==other||solidConnections[neighbor];
    }
    private static int direction(BlockState state) { return switch(state.property("facing","east")) { case "south"->1;case "west"->2;case "north"->3;default->0; }; }
    private static Kind kind(String name) {
        if (name.endsWith("_fence_gate")) return Kind.GATE;
        if (name.equals("nether_brick_fence")) return Kind.NETHER_FENCE;
        if (name.endsWith("_fence")) return Kind.WOOD_FENCE;
        if (name.endsWith("_wall")) return Kind.WALL;
        if (name.endsWith("_pane")||name.equals("iron_bars")) return Kind.PANE;
        return Kind.NONE;
    }
}
