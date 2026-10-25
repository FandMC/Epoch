package cn.fandmc.epoch.nbt;

/** A typed node in the Java Edition named-binary-tag format. */
public sealed interface NbtTag permits NbtNumber, NbtString, NbtList, NbtByteArray, NbtIntArray, NbtCompound {
    int type();
}
