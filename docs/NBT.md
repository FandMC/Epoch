# NBT package boundaries

The package cn.fandmc.epoch.nbt contains the immutable, typed NBT value model.
The package cn.fandmc.epoch.nbt.parser owns binary parsing, bounds and
serialization. Both live in epoch-api and depend only on the JDK.

NbtParser.parse(byte[]) reads one uncompressed named-root document, retaining
the root name, numeric tag widths, list element types, and byte/int array types.
It validates lengths, nesting depth, node count, malformed modified UTF-8, root
and child tag IDs, and trailing bytes. parseCompound is the convenience entry
for the compound payloads currently used by the game protocol.

NbtWriter.write(NbtDocument) serializes the same standard tag types and applies
matching output bounds. NbtCompound copies and normalizes nested Java values
into immutable typed tags; byte arrays, int arrays, lists and compounds do not
expose mutable backing storage.

Packet code passes bounded payload bytes to the parser and retains the original
packet payload where byte-for-byte forwarding is required. NBT parsing no
longer depends on PacketReader, so slot items and local save/resource readers
can use the same binary implementation without depending on network framing.
Compression wrappers and long-array tags outside the pinned 1.8.9 format are
not implemented here.
