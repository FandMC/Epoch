package cn.fandmc.epoch.item;

import cn.fandmc.epoch.asset.ResourceId;
import java.util.*;

/** Flattened namespaced item identities; numeric wire IDs never become registry handles. */
public final class ItemRegistry {
    public static final ItemRegistry EMPTY = new ItemRegistry(List.of());
    private final Map<ResourceId, Item> items;

    public ItemRegistry(Collection<? extends Item> entries) {
        Map<ResourceId, Item> values = new LinkedHashMap<>();
        for (Item item : entries) {
            Item previous = values.putIfAbsent(item.id(), Objects.requireNonNull(item));
            if (previous != null) throw new IllegalArgumentException("Duplicate item identity: " + item.id());
        }
        items = Collections.unmodifiableMap(values);
    }

    public int size() { return items.size(); }
    public Item item(ResourceId id) {
        Item item = items.get(id);
        if (item == null) throw new IllegalArgumentException("Unregistered item: " + id);
        return item;
    }
    public Optional<Item> find(ResourceId id) { return Optional.ofNullable(items.get(id)); }
    public Collection<Item> items() { return items.values(); }
}
