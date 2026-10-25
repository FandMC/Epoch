package cn.fandmc.epoch.nbt;

import java.util.Arrays;

public final class NbtIntArray implements NbtTag {
    private final int[] values;
    public NbtIntArray(int[] values) { this.values = values.clone(); }
    public int[] values() { return values.clone(); }
    @Override public int type() { return 11; }
    @Override public boolean equals(Object other) { return other instanceof NbtIntArray array && Arrays.equals(values, array.values); }
    @Override public int hashCode() { return Arrays.hashCode(values); }
    @Override public String toString() { return Arrays.toString(values); }
}
