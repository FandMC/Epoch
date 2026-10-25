package cn.fandmc.epoch.network;

/** Version identifiers are protocol configuration, not component names. */
public record ProtocolVersion(String gameVersion, int wireVersion) {
    public static final ProtocolVersion CURRENT = new ProtocolVersion("1.8.9", 47);
}
