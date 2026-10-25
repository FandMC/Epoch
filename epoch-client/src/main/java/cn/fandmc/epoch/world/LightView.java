package cn.fandmc.epoch.world;

import cn.fandmc.epoch.block.BlockRegistry;

/** Read-only stored light and block data; implementations define their coordinate origin. */
public interface LightView {
    BlockRegistry.Definition lightDefinition(int x, int y, int z);
    int rawLight(int x, int y, int z, boolean sky);
    boolean hasSkylight();
}
