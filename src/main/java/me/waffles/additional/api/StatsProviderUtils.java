package me.waffles.additional.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordinates player and guild requests across the available providers.
 *
 * Player and guild data are independent resources. Each cycle asks Abyss first.
 * Shmeado is asked as soon as Abyss fails, or in parallel once Abyss is slower
 * than it normally is, and the first usable answer wins. Missing resources are
 * retried on their own after an interruptible wait.
 */
public final class StatsProviderUtils {
    private static final int MAX_CYCLES = 3;
    private static final long WAIT_BETWEEN_CYCLES_MILLIS = 1000L;
    // Measured Abyss responses: player 0.8-1.4 s (a ~580 KB body), guild 0.2-0.4 s.
    // Past these delays Abyss is struggling. Shmeado needs ~4.5 s for a player
    // page and ~0.4 s for a guild, so starting it then bounds the total wait.
    private static final long PLAYER_HEDGE_MILLIS = 3000L;
    private static final long GUILD_HEDGE_MILLIS = 1500L;
    private static final String ABYSS_PLAYER_URL = "http://api.abyssoverlay.com/player?uuid=";
    private static final String ABYSS_GUILD_URL = "http://api.abyssoverlay.com/guild?uuid=";
    private static final String USER_AGENT = "node-ao/2.0.3";
    private static final Logger LOGGER = LogManager.getLogger("Additional");
    private static final Map<String, LockEntry> LOOKUP_LOCKS = new HashMap<>();
    private static final Object CACHE_GENERATION_LOCK = new Object();
    private static long CACHE_GENERATION;

