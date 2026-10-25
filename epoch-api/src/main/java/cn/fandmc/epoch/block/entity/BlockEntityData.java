package cn.fandmc.epoch.block.entity;

import cn.fandmc.epoch.nbt.NbtCompound;
import java.util.List;
import java.util.Objects;

/** Immutable data belonging to one world position, never stored on a shared BlockEntity type. */
public record BlockEntityData(List<String> signLines, NbtCompound tag) {
    public static final BlockEntityData EMPTY = new BlockEntityData(List.of(), NbtCompound.EMPTY);

    public BlockEntityData {
        signLines = List.copyOf(signLines);
        Objects.requireNonNull(tag);
    }

    public record Position(int x, int y, int z) { }
}
