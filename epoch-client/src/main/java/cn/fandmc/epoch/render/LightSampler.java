package cn.fandmc.epoch.render;

import cn.fandmc.epoch.world.LightView;

/** Stored nibble light -> render light. Never changes or relights the received world. */
public final class LightSampler {
    private final LightView view;
    private final boolean includeDown;

    /** Chunk rendering includes all six neighbors; live block entities use the world lookup. */
    public LightSampler(LightView view, boolean includeDown) {
        this.view = view;
        this.includeDown = includeDown;
    }

    public int level(int x, int y, int z, boolean sky) {
        if (sky && !view.hasSkylight()) return 0;
        if (y < 0 || y > 255) return sky ? 15 : 0;
        if (!view.lightDefinition(x,y,z).lighting().neighborBrightness()) return view.rawLight(x,y,z,sky);
        int value = view.rawLight(x,y+1,z,sky);
        if (includeDown) value = Math.max(value,view.rawLight(x,y-1,z,sky));
        value = Math.max(value,view.rawLight(x-1,y,z,sky));
        value = Math.max(value,view.rawLight(x+1,y,z,sky));
        value = Math.max(value,view.rawLight(x,y,z-1,sky));
        return Math.max(value,view.rawLight(x,y,z+1,sky));
    }

    /** Separate 8-bit channels retain quarter-level precision during vertex averaging. */
    public int packed(int x, int y, int z) {
        var definition = view.lightDefinition(x,y,z);
        int value = combined(x,y,z,definition.lighting().emission());
        if (value == 0 && definition.state().block().path().endsWith("_slab"))
            return combined(x,y-1,z,view.lightDefinition(x,y-1,z).lighting().emission());
        return value;
    }

    private int combined(int x, int y, int z, int emission) {
        return (level(x,y,z,true) << 20) | (Math.max(emission,level(x,y,z,false)) << 4);
    }

    public static float block(int packed) { return (packed & 255) / 240f; }
    public static float sky(int packed) { return ((packed >>> 16) & 255) / 240f; }
}
