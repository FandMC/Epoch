package cn.fandmc.epoch.network;

import cn.fandmc.epoch.config.ClientConfig;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.network.mapping.LegacyBlockMapping;
import cn.fandmc.epoch.network.handler.LoginPacketHandler;
import cn.fandmc.epoch.network.handler.PlayPacketHandler;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import cn.fandmc.epoch.network.packet.handshake.C00Handshake;
import cn.fandmc.epoch.network.packet.login.C00PacketLoginStart;
import cn.fandmc.epoch.network.packet.login.S02PacketLoginSuccess;
import cn.fandmc.epoch.network.transport.Connection;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GameSession implements AutoCloseable {
    private static final System.Logger LOG = System.getLogger(GameSession.class.getName());
    private final ClientConfig config;
    private final BlockRegistry registry;
    private final LegacyBlockMapping mapping;
    private final cn.fandmc.epoch.network.mapping.LegacyItemMapping items;
    public cn.fandmc.epoch.network.mapping.LegacyItemMapping items(){return items;}
    private final Connection connection = new Connection();
    private final ArrayBlockingQueue<SessionEvent> events = new ArrayBlockingQueue<>(256);
    private final ArrayBlockingQueue<java.util.List<Packet<ServerPlayPacketListener>>> outgoing = new ArrayBlockingQueue<>(256);
    /** Game-thread input actions, drained immediately before that tick's movement. */
    private final java.util.ArrayDeque<Packet<ServerPlayPacketListener>> tickActions = new java.util.ArrayDeque<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile String failure;
    private volatile long skippedPackets;
    private Thread reader;
    private Thread writer;

    public GameSession(ClientConfig config, BlockRegistry registry) throws IOException {
        this.config = config; this.registry = registry; this.mapping = new LegacyBlockMapping(registry);this.items=new cn.fandmc.epoch.network.mapping.LegacyItemMapping();
    }
    public boolean playing() { return connection.state() == ProtocolState.PLAY && !closed.get(); }
    public boolean closed() { return closed.get(); }
    public String failure() { return failure; }
    public long skippedPackets() { return skippedPackets; }
    public SessionEvent poll() { return events.poll(); }

    public synchronized void start() {
        if (reader != null || closed.get()) throw new IllegalStateException("Session already started or closed");
        reader = Thread.ofVirtual().name("epoch-network-reader").start(this::readLoop);
    }

    private void readLoop() {
        try {
            connection.connect(config.host(), config.port(), 5000);
            connection.send(new C00Handshake(ProtocolVersion.CURRENT.wireVersion(), config.host(), config.port(), ProtocolState.LOGIN));
            connection.send(new C00PacketLoginStart(config.username()));
            connection.readTimeout(30000);
            LoginPacketHandler login = new LoginPacketHandler(this);
            PlayPacketHandler play = new PlayPacketHandler(this, config.viewDistance(), registry, mapping);
            while (!closed.get()) {
                if (connection.state() == ProtocolState.LOGIN) {
                    var packet = connection.receive(PacketCatalog.LOGIN_CLIENTBOUND).orElseThrow();
                    packet.handle(login);
                } else {
                    var packet = connection.receive(PacketCatalog.PLAY_CLIENTBOUND);
                    if (packet.isPresent()) packet.get().handle(play);
                    else skippedPackets++;
                }
            }
        } catch (IOException | RuntimeException e) {
            fail(e);
        } finally { close(); }
    }

    public void loginSucceeded(S02PacketLoginSuccess packet) throws IOException {
        connection.enterPlay();
        writer = Thread.ofVirtual().name("epoch-network-writer").start(this::writeLoop);
        emit(new SessionEvent.LoggedIn(packet.uuid(), packet.username()));
    }

    public void compression(int threshold) throws IOException { connection.compression(threshold); }

    public void emit(SessionEvent event) throws IOException {
        try {
            if (!events.offer(event, 5, TimeUnit.SECONDS)) throw new IOException("Game event queue overloaded");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while delivering game event", e);
        }
    }

    private void writeLoop() {
        try {
            while (!closed.get()) for(var packet:outgoing.take())connection.send(packet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            fail(e);
            close();
        }
    }

    public void send(Packet<ServerPlayPacketListener> packet) throws IOException {
        if (!playing()) throw new IOException("Not in Play state");
        if (!outgoing.offer(java.util.List.of(packet))) throw new IOException("Outgoing packet queue overloaded");
    }

    /** GLFW callbacks must not send gameplay actions between movement and acknowledgements. */
    public void queueTickAction(Packet<ServerPlayPacketListener> packet) throws IOException {
        if(!playing())throw new IOException("Not in Play state");
        if(packet instanceof cn.fandmc.epoch.network.packet.play.C09PacketHeldItemChange
                &&tickActions.peekLast() instanceof cn.fandmc.epoch.network.packet.play.C09PacketHeldItemChange)tickActions.removeLast();
        if(tickActions.size()>=128)throw new IOException("Too many queued input actions");
        tickActions.addLast(packet);
    }

    /** Enqueues a complete client tick as one writer unit, with movement last. */
    public void sendTick(java.util.List<Packet<ServerPlayPacketListener>> movement) throws IOException {
        if(!playing())throw new IOException("Not in Play state");
        var batch=new java.util.ArrayList<Packet<ServerPlayPacketListener>>(tickActions.size()+movement.size());
        batch.addAll(tickActions);batch.addAll(movement);
        if(!outgoing.offer(java.util.List.copyOf(batch)))throw new IOException("Outgoing packet queue overloaded");
        tickActions.clear();
    }

    private void fail(Exception error) {
        if (closed.get()) return;
        failure = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        LOG.log(System.Logger.Level.WARNING, "Session ended: " + failure);
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { connection.close(); }
        catch (IOException e) { LOG.log(System.Logger.Level.DEBUG, "Socket close failed", e); }
        if (writer != null) writer.interrupt();
        if (reader != null && reader != Thread.currentThread()) reader.interrupt();
    }
}
