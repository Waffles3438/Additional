package me.waffles.additional;

import me.waffles.additional.command.StatsCommands;
import me.waffles.additional.config.ModConfig;
import me.waffles.additional.playerData.Bedwars;
import me.waffles.additional.playerData.Duels;
import me.waffles.additional.playerData.PlayerProfile;
import me.waffles.additional.util.EldestRemovalMap;
import net.fabricmc.api.ClientModInitializer;

public class Additional implements ClientModInitializer {
    public static final String MODID = "additional";
    public static final String NAME = "Additional";
    public static ModConfig config;
    public static EldestRemovalMap<String, Duels> duelsStatsList;
    public static EldestRemovalMap<String, Bedwars> bedwarsStatsList;
    public static EldestRemovalMap<String, PlayerProfile> playerProfileList;

    @Override
    public void onInitializeClient() {
        config = new ModConfig();
        config.preload();
        duelsStatsList = new EldestRemovalMap<>(ModConfig.maxCacheSize);
        bedwarsStatsList = new EldestRemovalMap<>(ModConfig.maxCacheSize);
        playerProfileList = new EldestRemovalMap<>(ModConfig.maxCacheSize);
        StatsCommands.register();
    }
}
