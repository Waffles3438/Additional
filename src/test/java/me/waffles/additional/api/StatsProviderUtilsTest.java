package me.waffles.additional.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StatsProviderUtilsTest {
    @Test
    public void validatedResponseFeedsStatsAndProfileWithoutChangingData() {
        String player = "{\"player\":{\"displayname\":\"Player\",\"newPackageRank\":\"VIP\","
                + "\"achievements\":{\"bedwars_level\":12},\"stats\":{"
                + "\"Bedwars\":{\"final_kills_bedwars\":10,\"final_deaths_bedwars\":2},"
                + "\"Duels\":{\"kills\":5,\"deaths\":2,\"wins\":12,\"losses\":3}}}}";
        FakeClient client = new FakeClient();
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS, player);
        client.responses(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS,
                "{\"tag\":\"CACHE\",\"tagColor\":\"GOLD\"}");
        StatsProviderUtils.ProfileData result = coordinator(client).fetch("Player", "uuid", true);
        var data = result.getPlayer().getData();
        String original = data.toString();
        var bedwars = AbyssAPIUtils.parseBedwarsPlayerData(data);
        var duels = AbyssAPIUtils.parseDuelsPlayerData(data);
        var profile = AbyssAPIUtils.parsePlayerProfilePlayerData(data, result.getGuild().getData());
        assertEquals(12, bedwars.getBedwarsStar());
        assertEquals(5.0, bedwars.getBedwarsFKDR(), 0.0);
        assertEquals(2.5, duels.getDuelsKDR(), 0.0);
        assertEquals(4.0, duels.getDuelsWLR(), 0.0);
        assertEquals("Player", profile.getDisplayName());
        assertEquals("§a[VIP]", profile.getRank());
        assertEquals("§6[CACHE]", profile.getGuildTag());
        assertEquals(original, data.toString());
    }

    private ExecutorService executor;
    private RecordingSleeper sleeper;

    @Before
    public void setUp() {
        // Room for both providers of both resources, as hedged rounds can run four requests.
        executor = Executors.newFixedThreadPool(4);
        sleeper = new RecordingSleeper();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void playerAndGuildRunConcurrentlyAndKeepIndependentProviders() throws Exception {
        BlockingClient client = new BlockingClient();
        StatsProviderUtils.Coordinator coordinator = coordinator(client);

        Thread fetchThread = new Thread(() ->
                client.result = coordinator.fetch("Player", "uuid", true)
        );
        fetchThread.start();

        assertTrue(client.bothAbyssRequestsStarted.await(2, TimeUnit.SECONDS));
        client.releaseAbyssRequests.countDown();
        fetchThread.join(2000);

        assertFalse(fetchThread.isAlive());
        assertTrue(client.result.getPlayer().isSuccess());
        assertTrue(client.result.getGuild().isSuccess());
        assertSame(StatsProviderUtils.Provider.ABYSS, client.result.getPlayer().getProvider());
        assertSame(StatsProviderUtils.Provider.ABYSS, client.result.getGuild().getProvider());
        assertEquals(0, sleeper.waits.size());
    }

    @Test
    public void failedPlayerFallsBackWithoutDiscardingSuccessfulGuild() {
        FakeClient client = new FakeClient();
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS, "");
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.SHMEADO, "{\"player\":{}}");
        client.responses(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS, "{\"tag\":\"ABYSS\"}");

        StatsProviderUtils.ProfileData result = coordinator(client).fetch("Player", "uuid", true);

        assertTrue(result.getPlayer().isSuccess());
        assertTrue(result.getGuild().isSuccess());
        assertSame(StatsProviderUtils.Provider.SHMEADO, result.getPlayer().getProvider());
        assertSame(StatsProviderUtils.Provider.ABYSS, result.getGuild().getProvider());
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS));
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.SHMEADO));
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS));
        assertEquals(0, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.SHMEADO));
        assertEquals(0, sleeper.waits.size());
    }

    @Test
    public void malformedNonEmptyPlayerFallsBackToShmeado() {
        FakeClient client = new FakeClient();
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS, "{malformed");
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.SHMEADO, "{\"player\":{}}");

        StatsProviderUtils.ProfileData result = coordinator(client).fetch("Player", "uuid", false);

        assertTrue(result.getPlayer().isSuccess());
        assertSame(StatsProviderUtils.Provider.SHMEADO, result.getPlayer().getProvider());
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS));
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.SHMEADO));
    }

    @Test
    public void retriesOnlyTheMissingResource() {
        FakeClient client = new FakeClient();
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS, "{\"player\":{}}");
        client.responses(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS, "", "", "");
        client.responses(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.SHMEADO, "", "", "{}");

        StatsProviderUtils.ProfileData result = coordinator(client).fetch("Player", "uuid", true);

        assertTrue(result.getPlayer().isSuccess());
        assertTrue(result.getGuild().isSuccess());
        assertSame(StatsProviderUtils.Provider.ABYSS, result.getPlayer().getProvider());
        assertSame(StatsProviderUtils.Provider.SHMEADO, result.getGuild().getProvider());
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS));
        assertEquals(0, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.SHMEADO));
        assertEquals(3, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS));
        assertEquals(3, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.SHMEADO));
        assertEquals(Arrays.asList(1000L, 1000L), new ArrayList<>(sleeper.waits));
    }

    @Test
    public void exhaustedGuildReturnsUnavailableAfterThreeCycles() {
        FakeClient client = new FakeClient();
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS, "{\"player\":{}}");

        StatsProviderUtils.ProfileData result = coordinator(client).fetch("Player", "uuid", true);

        assertTrue(result.getPlayer().isSuccess());
        assertTrue(result.getGuild().isUnavailable());
        assertEquals(3, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS));
        assertEquals(3, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.SHMEADO));
        assertEquals(Arrays.asList(1000L, 1000L), new ArrayList<>(sleeper.waits));
    }

    @Test
    public void interruptionStopsBeforeTheNextCycle() {
        FakeClient client = new FakeClient();
        sleeper.interruptOnSleep = true;

        StatsProviderUtils.ProfileData result = coordinator(client).fetch("Player", "uuid", true);

        assertTrue(result.isInterrupted());
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS));
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.SHMEADO));
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS));
        assertEquals(1, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.SHMEADO));
    }

    @Test
    public void playerOnlyLookupDoesNotRequestGuild() {
        FakeClient client = new FakeClient();
        client.responses(StatsProviderUtils.ResourceType.PLAYER, StatsProviderUtils.Provider.ABYSS, "{\"player\":{}}");

        StatsProviderUtils.ProfileData result = coordinator(client).fetch("Player", "uuid", false);

        assertTrue(result.getPlayer().isSuccess());
        assertEquals(StatsProviderUtils.Status.NOT_REQUESTED, result.getGuild().getStatus());
        assertEquals(0, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.ABYSS));
        assertEquals(0, client.calls(StatsProviderUtils.ResourceType.GUILD, StatsProviderUtils.Provider.SHMEADO));
    }

    @Test
    public void slowAbyssStartsShmeadoAndTheFirstUsableAnswerWins() throws Exception {
        CountDownLatch abyssCancelled = new CountDownLatch(1);
        AtomicInteger shmeadoCalls = new AtomicInteger();
        StatsProviderUtils.ProviderClient client = (resource, provider, username, uuid) -> {
            if (provider == StatsProviderUtils.Provider.ABYSS) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    abyssCancelled.countDown();
                }
                return "";
            }
            shmeadoCalls.incrementAndGet();
            return "{\"player\":{\"displayname\":\"Shmeado\"}}";
        };

        long started = System.nanoTime();
        StatsProviderUtils.ProfileData result = hedgedCoordinator(client).fetch("Player", "uuid", false);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertTrue(result.getPlayer().isSuccess());
        assertSame(StatsProviderUtils.Provider.SHMEADO, result.getPlayer().getProvider());
        assertEquals(1, shmeadoCalls.get());
        assertTrue("hedge did not cut the wait: " + elapsedMillis + " ms", elapsedMillis < 2000);
        // The losing Abyss request is cancelled rather than left running.
        assertTrue(abyssCancelled.await(2, TimeUnit.SECONDS));
        assertEquals(0, sleeper.waits.size());
    }

    @Test
    public void slowAbyssStillAnswersWhenHedgedShmeadoFails() {
        StatsProviderUtils.ProviderClient client = (resource, provider, username, uuid) -> {
            if (provider == StatsProviderUtils.Provider.SHMEADO) {
                return "";
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                return "";
            }
            return "{\"player\":{\"displayname\":\"Abyss\"}}";
        };

        StatsProviderUtils.ProfileData result = hedgedCoordinator(client).fetch("Player", "uuid", false);

        assertTrue(result.getPlayer().isSuccess());
        assertSame(StatsProviderUtils.Provider.ABYSS, result.getPlayer().getProvider());
        assertEquals(0, sleeper.waits.size());
    }

    @Test
    public void hedgedCycleFailsOnlyAfterBothProvidersFail() {
        AtomicInteger abyssCalls = new AtomicInteger();
        AtomicInteger shmeadoCalls = new AtomicInteger();
        StatsProviderUtils.ProviderClient client = (resource, provider, username, uuid) -> {
            if (provider == StatsProviderUtils.Provider.SHMEADO) {
                shmeadoCalls.incrementAndGet();
                return "";
            }
            abyssCalls.incrementAndGet();
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                return "";
            }
            return "";
        };

        StatsProviderUtils.ProfileData result = hedgedCoordinator(client).fetch("Player", "uuid", false);

        assertTrue(result.getPlayer().isUnavailable());
        assertEquals(3, abyssCalls.get());
        assertEquals(3, shmeadoCalls.get());
        assertEquals(Arrays.asList(1000L, 1000L), new ArrayList<>(sleeper.waits));
    }

    @Test
    public void fastAbyssNeverStartsShmeado() throws Exception {
        AtomicInteger shmeadoCalls = new AtomicInteger();
        StatsProviderUtils.ProviderClient client = (resource, provider, username, uuid) -> {
            if (provider == StatsProviderUtils.Provider.SHMEADO) {
                shmeadoCalls.incrementAndGet();
                return "";
            }
            return resource == StatsProviderUtils.ResourceType.PLAYER
                    ? "{\"player\":{}}"
                    : "{\"tag\":\"ABYSS\"}";
        };

        StatsProviderUtils.ProfileData result = hedgedCoordinator(client).fetch("Player", "uuid", true);
        // Leave time for a hedge that should not exist.
        Thread.sleep(150);

        assertTrue(result.getPlayer().isSuccess());
        assertTrue(result.getGuild().isSuccess());
        assertEquals(0, shmeadoCalls.get());
    }

    @Test
    public void invalidatedCacheGenerationRejectsLateCommit() {
        long generation = StatsProviderUtils.captureCacheGeneration();
        StatsProviderUtils.invalidateCacheGeneration();
        AtomicInteger commits = new AtomicInteger();

        assertFalse(StatsProviderUtils.commitIfCurrent(generation, () -> commits.incrementAndGet()));
        assertEquals(0, commits.get());
    }

    @Test
    public void samePlayerLookupsAreSerialized() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        AtomicInteger order = new AtomicInteger();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();

        Runnable lookup = () -> StatsProviderUtils.withPlayerLock("Player", () -> {
            int current = active.incrementAndGet();
            while (true) {
                int previous = maximumActive.get();
                if (current <= previous || maximumActive.compareAndSet(previous, current)) {
                    break;
                }
            }

            if (order.incrementAndGet() == 1) {
                firstEntered.countDown();
                try {
                    releaseFirst.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                secondEntered.countDown();
            }
            active.decrementAndGet();
        });

        Thread first = new Thread(lookup);
        Thread second = new Thread(lookup);
        first.start();
        assertTrue(firstEntered.await(1, TimeUnit.SECONDS));
        second.start();
        assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS));
        releaseFirst.countDown();

        first.join(2000);
        second.join(2000);
        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertTrue(secondEntered.getCount() == 0);
        assertEquals(1, maximumActive.get());
    }

    private StatsProviderUtils.Coordinator coordinator(StatsProviderUtils.ProviderClient client) {
        return new StatsProviderUtils.Coordinator(client, executor, sleeper);
    }

    /** Hedges after 50 ms so slow-provider cases run quickly. */
    private StatsProviderUtils.Coordinator hedgedCoordinator(StatsProviderUtils.ProviderClient client) {
        return new StatsProviderUtils.Coordinator(client, executor, sleeper, 50, 50);
    }

    private static String key(StatsProviderUtils.ResourceType resource, StatsProviderUtils.Provider provider) {
        return resource.name() + ":" + provider.name();
    }

    private static final class FakeClient implements StatsProviderUtils.ProviderClient {
        private final ConcurrentHashMap<String, Queue<String>> responses = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicInteger> calls = new ConcurrentHashMap<>();

        void responses(
                StatsProviderUtils.ResourceType resource,
                StatsProviderUtils.Provider provider,
                String... values
        ) {
            responses.put(key(resource, provider), new ConcurrentLinkedQueue<>(Arrays.asList(values)));
        }

        int calls(StatsProviderUtils.ResourceType resource, StatsProviderUtils.Provider provider) {
            AtomicInteger count = calls.get(key(resource, provider));
            return count == null ? 0 : count.get();
        }

        @Override
        public String fetch(
                StatsProviderUtils.ResourceType resource,
                StatsProviderUtils.Provider provider,
                String username,
                String uuid
        ) {
            calls.computeIfAbsent(key(resource, provider), ignored -> new AtomicInteger()).incrementAndGet();
            Queue<String> queue = responses.get(key(resource, provider));
            String response = queue == null ? "" : queue.poll();
            return response == null ? "" : response;
        }
    }

    private static final class BlockingClient implements StatsProviderUtils.ProviderClient {
        private final CountDownLatch bothAbyssRequestsStarted = new CountDownLatch(2);
        private final CountDownLatch releaseAbyssRequests = new CountDownLatch(1);
        private volatile StatsProviderUtils.ProfileData result;

        @Override
        public String fetch(
                StatsProviderUtils.ResourceType resource,
                StatsProviderUtils.Provider provider,
                String username,
                String uuid
        ) {
            if (provider == StatsProviderUtils.Provider.ABYSS) {
                bothAbyssRequestsStarted.countDown();
                try {
                    if (!bothAbyssRequestsStarted.await(2, TimeUnit.SECONDS)) {
                        throw new AssertionError("Player and guild did not start concurrently");
                    }
                    releaseAbyssRequests.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "";
                }
                return resource == StatsProviderUtils.ResourceType.PLAYER
                        ? "{\"player\":{}}"
                        : "{\"tag\":\"ABYSS\"}";
            }
            return "";
        }
    }

    private static final class RecordingSleeper implements StatsProviderUtils.Sleeper {
        private final Queue<Long> waits = new ConcurrentLinkedQueue<>();
        private volatile boolean interruptOnSleep;

        @Override
        public void sleep(long millis) throws InterruptedException {
            waits.add(millis);
            if (interruptOnSleep) {
                throw new InterruptedException();
            }
        }
    }
}
