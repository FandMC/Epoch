package cn.fandmc.epoch.block;

import cn.fandmc.epoch.asset.ResourceId;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Fully named state identity. No packed legacy ID or metadata lives here. */
public record BlockState(ResourceId block, Map<String, String> properties) {
    public BlockState {
        if (block == null || properties == null) throw new IllegalArgumentException("Missing block identity");
        properties = Collections.unmodifiableMap(new TreeMap<>(properties));
        properties.forEach((key, value) -> {
            if (!key.matches("[a-z0-9_]+") || !value.matches("[a-z0-9_-]+"))
                throw new IllegalArgumentException("Invalid block property: " + key);
        });
    }
    public boolean isAir() { return block.namespace().equals("minecraft") && block.path().equals("air"); }
    public String property(String key, String fallback) { return properties.getOrDefault(key, fallback); }
    @Override public String toString() {
        return block + (properties.isEmpty() ? "" : properties.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(",", "[", "]")));
    }
}
