package me.waffles.additional.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Coordinates player and guild requests across the available providers.
 *
 * Player and guild data are independent resources. Each missing resource gets
 * one Abyss attempt followed immediately by one Shmeado attempt per cycle.
 * Missing resources are retried on their own after an interruptible wait.
 */
public final class StatsProviderUtils {
    private static final int MAX_CYCLES = 3;
    private static final long WAIT_BETWEEN_CYCLES_MILLIS = 1000L;
    private static final String ABYSS_PLAYER_URL = "http://api.abyssoverlay.com/player?uuid=";
    private static final String ABYSS_GUILD_URL = "http://api.abyssoverlay.com/guild?uuid=";
    private static final String USER_AGENT = "node-ao/2.0.3";
    private static final Logger LOGGER = LogManager.getLogger("Additional");
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final Map<String, LockEntry> LOOKUP_LOCKS = new HashMap<>();
    private static final Object CACHE_GENERATION_LOCK = new Object();
    private static long CACHE_GENERATION;

    /**
     * Daemon workers prevent an unfinished network request from keeping the
     * client process alive. HTTP calls are still bounded by their own timeouts.
     */
    private static final ExecutorService REQUEST_EXECUTOR = Executors.newFixedThreadPool(
            4,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(
                            runnable,
                            "additional-api-" + THREAD_ID.incrementAndGet()
                    );
                    thread.setDaemon(true);
                    return thread;
                }
            }
    );

    private static final ProviderClient HTTP_PROVIDER_CLIENT = new ProviderClient() {
        @Override
        public String fetch(ResourceType resource, Provider provider, String username, String uuid) {
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

        Coordinator(ProviderClient providerClient, ExecutorService executor, Sleeper sleeper) {
            this.providerClient = providerClient;
            this.executor = executor;
            this.sleeper = sleeper;
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

                Future<ResourceResult> playerFuture = playerMissing
                        ? submit(ResourceType.PLAYER, username, uuid, cycle)
                        : null;
                Future<ResourceResult> guildFuture = guildMissing
                        ? submit(ResourceType.GUILD, username, uuid, cycle)
                        : null;

                try {
                    if (playerFuture != null) {
                        player = getResult(playerFuture, ResourceType.PLAYER, cycle);
                    }
                    if (guildFuture != null) {
                        guild = getResult(guildFuture, ResourceType.GUILD, cycle);
                    }
                } catch (InterruptedException e) {
                    cancel(playerFuture);
                    cancel(guildFuture);
                    Thread.currentThread().interrupt();
                    return interruptedProfile(player, guild);
                }

                if (player.isInterrupted() || guild.isInterrupted()) {
                    cancel(playerFuture);
                    cancel(guildFuture);
                    return interruptedProfile(player, guild);
                }
                if (Thread.currentThread().isInterrupted()) {
                    cancel(playerFuture);
                    cancel(guildFuture);
                    return interruptedProfile(player, guild);
                }

                playerMissing = !player.isSuccess();
                guildMissing = needGuild && !guild.isSuccess();

                if ((playerMissing || guildMissing) && cycle < MAX_CYCLES) {
                    try {
                        sleeper.sleep(WAIT_BETWEEN_CYCLES_MILLIS);
                    } catch (InterruptedException e) {
                        cancel(playerFuture);
                        cancel(guildFuture);
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

        private Future<ResourceResult> submit(
                ResourceType resource,
                String username,
                String uuid,
                int cycle
        ) {
            return executor.submit(() -> fetchResource(resource, username, uuid, cycle));
        }

        private ResourceResult fetchResource(
                ResourceType resource,
                String username,
                String uuid,
                int cycle
        ) {
            if (Thread.currentThread().isInterrupted()) {
                return interrupted(resource);
            }

            String abyssResponse = fetchSafely(resource, Provider.ABYSS, username, uuid);
            JsonObject abyssData = parseUsableResponse(resource, abyssResponse);
            if (abyssData != null) {
                return success(resource, abyssResponse, abyssData, Provider.ABYSS);
            }

            LOGGER.info(
                    "{} request failed through Abyss on cycle {}; trying Shmeado immediately.",
                    resourceName(resource),
                    cycle
            );
            if (Thread.currentThread().isInterrupted()) {
                return interrupted(resource);
            }

            String shmeadoResponse = fetchSafely(resource, Provider.SHMEADO, username, uuid);
            JsonObject shmeadoData = parseUsableResponse(resource, shmeadoResponse);
            if (shmeadoData != null) {
                return success(resource, shmeadoResponse, shmeadoData, Provider.SHMEADO);
            }

            LOGGER.info(
                    "{} request was unavailable through both providers on cycle {}.",
                    resourceName(resource),
                    cycle
            );
            return unavailable(resource);
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

        private ResourceResult getResult(
                Future<ResourceResult> future,
                ResourceType resource,
                int cycle
        ) throws InterruptedException {
            try {
                ResourceResult result = future.get();
                return result == null ? unavailable(resource) : result;
            } catch (CancellationException e) {
                return interrupted(resource);
            } catch (ExecutionException e) {
                LOGGER.warn(
                        "{} request task failed on cycle {}.",
                        resourceName(resource),
                        cycle,
                        e.getCause()
                );
                return unavailable(resource);
            }
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
