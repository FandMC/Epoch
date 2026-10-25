package cn.fandmc.epoch.block.entity;

import cn.fandmc.epoch.nbt.NbtCompound;
import java.util.List;
import java.util.Objects;

/** Semantic updates: numeric packet action IDs stay in the network handler. */
public sealed interface BlockEntityUpdate {
    record SignText(List<String> lines) implements BlockEntityUpdate {
        public SignText {
            lines = List.copyOf(lines);
            if (lines.size() != 4) throw new IllegalArgumentException("A sign has four lines");
        }
    }
    record Skull(NbtCompound tag) implements BlockEntityUpdate {
        public Skull { Objects.requireNonNull(tag); }
    }
}
