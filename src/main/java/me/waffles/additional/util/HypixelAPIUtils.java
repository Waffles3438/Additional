package me.waffles.additional.util;

import cc.polyfrost.oneconfig.libs.universal.UChat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.waffles.additional.command.DuelsStatsCommand;
import me.waffles.additional.playerData.Bedwars;
import me.waffles.additional.playerData.Duels;
import me.waffles.additional.playerData.PlayerProfile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// debugging imports
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import java.io.IOException;

public class HypixelAPIUtils {
    public static String fetchPlayerData(String urlString, String userAgent) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (userAgent != null) {
                connection.setRequestProperty("User-Agent", userAgent);
            }
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }

    public static PlayerProfile parsePlayerProfilePlayerData(String json, String guild) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
        JsonObject guildObject = new JsonParser().parse(guild).getAsJsonObject();

//        saveJsonObject(guildObject, "blank"); // debugging stuff

        JsonObject profile;

        try {
            if(!rootObject.get("player").isJsonNull()) {
                profile = rootObject
                        .getAsJsonObject("player");
            } else {
                return new PlayerProfile(
                        null,
                        null,
                        null
                );
            }
        } catch (Exception e) {
            UChat.chat("Something broke in parsePlayerProfilePlayerData");
            e.printStackTrace();
            return new PlayerProfile(
                    null,
                    null,
                    null
            );
        }

        String tag = guildObject.has("tag")
                ? guildObject.get("tag").getAsString()
                : null;

        if(tag != null) {
            tag = tag.replaceAll("âœ§", "✧")
                    .replaceAll("Î˜", "Θ")
                    .replaceAll("âœŒ", "✌")
                    .replaceAll("âœ¿", "✿")
                    .replaceAll("âœª", "✪")
                    .replaceAll("âžŠ", "➊")
                    .replaceAll("âœ–", "✖")
                    .replaceAll("â?¤", "❤")
                    .replaceAll("âœ“", "✓")
                    .replaceAll("[^a-zA-Z0-9✧θ✌✿✪➊✖❤✓]", "");
        }
        
        String tagColor = guildObject.has("tagColor")
                ? guildObject.get("tagColor").getAsString()
                : null;

        String displayName = profile.has("displayname")
                ? profile.get("displayname").getAsString()
                : null;

        String newPackageRank = profile.has("newPackageRank")
                ? profile.get("newPackageRank").getAsString()
                : null;

        String rankPlusColor = profile.has("rankPlusColor")
                ? profile.get("rankPlusColor").getAsString()
                : null;

        String monthlyPackageRank = profile.has("monthlyPackageRank")
                ? profile.get("monthlyPackageRank").getAsString()
                : null;

        String monthlyRankColor = profile.has("monthlyRankColor")
                ? profile.get("monthlyRankColor").getAsString()
                : null;

        String rank = profile.has("rank")
                ? profile.get("rank").getAsString()
                : null;

        String prefix = profile.has("prefix")
                ? profile.get("prefix").getAsString()
                : null;

        return new PlayerProfile(
                displayName,
                formatRank(displayName, newPackageRank, rankPlusColor, monthlyPackageRank, monthlyRankColor, rank, prefix),
                formatGuildTag(tag, tagColor)
        );
    }

    public static String formatGuildTag(String tag, String tagColor) {
        if(tag == null) {
            return "";
        }
        if (tagColor == null) {
            return "§7[" + tag + "]";
        }
        switch (tagColor) {
            case "DARK_AQUA":
                return "§3[" + tag + "]";
            case "DARK_GREEN":
                return "§2[" + tag + "]";
            case "YELLOW":
                return "§e[" + tag + "]";
            case "GOLD":
                return "§6[" + tag + "]";
            default:
                return "§7[" + tag + "]";
        }
    }

    public static String formatRank(String displayName, String newPackageRank, String rankPlusColor, String monthlyPackageRank, String monthlyRankColor, String rank, String prefix) {
        if (displayName == null) return null;
        if(displayName.equals("Technoblade"))  return "§d[PIG§b+++§d]";
        else if (displayName.equals("TommyInnit"))  return "§d[INNIT]";
        else if (prefix != null && prefix.equals("§6[MOJANG]")) return "§6[MOJANG]";
        else if (rank != null && rank.equals("STAFF")) return "§c[§6ዞ§c]";
        else if (rank != null && rank.equals("YOUTUBER")) return "§c[§fYOUTUBE§c]";
        else if (newPackageRank != null && newPackageRank.equals("MVP_PLUS") && rankPlusColor != null) {
            String color;
            switch (rankPlusColor) {
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
                case "DARK_GRAY":
                    color = "§8";
                    break;
                case "BLACK":
                    color = "§0";
                    break;
                case "DARK_BLUE":
                    color = "§1";
                    break;
                default:
                    color = "§c";
                    break;
            }
            if (monthlyPackageRank != null && monthlyPackageRank.equals("SUPERSTAR")) {
                if (monthlyRankColor != null && monthlyRankColor.equals("AQUA")) return "§b[MVP" + color + "++" + "§b]";
                else return "§6[MVP" + color + "++" + "§6]";
            } else {
                return "§b[MVP" + color + "+" + "§b]";
            }
        } else if (newPackageRank != null && newPackageRank.equals("MVP_PLUS")) return "§b[MVP§c+§b]";
        else if (newPackageRank != null && newPackageRank.equals("MVP")) return "§b[MVP]";
        else if (newPackageRank != null && newPackageRank.equals("VIP_PLUS")) return "§a[VIP§6+§a]";
        else if (newPackageRank != null && newPackageRank.equals("VIP")) return "§a[VIP]";
        else return "§7";
    }

    public static Duels parseDuelsPlayerData(String json) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
        JsonObject duelsStats, profile;

        try {
            if(!rootObject.get("player").isJsonNull()
                    && rootObject.get("player").getAsJsonObject().has("stats")
                    && rootObject.get("player").getAsJsonObject().getAsJsonObject("stats").has("Duels")) {
                profile = rootObject.get("player").getAsJsonObject();
                duelsStats = profile.getAsJsonObject("stats").getAsJsonObject("Duels");
            } else {
                return new Duels(
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        null
                );
            }
        } catch (Exception e) {
            UChat.chat("Something broke in parseDuelsPlayerData!");
            e.printStackTrace();
            return new Duels(
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    null
            );
        }

        assert duelsStats != null;
        int wins = duelsStats.has("wins")
                ? duelsStats.get("wins").getAsInt()
                : 0;
        int losses = duelsStats.has("losses")
                ? duelsStats.get("losses").getAsInt()
                : 0;
        int kills = duelsStats.has("kills")
                ? duelsStats.get("kills").getAsInt()
                : 0;
        int deaths = duelsStats.has("deaths")
                ? duelsStats.get("deaths").getAsInt()
                : 0;
        int current_winstreak = duelsStats.has("current_winstreak")
                ? duelsStats.get("current_winstreak").getAsInt()
                : -1;
        int best_winstreak = duelsStats.has("best_winstreak")
                ? duelsStats.get("best_winstreak").getAsInt()
                : -1;
        double wlr = (losses == 0)
                ? wins
                : (double) wins/losses;
        double kdr = (deaths == 0)
                ? kills
                : (double) kills/deaths;
        wlr = (double) Math.round(wlr * 100) / 100;
        kdr = (double) Math.round(kdr * 100) / 100;
        double level = profile.has("networkExp")
                ? (double) Math.round(getExactLevel(profile.get("networkExp").getAsDouble()) * 100) / 100
                : 0;

        String stringLevel = DuelsStatsCommand.levelColor(String.valueOf(level));

        return new Duels(
                kills,
                deaths,
                wins,
                current_winstreak,
                best_winstreak,
                wlr,
                kdr,
                stringLevel
        );
    }

