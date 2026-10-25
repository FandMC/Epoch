package cn.fandmc.epoch.network.codec;

import cn.fandmc.epoch.network.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PacketWriter {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final DataOutputStream output = new DataOutputStream(bytes);

    public PacketWriter() {}
    public PacketWriter varInt(int value) throws IOException { VarInts.write(output, value); return this; }
    public PacketWriter intValue(int value) throws IOException { output.writeInt(value); return this; }
    public PacketWriter longValue(long value) throws IOException { output.writeLong(value); return this; }
    public PacketWriter shortValue(int value) throws IOException { output.writeShort(value); return this; }
    public PacketWriter byteValue(int value) throws IOException { output.writeByte(value); return this; }
    public PacketWriter bool(boolean value) throws IOException { output.writeBoolean(value); return this; }
    public PacketWriter doubleValue(double value) throws IOException { output.writeDouble(value); return this; }
    public PacketWriter floatValue(float value) throws IOException { output.writeFloat(value); return this; }
    public PacketWriter bytes(byte[] value) throws IOException { output.write(value); return this; }
    public PacketWriter string(String value, int maxCharacters) throws IOException {
        if (value.length() > maxCharacters) throw new ProtocolException("String exceeds character limit");
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        return varInt(encoded.length).bytes(encoded);
    }
    public byte[] toByteArray() { return bytes.toByteArray(); }
}
