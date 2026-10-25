package cn.fandmc.epoch.network.packet.login;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ServerLoginPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record C01PacketEncryptionResponse(byte[] sharedSecret, byte[] verifyToken) implements Packet<ServerLoginPacketListener> {
    public C01PacketEncryptionResponse {
        if (sharedSecret.length == 0 || sharedSecret.length > 4096 || verifyToken.length == 0 || verifyToken.length > 4096)
            throw new IllegalArgumentException("Invalid encrypted response length");
        sharedSecret = sharedSecret.clone();
        verifyToken = verifyToken.clone();
    }
    @Override public byte[] sharedSecret() { return sharedSecret.clone(); }
    @Override public byte[] verifyToken() { return verifyToken.clone(); }
    public static C01PacketEncryptionResponse read(PacketReader input) throws IOException {
        return new C01PacketEncryptionResponse(PacketFields.byteArray(input, 4096), PacketFields.byteArray(input, 4096));
    }
    @Override public void write(PacketWriter output) throws IOException {
        PacketFields.byteArray(output, sharedSecret);
        PacketFields.byteArray(output, verifyToken);
    }
    @Override public void handle(ServerLoginPacketListener listener) throws IOException { listener.onEncryptionResponse(this); }
}
