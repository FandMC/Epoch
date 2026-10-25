package cn.fandmc.epoch.network.listener;

import cn.fandmc.epoch.network.packet.play.*;
import java.io.IOException;

public interface ServerPlayPacketListener extends PacketListener {
    void onClickWindow(C0EPacketClickWindow packet) throws IOException;
    void onCloseWindow(C0DPacketCloseWindow packet) throws IOException;
    void onHeldItemChange(C09PacketHeldItemChange packet) throws IOException;
    void onConfirmTransaction(C0FPacketConfirmTransaction packet) throws IOException;
    void onEntityAction(C0BPacketEntityAction packet) throws IOException;
    void onPlayerAbilities(C13PacketPlayerAbilities packet) throws IOException;
    void onKeepAlive(C00PacketKeepAlive packet) throws IOException;
    void onChat(C01PacketChatMessage packet) throws IOException;
    void onPlayer(C03PacketPlayer packet) throws IOException;
    void onPlayerPosition(C04PacketPlayerPosition packet) throws IOException;
    void onPlayerLook(C05PacketPlayerLook packet) throws IOException;
    void onPlayerPositionAndLook(C06PacketPlayerPosLook packet) throws IOException;
    void onClientSettings(C15PacketClientSettings packet) throws IOException;
    void onClientStatus(C16PacketClientStatus packet) throws IOException;
    void onCustomPayload(C17PacketCustomPayload packet) throws IOException;
    void onResourcePackStatus(C19PacketResourcePackStatus packet) throws IOException;
}
