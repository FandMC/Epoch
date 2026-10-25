package cn.fandmc.epoch.render;

import cn.fandmc.epoch.block.BlockState;

/** Tint providers are selected by block identity, never just by the presence of tintindex. */
public final class BlockColors {
    private BlockColors() {}
    public static float[] tint(BlockState state,boolean tinted) {
        if(!tinted)return new float[]{1,1,1};
        String block=state.block().path();
        if(block.equals("redstone_wire")){
            int power=Math.clamp(Integer.parseInt(state.property("power","0")),0,15);
            float strength=power/15f;
            float red=power==0?.3f:strength*.6f+.4f;
            float green=Math.max(0,strength*strength*.7f-.5f);
            float blue=Math.max(0,strength*strength*.6f-.7f);
            // BlockRedstoneWire.colorMultiplier packs 8-bit components.
            return new float[]{(int)(red*255)/255f,(int)(green*255)/255f,(int)(blue*255)/255f};
        }
        if(block.equals("spruce_leaves"))return rgb(0x619961);
        if(block.equals("birch_leaves"))return rgb(0x80a755);
        if(block.equals("grass_block")||block.contains("grass")||block.contains("fern")
                ||block.endsWith("_leaves")||block.equals("vine")||block.equals("sugar_cane")
                ||block.equals("lily_pad"))return new float[]{.56f,.79f,.32f};
        return new float[]{1,1,1};
    }
    private static float[] rgb(int value){return new float[]{(value>>16&255)/255f,(value>>8&255)/255f,(value&255)/255f};}
}
