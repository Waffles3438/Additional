package me.waffles.additional.config;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.utils.Notifications;
import me.waffles.additional.Additional;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;

public class ModConfig extends Config {
    public static boolean masterSwitch = false;

    @Switch(
            name = "No jump delay",
            size = OptionSize.DUAL,
            category = "Quality of Life",
            subcategory = "No Jump Delay"
    )
    public static boolean ndj = false;

    @Slider(
            name = "Jump ticks",
            min = 0, max = 10,
            step = 1,
            category = "Quality of Life",
            subcategory = "No Jump Delay"
    )
    public static int jumpTicks = 3;

    @KeyBind(
            name = "Toggle Nametag Features",
            category = "Quality of Life",
            subcategory = "Nametags",
            size = OptionSize.DUAL
    )
    public static OneKeyBind nametagsKeybind = new OneKeyBind(UKeyboard.KEY_C);

    @Checkbox(
            name = "Show nametags on shift",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean nametagsOnShift = false;

    @Checkbox(
            name = "Show invisible player nametags",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean invisNametags = false;

    @Checkbox(
            name = "Extend nametag range",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean extendNametagRange = false;

    @Checkbox(
            name = "Show nametags behind walls",
            category = "Quality of Life",
            subcategory = "Nametags"
    )
    public static boolean nametagsThroughWalls = false;

    @Button(
            name = "Clear cache",
            text = "Clear",
            category = "Stat Checking"
    )
    Runnable runnable = () -> {
        Additional.bedwarsStatsList.clear();
        Additional.duelsStatsList.clear();
        Additional.playerProfileList.clear();
        Notifications.INSTANCE.send("Additional", "Cleared player cache", 3000);
    };

    public ModConfig() {
        super(new Mod(Additional.NAME, ModType.UTIL_QOL), Additional.MODID + ".json");
        initialize();
        addDependency("jumpTicks", "ndj");
        addDependency("nametagsOnShift", "masterSwitch");
        addDependency("invisNametags", "masterSwitch");
        addDependency("extendNametagRange", "masterSwitch");
        addDependency("nametagsThroughWalls", "masterSwitch");
        registerKeyBind(nametagsKeybind, () -> {
            Notifications.INSTANCE.send("Addition", (masterSwitch ? "Disabled" : "Enabled") + " nametag additions", 3000);
            masterSwitch = !masterSwitch;
        });
    }
}

