package me.waffles.additional.util;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import me.waffles.additional.api.MojangAPIUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerInfo;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class PlayerUuidResolver {
    public static final PlayerUuidResolver INSTANCE =
            new PlayerUuidResolver(MojangAPIUtils::fetchUuid, Ticker.systemTicker());
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final Cache<String, String> uuidCache;
    private final Function<String, String> externalLookup;

    PlayerUuidResolver(Function<String, String> externalLookup, Ticker ticker) {
        this.externalLookup = externalLookup;
        // Bound memory usage and periodically refresh names that may change owners.
        this.uuidCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .ticker(ticker)
                .build();
    }

    // Call on the client thread, before dispatching the asynchronous lookup.
    public static GameProfile findLocalProfile(String username) {
        if (!isValidUsername(username)) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GameProfile sessionProfile = minecraft.getSession().getProfile();
        if (matches(sessionProfile, username)) {
            return sessionProfile;
        }

        ClientPlayNetworkHandler netHandler = minecraft.getNetworkHandler();
        if (netHandler != null) {
            for (PlayerInfo playerInfo : netHandler.getOnlinePlayers()) {
                GameProfile profile = playerInfo.getProfile();
                if (matches(profile, username)) {
                    return profile;
                }
            }
        }
        return null;
    }

    // May perform a network request; call from the commands' background worker.
    public String resolveUuid(String username, GameProfile localProfile) {
        if (!isValidUsername(username)) {
            return null;
        }

        String key = username.toLowerCase(Locale.ROOT);
        if (matches(localProfile, username)) {
            String uuid = localProfile.getId().toString();
            uuidCache.put(key, uuid);
            return uuid;
        }

        String cachedUuid = uuidCache.getIfPresent(key);
        if (cachedUuid != null) {
            return cachedUuid;
        }

        String uuid = externalLookup.apply(username);
        if (uuid != null) {
            uuidCache.put(key, uuid);
        }
        return uuid;
    }

    private static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    private static boolean matches(GameProfile profile, String username) {
        return profile != null
                && username.equalsIgnoreCase(profile.getName())
                && profile.getId() != null
                // Account UUIDs are version 4; NPC/offline UUIDs need a real lookup.
                && profile.getId().version() == 4;
    }
}
