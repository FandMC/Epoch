package cn.fandmc.epoch.network.codec;

import cn.fandmc.epoch.network.*;
import cn.fandmc.epoch.network.listener.PacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PacketRegistry<L extends PacketListener> {
    @FunctionalInterface
    public interface Decoder<P> { P read(PacketReader input) throws IOException; }

    private final ProtocolState state;
    private final PacketDirection direction;
    private final int maximumKnownId;
    private final Map<Integer, Decoder<? extends Packet<L>>> decoders = new HashMap<>();
    private final Map<Class<?>, Integer> ids = new HashMap<>();
    private boolean frozen;

    public PacketRegistry(ProtocolState state, PacketDirection direction, int maximumKnownId) {
        this.state = state;
        this.direction = direction;
        this.maximumKnownId = maximumKnownId;
    }

    public ProtocolState state() { return state; }
    public PacketDirection direction() { return direction; }

    public <P extends Packet<L>> PacketRegistry<L> register(int id, Class<P> type, Decoder<P> decoder) {
        if (frozen) throw new IllegalStateException("Packet registry is frozen");
        if (id < 0 || id > maximumKnownId || decoders.containsKey(id) || ids.containsKey(type))
            throw new IllegalArgumentException("Invalid or duplicate packet registration: " + id);
        decoders.put(id, decoder);
        ids.put(type, id);
        return this;
    }

    public PacketRegistry<L> freeze() { frozen = true; return this; }

    public Optional<Packet<L>> decode(byte[] data) throws IOException {
        PacketReader input = new PacketReader(data);
        int id = input.varInt();
        Decoder<? extends Packet<L>> decoder = decoders.get(id);
        if (decoder == null) {
            // 只允许跳过已知版本范围内尚未实现的 Play 包，不跨状态猜测 ID。
            if (state == ProtocolState.PLAY && direction == PacketDirection.CLIENTBOUND && id >= 0 && id <= maximumKnownId)
                return Optional.empty();
            throw new ProtocolException("Unregistered " + state + " " + direction + " packet: 0x" + Integer.toHexString(id));
        }
        try {
            Packet<L> packet = decoder.read(input);
            input.requireEnd();
            return Optional.of(packet);
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("Invalid " + state + " packet 0x" + Integer.toHexString(id) + ": " + e.getMessage(), e);
        }
    }

    public byte[] encode(Packet<?> packet) throws IOException {
        Integer id = ids.get(packet.getClass());
        if (id == null) throw new ProtocolException("Packet " + packet.getClass().getSimpleName() + " is not registered for " + state + " " + direction);
        PacketWriter output = new PacketWriter();
        output.varInt(id);
        packet.write(output);
        return output.toByteArray();
    }
}
