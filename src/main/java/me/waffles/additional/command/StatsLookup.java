package me.waffles.additional.command;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import me.waffles.additional.Additional;
import me.waffles.additional.api.AbyssAPIUtils;
import me.waffles.additional.api.StatsProviderUtils;
import me.waffles.additional.playerData.Bedwars;
import me.waffles.additional.playerData.Duels;
import me.waffles.additional.playerData.PlayerProfile;
import me.waffles.additional.util.ClientTasks;
import me.waffles.additional.util.PlayerUuidResolver;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.function.Function;

/** Lookup and caching shared by /bw and /d. */
final class StatsLookup {
    private static final Logger LOGGER = LogManager.getLogger("Additional");

    enum Mode {
        BEDWARS("Bedwars"),
        DUELS("Duels");

        private final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    /** Cached views of one player; a mode's stats may be null when only the other mode is cached. */
    record Snapshot(PlayerProfile profile, Bedwars bedwars, Duels duels) {
        private Object stats(Mode mode) {
            return mode == Mode.BEDWARS ? bedwars : duels;
        }
    }

    interface Printer {
        void print(String username, Snapshot snapshot);
    }

    private StatsLookup() {
    }

    /** Looks up the local player. Call on the client thread. */
    static void self(Mode mode, Printer printer) {
        // Session.getProfile() parses the UUID on every call; read it once.
        GameProfile session = Minecraft.getInstance().getSession().getProfile();
        String username = session.getName();
        String uuid = session.getId().toString();
        ClientTasks.runAsync(() -> fetchAndPrint(username, uuid, mode, printer));
    }

    /** Looks up a named player. Call on the client thread. */
    static void named(String username, Mode mode, Printer printer) {
        GameProfile localProfile = PlayerUuidResolver.findLocalProfile(username);
        ClientTasks.runAsync(() -> {
            if (username == null || username.isEmpty()) {
                ClientTasks.chat("Invalid player");
                return;
            }

            // Cached players print without resolving a UUID or taking the lock.
            Snapshot cached = cached(key(username));
            if (cached.profile() != null && cached.stats(mode) != null) {
                printer.print(username, cached);
                return;
            }

            String uuid = PlayerUuidResolver.INSTANCE.resolveUuid(username, localProfile);
            if (uuid == null) {
                ClientTasks.chat("Invalid player");
                return;
            }

            fetchAndPrint(username, uuid, mode, printer);
        });
    }

    private static void fetchAndPrint(String username, String uuid, Mode mode, Printer printer) {
        // /bw and /d for the same player share one request: the second waits
        // here and then finds both modes cached by the first.
        StatsProviderUtils.withPlayerLock(username, () -> {
            Snapshot snapshot = load(username, uuid, mode);
            if (snapshot != null) {
                printer.print(username, snapshot);
            }
        });
    }

    /** Returns cached or freshly fetched data, or null after reporting a failure. */
    private static Snapshot load(String username, String uuid, Mode mode) {
        String key = key(username);
        // Keep snapshots so expiration during a request cannot invalidate printing.
        Snapshot cached = cached(key);
        boolean needProfile = cached.profile() == null;
        if (!needProfile && cached.stats(mode) != null) {
            return cached;
        }

        long generation = StatsProviderUtils.captureCacheGeneration();
        StatsProviderUtils.ProfileData profileData =
                StatsProviderUtils.fetchProfileData(username, uuid, needProfile);
        if (profileData.isInterrupted()) {
            return null;
        }

        StatsProviderUtils.ResourceResult playerData = profileData.getPlayer();
        if (!playerData.isSuccess()) {
            LOGGER.warn("Player data was unavailable for {} after all provider cycles.", username);
            ClientTasks.chat("Something went wrong while fetching stats for " + username + ". Please try again.");
            return null;
        }

        StatsProviderUtils.ResourceResult guildData = profileData.getGuild();
        JsonObject guild = null;
        if (needProfile) {
            guild = guildData.isSuccess() ? guildData.getData() : new JsonObject();
        }
        Snapshot fetched = store(key, generation, playerData.getData(), guild, cached.profile());
        if (fetched == null) {
            // The cache was cleared while this request ran.
            return null;
        }
        if (fetched.profile() == null || fetched.stats(mode) == null) {
            ClientTasks.chat("Something went wrong while fetching stats for " + username + ". Please try again.");
            return null;
        }

        if (playerData.getProvider() == StatsProviderUtils.Provider.SHMEADO) {
            LOGGER.info("Using Shmeado instead of Abyss for {} {} player data.", username, mode.label);
        }
        if (needProfile) {
            if (guildData.isSuccess() && guildData.getProvider() == StatsProviderUtils.Provider.SHMEADO) {
                LOGGER.info("Using Shmeado instead of Abyss for {} {} guild data.", username, mode.label);
            }
            if (guildData.isUnavailable()) {
                LOGGER.warn("Guild data was unavailable for {}; showing stats without a guild tag.", username);
                ClientTasks.chat(username + " guild data is unavailable; showing stats without a guild tag.");
            }
        }
        return fetched;
    }

    /**
     * Parses one player response into every cached view. A player response
     * holds both modes' stats, so both are cached and the other command can
     * print without another request.
     *
     * @param guild the guild response, or null when cachedProfile is still current
     * @return the stored views, or null when the cache was cleared during the request
     */
    static Snapshot store(String key, long generation, JsonObject player, JsonObject guild,
                          PlayerProfile cachedProfile) {
        Bedwars bedwars = parse("Bedwars stats", player, AbyssAPIUtils::parseBedwarsPlayerData);
        Duels duels = parse("Duels stats", player, AbyssAPIUtils::parseDuelsPlayerData);
        PlayerProfile profile = guild == null
                ? cachedProfile
                : parse("profile", player, json -> AbyssAPIUtils.parsePlayerProfilePlayerData(json, guild));

        boolean committed = StatsProviderUtils.commitIfCurrent(generation, () -> {
            if (bedwars != null) {
                Additional.bedwarsStatsList.put(key, bedwars);
            }
            if (duels != null) {
                Additional.duelsStatsList.put(key, duels);
            }
            if (guild != null && profile != null) {
                Additional.playerProfileList.put(key, profile);
            }
        });
        return committed ? new Snapshot(profile, bedwars, duels) : null;
    }

    private static <T> T parse(String what, JsonObject player, Function<JsonObject, T> parser) {
        try {
            return parser.apply(player);
        } catch (RuntimeException e) {
            // An unexpected value in one mode must not discard the other mode.
            LOGGER.warn("Could not parse {} from the player response.", what, e);
            return null;
        }
    }

    private static Snapshot cached(String key) {
        return new Snapshot(
                Additional.playerProfileList.getIfPresent(key),
                Additional.bedwarsStatsList.getIfPresent(key),
                Additional.duelsStatsList.getIfPresent(key)
        );
    }

    private static String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
