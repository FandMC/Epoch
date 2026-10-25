package cn.fandmc.epoch.block;

/** Optical properties are independent of collision shapes and texture alpha. */
public record BlockLighting(int emission, int opacity, boolean neighborBrightness,
                            boolean opaqueCube, boolean fullCube, boolean translucent, float ambientOcclusion) { }
