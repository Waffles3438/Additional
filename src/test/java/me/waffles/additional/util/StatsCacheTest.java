package me.waffles.additional.util;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StatsCacheTest {
    private final AtomicLong elapsedNanos = new AtomicLong();
    private Cache<String, String> cache;

    @Before
    public void setUp() {
        cache = StatsCache.create(new Ticker() {
            @Override
            public long read() {
                return elapsedNanos.get();
            }
        });
    }

    @Test
    public void statsExpireAfterFiveMinutesEvenWhenRepeatedlyRead() {
        cache.put("player", "original stats");
        for (int minute = 1; minute < 5; minute++) {
            elapsedNanos.set(TimeUnit.MINUTES.toNanos(minute));
            assertEquals("original stats", cache.getIfPresent("player"));
        }
        elapsedNanos.set(TimeUnit.MINUTES.toNanos(5) - 1);
        assertEquals("original stats", cache.getIfPresent("player"));
        elapsedNanos.incrementAndGet();
        assertNull(cache.getIfPresent("player"));
    }

    @Test
    public void newlyFetchedStatsGetTheirOwnFiveMinuteLifetime() {
        cache.put("player", "original stats");
        elapsedNanos.set(TimeUnit.MINUTES.toNanos(5));
        assertNull(cache.getIfPresent("player"));
        cache.put("player", "updated stats");
        elapsedNanos.set(TimeUnit.MINUTES.toNanos(9));
        assertEquals("updated stats", cache.getIfPresent("player"));
        elapsedNanos.set(TimeUnit.MINUTES.toNanos(10));
        assertNull(cache.getIfPresent("player"));
    }

    @Test
    public void freshPlayersAreNotEvictedByCount() {
        for (int player = 0; player < 2000; player++) {
            cache.put("player" + player, "stats" + player);
        }
        for (int player = 0; player < 2000; player++) {
            assertEquals("stats" + player, cache.getIfPresent("player" + player));
        }
    }

    @Test
    public void cleanupRemovesExpiredPlayersAndKeepsFreshPlayers() {
        cache.put("oldPlayer", "old stats");
        elapsedNanos.set(TimeUnit.MINUTES.toNanos(3));
        cache.put("newPlayer", "new stats");
        elapsedNanos.set(TimeUnit.MINUTES.toNanos(5));
        cache.cleanUp();
        assertEquals(1, cache.size());
        assertNull(cache.getIfPresent("oldPlayer"));
        assertEquals("new stats", cache.getIfPresent("newPlayer"));
    }

    @Test
    public void manualClearRemovesFreshEntriesImmediately() {
        cache.put("player", "stats");
        cache.invalidateAll();
        assertNull(cache.getIfPresent("player"));
        assertEquals(0, cache.size());
    }
}
