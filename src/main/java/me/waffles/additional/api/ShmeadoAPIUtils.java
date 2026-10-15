package me.waffles.additional.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShmeadoAPIUtils {
    private static final Pattern BEDWARS_OBJECT = Pattern.compile("bedwars\\s*:\\s*\\{");
    private static final Pattern DUELS_OBJECT = Pattern.compile("duels\\s*:\\s*\\{");
    private static final String BASE_URL = "https://shmeado.club/player/stats/%s/";
    private static final String GUILD_URL = "https://shmeado.club/player/guild/%s/";
    private static final String USER_AGENT = "node-ao/2.0.3";

    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>([^<]*?)'s Stats \\| Shmeado</title>");
    private static final Pattern RANK_PATTERN = Pattern.compile("rank:\\{rank:([^,}]+),rankPlusColor:([^,}]+),monthlyRankColor:([^,}]+)");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\bbedwars_level\\s*:\\s*(-?\\d+)");
    private static final Pattern NET_EXP_PATTERN = Pattern.compile("\\bnetwork_experience\\s*:\\s*(-?\\d+)");
    private static final Pattern KEY_PATTERN = Pattern.compile("([{,]\\s*)([A-Za-z_$][\\w$]*|\\d+)(\\s*:)");
    private static final Pattern TRAILING_COMMA = Pattern.compile(",\\s*([}\\]])");
    private static final Pattern BAD_TOKEN = Pattern.compile("\\b(undefined|NaN|Infinity)\\b");
    private static final Pattern DOT_NUMBER = Pattern.compile("(?<![.\\w])\\.(\\d+)");
    private static final Pattern INVALID_NAME_OR_UUID_PATTERN =
            Pattern.compile("Invalid\\s+Name\\s*/\\s*UUID", Pattern.CASE_INSENSITIVE);
    private static final String NO_HYPIXEL_PLAYER_JSON = "{\"player\":null}";

    public static String fetchPlayerStatsJson(String username) {
        String page = AbyssAPIUtils.fetchPlayerData(
                String.format(BASE_URL, username),
                USER_AGENT,
                "*/*"
        );
        return parsePlayerStatsPage(username, page);
    }

    /**
     * Fetches and converts one Shmeado stats-page request without transport retries.
     * The alternating provider coordinator owns retry ordering and wait behavior.
     */
    public static String fetchPlayerStatsJsonOnce(String username) {
        String page = AbyssAPIUtils.fetchPlayerDataOnce(
                String.format(BASE_URL, username),
                USER_AGENT,
                "*/*"
        );
        return parsePlayerStatsPage(username, page);
    }

    private static String parsePlayerStatsPage(String username, String page) {
        if (page == null || page.isEmpty()) {
            return "";
        }

        if (INVALID_NAME_OR_UUID_PATTERN.matcher(page).find()) {
            return NO_HYPIXEL_PLAYER_JSON;
        }

        JsonObject player = new JsonObject();

        Matcher nameMatcher = TITLE_PATTERN.matcher(page);
        String displayName = nameMatcher.find()
                ? nameMatcher.group(1)
                : username;
        player.addProperty("displayname", displayName);

        Matcher rankMatcher = RANK_PATTERN.matcher(page);
        if (rankMatcher.find()) {
            applyRank(player, unquote(rankMatcher.group(1)), unquote(rankMatcher.group(2)), unquote(rankMatcher.group(3)));
        }

        Matcher levelMatcher = LEVEL_PATTERN.matcher(page);
        if (levelMatcher.find()) {
            player.add("achievements", new JsonObject());
            player.getAsJsonObject("achievements")
                    .addProperty("bedwars_level", Integer.parseInt(levelMatcher.group(1)));
        }

        Matcher netExpMatcher = NET_EXP_PATTERN.matcher(page);
        if (netExpMatcher.find()) {
            player.addProperty("networkExp", Long.parseLong(netExpMatcher.group(1)));
        }

        JsonObject stats = new JsonObject();

        JsonObject bedwars = sanitizeToJson(extractJsObject(page, BEDWARS_OBJECT));
        if (bedwars != null) {
            stats.add("Bedwars", copyNumericFields(bedwars));
        }

        JsonObject duels = sanitizeToJson(extractJsObject(page, DUELS_OBJECT));
        if (duels != null) {
            stats.add("Duels", copyNumericFields(duels));
        }

        player.add("stats", stats);

        JsonObject root = new JsonObject();
        root.add("player", player);
        return root.toString();
    }

    public static String fetchPlayerGuildJson(String uuid) {
        String response = AbyssAPIUtils.fetchPlayerData(
                String.format(GUILD_URL, uuid),
                USER_AGENT
        );
        return parsePlayerGuildResponse(response);
    }

    /**
     * Fetches and converts one Shmeado guild request without transport retries.
     * An empty JSON object means the request succeeded but the player has no
     * guild; an empty string means the provider was unavailable or malformed.
     */
    public static String fetchPlayerGuildJsonOnce(String uuid) {
        String response = AbyssAPIUtils.fetchPlayerDataOnce(
                String.format(GUILD_URL, uuid),
                USER_AGENT
        );
        return parsePlayerGuildResponse(response);
    }

    private static String parsePlayerGuildResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }

        try {
            JsonObject root = new JsonParser().parse(response).getAsJsonObject();
            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                return "";
            }
            if (!root.has("guild") || root.get("guild").isJsonNull()) {
                return "{}";
            }

            JsonObject guild = root.getAsJsonObject("guild");
            JsonObject result = new JsonObject();
            if (guild.has("tag") && !guild.get("tag").isJsonNull()) {
                result.addProperty("tag", guild.get("tag").getAsString());
            }
            if (guild.has("tagColor") && !guild.get("tagColor").isJsonNull()) {
                result.addProperty("tagColor", guild.get("tagColor").getAsString());
            }
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String unquote(String value) {
        return value.replace("`", "");
    }

    private static void applyRank(JsonObject player, String rank, String rankPlusColor, String monthlyRankColor) {
        if (rank == null) {
            return;
        }

        switch (rank) {
            case "MVP++":
                player.addProperty("newPackageRank", "MVP_PLUS");
                if (monthlyRankColor != null && !monthlyRankColor.isEmpty()) {
                    player.addProperty("monthlyPackageRank", "SUPERSTAR");
                    player.addProperty("monthlyRankColor", monthlyRankColor);
                }
                if (rankPlusColor != null && !rankPlusColor.isEmpty()) {
                    player.addProperty("rankPlusColor", rankPlusColor);
                }
                break;
            case "MVP+":
                player.addProperty("newPackageRank", "MVP_PLUS");
                if (rankPlusColor != null && !rankPlusColor.isEmpty()) {
                    player.addProperty("rankPlusColor", rankPlusColor);
                }
                break;
            case "MVP":
                player.addProperty("newPackageRank", "MVP");
                break;
            case "VIP+":
                player.addProperty("newPackageRank", "VIP_PLUS");
                break;
            case "VIP":
                player.addProperty("newPackageRank", "VIP");
                break;
            case "YOUTUBE":
                player.addProperty("rank", "YOUTUBER");
                break;
            case "ADMIN":
            case "MODERATOR":
            case "HELPER":
            case "OWNER":
            case "MOJANG":
                player.addProperty("rank", "STAFF");
                break;
        }
    }

    private static String extractJsObject(String page, Pattern keyPattern) {
        Matcher matcher = keyPattern.matcher(page);
        if (!matcher.find()) {
            return null;
        }

        int i = page.indexOf('{', matcher.start());
        int j = i;
        int depth = 0;

        while (j < page.length()) {
            char c = page.charAt(j);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    break;
                }
            } else if (c == '`' || c == '\'') {
                int k = page.indexOf(c, j + 1);
                if (k == -1) {
                    return null;
                }
                j = k;
            }
            j++;
        }

        return page.substring(i, j + 1);
    }

    private static JsonObject sanitizeToJson(String raw) {
        try {
            String s = raw.replace('`', '"');
            s = BAD_TOKEN.matcher(s).replaceAll("null");
            s = KEY_PATTERN.matcher(s).replaceAll("$1\"$2\"$3");
            s = TRAILING_COMMA.matcher(s).replaceAll("$1");
            s = DOT_NUMBER.matcher(s).replaceAll("0.$1");
            return new JsonParser().parse(s).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject copyNumericFields(JsonObject source) {
        JsonObject result = new JsonObject();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : source.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                result.add(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
