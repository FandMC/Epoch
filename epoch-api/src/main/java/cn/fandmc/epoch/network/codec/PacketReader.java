package cn.fandmc.epoch.network.codec;

import cn.fandmc.epoch.network.ProtocolException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class PacketReader {
    private final byte[] data;
    private final ByteArrayInputStream bytes;
    private final DataInputStream input;

    public PacketReader(byte[] data) {
        this.data = data;
        bytes = new ByteArrayInputStream(data);
        input = new DataInputStream(bytes);
    }

    public int varInt() throws IOException { return VarInts.read(input); }
    public int intValue() throws IOException { return input.readInt(); }
    public long longValue() throws IOException { return input.readLong(); }
    public short shortValue() throws IOException { return input.readShort(); }
    public int unsignedShort() throws IOException { return input.readUnsignedShort(); }
    public int unsignedByte() throws IOException { return input.readUnsignedByte(); }
    public byte byteValue() throws IOException { return input.readByte(); }
    public boolean bool() throws IOException { return input.readBoolean(); }
    public double doubleValue() throws IOException { return input.readDouble(); }
    public float floatValue() throws IOException { return input.readFloat(); }
    public int remaining() { return bytes.available(); }
    public java.util.UUID uuid() throws IOException { return new java.util.UUID(longValue(),longValue()); }
    public cn.fandmc.epoch.nbt.NbtCompound compound() throws IOException {
        int start=data.length-remaining();
        var parsed=cn.fandmc.epoch.nbt.parser.NbtParser.parsePrefix(data,start,remaining(),cn.fandmc.epoch.nbt.parser.NbtParser.Limits.DEFAULT);
        input.skipNBytes(parsed.bytesRead());
        if(parsed.document().root()==null)return cn.fandmc.epoch.nbt.NbtCompound.EMPTY;
        if(parsed.document().root() instanceof cn.fandmc.epoch.nbt.NbtCompound compound)return compound;
        throw new ProtocolException("Slot NBT root must be a compound");
    }

    public byte[] bytes(int length) throws IOException {
        if (length < 0 || length > remaining()) throw new EOFException("Invalid byte array length: " + length);
        return input.readNBytes(length);
    }

    public String string(int maxCharacters) throws IOException {
        int length = varInt();
        if (length < 0 || length > (long) maxCharacters * 4) throw new ProtocolException("String length out of bounds");
        try {
            String value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes(length))).toString();
            if (value.length() > maxCharacters) throw new ProtocolException("String exceeds character limit");
            return value;
        } catch (CharacterCodingException e) {
            throw new ProtocolException("Malformed UTF-8", e);
        }
    }

    public void requireEnd() throws ProtocolException {
        if (remaining() != 0) throw new ProtocolException("Unexpected trailing packet bytes: " + remaining());
    }
}
