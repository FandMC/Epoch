package cn.fandmc.epoch.network.handler;

import cn.fandmc.epoch.block.entity.BlockEntityUpdate;
import cn.fandmc.epoch.network.*;
import cn.fandmc.epoch.network.codec.PacketReader;
import cn.fandmc.epoch.network.listener.ClientPlayPacketListener;
import cn.fandmc.epoch.network.packet.play.*;
import cn.fandmc.epoch.world.ChunkColumn;
import cn.fandmc.epoch.block.BlockRegistry;
import cn.fandmc.epoch.network.mapping.LegacyBlockMapping;
import cn.fandmc.epoch.network.mapping.ChunkDecoder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PlayPacketHandler implements ClientPlayPacketListener {
    private final cn.fandmc.epoch.network.mapping.LegacyItemMapping items;
    private final GameSession session;
    private final int viewDistance;
    private final LegacyBlockMapping mapping;
    private final ChunkDecoder chunks;
    private int entityId;
    private int dimension;
    private boolean joined;

    public PlayPacketHandler(GameSession session, int viewDistance, BlockRegistry registry, LegacyBlockMapping mapping) {
        this.items=session.items();
        this.session = session;
        this.viewDistance = viewDistance;
        this.mapping = mapping;
        this.chunks = new ChunkDecoder(registry, mapping);
    }

    @Override public void onUpdateSign(S33PacketUpdateSign packet) throws IOException {
        requireJoined();session.emit(new SessionEvent.BlockEntity(packet.x(),packet.y(),packet.z(),
                new BlockEntityUpdate.SignText(packet.lines().stream().map(ChatText::plain).toList())));
    }
    @Override public void onUpdateBlockEntity(S35PacketUpdateBlockEntity packet) throws IOException {
        requireJoined();
        if(packet.action()==4)session.emit(new SessionEvent.BlockEntity(packet.x(),packet.y(),packet.z(),
                new BlockEntityUpdate.Skull(packet.data())));
    }
    @Override public void onKeepAlive(S00PacketKeepAlive packet) throws IOException { session.send(new C00PacketKeepAlive(packet.id())); }
    @Override public void onUpdateHealth(S06PacketUpdateHealth packet) throws IOException {
        session.emit(new SessionEvent.Health(packet.health(), packet.food(), packet.saturation()));
    }
    @Override public void onEntityProperties(S20PacketEntityProperties packet) throws IOException {
        session.emit(new SessionEvent.Properties(packet.entityId(), packet.properties()));
    }
    @Override public void onEntityEffect(S1DPacketEntityEffect packet) throws IOException {
        session.emit(new SessionEvent.Effect(packet.entityId(), packet.effectId(), packet.amplifier(), packet.duration(),packet.particles()));
    }
    @Override public void onRemoveEntityEffect(S1EPacketRemoveEntityEffect packet) throws IOException {
        session.emit(new SessionEvent.RemoveEffect(packet.entityId(), packet.effectId()));
    }
    @Override public void onConfirmTransaction(S32PacketConfirmTransaction packet) throws IOException {
        // Acknowledge only after earlier world/player events have reached the game thread.
        session.emit(new SessionEvent.Transaction(packet.windowId(), packet.actionNumber(), packet.accepted()));
    }
    @Override public void onTimeUpdate(S03PacketTimeUpdate packet)throws IOException{session.emit(new SessionEvent.Time(packet.worldAge(),packet.timeOfDay()));}
    @Override public void onEntityVelocity(S12PacketEntityVelocity packet)throws IOException{session.emit(new SessionEvent.Velocity(packet.entityId(),packet.x()/8000d,packet.y()/8000d,packet.z()/8000d));}
    @Override public void onPlayerAbilities(S39PacketPlayerAbilities packet)throws IOException{session.emit(new SessionEvent.Abilities(packet.flags(),packet.flySpeed(),packet.walkSpeed()));}

    @Override public void onJoinGame(S01PacketJoinGame packet) throws IOException {
        entityId = packet.entityId();
        dimension = packet.dimension();
        joined = true;
        session.send(new C15PacketClientSettings("zh_CN", viewDistance, 0, true, 127));
        session.send(C17PacketCustomPayload.brand("Epoch"));
        session.emit(new SessionEvent.Joined(entityId, dimension, packet.gameMode() & 7));
    }

    @Override public void onChat(S02PacketChat packet) throws IOException {
        session.emit(new SessionEvent.Chat(ChatText.plain(packet.json()), packet.position()));
    }

    @Override public void onRespawn(S07PacketRespawn packet) throws IOException {
        requireJoined();
        dimension = packet.dimension();
        session.emit(new SessionEvent.Respawned(dimension, packet.gameMode()));
    }

    @Override public void onPlayerPosition(S08PacketPlayerPosLook packet) throws IOException {
        requireJoined();
        session.emit(new SessionEvent.Position(packet));
    }

    @Override public void onChunkData(S21PacketChunkData packet) throws IOException {
        requireJoined();
        byte[] data = packet.data();
        if (packet.unload()) {
            if (data.length != 0 && data.length != 256) throw new ProtocolException("Invalid chunk unload payload");
            session.emit(new SessionEvent.Unload(packet.chunkX(), packet.chunkZ()));
        } else {
            ChunkColumn column = chunks.decode(packet.chunkX(), packet.chunkZ(), packet.sectionMask(), packet.groundUp(), dimension == 0, data);
            session.emit(new SessionEvent.Chunk(column, packet.groundUp()));
        }
    }

    @Override public void onMultiBlockChange(S22PacketMultiBlockChange packet) throws IOException {
        requireJoined();
        List<SessionEvent.BlockChange> changes = new ArrayList<>(packet.changes().size());
        for (S22PacketMultiBlockChange.Change change : packet.changes())
            changes.add(new SessionEvent.BlockChange((packet.chunkX() << 4) + change.x(), change.y(),
                    (packet.chunkZ() << 4) + change.z(), mapping.decode(change.blockState())));
        session.emit(new SessionEvent.Blocks(changes));
    }

    @Override public void onBlockChange(S23PacketBlockChange packet) throws IOException {
        requireJoined();
        session.emit(new SessionEvent.Blocks(List.of(new SessionEvent.BlockChange(packet.x(), packet.y(), packet.z(), mapping.decode(packet.blockState())))));
    }

    @Override public void onMapChunkBulk(S26PacketMapChunkBulk packet) throws IOException {
        requireJoined();
        for (S26PacketMapChunkBulk.Column column : packet.columns()) {
            ChunkColumn decoded = chunks.decode(column.x(), column.z(), column.sectionMask(), true, packet.skylight(), column.data());
            session.emit(new SessionEvent.Chunk(decoded, true));
        }
    }

    @Override public void onCustomPayload(S3FPacketCustomPayload packet) throws IOException {
        if (packet.channel().equals("MC|Brand")) {
            PacketReader input = new PacketReader(packet.data());
            String brand = input.string(32767);
            input.requireEnd();
            session.emit(new SessionEvent.ServerBrand(brand));
        } else {
            System.getLogger(PlayPacketHandler.class.getName()).log(System.Logger.Level.DEBUG,
                    "Unsupported plugin channel: " + packet.channel());
        }
    }

    @Override public void onDisconnect(S40PacketDisconnect packet) throws IOException {
        throw new ProtocolException("Disconnected: " + ChatText.plain(packet.reasonJson()));
    }
    @Override public void onSetCompression(S46PacketSetCompression packet) throws IOException { session.compression(packet.threshold()); }
    @Override public void onResourcePackSend(S48PacketResourcePackSend packet) throws IOException {
        session.send(new C19PacketResourcePackStatus(packet.hash(), C19PacketResourcePackStatus.Result.DECLINED));
    }

    private void requireJoined() throws ProtocolException {
        if (!joined) throw new ProtocolException("World packet received before Join Game");
    }

    @Override public void onPlayerListItem(S38PacketPlayerListItem p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onTeams(S3EPacketTeams p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onPlayerListHeaderFooter(S47PacketPlayerListHeaderFooter p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onScoreboardObjective(S3BPacketScoreboardObjective p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onUpdateScore(S3CPacketUpdateScore p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onDisplayScoreboard(S3DPacketDisplayScoreboard p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onOpenWindow(S2DPacketOpenWindow p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onCloseWindow(S2EPacketCloseWindow p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onWindowProperty(S31PacketWindowProperty p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onHeldItemChange(S09PacketHeldItemChange p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onExperience(S1FPacketSetExperience p)throws IOException {session.emit(new SessionEvent.Ui(p));}
    @Override public void onSetSlot(S2FPacketSetSlot p)throws IOException {session.emit(new SessionEvent.InventorySlot(p.windowId(),p.slot(),items.decode(p.item())));}
    @Override public void onWindowItems(S30PacketWindowItems p)throws IOException {
        var stacks=new ArrayList<cn.fandmc.epoch.item.ItemStack>(p.items().size());for(var item:p.items())stacks.add(items.decode(item));session.emit(new SessionEvent.InventoryItems(p.windowId(),stacks));
    }
}
