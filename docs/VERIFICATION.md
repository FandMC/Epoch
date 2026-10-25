# Verification status

Last updated: 2026-10-25 (Asia/Shanghai). This record separates source/build
checks, local checks and observed multiplayer behavior.

## Lighting and resource mappings (2026-10-25)

- The user confirmed the reported lighting problem was fixed. This is user
  gameplay feedback; the agent did not take over the running multiplayer client.
- The bounded face-lighting-oracle.jsh comparison passed 1,920 cases against
  the installed original client's ambient-occlusion calculation: all six face
  directions, full and partial faces, two independent light channels, randomized
  occluders/emission and permuted vertex order. Model-inherited AO opt-out was
  checked separately. Original classes are isolated in the verification loader;
  production Epoch does not depend on them.
- BlockLighting, LightView, LightSampler and FaceLighting separate optical
  properties, stored light, render sampling and per-vertex AO. Mesh sampling
  includes the lower neighbor; live block entities retain the world sampling
  convention. This work does not replace the existing shader/lightmap model.
- resource-model-fixes.jsh passed all 42 requested states: legacy smooth/cut
  sandstone and red sandstone, their seamless double-slab variants, all six
  infested blocks and all 16 power levels of both detector modes. It checked
  installed blockstate JSON, parsed model inheritance, PNG availability, wire
  round trips and nonempty mesh geometry. Switching detector modes dirtied the
  mesh and changed top-face UVs; switching back restored them.
- The representative scene rendered through WorldRenderer in a hidden GLFW
  OpenGL context on NVIDIA GeForce GTX 760, with 2,460 vertices and GL_NO_ERROR.
  Visual inspection confirms four sandstone variants, six infested blocks and
  distinct normal/inverted detector tops. The image is
  build/verification/resource-model-fixes.png. These are bounded local checks,
  not a new real-server verification of those block families.
- Full Gradle build passed. Both test tasks remain NO-SOURCE. Due to temporary
  system commit-memory pressure the successful invocation used one worker and
  a 160 MiB SerialGC build JVM; project JVM settings were not changed.
- Other unresolved model states remain outside this fix. The earlier
  render-light-fixes.jsh checks predate the per-vertex sampling API and are not
  evidence of verification of the final smooth-light implementation.

## Block hierarchy refactor (2026-10-25)

Block and the abstract BlockEntity subtype are implemented in epoch-api, with
typed client behaviors and separate per-position data. See BLOCKS.md for the
extension points and ownership rules.

- Full Gradle build passed after migrating all production callers. Both test
  tasks remain NO-SOURCE; no production launcher or test source was added.
- The bounded block-types.jsh check passed 16,977 assertions across 5,651 states,
  2,018 identities and 37 special states. This covers shared type identity,
  chunk indexing, semantic updates, wrong-type updates, replacement, partial
  sections, unload and dimension reset, plus migrated behavior hooks.
- jdeps reports that the built epoch-api JAR depends only on java.base.
- The existing bounded original-client comparison passed all 120 scenarios
  and 10,800 ticks after migration, with zero mismatches at tolerance 1e-10.
  It covers the existing web, lily-pad, snow and ladder fixtures. The web cases
  only confirm unchanged behavior; the multiplayer VL investigation is paused.
- Resource loading checked 1,382 affected model states. The representative scene
  rendered on a real GTX 760 hidden OpenGL context with 13 draws / 2,898 vertices
  and GL_NO_ERROR. The 1600x900 output matches the
  pre-refactor snapshot exactly: zero changed pixels out of 1,440,000. NBT,
  packet roundtrip and unload checks also passed. This is a bounded scene,
  not full-world rendering or gameplay coverage.
- The built client/API JARs contain no duplicate class definitions, old
  BlockEntityModels or world.BlockEntityData classes, or SessionEvent.Sign.
  Production sources still have exactly one main: cn.fandmc.epoch.Epoch.
- A partial chunk could previously preserve stale data when replacing a sign
  with a skull. Identity changes now remove old data before the merge;
  property-only changes and untouched sections retain it.

