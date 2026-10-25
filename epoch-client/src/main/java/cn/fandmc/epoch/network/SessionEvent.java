package cn.fandmc.epoch.network;

import cn.fandmc.epoch.network.packet.play.S08PacketPlayerPosLook;
import cn.fandmc.epoch.network.packet.play.S20PacketEntityProperties;
import cn.fandmc.epoch.world.ChunkColumn;
import cn.fandmc.epoch.block.BlockState;
import java.util.List;
import java.util.UUID;

public sealed interface SessionEvent {
    record Ui(cn.fandmc.epoch.network.packet.Packet<cn.fandmc.epoch.network.listener.ClientPlayPacketListener> packet) implements SessionEvent { }
    record InventorySlot(int window,int slot,cn.fandmc.epoch.item.ItemStack item) implements SessionEvent { }
    record InventoryItems(int window,List<cn.fandmc.epoch.item.ItemStack> items) implements SessionEvent { public InventoryItems {items=List.copyOf(items);} }
    record BlockEntity(int x,int y,int z,cn.fandmc.epoch.block.entity.BlockEntityUpdate update) implements SessionEvent {}
    record Health(float health, int food, float saturation) implements SessionEvent {}
    record Properties(int entityId, List<S20PacketEntityProperties.Property> values) implements SessionEvent {
        public Properties { values = List.copyOf(values); }
    }
    record Effect(int entityId, int effectId, int amplifier, int duration,boolean particles) implements SessionEvent {}
    record RemoveEffect(int entityId, int effectId) implements SessionEvent {}
    record Transaction(int windowId, short actionNumber, boolean accepted) implements SessionEvent {}
    record Time(long age,long day) implements SessionEvent {}
    record Velocity(int entityId,double x,double y,double z) implements SessionEvent {}
    record Abilities(int flags,float flySpeed,float walkSpeed) implements SessionEvent {}
    record LoggedIn(UUID uuid, String username) implements SessionEvent {}
    record Joined(int entityId, int dimension, int gameMode) implements SessionEvent {}
    record Respawned(int dimension, int gameMode) implements SessionEvent {}
    record Position(S08PacketPlayerPosLook packet) implements SessionEvent {}
    record Chunk(ChunkColumn column, boolean groundUp) implements SessionEvent {}
    record Unload(int x, int z) implements SessionEvent {}
    record BlockChange(int x, int y, int z, BlockState state) {}
    record Blocks(List<BlockChange> changes) implements SessionEvent {
        public Blocks { changes = List.copyOf(changes); }
    }
    record Chat(String text, int position) implements SessionEvent {}
    record ServerBrand(String brand) implements SessionEvent {}
}
