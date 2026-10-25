package cn.fandmc.epoch.nbt.parser;

import java.io.IOException;

public final class NbtParseException extends IOException {
    public NbtParseException(String message) { super(message); }
    public NbtParseException(String message, Throwable cause) { super(message, cause); }
}
