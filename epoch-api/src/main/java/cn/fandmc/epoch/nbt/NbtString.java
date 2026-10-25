package cn.fandmc.epoch.nbt;

import java.util.Objects;

public record NbtString(String value) implements NbtTag {
    public NbtString { Objects.requireNonNull(value); }
    @Override public int type() { return 8; }
}
