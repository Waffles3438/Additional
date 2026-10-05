package me.waffles.additional.config;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;

import me.waffles.additional.Additional;
import me.waffles.additional.api.StatsProviderUtils;

public class ModConfig extends Config {
    @Switch(title = "Enable nametag features", category = "Quality of Life", subcategory = "Nametags")
    public static boolean masterSwitch = false;

    @Switch(
            title = "No jump delay",
            category = "Quality of Life",
            subcategory = "No Jump Delay"
    )
    public static boolean ndj = false;

    @Slider(
            title = "Jump ticks",
            min = 0, max = 10,
            step = 1,
            category = "Quality of Life",
            subcategory = "No Jump Delay"
    )
    public static int jumpTicks = 3;

    @Keybind(
            title = "Toggle Nametag Features",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static OneConfigKeybind nametagsKeybind = KeybindHelper.builder()
            .key(Platform.compatibility().keys().getKeyC())
            .name("Toggle Nametag Features").category("Additional")
            .action((Runnable) ModConfig::toggleNametags).build();

    @Checkbox(
            title = "Show nametags on shift",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean nametagsOnShift = false;

    @Checkbox(
            title = "Show invisible player nametags",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean invisNametags = false;

    @Checkbox(
            title = "Extend nametag range",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean extendNametagRange = false;

    @Checkbox(
            title = "Show nametags behind walls",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean nametagsThroughWalls = false;

    @Switch(
            title = "Legit Mode",
            description = "Preserve vanilla nametag positioning while nametag features are disabled.",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean legitMode = false;

    /**
     * Legit Mode is intentionally independent from the checkbox dependency state:
     * the master switch always takes priority over it.
     */
    public static boolean isLegitModeActive() {
        return legitMode && !masterSwitch;
    }

    @Button(
            title = "Clear cache",
            description = "Stats expire after 5 minutes and refresh on the next lookup.",
            category = "Stat Checking"
    )
    public void clearCache() {
        StatsProviderUtils.invalidateCacheGeneration();
        Additional.bedwarsStatsList.invalidateAll();
        Additional.duelsStatsList.invalidateAll();
        Additional.playerProfileList.invalidateAll();
        Notifications.info("Additional", "Cleared player cache", 2f);
    }


    private static void toggleNametags() {
        Additional.config.getProperty("masterSwitch").setAs(!masterSwitch);
        Notifications.info("Additional", (masterSwitch ? "Enabled" : "Disabled") + " nametag additions", 2f);
    }

    public ModConfig() {
        super(Additional.MODID + ".json", Additional.NAME, Category.QOL);
        addDependency("jumpTicks", "ndj");
        addDependency("nametagsOnShift", "masterSwitch");
        addDependency("invisNametags", "masterSwitch");
        addDependency("extendNametagRange", "masterSwitch");
        addDependency("nametagsThroughWalls", "masterSwitch");

    }
}

