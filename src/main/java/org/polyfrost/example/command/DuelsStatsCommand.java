package org.polyfrost.example.command;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.polyfrost.example.Addition;
import org.polyfrost.example.config.ModConfig;
import org.polyfrost.example.util.Bedwars;
import org.polyfrost.example.util.Duels;
import org.polyfrost.example.util.Ranks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NavigableMap;
import java.util.TreeMap;

@Command(value = "d")
public class DuelsStatsCommand {

    private String uuid, connection, Username;
    private JsonObject profile, ach, d, bw;

    // Bedwars
    private int Bedwarsstar, Bedwarsfk, Bedwarsbb, Bedwarsw, Bedwarsl, Bedwarsfd, Bedwarsbl, Bedwarsws;
    private double Bedwarsfkdr, Bedwarswlr, Bedwarsbblr;

    // Duels
    private int Duelswins, Duelskills, exp, Duelslosses, Duelsdeaths, Duelscws, Duelsbws;
    private double Duelswlr, Duelskdr;
    private String Level;

    @Main
    private void main() {
        String player = Minecraft.getMinecraft().getSession().getProfile().getName();

        Multithreading.runAsync(() -> {
            boolean request = true;
            try {
                uuid = NetworkUtils.getJsonElement("https://api.mojang.com/users/profiles/minecraft/" + player).getAsJsonObject().get("id").getAsString();
                Username = NetworkUtils.getJsonElement("https://api.mojang.com/users/profiles/minecraft/" + player).getAsJsonObject().get("name").getAsString();
            } catch (Exception e) {
                if(Addition.properPlayerNames.containsKey(player.toLowerCase())) {
                    Username = Addition.properPlayerNames.get(player.toLowerCase());
                    request = false;
                } else {
                    UChat.chat("Invalid player");
                    return;
                }
            }

            connection = newConnection("https://api.hypixel.net/player?key=" + ModConfig.api + "&uuid=" + uuid);
            if (connection.isEmpty()) {
                request = false;
            }

            if(request) {
                if (connection.equals("{\"success\":true,\"player\":null}")) {
                    // player is nicked
                    UChat.chat(Username + " has never logged on Hypixel");
                    return;
                }

                try {
                    profile = getStringAsJson(connection).getAsJsonObject("player");
                    d = profile.getAsJsonObject("stats").getAsJsonObject("Duels");
                    ach = profile.getAsJsonObject("achievements");
                } catch (NullPointerException er) {
                    // never played duels or joined lobby
                    UChat.chat(Username + " has never played Duels");
                    try {
                        profile = getStringAsJson(connection).getAsJsonObject("player");
                        bw = profile.getAsJsonObject("stats").getAsJsonObject("Bedwars");
                        ach = profile.getAsJsonObject("achievements");
                        Bedwarsstar = getValue(ach, "bedwars_level");
                        Bedwarsfk = getValue(bw, "final_kills_bedwars");
                        Bedwarsbb = getValue(bw, "beds_broken_bedwars");
                        Bedwarsw = getValue(bw, "wins_bedwars");
                        Bedwarsfd = getValue(bw, "final_deaths_bedwars");
                        Bedwarsl = getValue(bw, "losses_bedwars");
                        Bedwarsbl = getValue(bw, "beds_lost_bedwars");
                        Bedwarsws = getValue(bw, "winstreak");

                        if (Bedwarsfd != 0) Bedwarsfkdr = (double) Bedwarsfk / (double) Bedwarsfd;
                        else Bedwarsfkdr = Bedwarsfk;
                        Bedwarsfkdr = (double) Math.round(Bedwarsfkdr * 100) / 100;

                        if (Bedwarsl != 0) Bedwarswlr = (double) Bedwarsw / (double) Bedwarsl;
                        else Bedwarswlr = Bedwarsw;
                        Bedwarswlr = (double) Math.round(Bedwarswlr * 100) / 100;

                        if (Bedwarsbl != 0) Bedwarsbblr = (double) Bedwarsbb / (double) Bedwarsbl;
                        else Bedwarsbblr = Bedwarsbb;
                        Bedwarsbblr = (double) Math.round(Bedwarsbblr * 100) / 100;

                        if(Bedwarsw != 0 && Bedwarsl != 0) Addition.bedwarsStatsList.put(Username, new Bedwars(Bedwarsstar, Bedwarsfk, Bedwarsbb, Bedwarsw, Bedwarsl, Bedwarsfd, Bedwarsbl, Bedwarsws, Bedwarsfkdr, Bedwarswlr, Bedwarsbblr));
                    } catch (NullPointerException err) {
                        // never played bedwars
                    }
                    return;
                }
                requestStats(player);
            } else getStats(Username);
        });
    }

