package org.polyfrost.example.config;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.utils.Notifications;
import org.polyfrost.example.Addition;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;

public class ModConfig extends Config {
    @Switch(
            name = "No jump delay",
            size = OptionSize.DUAL,
            subcategory = "No Jump Delay"
    )
    public static boolean ndj = false;

    @Slider(
            name = "Jump ticks",
            min = 0, max = 10,
            step = 1,
            subcategory = "No Jump Delay"
    )
    public static int jumpTicks = 3;

    @Switch(
            name = "Show nametags on shift",
            subcategory = "Nametags"
    )
    public static boolean nametagsOnShift = false;

    @Switch(
            name = "Show invisible player nametags",
            subcategory = "Nametags"
    )
    public static boolean invisNametags = false;

    @Switch(
            name = "Extend nametag range",
            subcategory = "Nametags"
    )
    public static boolean extendNametagRange = false;

    @Switch(
            name = "Show nametags behind walls",
            subcategory = "Nametags"
    )
    public static boolean nametagsThroughWalls = false;

    @Info(
            text = "Showing nametags infront of water and stained glass is is buggy",
            type = InfoType.ERROR,
            size = OptionSize.DUAL,
            subcategory = "Nametags"
    )
    public static boolean ignored2;

    @Text(
            name = "Hypixel API",
            secure = true, multiline = false,
            subcategory = "Stat Checking"
    )
    public static String api = "";

    @Button(
            name = "Clear cache",
            text = "Clear",
            subcategory = "Stat Checking"
    )
    Runnable runnable = () -> {
        Addition.bedwarsStatsList.clear();
        Addition.duelsStatsList.clear();
        Addition.playerRanks.clear();
        Notifications.INSTANCE.send("Addition", "Cleared player cache", 1000);
    };

    public ModConfig() {
        super(new Mod(Addition.NAME, ModType.UTIL_QOL), Addition.MODID + ".json");
        initialize();
        addDependency("jumpTicks", "ndj");
    }
}

