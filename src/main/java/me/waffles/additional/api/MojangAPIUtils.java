package me.waffles.additional.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import me.waffles.additional.util.EldestRemovalMap;
import java.util.regex.Pattern;

public class MojangAPIUtils {
    private static final String USER_AGENT = "node-ao/2.0.3";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{32}");

    static final long CACHE_TTL_NANOS = TimeUnit.MINUTES.toNanos(10);
    private static final EldestRemovalMap<String, CachedUuid> CACHE = new EldestRemovalMap<>(256);

    private record CachedUuid(String uuid, long fetchedAt) {}

    public static void clearCache() { CACHE.clear(); }

    private MojangAPIUtils() {
    }

    public static String fetchUuid(String username) {
        return fetchUuid(username, name -> AbyssAPIUtils.fetchPlayerData(
                "https://api.mojang.com/users/profiles/minecraft/" + name, USER_AGENT), System.nanoTime());
    }

    static String fetchUuid(String username, Function<String, String> request, long now) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return null;
        }

        String key = username.toLowerCase(Locale.ROOT);
        CachedUuid cached = CACHE.get(key);
        if (cached != null && now - cached.fetchedAt() < CACHE_TTL_NANOS) return cached.uuid();
        String response = request.apply(username);
        if (response == null || response.isEmpty()) {
            return null;
        }

        try {
            JsonObject profile = new JsonParser().parse(response).getAsJsonObject();
            if (!profile.has("id") || profile.get("id").isJsonNull()) {
                return null;
            }

            String compactUuid = profile.get("id").getAsString().replace("-", "");
            if (!UUID_PATTERN.matcher(compactUuid).matches()) {
                return null;
            }

            String uuid = UUID.fromString(
                    compactUuid.substring(0, 8) + "-"
                            + compactUuid.substring(8, 12) + "-"
                            + compactUuid.substring(12, 16) + "-"
                            + compactUuid.substring(16, 20) + "-"
                            + compactUuid.substring(20)
            ).toString();
            CACHE.put(key, new CachedUuid(uuid, now));
            return uuid;
        } catch (Exception ignored) {
            return null;
        }
    }
}