    @Main
    private void main(@Greedy String player) {
        if(player.isEmpty()) {
            UChat.chat("Enter a username");
            return;
        }

        Multithreading.runAsync(() -> {
            boolean request = true;
            try {
                uuid = NetworkUtils.getJsonElement("https://api.mojang.com/users/profiles/minecraft/" + player).getAsJsonObject().get("id").getAsString();
                Username = NetworkUtils.getJsonElement("https://api.mojang.com/users/profiles/minecraft/" + player).getAsJsonObject().get("name").getAsString();
            } catch (Exception e) {
                if (Addition.properPlayerNames.containsKey(player.toLowerCase())){
                    Username = Addition.properPlayerNames.get(player.toLowerCase());
                    request = false;
                } else {
                    UChat.chat("Invalid player");
                    return;
                }
            }

            connection = newConnection("https://api.hypixel.net/player?key=" + ModConfig.api + "&uuid=" + uuid);
            if (connection.isEmpty()) {
                request = false;
            }

            if(request) {
                if (connection.equals("{\"success\":true,\"player\":null}")) {
                    // player is nicked
                    UChat.chat(Username + " has never logged on Hypixel");
                    return;
                }

                try {
                    profile = getStringAsJson(connection).getAsJsonObject("player");
                    d = profile.getAsJsonObject("stats").getAsJsonObject("Duels");
                    ach = profile.getAsJsonObject("achievements");
                } catch (NullPointerException er) {
                    // never played duels or joined lobby
                    UChat.chat(Username + " has never played Duels");
                    try {
                        profile = getStringAsJson(connection).getAsJsonObject("player");
                        bw = profile.getAsJsonObject("stats").getAsJsonObject("Bedwars");
                        ach = profile.getAsJsonObject("achievements");
                        Bedwarsstar = getValue(ach, "bedwars_level");
                        Bedwarsfk = getValue(bw, "final_kills_bedwars");
                        Bedwarsbb = getValue(bw, "beds_broken_bedwars");
                        Bedwarsw = getValue(bw, "wins_bedwars");
                        Bedwarsfd = getValue(bw, "final_deaths_bedwars");
                        Bedwarsl = getValue(bw, "losses_bedwars");
                        Bedwarsbl = getValue(bw, "beds_lost_bedwars");
                        Bedwarsws = getValue(bw, "winstreak");

                        if (Bedwarsfd != 0) Bedwarsfkdr = (double) Bedwarsfk / (double) Bedwarsfd;
                        else Bedwarsfkdr = Bedwarsfk;
                        Bedwarsfkdr = (double) Math.round(Bedwarsfkdr * 100) / 100;

                        if (Bedwarsl != 0) Bedwarswlr = (double) Bedwarsw / (double) Bedwarsl;
                        else Bedwarswlr = Bedwarsw;
                        Bedwarswlr = (double) Math.round(Bedwarswlr * 100) / 100;

                        if (Bedwarsbl != 0) Bedwarsbblr = (double) Bedwarsbb / (double) Bedwarsbl;
                        else Bedwarsbblr = Bedwarsbb;
                        Bedwarsbblr = (double) Math.round(Bedwarsbblr * 100) / 100;

                        if(Bedwarsw != 0 && Bedwarsl != 0) Addition.bedwarsStatsList.put(Username, new Bedwars(Bedwarsstar, Bedwarsfk, Bedwarsbb, Bedwarsw, Bedwarsl, Bedwarsfd, Bedwarsbl, Bedwarsws, Bedwarsfkdr, Bedwarswlr, Bedwarsbblr));
                    } catch (NullPointerException err) {
                        // never played bedwars
                    }
                    return;
                }
                requestStats(player);
            } else getStats(Username);
        });
    }

