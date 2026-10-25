package cn.fandmc.epoch.nbt;

import java.util.Arrays;

public final class NbtByteArray implements NbtTag {
    private final byte[] values;
    public NbtByteArray(byte[] values) { this.values = values.clone(); }
    public byte[] values() { return values.clone(); }
    @Override public int type() { return 7; }
    @Override public boolean equals(Object other) { return other instanceof NbtByteArray array && Arrays.equals(values, array.values); }
    @Override public int hashCode() { return Arrays.hashCode(values); }
    @Override public String toString() { return Arrays.toString(values); }
}
