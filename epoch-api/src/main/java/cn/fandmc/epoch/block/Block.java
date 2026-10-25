package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;
import cn.fandmc.epoch.physics.Box;
import java.util.List;
import java.util.Objects;

/** Immutable block-type behavior, shared by all states and positions of this identity. */
public class Block {
    private final ResourceId id;

    public Block(ResourceId id) { this.id = Objects.requireNonNull(id); }
    public final ResourceId id() { return id; }

    /** Resource-defined collision is the default; contextual blocks may select another shape. */
    public List<Box> collision(BlockState state, List<Box> shape, BlockCollisionContext context) {
        return shape;
    }

    public boolean climbable() { return false; }
    public float slipperiness() { return .6f; }
    public double landingVelocity(double verticalVelocity, boolean sneaking) { return 0; }

    /** Called once for each overlapping block after movement/collision resolution. */
    public void onInside(BlockContact contact) { }
}