The initial parallel verification attempt exhausted Windows commit memory.
Those runs did not validate behavior. Follow-up checks use serial execution,
limited processor counts, Serial GC and bounded JVM heaps. Reports from the
failed harnesses are retained under build/verification/block-refactor-memory-errors.
No running game or proxy was stopped. No real-server gameplay result is claimed.
Successful check logs are block-refactor-types.log, block-refactor-contacts.log
and block-refactor-rendering.log under build/verification/.

## Build and entry

- Java 25.0.2, Gradle Wrapper 9.8.2, Windows x64.
- The latest `build` completed successfully after the Block/BlockEntity hierarchy
  refactor. Both modules' `test` tasks reported `NO-SOURCE`; this
  is not a claim that an automated gameplay test suite passed.
- Production Java has one `main`, `cn.fandmc.epoch.Epoch`. Diagnostic checks are
  temporary JShell scripts under `build/verification/`, not game launchers or
  additional Gradle run tasks.
- `./gradlew.bat run test --dry-run` resolved the existing client run task and
  both module test tasks successfully. This was task-graph verification only;
  the dry run did not open a window or execute tests.
- The build used a temporary smaller Gradle daemon heap because an earlier
  default-heap attempt hit Windows committed-memory exhaustion:

```powershell
./gradlew.bat build --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xms32m -Xmx384m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8' --console=plain
```

## Bounded local checks

No test-source files or production diagnostic entry points were added. The
following checks exercise actual Epoch classes and the local imported resources.
They do not establish server acceptance of movement.

| Check | Result | Scope |
| --- | --- | --- |
| `movement.jsh` | 24 checks passed | Movement packet selection, reminders, corrections and selected physics/attribute calculations |
| `block-states.jsh` | 27 checks passed | Neighbor states, mapping, chat logging and local original model JSON |
| Model coverage within that check | 1,200 affected states loaded | 520 stair states, 4,160 stair occupancy comparisons, 24 button states and connected blocks |
| `pane-collision.jsh` | 80 scenarios, no local penetration | Adjacent/separated panes, both axes/ends, lateral offsets and walk/sprint |
| `pane-angles.jsh` | 240 scenarios, no local penetration | Adjacent panes, angled approaches and jumping |
| `pane-boundary.jsh` | 5 checks passed on the current build | All 32 plain-pane/iron-bar aliases, 16 distinct stained colors, ChunkDecoder, S23 decoding plus mapping, adjacent-state resolution |
| `doors-legacy-panes.jsh` | 8 checks passed on the current build | 448 door model states, 448 lower/upper metadata pairs, both pane axes/endpoints, isolated legacy cross and replayed endpoint approach |
| `pane-version-gap.jsh` | 4 sprint approaches passed on the current build | Both endpoints on both axes agree exactly with the 1.8 collision formula; the previous 0.0625 local difference is zero |
| `fluids.jsh` | 23 checks passed on the current build | Body-edge contact, surface exit, swim jump rounding, air gravity recovery, water current, flight, lava, bank clearance, chunk boundary and a 45-tick swim/climb |
| Water-current comparison within that check | 3,168 cases matched the local original implementation | All 198 neighbor block types, four directions and source levels 0/7/8/15; invokes the original flow method only in a temporary verification class loader |
| `fluid-entry.jsh` | 11 checks passed after reproducing 9 failures before the fix | Four-direction dry-to-water entry, next wet tick without double push, still water, flight and entry concurrent with horizontal collision |
| `ladder.jsh` | 13 checks passed on the current build | Four 2/16 attachment shapes, collision/climbing, entering/leaving ladder height, float clamp, sneak and noClip |
| `screens-respawn.jsh` | 43 checks passed | Lifecycle, owner thread, reentrant navigation, stale close, event isolation, Unicode deletion and respawn socket loopback with compression disabled/threshold 1/threshold 256 |
| `screen-render.jsh` | Five Skija raster previews rendered | Main menu, pause, death-ready, death-waiting and 640x480 death page; the last two were visually inspected |
| `lava-reference.jsh` | 72 scenarios / 6,480 ticks matched | Local original living-entity movement against Epoch; changing yaw, sprint, jump, floors and external velocity |
| `lava-boundaries.jsh` | 360 scenarios / 32,400 ticks matched | Narrow lava stream, stone/slab/soul-sand floors, stone/slab/stair banks and scripted fluid block changes |
| `lava-thresholds.jsh` | 80 scenarios / 6,400 ticks matched | Both legacy lava IDs, levels 0/1/7/8/15, contact-height boundaries and world coordinates around 200 |
| `lava-descent.jsh` | 128 scenarios / 12,800 ticks matched | Descending slab/stair terrain, eight metadata variants, still/flowing lava and jump/no-jump |
| `lava-mixed-steps.jsh` | 64 scenarios / 10,240 ticks matched | Seeded mixed stair directions/halves and slab heights, changing controls and falling lava |
| `lava-ascent.jsh` | 32 scenarios / 6,400 ticks matched | Forward ascent through lava across slab/stair banks, jump/no-jump and both still/flowing blocks |

