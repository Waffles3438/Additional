package org.polyfrost.example.config;

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.Notifications;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.polyfrost.example.Addition;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

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

    @Text(
            name = "Hypixel API",
            secure = true,
            category = "Stat Checking"
    )
    public static String api = "";

    @Button(
            name = "Clear cache",
            text = "Clear",
            category = "Stat Checking",
            size = OptionSize.DUAL
    )
    Runnable clearCache = () -> {
        Addition.bedwarsStatsList.clear();
        Addition.duelsStatsList.clear();
        Addition.playerRanks.clear();
        Addition.properPlayerNames.clear();
        Notifications.INSTANCE.send("Addition", "Cleared player cache", 3000);
    };

    @Switch(
            name = "Guild Sniper",
            category = "Guild Sniping"
    )
    public static boolean guildSniper = false;

    @Text(
            name = "Guild Name",
            category = "Guild Sniping"
    )
    public static String guildName = "";

    @Exclude
    private JsonArray guild;

    public static ArrayList<String> guildMembers = new ArrayList<>();

    @Button(
            name = "Get guild members",
            text = "Get members",
            category = "Guild Sniping",
            size = OptionSize.DUAL
    )
    Runnable get = () -> Multithreading.runAsync(() -> {
        String connection = newConnection("https://api.hypixel.net/v2/guild?key=" + ModConfig.api + "&name=" + ModConfig.guildName);
        try {
            guild = getStringAsJson(connection).getAsJsonObject("guild").getAsJsonArray("members");
            Notifications.INSTANCE.send("Addition", "Added guild members", 3000);
            guildMembers.clear();
        } catch (Exception e) {
            Notifications.INSTANCE.send("Addition", ModConfig.guildName + " does not exist", 3000);
            e.printStackTrace();
            return;
        }

        for(int i = 0; i < guild.size(); i++) {
            try {
                JsonObject Gmember = guild.get(i).getAsJsonObject();
                guildMembers.add(getString(Gmember));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    });

    public ModConfig() {
        super(new Mod(Addition.NAME, ModType.UTIL_QOL), Addition.MODID + ".json");
        initialize();
        addDependency("jumpTicks", "ndj");
        addDependency("nametagsOnShift", "masterSwitch");
        addDependency("invisNametags", "masterSwitch");
        addDependency("extendNametagRange", "masterSwitch");
        addDependency("nametagsThroughWalls", "masterSwitch");
        addDependency("guildName", "guildSniper");
        addDependency("get", "guildSniper");
        registerKeyBind(nametagsKeybind, () -> {
            if(masterSwitch) {
                masterSwitch = false;
                Notifications.INSTANCE.send("Addition", "Disabled nametag addtions", 3000);
            } else {
                masterSwitch = true;
                Notifications.INSTANCE.send("Addition", "Enabled nametag addtions", 3000);
            }
        });
    }

    private String getString(JsonObject type) {
        try {
            return type.get("uuid").getAsString();
        } catch (NullPointerException er) {
            return null;
        }
    }

    private JsonObject getStringAsJson(String text) {
        return new JsonParser().parse(text).getAsJsonObject();
    }

    private String newConnection(String link) {
        URL url;
        String result = "";
        HttpURLConnection con = null;
        try {
            url = new URL(link);
            con = (HttpURLConnection) url.openConnection();
            result = getContents(con);
        } catch (IOException ignored) { }
        finally {
            if (con != null) con.disconnect();
        }
        return result;
    }

    private String getContents(HttpURLConnection con) {
        if (con != null) {
            // since BufferedReader is defined within try catch, close is called regardless of completion
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String input;
                StringBuilder sb = new StringBuilder();
                while ((input = br.readLine()) != null) {
                    sb.append(input);
                }
                return sb.toString();
            } catch (IOException ignored) { }
        }
        return "";
    }
}

