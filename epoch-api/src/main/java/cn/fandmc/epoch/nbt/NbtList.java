package cn.fandmc.epoch.nbt;

import java.util.List;

/** A list retains its declared element type even when it has no values. */
public record NbtList(int elementType, List<NbtTag> values) implements NbtTag {
    public NbtList {
        if (elementType < 0 || elementType > 11) throw new IllegalArgumentException("Invalid NBT list element type");
        values = List.copyOf(values);
        if (!values.isEmpty() && elementType == 0) throw new IllegalArgumentException("A non-empty NBT list cannot contain end tags");
        for (NbtTag value : values)
            if (value.type() != elementType) throw new IllegalArgumentException("NBT list element type mismatch");
    }
    @Override public int type() { return 9; }
}