    private String formatColors(int stat, int god) {
        if(stat > god*2) return "§0" + stat;
        else if(stat >= god) return "§4" + stat;
        else if(stat > god*0.875) return "§c" + stat;
        else if(stat > god*0.75) return "§6" + stat;
        else if(stat > god*0.625) return "§e" + stat;
        else if(stat > god*0.5) return "§2" + stat;
        else if(stat > god*0.375) return "§a" + stat;
        else if(stat > god*0.25) return "§b" + stat;
        else if(stat > god*0.125) return "§f" + stat;
        else return "§7" + stat;
    }

    private String formatColors(double stat, int god) {
        if(stat > god*2) return "§0" + stat;
        else if(stat >= god) return "§4" + stat;
        else if(stat > god*0.875) return "§c" + stat;
        else if(stat > god*0.75) return "§6" + stat;
        else if(stat > god*0.625) return "§e" + stat;
        else if(stat > god*0.5) return "§2" + stat;
        else if(stat > god*0.375) return "§a" + stat;
        else if(stat > god*0.25) return "§b" + stat;
        else if(stat > god*0.125) return "§f" + stat;
        else return "§7" + stat;
    }

    private void getStats(String Player) {
        if(!Addition.duelsStatsList.containsKey(Player)) {
            UChat.chat(Player + " is not cached");
            return;
        }
        Ranks rankStuff = Addition.playerRanks.get(Player);
        rank = rankStuff.getRank();
        special = rankStuff.getSpecial();
        monthly = rankStuff.getMonthly();
        MVPPlusPlusCheck = rankStuff.getMVPPlusPlusCheck();
        plusColor = rankStuff.getplusColor();
        admin = rankStuff.getAdmin();
        Duels duelsStats = Addition.duelsStatsList.get(Player);
        Duelsdeaths = duelsStats.getDuelsDeaths();
        Duelsbws = duelsStats.getDuelsBWS();
        Duelscws = duelsStats.getDuelsCWS();
        Duelskdr = duelsStats.getDuelsKDR();
        Duelskills = duelsStats.getDuelsKills();
        Duelslosses = duelsStats.getDuelsLosses();
        Duelswins = duelsStats.getDuelsWins();
        Duelswlr = duelsStats.getDuelsWLR();
        Level = duelsStats.getLevel();
        UChat.chat("§9------------------------------------------");
        UChat.chat(getPlayerDivision(Duelswins) + formatWithoutRequestRank(Username));
        UChat.chat("Level: " + Level);
        UChat.chat("WLR: " + formatColors(Duelswlr, 10));
        UChat.chat("Wins: " + formatColors(Duelswins, 20000));
        UChat.chat("KDR: " + formatColors(Duelskdr, 10));
        UChat.chat("Kills: " + formatColors(Duelskills, 20000));
        if(Duelscws != -1 && Duelsbws != -1) {
            UChat.chat("Current Winstreak: " + Duelscws);
            UChat.chat("Best Winstreak: " + Duelsbws);
        }
        UChat.chat("§9------------------------------------------");
    }

