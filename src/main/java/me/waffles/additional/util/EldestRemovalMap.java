package me.waffles.additional.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class EldestRemovalMap<K, V> extends LinkedHashMap<K, V> {
    private final int MAX_SIZE;

    public EldestRemovalMap(int maxSize) {
        super(maxSize, 0.75f, true); // true = access order (LRU behavior)
        this.MAX_SIZE = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > MAX_SIZE; // Remove oldest if size exceeds limit
    }
}

