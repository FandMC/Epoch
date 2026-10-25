package cn.fandmc.epoch.block;

/** Collision policy is explicit and immutable; no window or networking types leak into the API. */
public record BlockCollisionContext(boolean modernLadders) {
    public static final BlockCollisionContext LEGACY=new BlockCollisionContext(false);
    public static final BlockCollisionContext MODERN_LADDERS=new BlockCollisionContext(true);
}
