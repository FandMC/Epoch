# Network and game-state boundary

Minecraft Java Edition 1.8.9 uses wire protocol 47. ProtocolVersion owns this
version metadata; PacketCatalog owns packet registrations. The api module
contains no rendering, networking transport, Gson, or Minecraft engine classes.
Each implemented Cxx/Sxx packet has immutable fields, read/write methods, and
typed listener dispatch. The client module owns actual behavior.

## Implemented packet registrations

IDs below are hexadecimal. This is a coverage list, not full protocol support.

| State | Serverbound | Clientbound |
| --- | --- | --- |
| Handshake | 00 Handshake | none |
| Status | 00 Query, 01 Ping | 00 ServerInfo, 01 Pong |
| Login | 00 LoginStart, 01 EncryptionResponse | 00 Disconnect, 01 EncryptionRequest, 02 LoginSuccess, 03 EnableCompression |
| Play | 00 KeepAlive, 01 Chat, 03 Player, 04 Position, 05 Look, 06 PositionAndLook, 0B EntityAction, 0F ConfirmTransaction, 13 Abilities, 15 Settings, 16 ClientStatus, 17 CustomPayload, 19 ResourcePackStatus | 00 KeepAlive, 01 Join, 02 Chat, 03 Time, 06 Health, 07 Respawn, 08 PositionAndLook, 12 Velocity, 1D Effect, 1E RemoveEffect, 20 Properties, 21 Chunk, 22 MultiBlockChange, 23 BlockChange, 26 ChunkBulk, 32 ConfirmTransaction, 39 Abilities, 3F CustomPayload, 40 Disconnect, 46 Compression, 48 ResourcePack |

Encryption message fields exist, but authentication/encrypted login is not
implemented. Resource pack requests are declined. Other known Play IDs are
skipped as complete frames and counted; unsupported Login/Status messages fail.

## Runtime ordering

Connection performs framing/compression and registry encoding/decoding.
GameSession has bounded reader/writer/event queues. PlayPacketHandler publishes
immutable events; GameState applies them on the game thread before movement.
KeepAlive is echoed promptly through the outgoing queue. A rejected transaction
for the player inventory is acknowledged only after earlier game events have
been applied. Other inventory windows are not implemented yet.

PlayerMovementSender remembers the last reported position and rotation. The
position threshold is squared displacement greater than 0.0009; twenty ticks
without position reporting trigger a subsequent reminder. Rotation is included
only when yaw/pitch changes. Ground-only packets continue every simulation tick.
A server correction receives a combined response with onGround=false and resets
the reporting baseline. Respawn resets movement, sprint/sneak and jump state.

Health zero switches GameState to DEAD. The death page requests C16 ClientStatus
with action PERFORM_RESPAWN (VarInt 0) once and enters RESPAWN_REQUESTED. S07 is
published as SessionEvent.Respawned; it resets the player and enters RESPAWNING.
Chunks survive same-dimension respawns and are cleared on dimension changes.
The subsequent S08 response completes the transition to ALIVE. Health updates
alone do not prematurely dismiss the waiting death page.

Entity attributes use add, multiply-base and multiply-total modifier operations.
The sprint UUID is applied locally once; the Abilities walkSpeed field is kept
separate from the movement attribute. Health/food and blindness gate sprinting;
jump boost changes jump velocity. Effect removal is server-authoritative.
This is not a complete potion/inventory implementation.

LegacyBlockMapping converts wire block IDs/metadata to named states. Stair
shape is not stored in 1.8 metadata: decode uses straight base state and the
neighbor resolver supplies render/collision shape. Encoding a resolved stair
normalizes the shape back to the same wire facing/half. Buttons preserve
floor/wall/ceiling in internal state and convert only when selecting old assets.
Wall, fence, pane and gate neighborhood properties are also derived locally,
then normalized back to the base state on encoding. Plain glass panes and iron
bars map all sixteen metadata values to their default named state; stained
panes keep the color. The optional preferred-wire encoding retains a compatible
alias without putting packed legacy IDs into world state.

Movement and packet tracing have been removed at the user's request.
Ordinary chat and failure logging remain enabled.

Play clientbound 33 UpdateSign and 35 UpdateBlockEntity are also registered.
The JDK-only NBT parser package handles legacy tags 1-11, modified UTF-8 strings,
bounded nesting and bounded allocation. Block-entity changes are applied on
the game thread; chunk replacement, block replacement and unload clear stale
data. Runtime world/block state remains flattened and namespaced.

Position-2 S02 chat messages update only Action Bar state for sixty ticks.
Normal/system chat still reaches the history and [CHAT] logger. Join and
respawn clear the prior Action Bar.

## References

The local 1.8.9 protocol archive and original local resource JSON are the primary
wire and asset references. Grim source was read to understand the reported
AimDuplicateLook, BadPacketsV and Simulation behavior; no Grim files were edited.
Gameplay uses independent Epoch implementations and has no Grim dependency.

Run ./gradlew.bat build from the project root. Query Status and connect from the
single desktop entry using ./gradlew.bat run. See VERIFICATION.md for results.
# HUD/window synchronization

Server packets now populate held items, XP, windows/slots/properties, player list,
teams, objectives, scores and TAB header/footer on the game thread. Wire SlotData
uses the standalone bounded NBT parser; LegacyItemMapping converts numeric item
variants to namespaced items before they enter gameplay. Item visuals and stack
limits derive from 614 variants in the locally extracted original registry.

GLFW callbacks queue gameplay actions instead of writing immediately. The writer
consumes a complete action + movement batch atomically relative to other queued
replies. This repairs the observed held-item Post ordering defect; it does not
disable checks or manufacture movement packets. KeepAlive remains responsive on
the network thread, and server transaction replies follow game-thread state apply.
