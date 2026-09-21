package me.waffles.additional.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class BotUtils {

    // Cleared whenever the client changes worlds, including disconnects.
    private static final Map<UUID, Boolean> botCache = new ConcurrentHashMap<>();

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9_]");

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return true;
        PlayerEntity player = (PlayerEntity) entity;
        UUID uuid = player.getUuid();

        Boolean cached = botCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        if (uuid.version() == 2) {
            botCache.put(uuid, true);
            return true;
        }

        // Null between leaving a world and joining the next, and on the main menu.
        // The rendering mixins can still be queried in that
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

        botCache.put(uuid, result);
        return result;
    }

    public static void clearCache() {
        botCache.clear();
    }
}