The movement, block-state and 80/240-scenario pane rows record earlier checks;
only rows marked current build were rerun in this pass. Verification scripts
remain under build/verification and are not production application entry points.
The fluid, fluid-entry and ladder checks were rerun after the final movement
changes. No new test-source files or Gradle verification launchers were added.
Lava comparisons require each position/velocity component to differ by no more
than 1e-10, plus identical ground and horizontal-collision flags. They compare
fixed logical inputs, not recorded input from the reported multiplayer scene.
No Trace classes or tracing hooks remain in production source or the client JAR.
Historical log files are preserved; new chat and ordinary errors remain logged.

The generated registry currently contains 5,651 interned state identities.
Across the 198 legacy block IDs, 1,513 wire values map to recognized named states
and 1,655 retain explicit unmapped identities. This is not full mapping coverage.
Material flags for water currents were extracted read-only from the local 1.8.9
registry; the client JAR hash and method provenance are in
tools/data/block-materials-1.8.json. Runtime state still uses flattened names.

## Actual runtime observations

The user has observed the desktop window, server connection, world rendering
and original textures working. Runtime logs confirm received chat is written
with `[CHAT]`. These observations do not establish complete gameplay support.

The user confirmed colored glass pane rendering works. The initial endpoint
rollback also occurs with the original 1.8.9 client on the newer backend.
Modern endpoint shapes were tried at the user's request, then explicitly
reverted to 1.8.9 collision to prioritize protocol/Grim prediction consistency.
The old shapes can still encounter the newer backend's endpoint correction.

One confirmed mapping defect was fixed: nonzero metadata for plain glass panes
and iron bars previously produced unknown states instead of the default state.
This is verified against the local original implementation and checked through
Epoch's wire decoding. There is no evidence yet that those metadata values were
present at the user's rollback location.

The 1.8.9 pane endpoint boxes agree with the local original class bytecode and
Grim's old-client collision path. Restoring these eliminates the local 1/16
shape difference seen with modern endpoints. This is not evidence that the
remote server's Simulation violations have stopped.

## Remaining reproduction

### Flowing lava and uneven terrain

The user reports that original 1.8.9 does not trigger Simulation violations in
the same flowing-lava location, while Epoch does, especially when continuously
moving/jumping around slabs, steps or height changes. This remains unresolved:
none of the bounded local movement comparisons reproduced the supplied
0.001812 / 0.009625 / 0.004813 / 0.006109 server offsets.

A later exploratory lava-edge-step check found 35 mismatches among 3,200
single-step cases. Those cases have not been classified as valid initial
states versus fixture/physics differences. They remain unresolved. Further
lava investigation is paused at the user's request; earlier passing rows
must not be read as exhaustive lava correctness.

The later rendering, Action Bar and block-contact implementation and checks
are recorded in BLOCK-CONTACTS.md. The build after those changes passed; both
test tasks still reported NO-SOURCE. Earlier table rows retain their original
scope and are not all rerun by that later build.

Two independent defects were confirmed and corrected in PlayerController:
inside-block soul-sand slowdown was missing (6 of 30 initial reference cases
failed), and moveFlying folded the yaw conversion into a constant instead of
the original float multiply-then-divide (40 of 72 turning scenarios differed
before correction). Sprint-jump keeps its distinct original constant. Neither
defect has been demonstrated to explain this particular multiplayer report.

