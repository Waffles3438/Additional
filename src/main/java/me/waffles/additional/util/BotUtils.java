package me.waffles.additional.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.UUID;

public class BotUtils {
    private static final HashMap<UUID, Boolean> botCache = new HashMap<>();

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return true;
        EntityPlayer player = (EntityPlayer) entity;
        UUID uuid = player.getUniqueID();

        Boolean cached = botCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        if (uuid.version() == 2) {
            botCache.put(uuid, true);
            return true;
        }

        // Not in tab list — could be a real player whose PlayerListItem
        // packet just hasn't arrived yet. Don't cache; try again later.
        NetworkPlayerInfo info = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(uuid);
        if (info == null) {
            return true;
        }

        String name = player.getName();
        boolean result;
        if (name == null || name.replaceAll("[^a-zA-Z0-9_]", "").isEmpty()) {
            result = true;
        } else {
            result = name.contains("[NPC]") || name.contains("[BOT]") || name.contains("npc-");
        }

        botCache.put(uuid, result);
        return result;
    }

    public static void clearCache() {
        botCache.clear();
    }
}