package me.waffles.additional.util;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public final class StatsCache {
    private StatsCache() {
    }

    public static <V> Cache<String, V> create() {
        return create(Ticker.systemTicker());
    }

    static <V> Cache<String, V> create(Ticker ticker) {
        // Reads do not extend the lifetime, and there is no player-count limit.
        return CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .ticker(ticker)
                .build();
    }
}