    /**
     * One virtual thread per HTTP request. A request that loses the race is
     * cancelled, and interrupting a virtual thread blocked on a socket closes
     * that socket, so abandoned requests end early instead of holding a thread.
     */
    private static final ExecutorService REQUEST_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("additional-api-", 1).factory());

    /** Bounds load on the provider APIs when many players are checked at once. */
    private static final Semaphore REQUEST_PERMITS = new Semaphore(8);

    private static final ProviderClient HTTP_PROVIDER_CLIENT = new ProviderClient() {
        @Override
        public String fetch(ResourceType resource, Provider provider, String username, String uuid) {
            try {
                REQUEST_PERMITS.acquire();
            } catch (InterruptedException e) {
                // Cancelled while queued because the other provider already answered.
                Thread.currentThread().interrupt();
                return "";
            }
            try {
                return fetchNow(resource, provider, username, uuid);
            } finally {
                REQUEST_PERMITS.release();
            }
        }

        private String fetchNow(ResourceType resource, Provider provider, String username, String uuid) {
            if (resource == ResourceType.PLAYER) {
                return provider == Provider.ABYSS
                        ? AbyssAPIUtils.fetchPlayerDataOnce(ABYSS_PLAYER_URL + uuid, USER_AGENT)
                        : ShmeadoAPIUtils.fetchPlayerStatsJsonOnce(username);
            }

            if (provider == Provider.ABYSS) {
                String response = AbyssAPIUtils.fetchPlayerDataOnce(ABYSS_GUILD_URL + uuid, USER_AGENT);
                return normalizeAbyssGuildResponse(response);
            }
            return ShmeadoAPIUtils.fetchPlayerGuildJsonOnce(uuid);
        }
    };

    private static final Coordinator PROFILE_COORDINATOR =
            new Coordinator(HTTP_PROVIDER_CLIENT, REQUEST_EXECUTOR, Thread::sleep);

    private StatsProviderUtils() {
    }

    /**
     * Serializes lookups for one player across the Bedwars and Duels commands
     * while allowing different players to fetch concurrently.
     */
    public static void withPlayerLock(String username, Runnable action) {
        String key = username.toLowerCase(Locale.ROOT);
        LockEntry entry;
        synchronized (LOOKUP_LOCKS) {
            entry = LOOKUP_LOCKS.computeIfAbsent(key, ignored -> new LockEntry());
            entry.references++;
        }
        entry.lock.lock();
        try {
            action.run();
        } finally {
            entry.lock.unlock();
            synchronized (LOOKUP_LOCKS) {
                if (--entry.references == 0) {
                    LOOKUP_LOCKS.remove(key, entry);
                }
            }
        }
    }

    /** Returns the cache generation captured before a lookup begins. */
    public static long captureCacheGeneration() {
        synchronized (CACHE_GENERATION_LOCK) {
            return CACHE_GENERATION;
        }
    }

    /**
     * Commits cache mutations only if the cache was not invalidated while the
     * network request was running.
     */
    public static boolean commitIfCurrent(long generation, Runnable action) {
        synchronized (CACHE_GENERATION_LOCK) {
            if (generation != CACHE_GENERATION) {
                return false;
            }
            action.run();
            return true;
        }
    }

    /** Invalidates results from all lookups that are currently in flight. */
    public static void invalidateCacheGeneration() {
        synchronized (CACHE_GENERATION_LOCK) {
            CACHE_GENERATION++;
        }
    }

    public enum Provider {
        ABYSS,
        SHMEADO
    }

    public enum ResourceType {
        PLAYER,
        GUILD
    }

    public enum Status {
        SUCCESS,
        UNAVAILABLE,
        INTERRUPTED,
        NOT_REQUESTED
    }

    /** Result for one independent player or guild resource. */
    public static final class ResourceResult {
        private final ResourceType resource;
        private final String json;
        private final JsonObject data;
        private final Provider provider;
        private final Status status;

        private ResourceResult(ResourceType resource, String json, JsonObject data, Provider provider, Status status) {
            this.resource = resource;
            this.json = json;
            this.data = data;
            this.provider = provider;
            this.status = status;
        }

        public ResourceType getResource() {
            return resource;
        }

        public String getJson() {
            return json;
        }

        /** The validated response, shared by the read-only stats/profile parsers. */
        public JsonObject getData() { return data; }

        public Provider getProvider() {
            return provider;
        }

        public Status getStatus() {
            return status;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public boolean isUnavailable() {
            return status == Status.UNAVAILABLE;
        }

        public boolean isInterrupted() {
            return status == Status.INTERRUPTED;
        }
    }

    /** Combined result that retains the provider selected for each resource. */
    public static final class ProfileData {
        private final ResourceResult player;
        private final ResourceResult guild;
        private final boolean interrupted;

        private ProfileData(ResourceResult player, ResourceResult guild, boolean interrupted) {
            this.player = player;
            this.guild = guild;
            this.interrupted = interrupted;
        }

        public ResourceResult getPlayer() {
            return player;
        }

        public ResourceResult getGuild() {
            return guild;
        }

        public boolean isInterrupted() {
            return interrupted || player.isInterrupted() || guild.isInterrupted();
        }
    }

    /**
     * Compatibility result for callers that only need player data.
     */
    public static final class PlayerData {
        private final String json;
        private final Provider provider;
        private final boolean interrupted;

        private PlayerData(String json, Provider provider, boolean interrupted) {
            this.json = json;
            this.provider = provider;
            this.interrupted = interrupted;
        }

        public String getJson() {
            return json;
        }

        public Provider getProvider() {
            return provider;
        }

        public boolean isInterrupted() {
            return interrupted;
        }
    }

    /**
     * Fetches the player resource and, when requested, the guild resource.
     * Player and guild calls run independently and may use different providers.
     */
    public static ProfileData fetchProfileData(String username, String uuid, boolean needGuild) {
        return PROFILE_COORDINATOR.fetch(username, uuid, needGuild);
    }

    /**
     * Preserves the earlier player-only API while using the new coordinator.
     */
    public static PlayerData fetchPlayerData(String username, String uuid) {
        ProfileData profileData = fetchProfileData(username, uuid, false);
        ResourceResult player = profileData.getPlayer();
        return new PlayerData(
                player.getJson(),
                player.getProvider(),
                profileData.isInterrupted()
        );
    }

    /**
     * Injectable coordinator used by production code and deterministic tests.
     */
    static final class Coordinator {
        private final ProviderClient providerClient;
        private final ExecutorService executor;
        private final Sleeper sleeper;
        private final long playerHedgeNanos;
        private final long guildHedgeNanos;

        Coordinator(ProviderClient providerClient, ExecutorService executor, Sleeper sleeper) {
            this(providerClient, executor, sleeper, PLAYER_HEDGE_MILLIS, GUILD_HEDGE_MILLIS);
        }

        Coordinator(ProviderClient providerClient, ExecutorService executor, Sleeper sleeper,
                    long playerHedgeMillis, long guildHedgeMillis) {
            this.providerClient = providerClient;
            this.executor = executor;
            this.sleeper = sleeper;
            this.playerHedgeNanos = TimeUnit.MILLISECONDS.toNanos(playerHedgeMillis);
            this.guildHedgeNanos = TimeUnit.MILLISECONDS.toNanos(guildHedgeMillis);
        }

        ProfileData fetch(String username, String uuid, boolean needGuild) {
            ResourceResult player = unavailable(ResourceType.PLAYER);
            ResourceResult guild = needGuild
                    ? unavailable(ResourceType.GUILD)
                    : notRequested(ResourceType.GUILD);
            boolean playerMissing = true;
            boolean guildMissing = needGuild;

            for (int cycle = 1; cycle <= MAX_CYCLES && (playerMissing || guildMissing); cycle++) {
                if (Thread.currentThread().isInterrupted()) {
                    return interruptedProfile(player, guild);
                }

                Round round = new Round(username, uuid, cycle);
                if (playerMissing) {
                    round.start(ResourceType.PLAYER);
                }
                if (guildMissing) {
                    round.start(ResourceType.GUILD);
                }

                try {
                    round.await();
                } catch (InterruptedException e) {
                    round.cancelAll();
                    Thread.currentThread().interrupt();
                    return interruptedProfile(player, guild);
                }

                if (playerMissing) {
                    player = round.result(ResourceType.PLAYER);
                }
                if (guildMissing) {
                    guild = round.result(ResourceType.GUILD);
                }

                playerMissing = !player.isSuccess();
                guildMissing = needGuild && !guild.isSuccess();

                if ((playerMissing || guildMissing) && cycle < MAX_CYCLES) {
                    try {
                        sleeper.sleep(WAIT_BETWEEN_CYCLES_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return interruptedProfile(player, guild);
                    }
                }
            }

            if (Thread.currentThread().isInterrupted()) {
                return interruptedProfile(player, guild);
            }
            return new ProfileData(player, guild, false);
        }

        private String fetchSafely(
                ResourceType resource,
                Provider provider,
                String username,
                String uuid
        ) {
            try {
                return providerClient.fetch(resource, provider, username, uuid);
            } catch (Exception e) {
                LOGGER.warn(
                        "{} request through {} failed on the current attempt.",
                        resourceName(resource),
                        provider,
                        e
                );
                return "";
            }
        }

        /**
         * One cycle for the resources that are still missing. Only this
         * coordinating thread reads or changes the request state; the request
         * threads report through the queue.
         */
        private final class Round {
            private final String username;
            private final String uuid;
            private final int cycle;
            private final BlockingQueue<Attempt> completed = new LinkedBlockingQueue<>();
            private final Request[] requests = new Request[ResourceType.values().length];

            private Round(String username, String uuid, int cycle) {
                this.username = username;
                this.uuid = uuid;
                this.cycle = cycle;
            }

            private void start(ResourceType resource) {
                long hedgeDelay = resource == ResourceType.PLAYER ? playerHedgeNanos : guildHedgeNanos;
                Request request = new Request(resource, System.nanoTime() + hedgeDelay);
                requests[resource.ordinal()] = request;
                request.abyss = submit(resource, Provider.ABYSS);
            }

            /** Returns once every started resource has an answer or both providers failed. */
            private void await() throws InterruptedException {
                while (true) {
                    boolean pending = false;
                    long waitNanos = Long.MAX_VALUE;
                    long now = System.nanoTime();
                    for (Request request : requests) {
                        if (request == null || request.result != null) {
                            continue;
                        }
                        pending = true;
                        if (request.shmeado != null) {
                            continue;
                        }
                        long remaining = request.hedgeAt - now;
                        if (remaining > 0) {
                            waitNanos = Math.min(waitNanos, remaining);
                        } else {
                            LOGGER.info(
                                    "{} request through Abyss is slow on cycle {}; starting Shmeado in parallel.",
                                    resourceName(request.resource),
                                    cycle
                            );
                            request.shmeado = submit(request.resource, Provider.SHMEADO);
                        }
                    }
                    if (!pending) {
                        return;
                    }

                    Attempt attempt = waitNanos == Long.MAX_VALUE
                            ? completed.take()
                            : completed.poll(waitNanos, TimeUnit.NANOSECONDS);
                    if (attempt != null) {
                        accept(attempt);
                    }
                }
            }

            private void accept(Attempt attempt) {
                Request request = requests[attempt.resource().ordinal()];
                if (request.result != null) {
                    // The slower provider of a request that already has an answer.
                    return;
                }
                if (attempt.data() != null) {
                    request.result = success(attempt.resource(), attempt.json(), attempt.data(), attempt.provider());
                    cancel(attempt.provider() == Provider.ABYSS ? request.shmeado : request.abyss);
                    return;
                }

                if (attempt.provider() == Provider.ABYSS) {
                    request.abyssFailed = true;
                    if (request.shmeado == null) {
                        LOGGER.info(
                                "{} request failed through Abyss on cycle {}; trying Shmeado immediately.",
                                resourceName(request.resource),
                                cycle
                        );
                        request.shmeado = submit(request.resource, Provider.SHMEADO);
                        return;
                    }
                } else {
                    // Abyss may still answer after a hedged Shmeado request fails.
                    request.shmeadoFailed = true;
                }

                if (request.abyssFailed && request.shmeadoFailed) {
                    LOGGER.info(
                            "{} request was unavailable through both providers on cycle {}.",
                            resourceName(request.resource),
                            cycle
                    );
                    request.result = unavailable(request.resource);
                }
            }

            private ResourceResult result(ResourceType resource) {
                return requests[resource.ordinal()].result;
            }

            private void cancelAll() {
                for (Request request : requests) {
                    if (request != null) {
                        cancel(request.abyss);
                        cancel(request.shmeado);
                    }
                }
            }

            private Future<?> submit(ResourceType resource, Provider provider) {
                return executor.submit(() -> {
                    String response = "";
                    JsonObject data = null;
                    try {
                        response = fetchSafely(resource, provider, username, uuid);
                        data = parseUsableResponse(resource, response);
                    } finally {
                        // Report even after an Error so await() can never wait forever.
                        completed.add(new Attempt(resource, provider, response, data));
                    }
                });
            }
        }

        private static final class Request {
            private final ResourceType resource;
            private final long hedgeAt;
            private Future<?> abyss;
            private Future<?> shmeado;
            private boolean abyssFailed;
            private boolean shmeadoFailed;
            private ResourceResult result;

            private Request(ResourceType resource, long hedgeAt) {
                this.resource = resource;
                this.hedgeAt = hedgeAt;
            }
        }

        /** One provider's answer; data is null when the response was unusable. */
        private record Attempt(ResourceType resource, Provider provider, String json, JsonObject data) {
        }
    }

    private static final class LockEntry {
        private final ReentrantLock lock = new ReentrantLock();
        // All reference changes are under LOOKUP_LOCKS' monitor.
        private int references;
    }

    interface ProviderClient {
        String fetch(ResourceType resource, Provider provider, String username, String uuid);
    }

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private static ResourceResult success(ResourceType resource, String json, JsonObject data, Provider provider) {
        return new ResourceResult(resource, json, data, provider, Status.SUCCESS);
    }

    private static ResourceResult unavailable(ResourceType resource) {
        return new ResourceResult(resource, null, null, null, Status.UNAVAILABLE);
    }

    private static ResourceResult interrupted(ResourceType resource) {
        return new ResourceResult(resource, null, null, null, Status.INTERRUPTED);
    }

    private static ResourceResult notRequested(ResourceType resource) {
        return new ResourceResult(resource, null, null, null, Status.NOT_REQUESTED);
    }

    private static ProfileData interruptedProfile(ResourceResult player, ResourceResult guild) {
        ResourceResult interruptedPlayer = player.isSuccess()
                ? player
                : interrupted(ResourceType.PLAYER);
        ResourceResult interruptedGuild = guild.isSuccess() || guild.getStatus() == Status.NOT_REQUESTED
                ? guild
                : interrupted(ResourceType.GUILD);
        return new ProfileData(interruptedPlayer, interruptedGuild, true);
    }

    private static void cancel(Future<?> future) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    private static JsonObject parseUsableResponse(ResourceType resource, String response) {
        if (!hasResponse(response)) {
            return null;
        }

        try {
            JsonElement root = new JsonParser().parse(response);
            if (!root.isJsonObject()) {
                return null;
            }
            if (resource == ResourceType.GUILD) {
                return root.getAsJsonObject();
            }

            JsonElement player = root.getAsJsonObject().get("player");
            return player != null && (player.isJsonNull() || player.isJsonObject()) ? root.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasResponse(String response) {
        return response != null && !response.isEmpty();
    }

    private static String resourceName(ResourceType resource) {
        return resource.name().toLowerCase(Locale.ROOT);
    }

    private static String normalizeAbyssGuildResponse(String response) {
        if (!hasResponse(response)) {
            return "";
        }

        try {
            JsonObject root = new JsonParser().parse(response).getAsJsonObject();
            boolean hasSuccess = root.has("success");
            if (hasSuccess && !root.get("success").getAsBoolean()) {
                return "";
            }

            JsonObject source;
            if (root.has("guild")) {
                if (root.get("guild").isJsonNull()) {
                    return "{}";
                }
                source = root.getAsJsonObject("guild");
            } else if (root.has("tag") || root.has("tagColor")) {
                source = root;
            } else if (hasSuccess) {
                return "{}";
            } else {
                return "";
            }

            JsonObject result = new JsonObject();
            if (source.has("tag") && !source.get("tag").isJsonNull()) {
                result.addProperty("tag", source.get("tag").getAsString());
            }
            if (source.has("tagColor") && !source.get("tagColor").isJsonNull()) {
                result.addProperty("tagColor", source.get("tagColor").getAsString());
            }
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
