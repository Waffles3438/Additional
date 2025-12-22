package me.waffles.addition.util;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.waffles.addition.command.DuelsStatsCommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

                Pattern pattern = Pattern.compile(
                        "playerData = JSON.parse\\(decodeURIComponent\\(\"(.*?)\"\\)\\)"
                );
                Matcher matcher = pattern.matcher(response.toString());

                if (matcher.find()) {
                    String playerDataEncoded = matcher.group(1);
                    return URLDecoder.decode(playerDataEncoded, "UTF-8");
                }

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

    public static PlayerProfile parsePlayerProfilePlayerData(String json) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();

        boolean inGuild = true;

        JsonObject guild = null, profile;

        try {
            profile = rootObject
                    .getAsJsonObject("profile");
            if(profile == null) {
                return new PlayerProfile(
                        null,
                        null
                );
            }
        } catch (Exception e) {
            UChat.chat("Something broke in parsePlayerProfilePlayerData");
            e.printStackTrace();
            return new PlayerProfile(
                    null,
                    null
            );
        }

        try {
            guild = rootObject
                    .getAsJsonObject("guild");
        } catch (Exception e) {
            inGuild = false;
            e.printStackTrace();
        }

        String guildTag = "";
        if (inGuild) {
            guildTag = guild.has("tag")
                    ? guild.get("tag").getAsString()
                    : "";
        }

        String rank = profile.has("tag")
                ? profile.get("tag").getAsString()
                : "";

        return new PlayerProfile(
                rank,
                guildTag
        );
    }

    public static Duels parseDuelsPlayerData(String json) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();

        JsonObject duelsStats = null, profile = null;

        try {
            if(rootObject.has("stats") && rootObject.getAsJsonObject("stats").has("Duels") && rootObject.has("profile")) {
                duelsStats = rootObject
                        .getAsJsonObject("stats")
                        .getAsJsonObject("Duels");
                profile = rootObject
                        .getAsJsonObject("profile");
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
        assert profile != null;
        double level = profile.has("network_level")
                ? profile.get("network_level").getAsDouble()
                : 0;
        level = (double) Math.round(level * 100) / 100;
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

    public static Bedwars parseBedwarsPlayerData(String json) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();

        JsonObject achievements = null;
        JsonObject bedwarsStats = null;
        try {
            if(rootObject.has("achievements") && rootObject.has("stats") && rootObject.getAsJsonObject("stats").has("Bedwars")) {
                bedwarsStats = rootObject
                        .getAsJsonObject("stats")
                        .getAsJsonObject("Bedwars");
                achievements = rootObject.getAsJsonObject("achievements");
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
        }

        assert achievements != null;
        int stars = achievements.has("bedwars_level")
                ? achievements.get("bedwars_level").getAsInt()
                : 0;

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
}
