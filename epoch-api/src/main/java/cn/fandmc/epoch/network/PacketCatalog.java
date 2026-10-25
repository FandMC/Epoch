package cn.fandmc.epoch.network;

import cn.fandmc.epoch.network.codec.PacketRegistry;
import cn.fandmc.epoch.network.listener.*;
import cn.fandmc.epoch.network.packet.handshake.C00Handshake;
import cn.fandmc.epoch.network.packet.login.*;
import cn.fandmc.epoch.network.packet.play.*;
import cn.fandmc.epoch.network.packet.status.*;

/** Packet registrations for the configured Minecraft release. */
public final class PacketCatalog {

    public static final PacketRegistry<ServerHandshakePacketListener> HANDSHAKE_SERVERBOUND =
            new PacketRegistry<ServerHandshakePacketListener>(ProtocolState.HANDSHAKE, PacketDirection.SERVERBOUND, 0x00)
                    .register(0x00, C00Handshake.class, C00Handshake::read).freeze();

    public static final PacketRegistry<ServerStatusPacketListener> STATUS_SERVERBOUND =
            new PacketRegistry<ServerStatusPacketListener>(ProtocolState.STATUS, PacketDirection.SERVERBOUND, 0x01)
                    .register(0x00, C00PacketServerQuery.class, C00PacketServerQuery::read)
                    .register(0x01, C01PacketPing.class, C01PacketPing::read).freeze();

    public static final PacketRegistry<ClientStatusPacketListener> STATUS_CLIENTBOUND =
            new PacketRegistry<ClientStatusPacketListener>(ProtocolState.STATUS, PacketDirection.CLIENTBOUND, 0x01)
                    .register(0x00, S00PacketServerInfo.class, S00PacketServerInfo::read)
                    .register(0x01, S01PacketPong.class, S01PacketPong::read).freeze();

    public static final PacketRegistry<ServerLoginPacketListener> LOGIN_SERVERBOUND =
            new PacketRegistry<ServerLoginPacketListener>(ProtocolState.LOGIN, PacketDirection.SERVERBOUND, 0x01)
                    .register(0x00, C00PacketLoginStart.class, C00PacketLoginStart::read)
                    .register(0x01, C01PacketEncryptionResponse.class, C01PacketEncryptionResponse::read).freeze();

    public static final PacketRegistry<ClientLoginPacketListener> LOGIN_CLIENTBOUND =
            new PacketRegistry<ClientLoginPacketListener>(ProtocolState.LOGIN, PacketDirection.CLIENTBOUND, 0x03)
                    .register(0x00, S00PacketDisconnect.class, S00PacketDisconnect::read)
                    .register(0x01, S01PacketEncryptionRequest.class, S01PacketEncryptionRequest::read)
                    .register(0x02, S02PacketLoginSuccess.class, S02PacketLoginSuccess::read)
                    .register(0x03, S03PacketEnableCompression.class, S03PacketEnableCompression::read).freeze();

    public static final PacketRegistry<ServerPlayPacketListener> PLAY_SERVERBOUND =
            new PacketRegistry<ServerPlayPacketListener>(ProtocolState.PLAY, PacketDirection.SERVERBOUND, 0x19)
                    .register(0x09, C09PacketHeldItemChange.class, C09PacketHeldItemChange::read)
                    .register(0x0d, C0DPacketCloseWindow.class, C0DPacketCloseWindow::read)
                    .register(0x0e, C0EPacketClickWindow.class, C0EPacketClickWindow::read)
                    .register(0x00, C00PacketKeepAlive.class, C00PacketKeepAlive::read)
                    .register(0x01, C01PacketChatMessage.class, C01PacketChatMessage::read)
                    .register(0x03, C03PacketPlayer.class, C03PacketPlayer::read)
                    .register(0x04, C04PacketPlayerPosition.class, C04PacketPlayerPosition::read)
                    .register(0x05, C05PacketPlayerLook.class, C05PacketPlayerLook::read)
                    .register(0x06, C06PacketPlayerPosLook.class, C06PacketPlayerPosLook::read)
                    .register(0x0b, C0BPacketEntityAction.class, C0BPacketEntityAction::read)
                    .register(0x0f, C0FPacketConfirmTransaction.class, C0FPacketConfirmTransaction::read)
                    .register(0x13, C13PacketPlayerAbilities.class, C13PacketPlayerAbilities::read)
                    .register(0x15, C15PacketClientSettings.class, C15PacketClientSettings::read)
                    .register(0x16, C16PacketClientStatus.class, C16PacketClientStatus::read)
                    .register(0x17, C17PacketCustomPayload.class, C17PacketCustomPayload::read)
                    .register(0x19, C19PacketResourcePackStatus.class, C19PacketResourcePackStatus::read).freeze();

