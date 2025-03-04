package org.polyfrost.example.config;

import cc.polyfrost.oneconfig.config.annotations.Info;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.InfoType;
import org.polyfrost.example.Addition;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Switch;
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
            type = InfoType.INFO
    )
    public static boolean ignored2;

    @Text(
            name = "Hypixel API",
            secure = true, multiline = false,
            subcategory = "Stat Checking"
    )
    public static String api = "";

    public ModConfig() {
        super(new Mod(Addition.NAME, ModType.UTIL_QOL), Addition.MODID + ".json");
        initialize();
        addDependency("jumpTicks", "ndj");
    }
}

