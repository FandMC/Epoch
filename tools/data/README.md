# Registry source data

`import_block_registry.py` consumes pinned local JSON and prepared resource files;
it does not download dependencies or load original game classes at runtime.
Legacy numeric block identities are translated to flattened states by the importer
and by the network compatibility mapping.

`block-materials-1.8.json` records all 198 registered block types from the local
Minecraft 1.8.9 installation. Its source section includes the client JAR SHA-1.
The data was extracted read-only with an isolated verification class loader:
initialize `afh.S()`, look up `afh.c(id)`, obtain material with `afh.t()`.
`arm.c()` provides `blocksMovement`. `arm.a()` supplies the solid-material flag
used by horizontal liquid-face checks, with material `arm.w` (ice) excluded.
These flags are deliberately independent of opacity and collision geometry.
The recorded `materialField` is source provenance, not a runtime identifier.

Regenerate bundled states with `python tools/import_block_registry.py`. The
material flags propagate to neighbor-derived states, including doors and panes.
No original Minecraft class files are bundled into Epoch.

item-properties-1.8.json and item-models-1.8.json record original stack limits,
durability, metadata variants, model locations, translation keys and layer tints.
import_item_mapping.py emits the runtime compatibility mapping from these data.
Model JSON and texture images remain local resources, imported with import_assets.py.
