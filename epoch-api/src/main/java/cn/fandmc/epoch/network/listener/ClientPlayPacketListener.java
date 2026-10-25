package cn.fandmc.epoch.network.listener;

import cn.fandmc.epoch.network.packet.play.*;
import java.io.IOException;

public interface ClientPlayPacketListener extends PacketListener {
    void onPlayerListItem(S38PacketPlayerListItem packet) throws IOException;
    void onTeams(S3EPacketTeams packet) throws IOException;
    void onPlayerListHeaderFooter(S47PacketPlayerListHeaderFooter packet) throws IOException;
    void onDisplayScoreboard(S3DPacketDisplayScoreboard packet) throws IOException;
    void onUpdateScore(S3CPacketUpdateScore packet) throws IOException;
    void onScoreboardObjective(S3BPacketScoreboardObjective packet) throws IOException;
    void onWindowProperty(S31PacketWindowProperty packet) throws IOException;
    void onWindowItems(S30PacketWindowItems packet) throws IOException;
    void onSetSlot(S2FPacketSetSlot packet) throws IOException;
    void onCloseWindow(S2EPacketCloseWindow packet) throws IOException;
    void onOpenWindow(S2DPacketOpenWindow packet) throws IOException;
    void onExperience(S1FPacketSetExperience packet) throws IOException;
    void onHeldItemChange(S09PacketHeldItemChange packet) throws IOException;
    void onUpdateSign(S33PacketUpdateSign packet) throws IOException;
    void onUpdateBlockEntity(S35PacketUpdateBlockEntity packet) throws IOException;
    void onUpdateHealth(S06PacketUpdateHealth packet) throws IOException;
    void onEntityProperties(S20PacketEntityProperties packet) throws IOException;
    void onEntityEffect(S1DPacketEntityEffect packet) throws IOException;
    void onRemoveEntityEffect(S1EPacketRemoveEntityEffect packet) throws IOException;
    void onConfirmTransaction(S32PacketConfirmTransaction packet) throws IOException;
    void onTimeUpdate(S03PacketTimeUpdate packet) throws IOException;
    void onEntityVelocity(S12PacketEntityVelocity packet) throws IOException;
    void onPlayerAbilities(S39PacketPlayerAbilities packet) throws IOException;
    void onKeepAlive(S00PacketKeepAlive packet) throws IOException;
    void onJoinGame(S01PacketJoinGame packet) throws IOException;
    void onChat(S02PacketChat packet) throws IOException;
    void onRespawn(S07PacketRespawn packet) throws IOException;
    void onPlayerPosition(S08PacketPlayerPosLook packet) throws IOException;
    void onChunkData(S21PacketChunkData packet) throws IOException;
    void onMultiBlockChange(S22PacketMultiBlockChange packet) throws IOException;
    void onBlockChange(S23PacketBlockChange packet) throws IOException;
    void onMapChunkBulk(S26PacketMapChunkBulk packet) throws IOException;
    void onCustomPayload(S3FPacketCustomPayload packet) throws IOException;
    void onDisconnect(S40PacketDisconnect packet) throws IOException;
    void onSetCompression(S46PacketSetCompression packet) throws IOException;
    void onResourcePackSend(S48PacketResourcePackSend packet) throws IOException;
}
