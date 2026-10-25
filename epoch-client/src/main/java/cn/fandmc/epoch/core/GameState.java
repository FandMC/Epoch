package cn.fandmc.epoch.core;

import cn.fandmc.epoch.network.GameSession;
import cn.fandmc.epoch.network.SessionEvent;
import cn.fandmc.epoch.network.packet.play.C0FPacketConfirmTransaction;
import cn.fandmc.epoch.network.packet.play.C16PacketClientStatus;
import cn.fandmc.epoch.player.PlayerState;
import cn.fandmc.epoch.player.PlayerMovementSender;
import cn.fandmc.epoch.player.PlayerController;
import cn.fandmc.epoch.world.ClientWorld;
import cn.fandmc.epoch.block.BlockRegistry;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/** Owned exclusively by the game thread. Network threads publish immutable events. */
public final class GameState {
    public final cn.fandmc.epoch.hud.ScoreboardState scoreboard=new cn.fandmc.epoch.hud.ScoreboardState();
    public final cn.fandmc.epoch.hud.PlayerListState playerList=new cn.fandmc.epoch.hud.PlayerListState();
    public final cn.fandmc.epoch.hud.EffectState effects=new cn.fandmc.epoch.hud.EffectState();
    public final cn.fandmc.epoch.inventory.InventoryState inventory=new cn.fandmc.epoch.inventory.InventoryState();
    public int experienceLevel,totalExperience;public float experienceProgress;
    public long windowRevision;
    public enum LifeState { ALIVE, DEAD, RESPAWN_REQUESTED, RESPAWNING }
    private static final System.Logger LOG = System.getLogger(GameState.class.getName());
    public final ClientWorld world;
    public GameState(BlockRegistry registry) { world = new ClientWorld(registry); }
    public final PlayerState player = new PlayerState();
    public final PlayerMovementSender movementSender = new PlayerMovementSender();
    public final PlayerController movement = new PlayerController();
    private final Deque<String> chat = new ArrayDeque<>();
    public String username = "", brand = "";
    public int entityId, gameMode;
    public long receivedChunks, corrections, blockChanges;
    private LifeState lifeState=LifeState.ALIVE;
    private String actionBar="";
    private int actionBarTicks;
    public String actionBar(){return actionBar;}
    public int actionBarTicks(){return actionBarTicks;}
    public void tick(){world.tick();effects.tick();if(actionBarTicks>0&&--actionBarTicks==0)actionBar="";}

    public LifeState lifeState() { return lifeState; }
    public boolean deathScreenVisible() { return lifeState!=LifeState.ALIVE; }
    public void requestRespawn(GameSession session) throws IOException {
        if (lifeState!=LifeState.DEAD) return;
        session.send(new C16PacketClientStatus(C16PacketClientStatus.Action.PERFORM_RESPAWN));
        lifeState=LifeState.RESPAWN_REQUESTED;
    }

