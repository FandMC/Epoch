package cn.fandmc.epoch.network.packet.play;

import cn.fandmc.epoch.network.ProtocolException;
import cn.fandmc.epoch.network.codec.*;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record S20PacketEntityProperties(int entityId, List<Property> properties) implements Packet<ClientPlayPacketListener> {
    public S20PacketEntityProperties { properties = List.copyOf(properties); }
    public record Modifier(UUID id, double amount, int operation) {
        public Modifier {
            if (id == null || !Double.isFinite(amount) || operation < 0 || operation > 2)
                throw new IllegalArgumentException("Invalid attribute modifier");
        }
    }
    public record Property(String key, double base, List<Modifier> modifiers) {
        public Property {
            if (key == null || key.length() > 64 || !Double.isFinite(base))
                throw new IllegalArgumentException("Invalid attribute property");
            modifiers = List.copyOf(modifiers);
        }
    }
    public static S20PacketEntityProperties read(PacketReader input) throws IOException {
        int entity = input.varInt(), count = input.intValue();
        if (count < 0 || count > input.remaining() / 10) throw new ProtocolException("Invalid property count");
        List<Property> properties = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String key = input.string(64);
            double base = input.doubleValue();
            int size = input.varInt();
            if (size < 0 || size > input.remaining() / 25) throw new ProtocolException("Invalid modifier count");
            List<Modifier> modifiers = new ArrayList<>(size);
            for (int j = 0; j < size; j++) modifiers.add(new Modifier(
                    new UUID(input.longValue(), input.longValue()), input.doubleValue(), input.unsignedByte()));
            properties.add(new Property(key, base, modifiers));
        }
        return new S20PacketEntityProperties(entity, properties);
    }
    @Override public void write(PacketWriter output) throws IOException {
        output.varInt(entityId).intValue(properties.size());
        for (Property property : properties) {
            output.string(property.key(), 64).doubleValue(property.base()).varInt(property.modifiers().size());
            for (Modifier modifier : property.modifiers()) output.longValue(modifier.id().getMostSignificantBits())
                    .longValue(modifier.id().getLeastSignificantBits()).doubleValue(modifier.amount()).byteValue(modifier.operation());
        }
    }
    @Override public void handle(ClientPlayPacketListener listener) throws IOException { listener.onEntityProperties(this); }
}
