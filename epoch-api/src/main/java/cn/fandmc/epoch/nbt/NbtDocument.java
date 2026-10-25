package cn.fandmc.epoch.nbt;

import java.util.Objects;

/** A named NBT root. A null root represents TAG_End, used for absent item data. */
public record NbtDocument(String name, NbtTag root) {
    public NbtDocument { Objects.requireNonNull(name); }
    public int rootType() { return root == null ? 0 : root.type(); }
}
