package cn.fandmc.epoch.network.packet.login;

import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientLoginPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;

public record S01PacketEncryptionRequest(String serverId, byte[] publicKey, byte[] verifyToken) implements Packet<ClientLoginPacketListener> {
    public S01PacketEncryptionRequest {
        if (serverId == null || serverId.length() > 20 || publicKey.length == 0 || publicKey.length > 4096
                || verifyToken.length == 0 || verifyToken.length > 4096) throw new IllegalArgumentException("Invalid encryption request");
        publicKey = publicKey.clone();
        verifyToken = verifyToken.clone();
    }
    @Override public byte[] publicKey() { return publicKey.clone(); }
    @Override public byte[] verifyToken() { return verifyToken.clone(); }
    public static S01PacketEncryptionRequest read(PacketReader input) throws IOException {
        return new S01PacketEncryptionRequest(input.string(20), PacketFields.byteArray(input, 4096), PacketFields.byteArray(input, 4096));
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.string(serverId, 20);
        PacketFields.byteArray(output, publicKey);
        PacketFields.byteArray(output, verifyToken);
    }
    @Override public void handle(ClientLoginPacketListener listener) throws IOException { listener.onEncryptionRequest(this); }
}