    private void requestStats(String player){
        try {
            profile = getStringAsJson(connection).getAsJsonObject("player");
            d = profile.getAsJsonObject("stats").getAsJsonObject("Duels");
            ach = profile.getAsJsonObject("achievements");
        } catch (NullPointerException er) {
            // never played bedwars or joined lobby
            UChat.chat(Username + " has never played Duels");
            return;
        }

        // Duels
        exp = getValue(profile, "networkExp");
        Level = levelColor(String.valueOf((double) Math.round(getExactLevel(exp) * 100) / 100));

        Duelscws = getValue(d, "current_winstreak");
        Duelsbws = getValue(d, "best_overall_winstreak");
        Duelswins = getValue(d, "wins");
        Duelskills = getValue(d, "kills");
        Duelsdeaths = getValue(d, "deaths");
        Duelslosses = getValue(d, "losses");

        if (Duelslosses != 0) Duelswlr = (double) Duelswins / (double) Duelslosses;
        else Duelswlr = Duelswins;
        Duelswlr = (double) Math.round(Duelswlr * 100) / 100;

        if (Duelsdeaths != 0) Duelskdr = (double) Duelskills / (double) Duelsdeaths;
        else Duelskdr = Duelskills;
        Duelskdr = (double) Math.round(Duelskdr * 100) / 100;

        // Bedwars

        try {
            bw = profile.getAsJsonObject("stats").getAsJsonObject("Bedwars");
        } catch (NullPointerException ignored) {

        }

        Bedwarsstar = getValue(ach, "bedwars_level");
        Bedwarsfk = getValue(bw, "final_kills_bedwars");
        Bedwarsbb = getValue(bw, "beds_broken_bedwars");
        Bedwarsw = getValue(bw, "wins_bedwars");
        Bedwarsfd = getValue(bw, "final_deaths_bedwars");
        Bedwarsl = getValue(bw, "losses_bedwars");
        Bedwarsbl = getValue(bw, "beds_lost_bedwars");
        Bedwarsws = getValue(bw, "winstreak");

        if (Bedwarsfd != 0) Bedwarsfkdr = (double) Bedwarsfk / (double) Bedwarsfd;
        else Bedwarsfkdr = Bedwarsfk;
        Bedwarsfkdr = (double) Math.round(Bedwarsfkdr * 100) / 100;

        if (Bedwarsl != 0) Bedwarswlr = (double) Bedwarsw / (double) Bedwarsl;
        else Bedwarswlr = Bedwarsw;
        Bedwarswlr = (double) Math.round(Bedwarswlr * 100) / 100;

        if (Bedwarsbl != 0) Bedwarsbblr = (double) Bedwarsbb / (double) Bedwarsbl;
        else Bedwarsbblr = Bedwarsbb;
        Bedwarsbblr = (double) Math.round(Bedwarsbblr * 100) / 100;

        if (Duelswins != 0 || Duelslosses != 0) {
            UChat.chat("§9------------------------------------------");
            UChat.chat(getPlayerDivision(Duelswins) + formatRank(profile, Username));
            UChat.chat("Level: " + Level);
            UChat.chat("WLR: " + formatColors(Duelswlr, 10));
            UChat.chat("Wins: " + formatColors(Duelswins, 30000));
            UChat.chat("KDR: " + formatColors(Duelskdr, 10));
            UChat.chat("Kills: " + formatColors(Duelskills, 30000));
            if(Duelscws != -1 && Duelsbws != -1) {
                UChat.chat("Current Winstreak: " + Duelscws);
                UChat.chat("Best Winstreak: " + Duelsbws);
            }
            UChat.chat("§9------------------------------------------");
            Addition.duelsStatsList.remove(Username);
            Addition.duelsStatsList.put(Username, new Duels(Duelskills, Duelsdeaths, Duelswins, Duelslosses, Duelscws, Duelsbws, Duelswlr, Duelskdr, Level));
            if(!Addition.properPlayerNames.containsKey(player.toLowerCase())) Addition.properPlayerNames.put(player.toLowerCase(), Username);
        } else {
            UChat.chat(Username + " has never played Duels");
        }

        if(Addition.bedwarsStatsList.containsKey(Username) && (Bedwarsl != 0 || Bedwarsw != 0)) Addition.bedwarsStatsList.remove(Username);
        if(Bedwarsl != 0 || Bedwarsw != 0) {
            Addition.bedwarsStatsList.put(Username, new Bedwars(Bedwarsstar, Bedwarsfk, Bedwarsbb, Bedwarsw, Bedwarsl, Bedwarsfd, Bedwarsbl, Bedwarsws, Bedwarsfkdr, Bedwarswlr, Bedwarsbblr));
            if(!Addition.properPlayerNames.containsKey(player.toLowerCase())) Addition.properPlayerNames.put(player.toLowerCase(), Username);
        }
        Addition.playerRanks.remove(Username);
        Addition.playerRanks.put(Username, new Ranks(rank, special, monthly, MVPPlusPlusCheck, plusColor, admin));
    }

    private String levelColor(String level) {
        double lvl = Double.parseDouble(level);
        if(lvl < 35) return "§c" + level;
        else if(lvl < 45) return "§6" + level;
        else if(lvl < 55) return "§a" + level;
        else if(lvl < 65) return "§e" + level;
        else if(lvl < 75) return "§d" + level;
        else if(lvl < 85) return "§f" + level;
        else if(lvl < 95) return "§9" + level;
        else if(lvl < 150) return "§2" + level;
        else if(lvl < 200) return "§4" + level;
        else if(lvl < 250) return "§5" + level;
        else return "§0" + level;
    }

