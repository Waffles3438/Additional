package me.waffles.additional;

import com.google.common.cache.Cache;
import me.waffles.additional.command.StatsCommands;
import me.waffles.additional.config.ModConfig;
import me.waffles.additional.mixin.EntityLivingBaseAccessor;
import me.waffles.additional.playerData.Bedwars;
import me.waffles.additional.playerData.Duels;
import me.waffles.additional.playerData.PlayerProfile;
import me.waffles.additional.util.BotUtils;
import me.waffles.additional.util.StatsCache;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class Additional implements ClientModInitializer {
    public static final String MODID = "additional";
    public static final String NAME = "Additional";
    public static ModConfig config;
    public static Cache<String, Duels> duelsStatsList;
    public static Cache<String, Bedwars> bedwarsStatsList;
    public static Cache<String, PlayerProfile> playerProfileList;
    private static int cacheCleanupTicks;

    @Override
    public void onInitializeClient() {
        duelsStatsList = StatsCache.create();
        bedwarsStatsList = StatsCache.create();
        playerProfileList = StatsCache.create();
        config = new ModConfig();
        config.preload();
        StatsCommands.register();
    }

    public static void tickStart() {
        Minecraft minecraft = Minecraft.getInstance();
        if (config != null && minecraft.player != null && ModConfig.ndj) {
            EntityLivingBaseAccessor player = (EntityLivingBaseAccessor) minecraft.player;
            if (player.getJumpTicks() > ModConfig.jumpTicks) player.setJumpTicks(ModConfig.jumpTicks);
        }
    }

    public static void tickEnd() {
        if (config != null && ++cacheCleanupTicks >= 1200) {
            cacheCleanupTicks = 0;
            bedwarsStatsList.cleanUp();
            duelsStatsList.cleanUp();
            playerProfileList.cleanUp();
        }
    }

    public static void worldChanged() {
        // Includes lobby/game transfers and disconnects.
        BotUtils.clearCache();
    }
}