The temporary reference loader calls the user's installed original 1.8.9
living-entity integration and block collisions. It provides a controlled world
and suppresses server-only landing particles, damage and entity pushes. It is
not a complete EntityPlayerSP lifecycle, network timing or Grim simulation.
Matching those scenarios therefore does not establish multiplayer acceptance.
The installed JAR and reference sources were only read. Runtime Epoch has no
dependency on these verification adapters or original game classes.

Original 1.8.9 and the inspected Grim legacy path do not apply a lava-current
push; no modern lava current or liquid-height rule was added to mask the issue.
Actual player tick/network timing and the exact reported terrain remain
unverified. No additional multiplayer login or takeover of the user's client
was performed. No zero-VL claim is made.

### Screens and respawn

The 43 lifecycle/socket checks cover one-shot C16 respawn, health gating, S07
dimension handling, S08 acknowledgment, same-dimension chunk retention and
cross-dimension clearing. Five previews use real Skija raster drawing, not
the GLFW/OpenGL surface. Native window display and real-server death/respawn
have not been exercised in this pass.

### Water exit and other unfinished work

No new multiplayer session was started in this pass, and the user's running
client was not interrupted. Restart the desktop client to load the rebuilt
classes and resources, then repeat falling into water, swimming and climbing
onto land against the actual proxy/server. Normal received chat remains in
run/logs/epoch-*.log. No proxy settings or network exposure were changed.

The reported BadPacketsE/R/X sequence after water entry is not yet proven fixed.
Earlier socket-loopback checks established local movement writing, compression
and correction acknowledgments, but did not identify the remote failure.
The fluid changes repair known local movement mismatches and need multiplayer
validation; no claim of zero Grim violations is made.

The user subsequently narrowed Simulation alerts to flowing water, with still
water unaffected. A second defect was confirmed in tick ordering: when the
tick starts dry, original `EntityLivingBase.updateFallState` checks water again
at the moved position. That current is applied before collision velocity resets
and before drag, without changing the already-selected travel branch. Epoch
omitted this entry current. PlayerController now performs that conditional
post-movement check; an already-wet tick does not receive a second push. The
temporary entry check failed 9 of 11 cases before the production fix and passes
all 11 after it. Multiplayer acceptance of this latest fix remains unverified.

Depth Strider/enchantment-dependent movement, remaining contextual collisions,
entity rendering, inventory/block interaction, encrypted authentication and
complete gameplay compatibility remain incomplete. A successful build or Login
must not be reported as a fully playable client.


## 2026-10-25 HUD refactor and held-item Post ordering

- Gradle build succeeded for epoch-api and epoch-client. Existing test tasks
  report NO-SOURCE; no permanent test launchers or extra main methods were added.
- Temporary JShell verification under ignored build/verification decoded adjacent
  NBT slots, checked item round trips, split/place/merge/Shift inventory behavior,
  transaction rejection/resynchronization and player-list packet round trips.
- Real hidden GLFW OpenGL 3.3 + Skija on NVIDIA GeForce GTX 760 rendered HUD, TAB,
  inventory and doubled-DPI previews. Eighteen distinct item icons were GPU baked;
  the exercised icons had no unresolved resources or GL errors, including cleanup.
  All 612 nonempty original item model chains parsed successfully. The remaining
  two legacy entries have no original item model. These scenes use explicitly
  constructed verification data, not a live server.
- Original potion classes were read in an isolated local verification loader to
  confirm all 23 effect names and icon indices. The installation was not modified.
- The local Grim Post check confirms held-item and pre-1.13 inventory actions
  belong before movement. A real TCP loopback login exercised 100 client ticks
  and 600 framed packets: 300 input actions, 100 movement packets, 100 transaction
  acknowledgments and 100 concurrent KeepAlives. Action/movement batches remained
  contiguous and repeated pending hotbar changes coalesced.
- Operation tutorials and cursor-capture instructions were removed from runtime UI.
  This pass does not establish zero Post VL on the user's remote server; restart
  and verify switching with the rebuilt client for multiplayer acceptance.

Known remaining UI limitations are enumerated in GUI.md. No Minecraft asset JAR,
textures, runtime credentials, screenshots, logs or verification helpers are
part of the public source commit. Original assets can be imported locally using
tools/import_assets.py.
