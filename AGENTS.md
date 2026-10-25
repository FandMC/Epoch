# Epoch engineering conventions

- Continue the existing epoch-api / epoch-client split. Java 25, root package
  cn.fandmc.epoch, Windows x64, Gradle Wrapper and Kotlin DSL are required.
- Keep exactly one production Java main: cn.fandmc.epoch.Epoch, starting the
  desktop game. Do not add diagnostic launcher classes, separate run tasks,
  command dispatch modes, or renamed wrappers preserving redundant entry flows.
- Name components by responsibility (PacketCatalog, LegacyBlockMapping,
  ChunkDecoder). Keep version numbers in protocol configuration/data directories,
  not in class names. Status is a server-list service called by the game UI.
- Do not add tests unless necessary for the change. Prefer compilation and bounded
  verification against the actual local resources, graphics runtime and server.
- The world and gameplay use flattened namespaced block states and properties.
  Protocol 47 numeric block IDs and metadata belong at the network compatibility
  boundary. Never make packed legacy values the internal state representation.
- Parse the actual resource JSON (blockstates, model inheritance, textures and
  animation metadata); do not substitute arbitrary visual models for missing data.
- Preserve user files and existing implementations. The Minecraft installation,
  protocol archive and NyaClient reference project are read-only.
- Do not report successful gameplay based only on compilation, window startup,
  Status or Login. State exactly which integration stages were verified.