    public int drain(GameSession session) throws IOException {
        int count = 0;
        SessionEvent event;
        while (count < 512 && (event = session.poll()) != null) {
            switch (event) {
                case SessionEvent.Health e -> {
                    player.health=e.health();player.food=e.food();player.saturation=e.saturation();
                    if (lifeState==LifeState.ALIVE||lifeState==LifeState.DEAD)
                        lifeState=player.health<=0?LifeState.DEAD:LifeState.ALIVE;
                }
                case SessionEvent.Properties e -> { if(e.entityId()==entityId)player.attributes.update(e.values()); }
                case SessionEvent.Effect e -> { if(e.entityId()==entityId){player.effects.put(e.effectId(),e.amplifier());effects.put(e.effectId(),e.amplifier(),e.duration(),e.particles());} }
                case SessionEvent.RemoveEffect e -> { if(e.entityId()==entityId){player.effects.remove(e.effectId());effects.remove(e.effectId());} }
                case SessionEvent.Transaction e -> {
                    inventory.transaction(e.windowId(),e.actionNumber(),e.accepted());
                    if (!e.accepted() && (e.windowId()==0||e.windowId()==inventory.windowId())) session.send(new C0FPacketConfirmTransaction(e.windowId(),e.actionNumber(),true));
                }
                case SessionEvent.Time e -> world.time(e.day());
                case SessionEvent.Velocity e -> {
                    if(e.entityId()==entityId){player.velocityX=e.x();player.velocityY=e.y();player.velocityZ=e.z();}
                }
                case SessionEvent.Abilities e -> {
                    player.abilityFlags=e.flags();player.allowFlying=(e.flags()&4)!=0;player.flying=(e.flags()&2)!=0;
                    player.flySpeed=e.flySpeed();player.walkSpeed=e.walkSpeed();
                }
                case SessionEvent.LoggedIn e -> username = e.username();
                case SessionEvent.Joined e -> {
                    scoreboard.clear();playerList.clear();effects.clear();inventory.reset();windowRevision++;experienceLevel=totalExperience=0;experienceProgress=0;
                    actionBar="";actionBarTicks=0;
                    lifeState=LifeState.ALIVE;
                    entityId = e.entityId();
                    gameMode = e.gameMode();
                    world.reset(e.dimension());
                    player.reset();
                    movementSender.reset();
                    movement.reset();
                    player.noClip=gameMode==3;
                }
                case SessionEvent.Respawned e -> {
                    effects.clear();inventory.close();windowRevision++;
                    actionBar="";actionBarTicks=0;
                    boolean afterDeath=deathScreenVisible();
                    // Same-dimension respawns retain chunks, as the server may not resend them.
                    if (world.dimension()!=e.dimension()) world.reset(e.dimension());
                    gameMode=e.gameMode();
                    player.reset();
                    player.noClip=gameMode==3;
                    movementSender.reset();
                    movement.reset();
                    lifeState=afterDeath?LifeState.RESPAWNING:LifeState.ALIVE;
                }
                case SessionEvent.Position e -> {
                    player.correct(e.packet());
                    corrections++;
                    if (session.playing()) movementSender.acknowledge(session, player);
                    if (lifeState==LifeState.RESPAWNING) lifeState=LifeState.ALIVE;
                }
                case SessionEvent.Chunk e -> {
                    world.apply(e.column(), e.groundUp());
                    receivedChunks++;
                }
                case SessionEvent.Unload e -> world.unload(e.x(), e.z());
                case SessionEvent.Blocks e -> {
                    for (var change : e.changes()) world.setState(change.x(), change.y(), change.z(), change.state());
                    blockChanges += e.changes().size();
                }
                case SessionEvent.BlockEntity e -> world.updateBlockEntity(e.x(),e.y(),e.z(),e.update());
                case SessionEvent.Chat e -> {
                    if(e.position()==2){actionBar=e.text();actionBarTicks=60;}
                    else addChat(e.text());
                }
                case SessionEvent.ServerBrand e -> brand = e.brand();
                case SessionEvent.InventorySlot e -> inventory.set(e.window(),e.slot(),e.item());
                case SessionEvent.InventoryItems e -> inventory.items(e.window(),e.items());
                case SessionEvent.Ui e -> {
                    applyUi(e.packet());
                    if(e.packet() instanceof cn.fandmc.epoch.network.packet.play.S09PacketHeldItemChange p&&p.slot()>=0&&p.slot()<9)
                        session.queueTickAction(new cn.fandmc.epoch.network.packet.play.C09PacketHeldItemChange(p.slot()));
                }
            }
            count++;
        }
        return count;
    }

    public void addChat(String message) {
        for (String line : message.split("\\R")) {
            LOG.log(System.Logger.Level.INFO, "[CHAT] " + line);
            chat.addLast(line);
            while (chat.size() > 100) chat.removeFirst();
        }
    }

    public Deque<String> chat() { return chat; }
    private void applyUi(cn.fandmc.epoch.network.packet.Packet<?> packet){
        switch(packet){
            case cn.fandmc.epoch.network.packet.play.S38PacketPlayerListItem p -> playerList.update(p);
            case cn.fandmc.epoch.network.packet.play.S47PacketPlayerListHeaderFooter p -> playerList.header(p.header(),p.footer());
            case cn.fandmc.epoch.network.packet.play.S3EPacketTeams p -> scoreboard.team(p);
            case cn.fandmc.epoch.network.packet.play.S3BPacketScoreboardObjective p -> scoreboard.objective(p);
            case cn.fandmc.epoch.network.packet.play.S3CPacketUpdateScore p -> scoreboard.score(p);
            case cn.fandmc.epoch.network.packet.play.S3DPacketDisplayScoreboard p -> scoreboard.display(p);
            case cn.fandmc.epoch.network.packet.play.S09PacketHeldItemChange p -> inventory.selected(p.slot());
            case cn.fandmc.epoch.network.packet.play.S1FPacketSetExperience p -> {experienceLevel=p.level();totalExperience=p.total();experienceProgress=p.progress();}
            case cn.fandmc.epoch.network.packet.play.S2DPacketOpenWindow p -> {inventory.open(p.windowId(),p.type(),cn.fandmc.epoch.network.ChatText.formatted(p.title()),p.slots());windowRevision++;}
            case cn.fandmc.epoch.network.packet.play.S2EPacketCloseWindow p -> {if(p.windowId()==inventory.windowId()){inventory.close();windowRevision++;}}
            case cn.fandmc.epoch.network.packet.play.S31PacketWindowProperty p -> inventory.property(p.windowId(),p.property(),p.value());
            default -> throw new IllegalArgumentException("Not a UI state packet: "+packet.getClass().getSimpleName());
        }
    }
}