    public static final PacketRegistry<ClientPlayPacketListener> PLAY_CLIENTBOUND =
            new PacketRegistry<ClientPlayPacketListener>(ProtocolState.PLAY, PacketDirection.CLIENTBOUND, 0x49)
                    .register(0x38, S38PacketPlayerListItem.class, S38PacketPlayerListItem::read)
                    .register(0x3e, S3EPacketTeams.class, S3EPacketTeams::read)
                    .register(0x09, S09PacketHeldItemChange.class, S09PacketHeldItemChange::read)
                    .register(0x1f, S1FPacketSetExperience.class, S1FPacketSetExperience::read)
                    .register(0x2d, S2DPacketOpenWindow.class, S2DPacketOpenWindow::read)
                    .register(0x2e, S2EPacketCloseWindow.class, S2EPacketCloseWindow::read)
                    .register(0x2f, S2FPacketSetSlot.class, S2FPacketSetSlot::read)
                    .register(0x30, S30PacketWindowItems.class, S30PacketWindowItems::read)
                    .register(0x31, S31PacketWindowProperty.class, S31PacketWindowProperty::read)
                    .register(0x3b, S3BPacketScoreboardObjective.class, S3BPacketScoreboardObjective::read)
                    .register(0x3c, S3CPacketUpdateScore.class, S3CPacketUpdateScore::read)
                    .register(0x3d, S3DPacketDisplayScoreboard.class, S3DPacketDisplayScoreboard::read)
                    .register(0x47, S47PacketPlayerListHeaderFooter.class, S47PacketPlayerListHeaderFooter::read)
                    .register(0x00, S00PacketKeepAlive.class, S00PacketKeepAlive::read)
                    .register(0x01, S01PacketJoinGame.class, S01PacketJoinGame::read)
                    .register(0x02, S02PacketChat.class, S02PacketChat::read)
                    .register(0x03, S03PacketTimeUpdate.class, S03PacketTimeUpdate::read)
                    .register(0x06, S06PacketUpdateHealth.class, S06PacketUpdateHealth::read)
                    .register(0x07, S07PacketRespawn.class, S07PacketRespawn::read)
                    .register(0x08, S08PacketPlayerPosLook.class, S08PacketPlayerPosLook::read)
                    .register(0x12, S12PacketEntityVelocity.class, S12PacketEntityVelocity::read)
                    .register(0x1d, S1DPacketEntityEffect.class, S1DPacketEntityEffect::read)
                    .register(0x1e, S1EPacketRemoveEntityEffect.class, S1EPacketRemoveEntityEffect::read)
                    .register(0x20, S20PacketEntityProperties.class, S20PacketEntityProperties::read)
                    .register(0x21, S21PacketChunkData.class, S21PacketChunkData::read)
                    .register(0x22, S22PacketMultiBlockChange.class, S22PacketMultiBlockChange::read)
                    .register(0x23, S23PacketBlockChange.class, S23PacketBlockChange::read)
                    .register(0x26, S26PacketMapChunkBulk.class, S26PacketMapChunkBulk::read)
                    .register(0x32, S32PacketConfirmTransaction.class, S32PacketConfirmTransaction::read)
                    .register(0x33, S33PacketUpdateSign.class, S33PacketUpdateSign::read)
                    .register(0x35, S35PacketUpdateBlockEntity.class, S35PacketUpdateBlockEntity::read)
                    .register(0x39, S39PacketPlayerAbilities.class, S39PacketPlayerAbilities::read)
                    .register(0x3f, S3FPacketCustomPayload.class, S3FPacketCustomPayload::read)
                    .register(0x40, S40PacketDisconnect.class, S40PacketDisconnect::read)
                    .register(0x46, S46PacketSetCompression.class, S46PacketSetCompression::read)
                    .register(0x48, S48PacketResourcePackSend.class, S48PacketResourcePackSend::read).freeze();

    private PacketCatalog() {}

    public static PacketRegistry<?> serverbound(ProtocolState state) {
        return switch (state) {
            case HANDSHAKE -> HANDSHAKE_SERVERBOUND;
            case STATUS -> STATUS_SERVERBOUND;
            case LOGIN -> LOGIN_SERVERBOUND;
            case PLAY -> PLAY_SERVERBOUND;
        };
    }
}