//    public static void saveJsonObject(JsonObject jsonObject, String filePath)
//            throws IOException {
//
//        Gson gson = new GsonBuilder()
//                .setPrettyPrinting()
//                .create();
//
//        String formattedJson = gson.toJson(jsonObject);
//
//        Path path = Paths.get(filePath);
//        if (path.getParent() != null) {
//            Files.createDirectories(path.getParent());
//        }
//
//        Files.write(path, formattedJson.getBytes(StandardCharsets.UTF_8));
//    }

    private static final double BASE = 10_000;
    private static final double GROWTH = 2_500;

    private static double getLevel(double exp) {
        double REVERSE_PQ_PREFIX = -(BASE - 0.5 * GROWTH) / GROWTH;
        double REVERSE_CONST = REVERSE_PQ_PREFIX * REVERSE_PQ_PREFIX;
        double GROWTH_DIVIDES_2 = 2 / GROWTH;
        return exp < 0 ? 1 : Math.floor(1 + REVERSE_PQ_PREFIX + Math.sqrt(REVERSE_CONST + GROWTH_DIVIDES_2 * exp));
    }

    private static double getExactLevel(double exp) {
        return getLevel(exp) + getPercentageToNextLevel(exp);
    }

    private static double getTotalExpToFullLevel(double level) {
        double HALF_GROWTH = 0.5 * GROWTH;
        return (HALF_GROWTH * (level - 2) + BASE) * (level - 1);
    }

    private static double getTotalExpToLevel(double level) {
        double lv = Math.floor(level), x0 = getTotalExpToFullLevel(lv);
        if (level == lv) return x0;
        return (getTotalExpToFullLevel(lv + 1) - x0) * (level % 1) + x0;
    }

    private static double getPercentageToNextLevel(double exp) {
        double lv = getLevel(exp), x0 = getTotalExpToLevel(lv);
        return (exp - x0) / (getTotalExpToLevel(lv + 1) - x0);
    }

    public static Bedwars parseBedwarsPlayerData(String json) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
