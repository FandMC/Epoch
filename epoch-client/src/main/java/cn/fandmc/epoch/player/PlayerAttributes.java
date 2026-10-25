package cn.fandmc.epoch.player;

import cn.fandmc.epoch.network.packet.play.S20PacketEntityProperties;
import java.util.List;
import java.util.UUID;

/** Server attributes evaluated in add, multiply-base, multiply-total order. */
public final class PlayerAttributes {
    private static final UUID SPRINT = UUID.fromString("662a6b8d-da3e-4c1c-8813-96ea6097278d");
    private double base = .1f;
    private List<S20PacketEntityProperties.Modifier> modifiers = List.of();

    public void reset() { base=.1f;modifiers=List.of(); }

    public void update(List<S20PacketEntityProperties.Property> properties) {
        for (var property : properties) if (property.key().equals("generic.movementSpeed")) {
            base = property.base();
            // Sprint is locally controlled; do not apply both the server and local modifier.
            modifiers = property.modifiers().stream().filter(m -> !m.id().equals(SPRINT)).toList();
        }
    }

    public float movementSpeed(boolean sprinting) {
        double value = base;
        for (var modifier : modifiers) if (modifier.operation()==0) value+=modifier.amount();
        double added = value;
        for (var modifier : modifiers) if (modifier.operation()==1) value+=added*modifier.amount();
        for (var modifier : modifiers) if (modifier.operation()==2) value*=1+modifier.amount();
        if (sprinting) value*=1+(double).3f;
        return (float)Math.clamp(value,0,1024);
    }
}
