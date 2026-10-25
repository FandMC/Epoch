package cn.fandmc.epoch.network.transport;

import cn.fandmc.epoch.network.*;
import cn.fandmc.epoch.network.codec.PacketRegistry;
import cn.fandmc.epoch.network.listener.PacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import cn.fandmc.epoch.network.packet.handshake.C00Handshake;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;

public final class Connection implements AutoCloseable {
    private final Socket socket = new Socket();
    private InputStream input;
    private OutputStream output;
    private volatile ProtocolState state = ProtocolState.HANDSHAKE;
    private volatile int compressionThreshold = -1;

    public void connect(String host, int port, int timeoutMillis) throws IOException {
        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
        socket.setSoTimeout(timeoutMillis);
        socket.setTcpNoDelay(true);
        input = new BufferedInputStream(socket.getInputStream());
        output = new BufferedOutputStream(socket.getOutputStream());
    }

    public ProtocolState state() { return state; }
    public void readTimeout(int timeoutMillis) throws IOException { socket.setSoTimeout(timeoutMillis); }

    public <L extends PacketListener> Optional<Packet<L>> receive(PacketRegistry<L> registry) throws IOException {
        if (registry.state() != state || registry.direction() != PacketDirection.CLIENTBOUND)
            throw new ProtocolException("Wrong decoder for connection state " + state);
        if (input == null) throw new IOException("Connection has not been opened");
        return registry.decode(PacketFraming.read(input, compressionThreshold));
    }

    public synchronized void send(Packet<?> packet) throws IOException {
        if (output == null || socket.isClosed()) throw new IOException("Connection is closed or not opened");
        PacketFraming.write(output, PacketCatalog.serverbound(state).encode(packet), compressionThreshold);
        if (packet instanceof C00Handshake handshake) state = handshake.nextState();
    }

    public synchronized void enterPlay() throws ProtocolException {
        if (state != ProtocolState.LOGIN) throw new ProtocolException("Play transition requires Login state");
        state = ProtocolState.PLAY;
    }

    public synchronized void compression(int threshold) throws ProtocolException {
        if (state != ProtocolState.LOGIN && state != ProtocolState.PLAY)
            throw new ProtocolException("Compression cannot change in " + state);
        if (threshold < -1) throw new ProtocolException("Invalid compression threshold");
        compressionThreshold = threshold;
    }

    // close 不获取发送锁，必须能打断阻塞的 socket write/read。
    @Override public void close() throws IOException { socket.close(); }
}
