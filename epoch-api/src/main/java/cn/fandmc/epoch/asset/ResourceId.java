package cn.fandmc.epoch.asset;

/** A resource identity, never a filesystem path or a network numeric ID. */
public record ResourceId(String namespace, String path) {
    public ResourceId {
        if (namespace == null || !namespace.matches("[a-z0-9_.-]+") || path == null
                || !path.matches("[a-z0-9_./-]+") || path.startsWith("/"))
            throw new IllegalArgumentException("Invalid resource identifier");
        for (String segment : path.split("/", -1))
            if (segment.isEmpty() || segment.equals(".") || segment.equals(".."))
                throw new IllegalArgumentException("Invalid resource segment: " + path);
    }

    public static ResourceId parse(String value) {
        int colon = value.indexOf(':');
        return colon < 0 ? new ResourceId("minecraft", value)
                : new ResourceId(value.substring(0, colon), value.substring(colon + 1));
    }

    public String assetPath(String category, String suffix) { return namespace + "/" + category + "/" + path + suffix; }
    @Override public String toString() { return namespace + ":" + path; }
}
