package cn.fandmc.epoch.player;

import cn.fandmc.epoch.network.GameSession;
import cn.fandmc.epoch.network.listener.ServerPlayPacketListener;
import cn.fandmc.epoch.network.packet.Packet;
import cn.fandmc.epoch.network.packet.play.*;
import java.io.IOException;

/** Game-thread movement reporting. Positions accumulate relative to the last position sent. */
public final class PlayerMovementSender {
    private double x, y, z;
    private float yaw, pitch;
    private int positionTicks;
    private boolean initialized, sprinting, sneaking;

    public void reset() {
        initialized = sprinting = sneaking = false;
        positionTicks = 0;
    }

    public void acknowledge(GameSession session, PlayerState player) throws IOException {
        session.send(new C06PacketPlayerPosLook(player.x, player.y, player.z, player.yaw, player.pitch, false));
        rememberPosition(player);
        yaw = player.yaw;
        pitch = player.pitch;
        initialized = true;
        positionTicks = 0;
    }

    public void sendTick(GameSession session, int entityId, PlayerState player) throws IOException {
        var packets=new java.util.ArrayList<Packet<ServerPlayPacketListener>>(3);
        if (sprinting != player.sprinting) {
            packets.add(new C0BPacketEntityAction(entityId, player.sprinting
                    ? C0BPacketEntityAction.Action.START_SPRINTING : C0BPacketEntityAction.Action.STOP_SPRINTING, 0));
            sprinting = player.sprinting;
        }
        if (sneaking != player.sneaking) {
            packets.add(new C0BPacketEntityAction(entityId, player.sneaking
                    ? C0BPacketEntityAction.Action.START_SNEAKING : C0BPacketEntityAction.Action.STOP_SNEAKING, 0));
            sneaking = player.sneaking;
        }
        packets.add(next(player));
        session.sendTick(packets);
    }

    public Packet<ServerPlayPacketListener> next(PlayerState player) {
        double dx = player.x - x, dy = player.y - y, dz = player.z - z;
        // The reminder is checked before incrementing: twenty non-position ticks are allowed.
        boolean moved = !initialized || dx * dx + dy * dy + dz * dz > 9.0E-4 || positionTicks >= 20;
        boolean turned = !initialized || player.yaw != yaw || player.pitch != pitch;
        Packet<ServerPlayPacketListener> packet;
        if (moved && turned) packet = new C06PacketPlayerPosLook(player.x, player.y, player.z, player.yaw, player.pitch, player.onGround);
        else if (moved) packet = new C04PacketPlayerPosition(player.x, player.y, player.z, player.onGround);
        else if (turned) packet = new C05PacketPlayerLook(player.yaw, player.pitch, player.onGround);
        else packet = new C03PacketPlayer(player.onGround);
        positionTicks++;
        if (moved) { rememberPosition(player); positionTicks = 0; }
        if (turned) { yaw = player.yaw; pitch = player.pitch; }
        initialized = true;
        return packet;
    }

    private void rememberPosition(PlayerState player) {
        x = player.x; y = player.y; z = player.z;
    }
}