    private String rank, special, monthly, MVPPlusPlusCheck, plusColor, admin;

    private String formatWithoutRequestRank(String Player) {
        if(admin != null && admin.equals("§6[MOJANG]")) return "§6[MOJANG] " + Username;
        if(Player.equals("Technoblade")) {
            Player = "§d[PIG§b+++§d] " + Username;
        } else if (Player.equals("TommyInnit")) {
            Player = "§d[INNIT] " + Username;
        } else if (special.equals("YOUTUBER")) {
            Player = "§c[§fYOUTUBE§c] " + Username;
        } else if (special.equals("STAFF")) {
            Player = "§c[§6ዞ§c] " + Username;
        } else if (monthly != null && MVPPlusPlusCheck != null && rank.equals("MVP_PLUS") && monthly.equals("GOLD") && MVPPlusPlusCheck.equals("SUPERSTAR")) { // Gold MVP++ check
            String color = "§c";
            if (plusColor != null) {
                switch (plusColor) {
                    case "RED":
                        color = "§c";
                        break;
                    case "GOLD":
                        color = "§6";
                        break;
                    case "GREEN":
                        color = "§a";
                        break;
                    case "YELLOW":
                        color = "§e";
                        break;
                    case "LIGHT_PURPLE":
                        color = "§d";
                        break;
                    case "WHITE":
                        color = "§f";
                        break;
                    case "BLUE":
                        color = "§9";
                        break;
                    case "DARK_GREEN":
                        color = "§2";
                        break;
                    case "DARK_RED":
                        color = "§4";
                        break;
                    case "DARK_AQUA":
                        color = "§3";
                        break;
                    case "DARK_PURPLE":
                        color = "§5";
                        break;
                    case "GRAY":
                        color = "§7";
                        break;
                    case "BLACK":
                        color = "§0";
                        break;
                    case "DARK_BLUE":
                        color = "§1";
                        break;
                }
            }
            Player = "§6[MVP" + color + "++" + "§6] " + Username;
        } else if (rank.equals("MVP_PLUS"))  {
            String color = "§c";
            if (plusColor != null) {
                switch (plusColor) {
                    case "RED":
                        color = "§c";
                        break;
                    case "GOLD":
                        color = "§6";
                        break;
                    case "GREEN":
                        color = "§a";
                        break;
                    case "YELLOW":
                        color = "§e";
                        break;
                    case "LIGHT_PURPLE":
                        color = "§d";
                        break;
                    case "WHITE":
                        color = "§f";
                        break;
                    case "BLUE":
                        color = "§9";
                        break;
                    case "DARK_GREEN":
                        color = "§2";
                        break;
                    case "DARK_RED":
                        color = "§4";
                        break;
                    case "DARK_AQUA":
                        color = "§3";
                        break;
                    case "DARK_PURPLE":
                        color = "§5";
                        break;
                    case "GRAY":
                        color = "§7";
                        break;
                    case "BLACK":
                        color = "§0";
                        break;
                    case "DARK_BLUE":
                        color = "§1";
                        break;
                }
            }
            if(monthly != null && MVPPlusPlusCheck != null && monthly.equals("AQUA") && MVPPlusPlusCheck.equals("SUPERSTAR")) {
                Player = "§b[MVP" + color + "++" + "§b] " + Username;
            } else {
                Player = "§b[MVP" + color + "+" + "§b] " + Username;
            }
        } else if (rank.equals("MVP")) {
            Player = "§b[MVP] " + Username;
        } else if (rank.equals("VIP_PLUS")) {
            Player = "§a[VIP§6+§a] " + Username;
        } else if (rank.equals("VIP")) {
            Player = "§a[VIP] " + Username;
        } else {
            Player = "§7" + Username;
        }
        return Player;
    }

