package cn.fandmc.epoch.nbt;

import java.util.*;

/** Deeply immutable compound tag with typed convenience accessors. */
public final class NbtCompound implements NbtTag {
    public static final NbtCompound EMPTY = new NbtCompound(Map.of());
    private final Map<String, NbtTag> values;

    /** Accepts tags and familiar Java scalar/list values, normalizing them to typed immutable tags. */
    public NbtCompound(Map<String, ?> values) {
        Map<String, NbtTag> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key), tag(value)));
        this.values = Collections.unmodifiableMap(copy);
    }

    private static NbtTag tag(Object value) {
        if (value instanceof NbtTag result) return result;
        if (value instanceof Byte number) return NbtNumber.of(number);
        if (value instanceof Short number) return NbtNumber.of(number);
        if (value instanceof Integer number) return NbtNumber.of(number);
        if (value instanceof Long number) return NbtNumber.of(number);
        if (value instanceof Float number) return NbtNumber.of(number);
        if (value instanceof Double number) return NbtNumber.of(number);
        if (value instanceof String text) return new NbtString(text);
        if (value instanceof byte[] array) return new NbtByteArray(array);
        if (value instanceof int[] array) return new NbtIntArray(array);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> compound = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                if (!(key instanceof String name)) throw new IllegalArgumentException("NBT compound keys must be strings");
                compound.put(name, item);
            });
            return new NbtCompound(compound);
        }
        if (value instanceof List<?> list) {
            List<NbtTag> result = list.stream().map(NbtCompound::tag).toList();
            int elementType = result.isEmpty() ? 0 : result.getFirst().type();
            return new NbtList(elementType, result);
        }
        throw new IllegalArgumentException("Unsupported NBT value: " + (value == null ? "null" : value.getClass().getName()));
    }

    @Override public int type() { return 10; }
    public Map<String, NbtTag> values() { return values; }
    public NbtTag get(String key) { return values.get(key); }
    public String string(String key, String fallback) {
        return values.get(key) instanceof NbtString text ? text.value() : fallback;
    }
    public int integer(String key, int fallback) {
        return values.get(key) instanceof NbtNumber number ? number.intValue() : fallback;
    }
    public NbtCompound compound(String key) {
        return values.get(key) instanceof NbtCompound compound ? compound : EMPTY;
    }
    public List<NbtTag> list(String key) {
        return values.get(key) instanceof NbtList list ? list.values() : List.of();
    }

    @Override public boolean equals(Object other) {
        return other instanceof NbtCompound compound && values.equals(compound.values);
    }
    @Override public int hashCode() { return values.hashCode(); }
    @Override public String toString() { return values.toString(); }
}
