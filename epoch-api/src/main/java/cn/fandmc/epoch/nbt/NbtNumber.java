package cn.fandmc.epoch.nbt;

/** Numeric NBT value that retains its original byte, short, int, long, float or double type. */
public final class NbtNumber extends Number implements NbtTag {
    private final int type;
    private final long bits;

    private NbtNumber(int type, long bits) {
        if (type < 1 || type > 6) throw new IllegalArgumentException("Not an NBT numeric tag");
        this.type = type;
        this.bits = bits;
    }

    public static NbtNumber of(byte value) { return new NbtNumber(1, value); }
    public static NbtNumber of(short value) { return new NbtNumber(2, value); }
    public static NbtNumber of(int value) { return new NbtNumber(3, value); }
    public static NbtNumber of(long value) { return new NbtNumber(4, value); }
    public static NbtNumber of(float value) { return new NbtNumber(5, Float.floatToRawIntBits(value) & 0xffff_ffffL); }
    public static NbtNumber of(double value) { return new NbtNumber(6, Double.doubleToRawLongBits(value)); }
    public int type() { return type; }
    public long bits() { return bits; }

    @Override public int intValue() { return switch (type) {
        case 1, 2, 3, 4 -> (int) bits;
        case 5 -> (int) Float.intBitsToFloat((int) bits);
        case 6 -> (int) Double.longBitsToDouble(bits);
        default -> throw new AssertionError(type);
    }; }

    @Override public long longValue() { return switch (type) {
        case 1, 2, 3, 4 -> bits;
        case 5 -> (long) Float.intBitsToFloat((int) bits);
        case 6 -> (long) Double.longBitsToDouble(bits);
        default -> throw new AssertionError(type);
    }; }

    @Override public float floatValue() { return switch (type) {
        case 5 -> Float.intBitsToFloat((int) bits);
        case 6 -> (float) Double.longBitsToDouble(bits);
        default -> (float) bits;
    }; }

    @Override public double doubleValue() { return switch (type) {
        case 5 -> Float.intBitsToFloat((int) bits);
        case 6 -> Double.longBitsToDouble(bits);
        default -> bits;
    }; }

    @Override public byte byteValue() { return (byte) intValue(); }
    @Override public short shortValue() { return (short) intValue(); }
    @Override public boolean equals(Object other) {
        return other instanceof NbtNumber value && type == value.type && bits == value.bits;
    }
    @Override public int hashCode() { return 31 * type + Long.hashCode(bits); }
    @Override public String toString() {
        return type == 5 ? Float.toString(floatValue())
                : type == 6 ? Double.toString(doubleValue()) : Long.toString(bits);
    }
}
