# Epoch

Independent Minecraft Java Edition 1.8.9 desktop client. Java 25, LWJGL 3,
GLFW, OpenGL and HumbleUI Skija; Windows x64; root package cn.fandmc.epoch.

## Build and run

Install a Windows x64 Java 25 JDK and use the checked-in Gradle Wrapper.
From the project root, run:

    python tools/import_assets.py
    .\gradlew.bat build
    .\gradlew.bat test
    .\gradlew.bat run

The importer reads the locally installed .minecraft/versions/1.8.9/1.8.9.jar
by default. Use --jar with an explicit local path if necessary. Existing custom
resources are preserved. The original installation remains unchanged.
The only application entry is cn.fandmc.epoch.Epoch. It opens the desktop game;
Status querying is a main-menu service. There are no probe/status launchers.
Tests are not added by default; the current Gradle test tasks report NO-SOURCE.

## Modules

- epoch-api: JDK-only wire types, packet codecs, registries, typed listeners and
  the Screen/ScreenHost/GuiGraphics lifecycle and input API, plus Block and
  BlockEntity behavior/state contracts and the typed NBT model/parser API.
- epoch-client: transport, game state, assets, OpenGL world rendering, Skija UI,
  input, movement, physics and configuration. Depends on epoch-api.

## Resources and state

The runtime reads prepared files from assets/ (configurable in run/config.json).
For example minecraft/textures/blocks/stone.png resolves under assets/.
It does not load Minecraft game classes or modify the original installation.
Local imported assets are excluded from version control and distributions.
Blockstates, model inheritance, textures, element transforms and animation
metadata are parsed from the supplied resources; unresolved models are reported
in run/logs/unresolved-models.txt.

World state uses namespaced block identities and properties. Runtime numeric
handles are registry indexes, not wire IDs. LegacyBlockMapping translates only
at the network boundary. Block behavior uses shared Block instances; the abstract
BlockEntity base extends Block, with per-position data held separately by the
world. Dedicated typed model providers handle chests, signs and skulls. See
docs/BLOCKS.md for registration, lifecycle and rendering responsibilities.
The independent NBT model, parser and writer are described in docs/NBT.md.
Stair shape and wall/fence/pane connections are derived
from neighboring blocks for both meshing and collision. Stair wire metadata
contains only facing and half. Ordinary glass panes and iron bars ignore the
metadata nibble as in 1.8.9; stained panes retain their sixteen colors.
Button attachment faces are converted to the original resource variants.
Rebuild the bundled mapping with python tools/import_block_registry.py using
the pinned data in tools/data/ and the local resource directory.

## Current capabilities and limits

The desktop window, OpenGL/Skija integration, server connection, chunk textures,
and basic movement have been observed running locally. The client has fixed
20 Hz movement, swept collision, stepping, sprint/sneak/jump controls, camera
interpolation, CPU mesh workers and bounded render-thread GPU uploads.
Movement reporting selects ground-only, position, look or combined packets.
Server attributes, health, relevant effect state, velocity, corrections and
inventory transaction acknowledgments are synchronized with the game thread.
Water/lava contact uses the player body box. Water currents, falling currents,
liquid drag, swim input and wall-assisted exits follow the local 1.8.9 behavior.
First water contact during movement also applies the entry current before
collision velocity resets and drag, without pushing already-wet ticks twice.
Material flags come from the local original registry rather than render opacity.
Pane collision uses 1.8.9 endpoints, including the isolated cross.
Ladders use the original 2/16 collision depth and post-move climb detection.
The optional modernLadderCollision setting uses 3/16 backend geometry, but
conflicts with old-client prediction; it is disabled by default and in the
current local configuration. A newer backend can still correct old-client
ladder movement even when Epoch matches the original client.
Soul-sand contact slows horizontal velocity before liquid drag. Input rotation
preserves the original float multiplication/division order.
Cobweb contact marks the next move for horizontal 0.25 and vertical float-0.05
displacement scaling, clears momentum and does not stack for adjacent webs.
Snow layers retain their original zero-height first-layer collision; lily pads
use the full-width 1/64-high legacy box.

Quartz variants, note blocks, repeaters/comparators and connected redstone use
their original resource variants. Redstone tint depends on power, not grass.
Chests, signs and skulls have a separate texture-batched renderer. S33 sign text
and S35 block-entity NBT supply sign lines, skull type, rotation and skin data.
Chest lids currently stay closed. Skull skins with a valid texture property
can load asynchronously; name-only profiles use the default skin.

Main menu, connection, pause, chat, inventory and death pages share the Screen API.
The HUD includes server scoreboard/team decorations, potion durations, TAB headers,
hotbar items, equipment, health, food and XP. Shared Skija panels use a downsampled
GPU backdrop and bounded native text caches. Inventory icons parse actual resource
models and are baked with OpenGL into a bounded cache. Basic left/right slot
operations and ordinary-storage Shift transfers synchronize with server windows.
Gameplay input actions are sent with their movement tick to preserve packet order.
Death offers respawn after twenty ticks, sends one request and waits for the
server's Respawn and Position packets. Same-dimension respawns retain chunks.

This is still an incomplete client. Enchanted water movement, context-dependent collision
outside the implemented stair/door/wall/fence/pane cases, entity rendering, inventory
advanced interaction, authentication, and complete gameplay compatibility remain work
in progress. Compiling or connecting does not demonstrate Grim compatibility.

## Controls and logs

WASD moves, Space jumps, Ctrl starts sprinting, Shift sneaks, T opens chat,
E opens inventory, 1-9 or the wheel select a hotbar slot, TAB shows the player list,
F3 toggles diagnostics, and Esc opens/closes the pause menu. Returning to the main menu disconnects.
The death page cannot be dismissed with Esc. Flight requires server permission.
Settings are in run/config.json; the default server is 127.0.0.1:25566.

Received chat is logged line by line with a [CHAT] prefix through the same
logging system used by the runtime. UTF-8 logs rotate under run/logs/epoch-*.log;
concurrent instances may receive an extra filename suffix. Messages are not
cut to the visible HUD width or removed from logs when the HUD history expires.
The console also receives the chat log messages.
Position-2 chat packets display only in the timed Action Bar above the hotbar
area; they are excluded from chat history and [CHAT] logging.

Movement and packet tracing have been removed. Normal chat, errors and runtime
logs remain available. Multiplayer fluid movement still requires verification.

See docs/GUI.md, docs/NETWORK.md and docs/VERIFICATION.md for implementation boundaries
and the distinction between build checks and actual gameplay verification.
