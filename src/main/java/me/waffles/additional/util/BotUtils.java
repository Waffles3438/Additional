package me.waffles.additional.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;

import me.waffles.additional.render.PlayerNametagCache;
import java.util.UUID;
import java.util.regex.Pattern;

public class BotUtils {

    private static volatile long cacheGeneration;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9_]");

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return true;
        PlayerEntity player = (PlayerEntity) entity;
        PlayerNametagCache cache = (PlayerNametagCache) player;
        long generation = cacheGeneration;

        Boolean cached = cache.additional$getBot(generation);
        if (cached != null) {
            return cached;
        }

        UUID uuid = player.getUuid();

        if (uuid.version() == 2) {
            cache.additional$setBot(generation, true);
            return true;
        }

        // Null between leaving a world and joining the next, and on the main menu.
        // The late render pass and the nametag mixins can both still fire in that
        // window, so this cannot be dereferenced blind.
        ClientPlayNetworkHandler netHandler = Minecraft.getInstance().getNetworkHandler();
        if (netHandler == null) {
            return true; // not cached - we simply cannot tell yet
        }

        PlayerInfo info = netHandler.getOnlinePlayer(uuid);
        if (info == null) {
            return true; // not cached — tab entry may just not have arrived yet
        }

        // Use the tab-list profile's name, not player.getName() — the entity's
        // own GameProfile can be permanently null-named if SpawnPlayer raced
        // ahead of the PlayerListItem packet at spawn time.
        String name = info.getProfile().getName();
        boolean result = name == null || NON_ALPHANUMERIC.matcher(name).find();

        cache.additional$setBot(generation, result);
        return result;
    }

    public static void clearCache() {
        cacheGeneration++;
    }
}
