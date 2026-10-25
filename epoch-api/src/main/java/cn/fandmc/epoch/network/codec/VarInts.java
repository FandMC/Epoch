package cn.fandmc.epoch.network.codec;

import cn.fandmc.epoch.network.ProtocolException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class VarInts {
    private VarInts() {}

    public static int read(InputStream input) throws IOException {
        int result = 0;
        for (int index = 0; index < 5; index++) {
            int next = input.read();
            if (next == -1) throw new EOFException("Truncated VarInt");
            if (index == 4 && (next & 0xf0) != 0) throw new ProtocolException("VarInt exceeds 32 bits");
            result |= (next & 0x7f) << (7 * index);
            if ((next & 0x80) == 0) return result;
        }
        throw new ProtocolException("VarInt exceeds five bytes");
    }

    public static void write(OutputStream output, int value) throws IOException {
        do {
            int next = value & 0x7f;
            value >>>= 7;
            output.write(value == 0 ? next : next | 0x80);
        } while (value != 0);
    }
}
