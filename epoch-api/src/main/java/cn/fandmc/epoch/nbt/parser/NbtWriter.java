package cn.fandmc.epoch.nbt.parser;

import cn.fandmc.epoch.nbt.*;
import java.io.*;
import java.util.*;

/** Bounded serializer for named-root Java Edition NBT documents. */
public final class NbtWriter {
    private static final int MAX_BYTES = NbtParser.Limits.DEFAULT.maximumBytes();
    private NbtWriter() { }

    public static byte[] write(NbtDocument document) throws IOException {
        Objects.requireNonNull(document);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeByte(document.rootType());
        if (document.root() != null) {
            output.writeUTF(document.name());
            payload(output, document.root(), 0, new int[1]);
        }
        output.flush();
        if (bytes.size() > MAX_BYTES) throw new NbtParseException("Serialized NBT exceeds byte limit");
        return bytes.toByteArray();
    }

    private static void payload(DataOutputStream out, NbtTag tag, int depth, int[] nodes) throws IOException {
        if (depth > NbtParser.Limits.DEFAULT.maximumDepth()) throw new NbtParseException("NBT nesting depth exceeded");
        if (++nodes[0] > NbtParser.Limits.DEFAULT.maximumNodes()) throw new NbtParseException("NBT node limit exceeded");
        switch (tag.type()) {
            case 1 -> out.writeByte(((NbtNumber) tag).byteValue());
            case 2 -> out.writeShort(((NbtNumber) tag).shortValue());
            case 3 -> out.writeInt(((NbtNumber) tag).intValue());
            case 4 -> out.writeLong(((NbtNumber) tag).longValue());
            case 5 -> out.writeInt((int) ((NbtNumber) tag).bits());
            case 6 -> out.writeLong(((NbtNumber) tag).bits());
            case 7 -> {
                byte[] values = ((NbtByteArray) tag).values();
                collectionLength(values.length); out.writeInt(values.length); out.write(values);
            }
            case 8 -> out.writeUTF(((NbtString) tag).value());
            case 9 -> {
                NbtList list = (NbtList) tag;
                collectionLength(list.values().size()); out.writeByte(list.elementType()); out.writeInt(list.values().size());
                for (NbtTag value : list.values()) payload(out, value, depth + 1, nodes);
            }
            case 10 -> {
                Map<String, NbtTag> values = ((NbtCompound) tag).values();
                collectionLength(values.size());
                for (Map.Entry<String, NbtTag> entry : values.entrySet()) {
                    NbtTag value = entry.getValue();
                    out.writeByte(value.type()); out.writeUTF(entry.getKey()); payload(out, value, depth + 1, nodes);
                }
                out.writeByte(0);
            }
            case 11 -> {
                int[] values = ((NbtIntArray) tag).values();
                collectionLength(values.length); out.writeInt(values.length);
                for (int value : values) out.writeInt(value);
            }
            default -> throw new NbtParseException("Invalid NBT tag type: " + tag.type());
        }
        if (out.size() > MAX_BYTES) throw new NbtParseException("Serialized NBT exceeds byte limit");
    }

    private static void collectionLength(int count) throws NbtParseException {
        if (count > NbtParser.Limits.DEFAULT.maximumCollectionLength())
            throw new NbtParseException("NBT collection length exceeds bounds: " + count);
    }
}
