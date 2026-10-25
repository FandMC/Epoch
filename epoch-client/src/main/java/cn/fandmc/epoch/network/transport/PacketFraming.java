package cn.fandmc.epoch.network.transport;

import cn.fandmc.epoch.network.ProtocolException;
import cn.fandmc.epoch.network.codec.PacketReader;
import cn.fandmc.epoch.network.codec.VarInts;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class PacketFraming {
    public static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;
    private PacketFraming() {}

    public static byte[] read(InputStream input, int threshold) throws IOException {
        int length = VarInts.read(input);
        if (length <= 0 || length > MAX_PACKET_SIZE) throw new ProtocolException("Invalid frame length: " + length);
        byte[] frame = input.readNBytes(length);
        if (frame.length != length) throw new EOFException("Truncated packet frame");
        if (threshold < 0) return frame;
        PacketReader reader = new PacketReader(frame);
        int inflatedLength = reader.varInt();
        byte[] payload = reader.bytes(reader.remaining());
        if (inflatedLength == 0) {
            if (payload.length == 0 || payload.length >= threshold) throw new ProtocolException("Invalid uncompressed packet size");
            return payload;
        }
        if (inflatedLength < threshold || inflatedLength > MAX_PACKET_SIZE || inflatedLength < 0)
            throw new ProtocolException("Invalid decompressed packet size: " + inflatedLength);
        try (Inflater inflater = new Inflater()) {
            inflater.setInput(payload);
            byte[] result = new byte[inflatedLength];
            int offset = 0;
            while (offset < result.length) {
                int count = inflater.inflate(result, offset, result.length - offset);
                if (count == 0) break;
                offset += count;
            }
            // 必须消费 zlib 尾部，拒绝声明长度不足、额外流和截断数据。
            byte[] extra = new byte[1];
            int overflow = inflater.inflate(extra);
            if (offset != inflatedLength || overflow != 0 || !inflater.finished() || inflater.getRemaining() != 0)
                throw new ProtocolException("Compressed packet length mismatch");
            return result;
        } catch (DataFormatException e) {
            throw new ProtocolException("Invalid zlib packet", e);
        }
    }

    public static void write(OutputStream output, byte[] packet, int threshold) throws IOException {
        if (packet.length == 0 || packet.length > MAX_PACKET_SIZE) throw new ProtocolException("Invalid outgoing packet size");
        byte[] frame = packet;
        if (threshold >= 0) {
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            if (packet.length < threshold) {
                VarInts.write(data, 0);
                data.write(packet);
            } else {
                VarInts.write(data, packet.length);
                try (Deflater deflater = new Deflater()) {
                    deflater.setInput(packet);
                    deflater.finish();
                    byte[] buffer = new byte[8192];
                    while (!deflater.finished()) data.write(buffer, 0, deflater.deflate(buffer));
                }
            }
            frame = data.toByteArray();
        }
        if (frame.length > MAX_PACKET_SIZE) throw new ProtocolException("Outgoing frame too large");
        VarInts.write(output, frame.length);
        output.write(frame);
        output.flush();
    }
}
