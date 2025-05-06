package org.polyfrost.example.command;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
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

@Command(value = "bw")
public class BedwarsStatsCommand {

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
        Username = Minecraft.getMinecraft().getSession().getProfile().getName();
        uuid = Minecraft.getMinecraft().getSession().getProfile().getId().toString();

        Multithreading.runAsync(() -> {
            boolean request = true;

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
                    d = profile.getAsJsonObject("stats").getAsJsonObject("Bedwars");
                    ach = profile.getAsJsonObject("achievements");
                } catch (NullPointerException er) {
                    // never played bedwars or joined lobby
                    UChat.chat(Username + " has never played Bedwars");
                    try {
                        profile = getStringAsJson(connection).getAsJsonObject("player");
                        d = profile.getAsJsonObject("stats").getAsJsonObject("Duels");
                        ach = profile.getAsJsonObject("achievements");
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

                        if(Duelswins != 0 && Duelslosses != 0) Addition.duelsStatsList.put(Username, new Duels(Duelskills, Duelsdeaths, Duelswins, Duelslosses, Duelscws, Duelsbws, Duelswlr, Duelskdr, Level));
                    } catch (NullPointerException err) {
                        // never played duels or joined lobby
                    }
                    return;
                }
                requestStats(player);
            } else getStats(Username);
        });
    }

    @Main
    private void main(GameProfile player1) {
        Multithreading.runAsync(() -> {
            String player = player1.getName();
            boolean request = true;
            try {
                JsonObject minecraft = NetworkUtils.getJsonElement("https://api.mojang.com/users/profiles/minecraft/" + player).getAsJsonObject();
                uuid = minecraft.get("id").getAsString();
                Username = minecraft.get("name").getAsString();
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
                    d = profile.getAsJsonObject("stats").getAsJsonObject("Bedwars");
                    ach = profile.getAsJsonObject("achievements");
                } catch (NullPointerException er) {
                    // never played bedwars or joined lobby
                    UChat.chat(Username + " has never played Bedwars");
                    try {
                        profile = getStringAsJson(connection).getAsJsonObject("player");
                        d = profile.getAsJsonObject("stats").getAsJsonObject("Duels");
                        ach = profile.getAsJsonObject("achievements");
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

                        if(Duelswins != 0 && Duelslosses != 0) Addition.duelsStatsList.put(Username, new Duels(Duelskills, Duelsdeaths, Duelswins, Duelslosses, Duelscws, Duelsbws, Duelswlr, Duelskdr, Level));
                    } catch (NullPointerException err) {
                        // never played duels or joined lobby
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
        if (!Addition.bedwarsStatsList.containsKey(Player)) {
            UChat.chat(Player + " has never played Bedwars");
            return;
        }
        Ranks rankStuff = Addition.playerRanks.get(Player);
        rank = rankStuff.getRank();
        special = rankStuff.getSpecial();
        monthly = rankStuff.getMonthly();
        MVPPlusPlusCheck = rankStuff.getMVPPlusPlusCheck();
        plusColor = rankStuff.getplusColor();
        admin = rankStuff.getAdmin();
        Bedwars bedwarsStats = Addition.bedwarsStatsList.get(Player);
        Bedwarsstar = bedwarsStats.getBedwarsStar();
        Bedwarsfk = bedwarsStats.getBedwarsFinalKills();
        Bedwarsbb = bedwarsStats.getBedwarsBedBreaks();
        Bedwarsw = bedwarsStats.getBedwarsWins();
        Bedwarsl = bedwarsStats.getBedwarsLosses();
        Bedwarsfd = bedwarsStats.getBedwarsFinalDeaths();
        Bedwarsbl = bedwarsStats.getBedwarsBedsLost();
        Bedwarsws = bedwarsStats.getBedwarsWinStreak();
        Bedwarsfkdr = bedwarsStats.getBedwarsFKDR();
        Bedwarswlr = bedwarsStats.getBedwarsWLR();
        Bedwarsbblr = bedwarsStats.getBedwarsBBLR();
        UChat.chat("§9------------------------------------------");
        UChat.chat(getFormattedRank(Bedwarsstar) + " " + formatWithoutRequestRank(Username));
        UChat.chat("FKDR: " + formatColors(Bedwarsfkdr, 15));
        UChat.chat("Final kills: " + formatColors(Bedwarsfk, 25000));
        UChat.chat("WLR: " + formatColors(Bedwarswlr, 5));
        UChat.chat("Wins: " + formatColors(Bedwarsw, 20000));
        UChat.chat("BBLR: " + formatColors(Bedwarsbblr, 5));
        UChat.chat("Beds: " + formatColors(Bedwarsbb, 30000));
        if(Bedwarsws != -1) UChat.chat("Winstreak: " + Bedwarsws);
        UChat.chat("§9------------------------------------------");
    }

    private void requestStats(String player){
        try {
            profile = getStringAsJson(connection).getAsJsonObject("player");
            bw = profile.getAsJsonObject("stats").getAsJsonObject("Bedwars");
            ach = profile.getAsJsonObject("achievements");
        } catch (NullPointerException er) {
            // never played bedwars or joined lobby
            UChat.chat("Player has never played bedwars");
            return;
        }

        // Duels
        try {
            d = profile.getAsJsonObject("stats").getAsJsonObject("Duels");
        } catch (NullPointerException ignored) {

        }

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

        boolean added = false;
        if (Bedwarsl != 0 || Bedwarsw != 0) {
            UChat.chat("§9------------------------------------------");
            UChat.chat(getFormattedRank(Bedwarsstar) + " " + formatRank(profile, Username));
            UChat.chat("FKDR: " + formatColors(Bedwarsfkdr, 15));
            UChat.chat("Final kills: " + formatColors(Bedwarsfk, 25000));
            UChat.chat("WLR: " + formatColors(Bedwarswlr, 5));
            UChat.chat("Wins: " + formatColors(Bedwarsw, 20000));
            UChat.chat("BBLR: " + formatColors(Bedwarsbblr, 5));
            UChat.chat("Beds: " + formatColors(Bedwarsbb, 30000));
            if(Bedwarsws != -1) UChat.chat("Winstreak: " + Bedwarsws);
            UChat.chat("§9------------------------------------------");
            Addition.bedwarsStatsList.remove(Username);
            Addition.bedwarsStatsList.put(Username, new Bedwars(Bedwarsstar, Bedwarsfk, Bedwarsbb, Bedwarsw, Bedwarsl, Bedwarsfd, Bedwarsbl, Bedwarsws, Bedwarsfkdr, Bedwarswlr, Bedwarsbblr));
            if(!Addition.properPlayerNames.containsKey(player.toLowerCase())) Addition.properPlayerNames.put(player.toLowerCase(), Username);
        } else {
            UChat.chat(Username + " has never played Bedwars");
        }

        if(Addition.duelsStatsList.containsKey(Username) && (Duelslosses != 0 || Duelswins != 0)) Addition.duelsStatsList.remove(Username);
        if (Duelslosses != 0 || Duelswins != 0) {
            Addition.duelsStatsList.put(Username, new Duels(Duelskills, Duelsdeaths, Duelswins, Duelslosses, Duelscws, Duelsbws, Duelswlr, Duelskdr, Level));
            if(!Addition.properPlayerNames.containsKey(player.toLowerCase())) Addition.properPlayerNames.put(player.toLowerCase(), Username);
        }
        Addition.playerRanks.remove(Username);
        Addition.playerRanks.put(Username, new Ranks(rank, special, monthly, MVPPlusPlusCheck, plusColor, admin));
    }

    private int getValue(JsonObject type, String member) {
        try {
            return type.get(member).getAsInt();
        } catch (NullPointerException er) {
            if (member.equals("winstreak") || member.equals("current_winstreak") || member.equals("best_overall_winstreak")) return -1;
            return 0;
        }
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

    public enum Rank {
        STONE1("§7[_✫]"),
        STONE("§7[__✫]"),
        IRON("§f[___✫]"),
        GOLD("§6[___✫]"),
        DIAMOND("§b[___✫]"),
        EMERALD("§2[___✫]"),
        SAPPHIRE("§3[___✫]"),
        RUBY("§4[___✫]"),
        CRYSTAL("§d[___✫]"),
        OPAL("§9[___✫]"),
        AMETHYST("§5[___✫]"),
        RAINBOW("§c[§6_§e_§a_§b_§d✫§5]"),
        IRON_PRIME("§7[§f____§7✪§7]"),
        GOLD_PRIME("§7[§e____§6✪§7]"),
        DIAMOND_PRIME("§7[§b____§3✪§7]"),
        EMERALD_PRIME("§7[§a____§2✪§7]"),
        SAPPHIRE_PRIME("§7[§3____§9✪§7]"),
        RUBY_PRIME("§7[§c____§4✪§7]"),
        CRYSTAL_PRIME("§7[§d____§5✪§7]"),
        OPAL_PRIME("§7[§9____§1✪§7]"),
        AMETHYST_PRIME("§7[§5____§8✪§7]"),
        MIRROR("§8[§7_§f__§7_§8✪]"),
        LIGHT("§f[_§e__§6_§l⚝§6]"),
        DAWN("§6[_§f__§b_§3§l⚝§3]"),
        DUSK("§5[_§d__§6_§e§l⚝§e]"),
        AIR("§b[_§f__§7_§l⚝§8]"),
        WIND("§f[_§a__§2_§l⚝§2]"),
        NEBULA("§4[_§c__§d_§l⚝§d]"),
        THUNDER("§e[_§f__§8_§l⚝§8]"),
        EARTH("§a[_§2__§6_§l⚝§e]"),
        WATER("§b[_§3__§9_§l⚝§1]"),
        FIRE("§e[_§6__§c_§l⚝§4]"),
        THREEONE("§9[_§3__§60✥§e]"),
        THREETWO("§c[§4_§7__§4_§c✥]"),
        THREETHREE("§9[__§d_§6_✥§d]"),
        THREEFOUR("§2[_§d__§5_✥§2]"),
        THREEFIVE("§c[_§4__§2_§a✥]"),
        THREESIX("§a[__§b_§9_✥§1]"),
        THREESEVEN("§4[_§c__§b_§3✥]"),
        THREEEIGHT("§1[_§9_§5__§d✥§1]"),
        THREENINE("§c[_§a__§3_§9✥]"),
        FOURZERO("§5[_§c__§6_✥§e]"),
        FOURONE("§e[_§6_§c_§d_✥§5]"),
        FOURTWO("§1[§9_§3_§b_§f_§7✥]"),
        FOURTHREE("§0[§5_§8__§5_✥§0]"),
        FOURFOUR("§2[_§a_§e_§6_§5✥§d]"),
        FOURFIVE("§f[_§b__§3_✥]"),
        FOURSIX("§3[§b_§e__§6_§d✥§5]"),
        FOURSEVEN("§f[§4_§c__§9_§1✥§9]"),
        FOUREIGHT("§5[_§c_§6_§e_§b✥§3]"),
        FOURNINE("§2[§a_§f__§a_✥§2]"),
        FIVEZERO("§4[_§5_§9__§1✥§0]");

        private final String format;

        Rank(String format) {
            this.format = format;
        }

        public String getFormat() {
            return format;
        }
    }

    public String getFormattedRank(int star) {
        Rank rank = getRankForNumber(star);
        String starString = String.valueOf(star);
        StringBuilder txt = new StringBuilder(rank.getFormat());
        int starCounter = 0;
        for(int i = 0; i < txt.length(); i++){
            if(rank.getFormat().charAt(i) == '_'){
                txt.deleteCharAt(i);
                txt.insert(i, starString.charAt(starCounter));
                starCounter++;
            }
        }
        return txt.toString();
    }

    private Rank getRankForNumber(int number) {
        if (number < 10) return Rank.STONE1;
        else if (number < 100) return Rank.STONE;
        else if (number < 200) return Rank.IRON;
        else if (number < 300) return Rank.GOLD;
        else if (number < 400) return Rank.DIAMOND;
        else if (number < 500) return Rank.EMERALD;
        else if (number < 600) return Rank.SAPPHIRE;
        else if (number < 700) return Rank.RUBY;
        else if (number < 800) return Rank.CRYSTAL;
        else if (number < 900) return Rank.OPAL;
        else if (number < 1000) return Rank.AMETHYST;
        else if (number < 1100) return Rank.RAINBOW;
        else if (number < 1200) return Rank.IRON_PRIME;
        else if (number < 1300) return Rank.GOLD_PRIME;
        else if (number < 1400) return Rank.DIAMOND_PRIME;
        else if (number < 1500) return Rank.EMERALD_PRIME;
        else if (number < 1600) return Rank.SAPPHIRE_PRIME;
        else if (number < 1700) return Rank.RUBY_PRIME;
        else if (number < 1800) return Rank.CRYSTAL_PRIME;
        else if (number < 1900) return Rank.OPAL_PRIME;
        else if (number < 2000) return Rank.AMETHYST_PRIME;
        else if (number < 2100) return Rank.MIRROR;
        else if (number < 2200) return Rank.LIGHT;
        else if (number < 2300) return Rank.DAWN;
        else if (number < 2400) return Rank.DUSK;
        else if (number < 2500) return Rank.AIR;
        else if (number < 2600) return Rank.WIND;
        else if (number < 2700) return Rank.NEBULA;
        else if (number < 2800) return Rank.THUNDER;
        else if (number < 2900) return Rank.EARTH;
        else if (number < 3000) return Rank.WATER;
        else if (number < 3100) return Rank.FIRE;
        else if (number < 3200) return Rank.THREEONE;
        else if (number < 3300) return Rank.THREETWO;
        else if (number < 3400) return Rank.THREETHREE;
        else if (number < 3500) return Rank.THREEFOUR;
        else if (number < 3600) return Rank.THREEFIVE;
        else if (number < 3700) return Rank.THREESIX;
        else if (number < 3800) return Rank.THREESEVEN;
        else if (number < 3900) return Rank.THREEEIGHT;
        else if (number < 4000) return Rank.THREENINE;
        else if (number < 4100) return Rank.FOURZERO;
        else if (number < 4200) return Rank.FOURONE;
        else if (number < 4300) return Rank.FOURTWO;
        else if (number < 4400) return Rank.FOURTHREE;
        else if (number < 4500) return Rank.FOURFOUR;
        else if (number < 4600) return Rank.FOURFIVE;
        else if (number < 4700) return Rank.FOURSIX;
        else if (number < 4800) return Rank.FOURSEVEN;
        else if (number < 4900) return Rank.FOUREIGHT;
        else if (number < 5000) return Rank.FOURNINE;
        else if (number < 5100) return Rank.FIVEZERO;
        return Rank.RAINBOW;
    }

    private final double BASE = 10_000;
    private final double GROWTH = 2_500;

    private double getLevel(double exp) {
        double GROWTH_DIVIDES_2 = 2 / GROWTH;
        double REVERSE_PQ_PREFIX = -(BASE - 0.5 * GROWTH) / GROWTH;
        double REVERSE_CONST = REVERSE_PQ_PREFIX * REVERSE_PQ_PREFIX;
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
}
