package cn.fandmc.epoch.network.codec;

import cn.fandmc.epoch.network.ProtocolException;
import java.io.IOException;

public final class PacketFields {
    private PacketFields() {}

    public static byte[] byteArray(PacketReader input, int maximum) throws IOException {
        int length = input.varInt();
        if (length < 0 || length > maximum) throw new ProtocolException("Byte array exceeds limit: " + length);
        return input.bytes(length);
    }

    public static void byteArray(PacketWriter output, byte[] data) throws IOException {
        output.varInt(data.length).bytes(data);
    }

    public static void dimension(int dimension) {
        if (dimension < -1 || dimension > 1) throw new IllegalArgumentException("Invalid dimension: " + dimension);
    }

    public static void position(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Non-finite player position");
    }

    public static void rotation(float yaw, float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch))
            throw new IllegalArgumentException("Non-finite player rotation");
    }
}
