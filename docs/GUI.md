# Screen API

The JDK-only API lives in epoch-api under cn.fandmc.epoch.gui. The desktop
client supplies rendering through SkiaGuiGraphics and window events through
GLFW; no native window or GPU class is exposed by the API.

- Screen: title, viewport size, open/close/resize lifecycle, tick, rendering,
  keyboard, Unicode code points, mouse and focus callbacks. Override
  closesOnEscape or showsHud to control page behavior.
- ScreenHost: current(), show(screen) and close(requester). Null selects
  gameplay. Closing an outdated page cannot dismiss its replacement.
- ScreenManager: one active page on its creating game thread. Lifecycle
  navigation is serialized, including requests made inside open/close hooks.
  Events consumed while a page is open do not leak into gameplay even when
  the callback closes the page.
- GuiGraphics: frame-scoped drawing in logical window pixels; ARGB colors,
  fill, rounded rectangles, text, measurement and clipping. Text y denotes
  its baseline. Balance save/restore and do not retain the frame adapter.
- Button and ButtonScreen: shared button geometry, enabled state, hit testing
  and rendering. KeyInput separates keyboard events from text input.

MainMenuScreen, ConnectionScreen, PauseScreen, ChatScreen and DeathScreen use
this lifecycle. Opening a page releases the cursor; returning to focused
gameplay recaptures it. Multiplayer simulation continues behind the pause
menu. Chat deletion operates on Unicode code points. Text measurement and
drawing share the font fallback path.

DeathScreen waits twenty simulation ticks before enabling respawn, prevents
duplicate requests while waiting and cannot be closed with Escape. The game
state owns the actual request and server-driven completion; see NETWORK.md.

Action Bar messages are a separate HUD layer, centered above the hotbar area.
They last sixty ticks, fade over the final twenty ticks, and do not appear in
chat history or [CHAT] logs. Join and respawn clear old messages.

The API supports character callbacks but does not yet provide Windows IME
composition/candidate integration or a complete general-purpose widget kit.
Local lifecycle/loopback checks and Skija raster previews are documented in
VERIFICATION.md. They are not a multiplayer respawn acceptance test.

## HUD and inventory refactor (2026-10-25)

The NyaClient reference informed the shared panel palette, rounded backgrounds,
text caching, inventory slot spacing and effect/scoreboard layouts. Epoch does
not depend on NyaClient, Minecraft classes, RenderPearl or its Module system.

- UiTheme and HudElement live in epoch-api. GuiGraphics now exposes resource
  sprites and flattened ItemStack drawing without exposing native objects.
- HudRenderer composes ScoreboardHud, EffectsHud, PlayerListHud and HotbarHud.
  Scores/teams, player entries/header/footer, effect durations, experience,
  health, inventory slots and the selected slot come from server packets.
- Scoreboard and TAB snapshots are rebuilt on state revisions. FontBook keeps
  up to 1,024 native TextBlob layouts, including legacy colors/styles and
  system Unicode fallback. This is glyph placement, not a complex-script shaper.
- HudBackdrop captures the scene before UI, downsamples to 3/8 resolution and
  shares one GPU blur across panels. It refreshes every other frame; resizing
  invalidates it. Surfaces, snapshots, filters and fonts are explicitly closed.
- ItemModels reads original item JSON, texture variables, inherited elements
  and GUI transforms. ModelGeometry is shared with world models. ItemIcons
  bakes into 64-pixel OpenGL textures before world drawing, at most 12 new
  icons per frame, and keeps a 256-entry cache shared with Skija without
  per-frame GPU readback. Generated items retain texture layers and tints.
  Chests and skulls have dedicated geometry. Unsupported models show a visible
  missing-icon marker and are logged, never replaced with an arbitrary block.
- E opens InventoryScreen; 1-9/wheel select the hotbar; TAB displays players;
  F3 toggles the diagnostic overlay. Normal UI does not contain control tutorials.
  Screen lifecycle owns cursor capture and prevents events leaking into gameplay.
- Left/right pickup, splitting, placing, merging, swapping and outside dropping
  use C0E with server transaction IDs. Shift transfer supports the player storage
  and ordinary storage containers. Rejected clicks wait for S30 resynchronization.
  Server close/replacement uses a generation guard so old pages cannot close the
  replacement container. Server slot changes are echoed in the next client tick.
- Gameplay actions are queued by callbacks. GameSession sends each tick as a
  single writer batch ending in movement, so concurrent replies cannot appear
  between its held-item/click/ability actions and movement packet.

Remaining UI work: creative catalog, drag distribution, double-click collect,
number-key swapping inside containers, recipe prediction and specialized machine
controls; TAB player faces/hearts; full item tooltip/enchantment glint support;
banner item models, live compass/clock animation, and refreshed asynchronously
downloaded skull icons. Specialized
containers currently expose their server slots with a generic grid. This refactor
is a functional base, not a claim of complete vanilla inventory parity.
