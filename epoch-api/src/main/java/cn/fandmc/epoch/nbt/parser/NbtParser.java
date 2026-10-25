package cn.fandmc.epoch.nbt.parser;

import cn.fandmc.epoch.nbt.*;
import java.io.*;
import java.util.*;

/** Bounded parser for uncompressed, named-root Java Edition NBT. Independent of packet codecs. */
public final class NbtParser {
    public record Limits(int maximumBytes, int maximumDepth, int maximumNodes, int maximumCollectionLength) {
        public static final Limits DEFAULT = new Limits(2_097_152, 64, 262_144, 262_144);
        public Limits {
            if (maximumBytes < 1 || maximumDepth < 1 || maximumNodes < 1 || maximumCollectionLength < 0)
                throw new IllegalArgumentException("Invalid NBT parser limits");
        }
    }

    private NbtParser() { }

    public static NbtDocument parse(byte[] bytes) throws IOException { return parse(bytes, Limits.DEFAULT); }
    public static NbtDocument parse(byte[] bytes, Limits limits) throws IOException {
        Prefix prefix = parsePrefix(bytes, limits);
        if (prefix.bytesRead() != bytes.length)
            throw new NbtParseException("Unexpected trailing NBT bytes: " + (bytes.length - prefix.bytesRead()));
        return prefix.document();
    }

    /** Reads one complete document and reports its length when more protocol fields follow. */
    public static Prefix parsePrefix(byte[] bytes) throws IOException { return parsePrefix(bytes, Limits.DEFAULT); }
    public static Prefix parsePrefix(byte[] bytes, Limits limits) throws IOException {
        return parsePrefix(bytes,0,bytes.length,limits);
    }

    /** Parses a document in a packet without copying the remaining slot array. */
    public static Prefix parsePrefix(byte[] bytes,int start,int length,Limits limits) throws IOException {
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(limits);
        Objects.checkFromIndexSize(start,length,bytes.length);
        Cursor input = new Cursor(bytes,start,Math.min(length,limits.maximumBytes()), limits);
        int rootType = input.unsignedByte();
        if (rootType > 11) throw input.error("Invalid NBT root type: " + rootType);
        if (rootType == 0) {
            return new Prefix(new NbtDocument("", null), input.offset);
        }
        String name = input.text();
        NbtTag root = input.payload(rootType, 0);
        return new Prefix(new NbtDocument(name, root), input.offset);
    }

    public record Prefix(NbtDocument document, int bytesRead) { }

    public static NbtCompound parseCompound(byte[] bytes) throws IOException {
        NbtDocument document = parse(bytes);
        if (document.root() == null) return NbtCompound.EMPTY;
        if (document.root() instanceof NbtCompound compound) return compound;
        throw new NbtParseException("NBT root is not a compound");
    }

    private static final class Cursor {
        private final DataInputStream input;
        private final int length;
        private final Limits limits;
        private int offset;
        private int nodes;

        Cursor(byte[] bytes,int start,int size, Limits limits) {
            input = new DataInputStream(new ByteArrayInputStream(bytes,start,size));
            length = size;
            this.limits = limits;
        }

        int remaining() { return length - offset; }
        NbtParseException error(String message) { return new NbtParseException(message + " at byte " + offset); }

        int unsignedByte() throws IOException {
            try { int value = input.readUnsignedByte(); offset++; return value; }
            catch (EOFException e) { throw new NbtParseException("Unexpected end of NBT at byte " + offset, e); }
        }
        byte byteValue() throws IOException { return (byte) unsignedByte(); }
        short shortValue() throws IOException { return (short) unsignedShort(); }
        int unsignedShort() throws IOException {
            int value = unsignedByte() << 8;
            return value | unsignedByte();
        }
        int intValue() throws IOException {
            return unsignedByte() << 24 | unsignedByte() << 16 | unsignedByte() << 8 | unsignedByte();
        }
        long longValue() throws IOException {
            return (long) intValue() << 32 | (intValue() & 0xffff_ffffL);
        }

        String text() throws IOException {
            int size = unsignedShort();
            if (size > remaining()) throw error("NBT string length exceeds remaining input");
            byte[] data = new byte[size + 2];
            data[0] = (byte) (size >>> 8);
            data[1] = (byte) size;
            try {
                byte[] encoded = input.readNBytes(size);
                if (encoded.length != size) throw new EOFException();
                offset += size;
                System.arraycopy(encoded, 0, data, 2, size);
                return new DataInputStream(new ByteArrayInputStream(data)).readUTF();
            } catch (UTFDataFormatException e) {
                throw new NbtParseException("Malformed modified UTF-8 at byte " + offset, e);
            } catch (EOFException e) {
                throw new NbtParseException("Truncated NBT string at byte " + offset, e);
            }
        }

        NbtTag payload(int type, int depth) throws IOException {
            if (++nodes > limits.maximumNodes()) throw error("NBT node limit exceeded");
            if (depth > limits.maximumDepth()) throw error("NBT nesting depth exceeded");
            try {
                return switch (type) {
                    case 1 -> NbtNumber.of(byteValue());
                    case 2 -> NbtNumber.of(shortValue());
                    case 3 -> NbtNumber.of(intValue());
                    case 4 -> NbtNumber.of(longValue());
                    case 5 -> NbtNumber.of(Float.intBitsToFloat(intValue()));
                    case 6 -> NbtNumber.of(Double.longBitsToDouble(longValue()));
                    case 7 -> byteArray();
                    case 8 -> new NbtString(text());
                    case 9 -> list(depth);
                    case 10 -> compound(depth);
                    case 11 -> intArray();
                    default -> throw error("Invalid NBT tag type: " + type);
                };
            } catch (IllegalArgumentException e) {
                throw new NbtParseException("Invalid NBT payload at byte " + offset, e);
            }
        }

        private NbtByteArray byteArray() throws IOException {
            int count = length(1);
            byte[] values = new byte[count];
            for (int i = 0; i < count; i++) values[i] = byteValue();
            return new NbtByteArray(values);
        }
        private NbtIntArray intArray() throws IOException {
            int count = length(4);
            int[] values = new int[count];
            for (int i = 0; i < count; i++) values[i] = intValue();
            return new NbtIntArray(values);
        }
        private NbtList list(int depth) throws IOException {
            int type = unsignedByte();
            if (type > 11) throw error("Invalid NBT list element type: " + type);
            int count = length(type == 0 ? 0 : 1);
            if (type == 0 && count != 0) throw error("Non-empty NBT list has end elements");
            List<NbtTag> values = new ArrayList<>(count);
            for (int i = 0; i < count; i++) values.add(payload(type, depth + 1));
            return new NbtList(type, values);
        }
        private NbtCompound compound(int depth) throws IOException {
            Map<String, NbtTag> values = new LinkedHashMap<>();
            while (true) {
                int type = unsignedByte();
                if (type == 0) return new NbtCompound(values);
                if (type > 11) throw error("Invalid NBT compound tag type: " + type);
                String name = text();
                if (values.containsKey(name)) throw error("Duplicate NBT compound key: " + name);
                values.put(name, payload(type, depth + 1));
            }
        }
        private int length(int elementBytes) throws IOException {
            int count = intValue();
            if (count < 0 || count > limits.maximumCollectionLength()
                    || (long) count * elementBytes > remaining())
                throw error("NBT collection length exceeds bounds: " + count);
            return count;
        }
        void requireEnd() throws NbtParseException {
            if (remaining() != 0) throw error("Unexpected trailing NBT bytes: " + remaining());
        }
    }
}
