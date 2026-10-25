# Block types and block entities

The block API lives in epoch-api and uses only the JDK. ResourceId, BlockState
and Box also live there so behavior contracts do not depend on client rendering
or asset loaders.

## Type, state and position data

- Block is the reusable base for behavior. Each BlockRegistry owns one immutable
  instance per namespaced identity. Ordinary JSON-defined blocks use the base
  implementation directly; a class is added only when behavior differs.
- BlockState is the immutable namespaced identity plus properties. Its block()
  accessor remains a ResourceId. It does not contain numeric protocol metadata.
- BlockEntity extends Block and is an abstract type for blocks with dedicated
  models and position data. ChestBlockEntity, SignBlockEntity and SkullBlockEntity
  are the current concrete types. These instances are also shared, not one per
  world coordinate.
- BlockEntityData contains immutable data for one world position, owned by
  ClientWorld. Mutable sign text, skull profiles and future per-instance state
  must never be stored on a shared block type.

BlockRegistry associates every state definition with its shared Block. The
client's BlockTypes factory is the only namespaced type-selection point.
ChunkColumn indexes special blocks by type, and ordinary JSON model loading
recognizes the same types instead of maintaining another name list.

## Behavior dispatch

PlayerController delegates slipperiness, landing velocity, climbability and
inside-block contact to Block methods. CollisionWorld delegates shape selection
to Block.collision, passing the existing resource shape and an immutable policy.
The default ladder policy still uses the original 2/16 geometry; the optional
3/16 backend policy is implemented only in LadderBlock.

The refactor preserves movement order and constants. BlockContact exposes only
the mutations required by the implemented contact behaviors. It does not expose
the client, graphics context or network session.

## Updates and lifecycle

The network handler converts S33 and the supported S35 skull action into typed
BlockEntityUpdate values. Numeric action IDs stop at that boundary. GameState
forwards semantic updates to ClientWorld, which delegates to the block type.
Unsupported update types preserve current data and do not increment revision.

Replacing a block identity clears its old data, including partial chunk updates.
Property-only updates preserve it. Ground-up replacement, unload and dimension
reset clear the affected position data. Sections absent from a partial update
retain their data.

## Rendering

BlockEntityModel<T> is the typed CPU model base. ChestBlockEntityModel,
SignBlockEntityModel and SkullBlockEntityModel own geometry creation and caches.
Chest pairing, sign orientation/text and skull type/rotation belong to their
respective providers. ModelBoxGeometry contains their shared box/UV calculation.

BlockEntityRenderer owns visibility, texture batching, light sampling and GPU
submission. BlockEntityTextures retains ownership of texture resources and skin
loading. Providers hold no GPU objects. Renderer construction verifies that
every registered special block type has a provider.

To add a special type, register its Block implementation in BlockTypes and its
typed model in BlockEntityRenderer. Add semantic updates only when a supported
network packet supplies the data. Do not add a diagnostic entry point or depend
on original Minecraft game classes.

This refactor does not implement chest lid animation, new block models or new
physics. The existing block-state resolver, resource JSON parsing and legacy
wire mapping remain in place. The former BlockEntityModels switch helper and
world.BlockEntityData class are deleted, not retained as forwarding wrappers.

## Optical properties and vertex lighting

BlockRegistry.Definition groups optical flags in BlockLighting. Opacity,
emission, neighbor brightness, full/opaque cube flags, AO and AO-diagonal
translucency come from tools/data/block-light-1.8.json, extracted from the
installed original JAR identified by its SHA-1. The translucency flag here
controls AO sampling; it is not the alpha classification of a PNG texture.

LightView exposes raw received/incrementally updated light without applying
render-specific substitutions. MeshSnapshot captures all eight neighboring
columns so vertex samples also work across chunk corners. LightSampler applies
neighbor brightness and sampled-block emission. Chunk rendering samples all
six neighbors; the live-world block-entity path uses five, excluding down.

FaceLighting owns per-build scratch arrays. It computes four corner light/AO
values and interpolates side faces according to their geometry. Precomputed
spatial weights keep samples attached to the correct vertices after model
rotations. Packed light retains fractional levels instead of rounding each
vertex to a nibble. BlockModels inherits ambientocclusion from parent JSON;
models opting out and emitting blocks use flat light.

## Legacy resource names

Runtime names remain flattened. Resource aliases belong in
tools/import_block_registry.py and the generated registry:

- cut_sandstone / cut_red_sandstone select the original smooth_sandstone /
  smooth_red_sandstone resources (the old names describe today's cut variant).
- smooth_sandstone / smooth_red_sandstone select the corresponding double-slab
  resource's all variant, with the top texture on every face.
- daylight_detector keeps inverted and power in its runtime state. The inverted
  property selects daylight_detector_inverted; power selects its JSON variant.
- The six infested identities select the six original monster_egg blockstates.
  Their actual model JSON supplies the matching stone/cobblestone/brick textures.
