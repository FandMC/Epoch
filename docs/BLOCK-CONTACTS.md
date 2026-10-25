# Block contact and rendering verification — 2026-10-25

These results use Epoch production classes and the user's read-only local
1.8.9 installation. They are not a remote Grim acceptance test.

## Movement

PlayerController consumes the prior move's cobweb contact once, scales the
requested displacement by 0.25 / (double)0.05f / 0.25, and clears velocity
before clipping. The post-move contracted-body callback renews contact.
Reset clears the contact flag. Touching several webs does not compound it.

The installed original Entity bytecode confirms this order. A bounded adapter
calls its real living-entity integration and block collision code. The adapter
performs the base water-contact phase before the living tick and keeps motion
inside the loaded scene; an earlier fixture omitted that phase and allowed
entities to leave the loaded area, producing fixture-related differences.

Final block-contact-reference.jsh results, tolerance 1e-10 per component:

| Material | Scenarios | Ticks | Differences |
| --- | ---: | ---: | ---: |
| Cobweb | 24 | 2,160 | 0 |
| Lily pad | 24 | 2,160 | 0 |
| Snow, all eight layers | 48 | 4,320 | 0 |
| Ladder, four orientations with sideways input | 24 | 2,160 | 0 |

The check compares position, velocity, ground, horizontal collision and web
contact. It covers entries, falling, jumping and changing directional input.
It does not reproduce the exact multiplayer terrain, player lifecycle or
server's prediction state.

Snow collision height is (layers - 1)/8, including the first layer's
zero-height box. Lily pads use [0,0,0..1,1/64,1]. The generated registry and
its import script both carry these corrections.

## Ladder compatibility decision

The local Grim CollisionData.LADDER selects thickness 2/16 for old clients
and 3/16 for newer clients. The experimental 3/16 setting differs from the
original collision path in all 24 deliberately boundary-aligned comparisons:
it clips inward velocity sooner and applies a climb impulse at that boundary.
This demonstrates a local geometry mismatch, not the exact remote VL value.

The user chose protocol consistency after observing horizontal-movement VL.
run/config.json now has modernLadderCollision=false. The optional switch is
retained but off. The user previously observed that original 1.8.9 also has
climbing setbacks on this newer backend, so this rollback cannot promise both
no backend setbacks and no prediction alerts.

## Cobweb multiplayer report

The user reports repeated Simulation offsets near 0.029918 in Epoch and in
the original client. The original slowdown is now implemented and verified,
but the remote violation remains unresolved.
Further cobweb investigation is paused at the user's request. The block-type
refactor retains the existing slowdown behavior without a protocol workaround.

web-reporting.jsh measures a stable unsupported fall inside a web:

- Actual descent per tick: 0.003920000135 blocks.
- The 0.03 report threshold produces one position packet every eight ticks.
- Reported descent: 0.031360001078 blocks; seven intervening ground-only packets.
- Eighty ticks produce ten position packets with the correct real coordinates.

The local Grim PointThreeEstimator.determineCanSkipTick evaluates unscaled
starting vertical velocity when deciding whether hidden sub-threshold motion
is possible. controlsVerticalMovement does not include cobweb slowdown.
PredictionEngine applies the stuck multiplier later. This reveals a candidate
server prediction gap for accumulated web descent, but the full remote engine
and exact supplied offset have not been reproduced.

Forcing small position packets every tick is not a validated repair:
BadPacketsV rejects old-client position updates below the 0.03 threshold before
the reminder interval. Slowing/faking positions, skipping game ticks or adding
extra movement packets would change actual gameplay/protocol behavior. Epoch
does not do that. A complete repair needs verification and correction of the
server prediction/translation path. No Grim, proxy or remote server files were
modified, and no zero-VL result is claimed.

## Rendering and data

block-rendering.jsh loads 1,382 affected model states and checks their wire
mapping. The hidden real GLFW/OpenGL 3.3 context on the NVIDIA GTX 760 renders
13 draws / 2,898 vertices with GL_NO_ERROR. The saved image was inspected at
build/verification/block-rendering.png. Quartz variants, note blocks,
repeaters/comparators, red redstone dust, chest variants, signs and five skull
types are present. NBT modified UTF-8, raw S35 roundtrip and stale-data cleanup
also pass. Box orientation coverage is limited to the constructed scene.

Chest lids are static; open/close animation is not implemented. Remote custom
skin downloads and real-server sign/skull updates were not exercised. Name-only
skull profiles fall back to Steve. The scene's resource pass still reports
1,879 unresolved model states overall, including explicit unmapped identities;
this is not complete block coverage.

## Transport and UI

transport-actionbar.jsh passes with compression disabled and thresholds 1/256.
Each case records 242 movement packets, 126 position packets, at most twenty
non-position packets, no duplicate action transitions, and the expected
KeepAlive/transaction replies. Action Bar text stays out of chat and expires
after sixty ticks. The loopback closes its socket after receiving the terminal
marker; the resulting Truncated VarInt log is fixture teardown, not a live
server failure. No additional remote login was performed.

The water-exit BadPacketsE/R/X report remains unresolved. The local sender and
loopback pass; they do not establish how the remote proxy/backend handles the
same sequence. No blanket forced-position workaround was introduced.