    private String formatRank(JsonObject profile,String Player) {
        String admin = getString(profile, "prefix");
        if(admin != null && admin.equals("§6[MOJANG]")) return "§6[MOJANG] " + Username;
        if(getString(profile, "newPackageRank") != null) {
            rank = getString(profile, "newPackageRank");
        } else {
            rank = "non";
        }

        special = "nothing";
        if(getString(profile, "rank") != null){
            special = getString(profile, "rank");
        }

        monthly = getString(profile, "monthlyRankColor");
        MVPPlusPlusCheck = getString(profile, "monthlyPackageRank");

        if(Player.equals("Technoblade")) {
            Player = "§d[PIG§b+++§d] " + Username;
        } else if (Player.equals("TommyInnit")) {
            Player = "§d[INNIT] " + Username;
        } else if (special.equals("YOUTUBER")) {
            Player = "§c[§fYOUTUBE§c] " + Username;
        } else if (special.equals("STAFF")) {
            Player = "§c[§6ዞ§c] " + Username;
        } else if (monthly != null && MVPPlusPlusCheck != null && rank.equals("MVP_PLUS") && monthly.equals("GOLD") && MVPPlusPlusCheck.equals("SUPERSTAR")) { // Gold MVP++ check
            plusColor = getString(profile, "rankPlusColor");
            String color = "§c";
            if (plusColor != null) {
                switch (plusColor) {
                    case "RED":
                        color = "§c";
                        break;
                    case "GOLD":
                        color = "§6";
                        break;
                    case "GREEN":
                        color = "§a";
                        break;
                    case "YELLOW":
                        color = "§e";
                        break;
                    case "LIGHT_PURPLE":
                        color = "§d";
                        break;
                    case "WHITE":
                        color = "§f";
                        break;
                    case "BLUE":
                        color = "§9";
                        break;
                    case "DARK_GREEN":
                        color = "§2";
                        break;
                    case "DARK_RED":
                        color = "§4";
                        break;
                    case "DARK_AQUA":
                        color = "§3";
                        break;
                    case "DARK_PURPLE":
                        color = "§5";
                        break;
                    case "GRAY":
                        color = "§7";
                        break;
                    case "BLACK":
                        color = "§0";
                        break;
                    case "DARK_BLUE":
                        color = "§1";
                        break;
                }
            }
            Player = "§6[MVP" + color + "++" + "§6] " + Username;
        } else if (rank.equals("MVP_PLUS"))  {
            plusColor = getString(profile, "rankPlusColor");
            String color = "§c";
            if (plusColor != null) {
                switch (plusColor) {
                    case "RED":
                        color = "§c";
                        break;
                    case "GOLD":
                        color = "§6";
                        break;
                    case "GREEN":
                        color = "§a";
                        break;
                    case "YELLOW":
                        color = "§e";
                        break;
                    case "LIGHT_PURPLE":
                        color = "§d";
                        break;
                    case "WHITE":
                        color = "§f";
                        break;
                    case "BLUE":
                        color = "§9";
                        break;
                    case "DARK_GREEN":
                        color = "§2";
                        break;
                    case "DARK_RED":
                        color = "§4";
                        break;
                    case "DARK_AQUA":
                        color = "§3";
                        break;
                    case "DARK_PURPLE":
                        color = "§5";
                        break;
                    case "GRAY":
                        color = "§7";
                        break;
                    case "BLACK":
                        color = "§0";
                        break;
                    case "DARK_BLUE":
                        color = "§1";
                        break;
                }
            }
            if(monthly != null && MVPPlusPlusCheck != null && monthly.equals("AQUA") && MVPPlusPlusCheck.equals("SUPERSTAR")) {
                Player = "§b[MVP" + color + "++" + "§b] " + Username;
            } else {
                Player = "§b[MVP" + color + "+" + "§b] " + Username;
            }
        } else if (rank.equals("MVP")) {
            Player = "§b[MVP] " + Username;
        } else if (rank.equals("VIP_PLUS")) {
            Player = "§a[VIP§6+§a] " + Username;
        } else if (rank.equals("VIP")) {
            Player = "§a[VIP] " + Username;
        } else {
            Player = "§7" + Username;
        }
        return Player;
    }

    private int getValue(JsonObject type, String member) {
        try {
            return type.get(member).getAsInt();
        } catch (NullPointerException er) {
            if (member.equals("winstreak") || member.equals("current_winstreak") || member.equals("best_overall_winstreak")) return -1;
            return 0;
        }
    }