//        saveJsonObject(rootObject, "player-data.json"); debugging stuff

        JsonObject achievements;
        JsonObject bedwarsStats;

        try {
            if(!rootObject.get("player").isJsonNull()
                    && rootObject.getAsJsonObject("player").has("achievements")
                    && rootObject.getAsJsonObject("player").has("stats")
                    && rootObject.getAsJsonObject("player").getAsJsonObject("stats").has("Bedwars")) {
                bedwarsStats = rootObject
                        .getAsJsonObject("player")
                        .getAsJsonObject("stats")
                        .getAsJsonObject("Bedwars");
                achievements = rootObject.getAsJsonObject("player").getAsJsonObject("achievements");
            } else {
                return new Bedwars(
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1,
                        -1
                );
            }
        } catch (Exception e) {
            UChat.chat("Something broke in parseBedwarsPlayerData!");
            e.printStackTrace();
            return new Bedwars(
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1
            );
        }

        assert achievements != null;
        int stars = achievements.has("bedwars_level")
                ? achievements.get("bedwars_level").getAsInt()
                : 0;

        assert bedwarsStats != null;
        int finalKills = bedwarsStats.has("final_kills_bedwars")
                ? bedwarsStats.get("final_kills_bedwars").getAsInt()
                : 0;
        int finalDeaths = bedwarsStats.has("final_deaths_bedwars")
                ? bedwarsStats.get("final_deaths_bedwars").getAsInt()
                : 0;
        double fkdr = (finalDeaths == 0)
                ? finalKills
                : (double) finalKills / finalDeaths;
        int winstreak = bedwarsStats.has("winstreak")
                ? bedwarsStats.get("winstreak").getAsInt()
                : -1;
        int wins = bedwarsStats.has("wins_bedwars")
                ? bedwarsStats.get("wins_bedwars").getAsInt()
                : 0;
        int losses = bedwarsStats.has("losses_bedwars")
                ? bedwarsStats.get("losses_bedwars").getAsInt()
                : 0;
        double wlr = (losses ==0)
                ? wins
                : (double) wins/losses;
        int bedsBroken = bedwarsStats.has("beds_broken_bedwars")
                ? bedwarsStats.get("beds_broken_bedwars").getAsInt()
                : 0;
        int bedsLost = bedwarsStats.has("beds_lost_bedwars")
                ? bedwarsStats.get("beds_lost_bedwars").getAsInt()
                : 0;
        double bblr = (bedsLost == 0)
                ? bedsBroken
                : (double) bedsBroken/bedsLost;
        fkdr = (double) Math.round(fkdr * 100) / 100;
        wlr = (double) Math.round(wlr * 100) / 100;
        bblr = (double) Math.round(bblr * 100) / 100;

        return new Bedwars(
            stars,
            finalKills,
            bedsBroken,
            wins,
            winstreak,
            fkdr,
            wlr,
            bblr
        );
    }

//    debugging stuff
//    public static void saveRootObjectAsJson(JsonObject rootObject) {
//        Gson gson = new GsonBuilder()
//                .setPrettyPrinting()
//                .create();
//
//        String jsonOutput = gson.toJson(rootObject);
//
//        try {
//            Files.write(
//                    Paths.get("yt.json"),
//                    jsonOutput.getBytes(StandardCharsets.UTF_8)
//            );
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
