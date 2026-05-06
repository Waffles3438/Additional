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
        return botCache.computeIfAbsent(entity.getUniqueID(), uuid -> {
            if (!(entity instanceof EntityPlayer)) return true;

            NetworkPlayerInfo info = Minecraft.getMinecraft()
                    .getNetHandler()
                    .getPlayerInfo(uuid);

            if (info == null) return true;

            String name = entity.getName();
            if (name == null) return true;
            return name.contains("[NPC]") || name.contains("[BOT]") || name.contains("npc-");
        });
    }

    public static void clearCache() {
        botCache.clear();
    }
}