    private String getString(JsonObject type, String member) {
        try {
            return type.get(member).getAsString();
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

    private final double BASE = 10_000;
    private final double GROWTH = 2_500;

    private double getLevel(double exp) {
        double REVERSE_PQ_PREFIX = -(BASE - 0.5 * GROWTH) / GROWTH;
        double REVERSE_CONST = REVERSE_PQ_PREFIX * REVERSE_PQ_PREFIX;
        double GROWTH_DIVIDES_2 = 2 / GROWTH;
        return exp < 0 ? 1 : Math.floor(1 + REVERSE_PQ_PREFIX + Math.sqrt(REVERSE_CONST + GROWTH_DIVIDES_2 * exp));
    }

    private double getExactLevel(double exp) {
        return getLevel(exp) + getPercentageToNextLevel(exp);
    }

    private double getTotalExpToFullLevel(double level) {
        double HALF_GROWTH = 0.5 * GROWTH;
        return (HALF_GROWTH * (level - 2) + BASE) * (level - 1);
    }

    private double getTotalExpToLevel(double level) {
        double lv = Math.floor(level), x0 = getTotalExpToFullLevel(lv);
        if (level == lv) return x0;
        return (getTotalExpToFullLevel(lv + 1) - x0) * (level % 1) + x0;
    }

    private double getPercentageToNextLevel(double exp) {
        double lv = getLevel(exp), x0 = getTotalExpToLevel(lv);
        return (exp - x0) / (getTotalExpToLevel(lv + 1) - x0);
    }

    private static final NavigableMap<Integer, String> DIVISIONS = new TreeMap<>();

    static {
        DIVISIONS.put(100, "§8Rookie");
        DIVISIONS.put(120, "§8Rookie II");
        DIVISIONS.put(140, "§8Rookie III");
        DIVISIONS.put(160, "§8Rookie IV");
        DIVISIONS.put(180, "§8Rookie V");
        DIVISIONS.put(200, "§fIron");
        DIVISIONS.put(260, "§fIron II");
        DIVISIONS.put(320, "§fIron III");
        DIVISIONS.put(380, "§fIron IV");
        DIVISIONS.put(440, "§fIron V");
        DIVISIONS.put(500, "§6Gold");
        DIVISIONS.put(600, "§6Gold II");
        DIVISIONS.put(700, "§6Gold III");
        DIVISIONS.put(800, "§6Gold IV");
        DIVISIONS.put(900, "§6Gold V");
        DIVISIONS.put(1000, "§3Diamond");
        DIVISIONS.put(1200, "§3Diamond II");
        DIVISIONS.put(1400, "§3Diamond III");
        DIVISIONS.put(1600, "§3Diamond IV");
        DIVISIONS.put(1800, "§3Diamond V");
        DIVISIONS.put(2000, "§2Master");
        DIVISIONS.put(2400, "§2Master II");
        DIVISIONS.put(2800, "§2Master III");
        DIVISIONS.put(3200, "§2Master IV");
        DIVISIONS.put(3600, "§2Master V");
        DIVISIONS.put(4000, "§4§lLegend");
        DIVISIONS.put(5200, "§4§lLegend II");
        DIVISIONS.put(6400, "§4§lLegend III");
        DIVISIONS.put(7600, "§4§lLegend IV");
        DIVISIONS.put(8800, "§4§lLegend V");
        DIVISIONS.put(10000, "§e§lGrandmaster");
        DIVISIONS.put(12000, "§e§lGrandmaster II");
        DIVISIONS.put(14000, "§e§lGrandmaster III");
        DIVISIONS.put(16000, "§e§lGrandmaster IV");
        DIVISIONS.put(18000, "§e§lGrandmaster V");
        DIVISIONS.put(20000, "§5§lGodlike");
        DIVISIONS.put(26000, "§5§lGodlike II");
        DIVISIONS.put(32000, "§5§lGodlike III");
        DIVISIONS.put(38000, "§5§lGodlike IV");
        DIVISIONS.put(44000, "§5§lGodlike V");
        DIVISIONS.put(50000, "§b§lCelestial");
        DIVISIONS.put(60000, "§b§lCelestial II");
        DIVISIONS.put(70000, "§b§lCelestial III");
        DIVISIONS.put(80000, "§b§lCelestial IV");
        DIVISIONS.put(90000, "§b§lCelestial V");
        DIVISIONS.put(100000, "§d§lDivine");
        DIVISIONS.put(120000, "§d§lDivine II");
        DIVISIONS.put(140000, "§d§lDivine III");
        DIVISIONS.put(160000, "§d§lDivine IV");
        DIVISIONS.put(180000, "§d§lDivine V");
        DIVISIONS.put(200000, "§c§lAscended");
        DIVISIONS.put(220000, "§c§lAscended II");
        DIVISIONS.put(240000, "§c§lAscended III");
        DIVISIONS.put(260000, "§c§lAscended IV");
        DIVISIONS.put(280000, "§c§lAscended V");
        DIVISIONS.put(300000, "§c§lAscended VI");
        DIVISIONS.put(320000, "§c§lAscended VII");
        DIVISIONS.put(340000, "§c§lAscended VIII");
        DIVISIONS.put(360000, "§c§lAscended IX");
        DIVISIONS.put(380000, "§c§lAscended X");
        DIVISIONS.put(400000, "§c§lAscended XI");
        DIVISIONS.put(420000, "§c§lAscended XII");
        DIVISIONS.put(440000, "§c§lAscended XIII");
        DIVISIONS.put(460000, "§c§lAscended XIV");
        DIVISIONS.put(480000, "§c§lAscended XV");
        DIVISIONS.put(500000, "§c§lAscended XVI");
        DIVISIONS.put(520000, "§c§lAscended XVII");
        DIVISIONS.put(540000, "§c§lAscended XVIII");
        DIVISIONS.put(560000, "§c§lAscended XIX");
        DIVISIONS.put(580000, "§c§lAscended XX");
        DIVISIONS.put(600000, "§c§lAscended XXI");
        DIVISIONS.put(620000, "§c§lAscended XXII");
        DIVISIONS.put(640000, "§c§lAscended XXIII");
        DIVISIONS.put(660000, "§c§lAscended XXIV");
        DIVISIONS.put(680000, "§c§lAscended XXV");
        DIVISIONS.put(700000, "§c§lAscended XXVI");
        DIVISIONS.put(720000, "§c§lAscended XXVII");
        DIVISIONS.put(740000, "§c§lAscended XXVIII");
        DIVISIONS.put(760000, "§c§lAscended XXIX");
        DIVISIONS.put(780000, "§c§lAscended XXX");
        DIVISIONS.put(800000, "§c§lAscended XXXI");
        DIVISIONS.put(820000, "§c§lAscended XXXII");
        DIVISIONS.put(840000, "§c§lAscended XXXIII");
        DIVISIONS.put(860000, "§c§lAscended XXXIV");
        DIVISIONS.put(880000, "§c§lAscended XXXV");
        DIVISIONS.put(900000, "§c§lAscended XXXVI");
        DIVISIONS.put(920000, "§c§lAscended XXXVII");
        DIVISIONS.put(940000, "§c§lAscended XXXVIII");
        DIVISIONS.put(960000, "§c§lAscended XXXIX");
        DIVISIONS.put(980000, "§c§lAscended XL");
        DIVISIONS.put(1000000, "§c§lAscended XLI");
        DIVISIONS.put(1020000, "§c§lAscended XLII");
        DIVISIONS.put(1040000, "§c§lAscended XLIII");
        DIVISIONS.put(1060000, "§c§lAscended XLIV");
        DIVISIONS.put(1080000, "§c§lAscended XLV");
        DIVISIONS.put(1100000, "§c§lAscended XLVI");
        DIVISIONS.put(1120000, "§c§lAscended XLVII");
        DIVISIONS.put(1140000, "§c§lAscended XLVIII");
        DIVISIONS.put(1160000, "§c§lAscended XLIX");
        DIVISIONS.put(1180000, "§c§lAscended L");
    }

    public static String getPlayerDivision(int wins) {
        if (wins < 100) {
            return "";
        }

        return DIVISIONS.floorEntry(wins).getValue() + " ";
    }
}
