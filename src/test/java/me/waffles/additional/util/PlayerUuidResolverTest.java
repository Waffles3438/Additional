package me.waffles.additional.util;

import com.google.common.base.Ticker;
import com.mojang.authlib.GameProfile;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

public class PlayerUuidResolverTest {
    @Test
    public void overlappingCaseInsensitiveLookupsShareOneNetworkRequest() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        PlayerUuidResolver shared = new PlayerUuidResolver(name -> {
            calls.incrementAndGet();
            entered.countDown();
            try { release.await(); } catch (InterruptedException e) { throw new AssertionError(e); }
            return REMOTE_UUID.toString();
        }, Ticker.systemTicker());
        FutureTask<String> first = new FutureTask<>(() -> shared.resolveUuid("Player", null));
        FutureTask<String> second = new FutureTask<>(() -> shared.resolveUuid("pLaYeR", null));
        Thread firstThread = new Thread(first);
        Thread secondThread = new Thread(second);
        firstThread.start();
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            secondThread.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (secondThread.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            assertEquals(Thread.State.WAITING, secondThread.getState());
            assertEquals(1, calls.get());
        } finally {
            release.countDown();
            firstThread.join(2000);
            secondThread.join(2000);
        }
        assertEquals(REMOTE_UUID.toString(), first.get(2, TimeUnit.SECONDS));
        assertEquals(REMOTE_UUID.toString(), second.get(2, TimeUnit.SECONDS));
    }

    @Test
    public void externalFailuresKeepTheirOriginalExceptionAndCanRetry() {
        IllegalArgumentException failure = new IllegalArgumentException("provider failed");
        AtomicInteger calls = new AtomicInteger();
        PlayerUuidResolver shared = new PlayerUuidResolver(name -> {
            if (calls.incrementAndGet() == 1) throw failure;
            return REMOTE_UUID.toString();
        }, Ticker.systemTicker());
        try {
            shared.resolveUuid("Player", null);
            throw new AssertionError("Expected provider failure");
        } catch (IllegalArgumentException actual) {
            assertSame(failure, actual);
        }
        assertEquals(REMOTE_UUID.toString(), shared.resolveUuid("Player", null));
        assertEquals(2, calls.get());
    }

    private static final UUID LOCAL_UUID = UUID.fromString("12345678-1234-4234-8234-123456789abc");
    private static final UUID REMOTE_UUID = UUID.fromString("87654321-4321-4321-8321-cba987654321");
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicLong elapsedNanos = new AtomicLong();
    private PlayerUuidResolver resolver;
    private String response;

    @Before
    public void setUp() {
        response = REMOTE_UUID.toString();
        resolver = new PlayerUuidResolver(username -> {
            requests.incrementAndGet();
            return response;
        }, new Ticker() {
            @Override
            public long read() {
                return elapsedNanos.get();
            }
        });
    }

    @Test
    public void localProfileAvoidsNetworkAndRemainsCachedAfterPlayerLeaves() {
        assertEquals(LOCAL_UUID.toString(), resolver.resolveUuid("pLaYeR", new GameProfile(LOCAL_UUID, "Player")));
        assertEquals(LOCAL_UUID.toString(), resolver.resolveUuid("PLAYER", null));
        assertEquals(0, requests.get());
    }

    @Test
    public void remotePlayerIsLookedUpOnceAndReusedAcrossNameCasing() {
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("Player", null));
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("pLaYeR", null));
        assertEquals(1, requests.get());
    }

    @Test
    public void localProfileOverridesAnOlderCachedMapping() {
        resolver.resolveUuid("Player", null);
        assertEquals(LOCAL_UUID.toString(), resolver.resolveUuid("Player", new GameProfile(LOCAL_UUID, "Player")));
        assertEquals(LOCAL_UUID.toString(), resolver.resolveUuid("Player", null));
        assertEquals(1, requests.get());
    }

    @Test
    public void incompleteMismatchedAndSyntheticProfilesUseExternalLookup() {
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("Incomplete", new GameProfile(null, "Incomplete")));
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("Mismatch", new GameProfile(LOCAL_UUID, "SomeoneElse")));
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("Unnamed", new GameProfile(LOCAL_UUID, null)));
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("Npc", new GameProfile(
                UUID.fromString("12345678-1234-2234-8234-123456789abc"), "Npc")));
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("Offline", new GameProfile(
                UUID.fromString("12345678-1234-3234-8234-123456789abc"), "Offline")));
        assertEquals(5, requests.get());
    }

    @Test
    public void failedLookupsAreRetriedInsteadOfCached() {
        response = null;
        assertNull(resolver.resolveUuid("Player", null));
        response = REMOTE_UUID.toString();
        assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("Player", null));
        assertEquals(2, requests.get());
    }

    @Test
    public void expiredMappingIsRefreshedEvenWhenFrequentlyRead() {
        resolver.resolveUuid("Player", null);
        elapsedNanos.set(TimeUnit.MINUTES.toNanos(59));
        resolver.resolveUuid("Player", null);
        assertEquals(1, requests.get());
        response = LOCAL_UUID.toString();
        elapsedNanos.set(TimeUnit.HOURS.toNanos(1));
        assertEquals(LOCAL_UUID.toString(), resolver.resolveUuid("Player", null));
        assertEquals(2, requests.get());
    }

    @Test
    public void invalidNamesNeverReachExternalLookup() {
        for (String username : new String[] {null, "", "bad name", "name/", "abcdefghijklmnopq"}) {
            assertNull(resolver.resolveUuid(username, null));
        }
        assertEquals(0, requests.get());
    }

    @Test
    public void cacheKeysAreIndependentOfSystemLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            resolver.resolveUuid("INDIGO", null);
            assertEquals(REMOTE_UUID.toString(), resolver.resolveUuid("indigo", null));
            assertEquals(1, requests.get());
        } finally {
            Locale.setDefault(original);
        }
    }
}
