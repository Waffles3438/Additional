package me.waffles.addition.command;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import me.waffles.addition.Addition;
import me.waffles.addition.util.Bedwars;
import me.waffles.addition.util.HypixelAPIUtils;
import me.waffles.addition.util.PlayerProfile;
import net.minecraft.client.Minecraft;

import java.io.IOException;

@Command(value = "bw")
public class BedwarsStatsCommand {

    @Main
    private void main() {
        String Username = Minecraft.getMinecraft().getSession().getProfile().getName();
        String uuid = Minecraft.getMinecraft().getSession().getProfile().getId().toString();

        Multithreading.runAsync(() ->
            fetchAndPrintStats(Username, uuid)
        );

    }

    @Main
    private void main(GameProfile player1) {
        Multithreading.runAsync(() -> {
            String player = player1.getName();
            String Username, uuid;
            try {
                uuid = player1.getId().toString();
                Username = player1.getName();
            } catch (Exception e) {
                UChat.chat("Invalid player");
                return;
            }

            fetchAndPrintStats(Username, uuid);
        });
    }

    private void fetchAndPrintStats(String Username, String uuid) {

        // fetch stats here
        if(!Addition.bedwarsStatsList.containsKey(Username.toLowerCase())) {
            try {
                Addition.bedwarsStatsList.put(Username.toLowerCase(), fetchPlayerBedwarsStats(uuid));
                Addition.playerProfileList.put(Username.toLowerCase(), fetchPlayerProfileData(uuid));
            } catch (IOException e) {
                UChat.chat("Something broke while fetching stats!");
                throw new RuntimeException(e);
            }
        }

        // prints stats here
        printStats(Username);
    }

    private void printStats(String Username) {
        PlayerProfile profile = Addition.playerProfileList.get(Username.toLowerCase());

        if(profile.getDisplayName() == null) {
            UChat.chat(Username + " has no Hypixel stats.");
            return;
        }
        String formattedName = profile.getDisplayName();

        Bedwars bedwarsStats = Addition.bedwarsStatsList.get(Username.toLowerCase());

        int bedwarsstar = bedwarsStats.getBedwarsStar();
        if (bedwarsstar == -1) {
            UChat.chat(Username + " has never played Bedwars");
            return;
        }

        formattedName = formateName(profile, formattedName);

        int bedwarsfk = bedwarsStats.getBedwarsFinalKills();
        int bedwarsbb = bedwarsStats.getBedwarsBedBreaks();
        int bedwarsw = bedwarsStats.getBedwarsWins();
        int bedwarsws = bedwarsStats.getBedwarsWinStreak();
        double bedwarsfkdr = bedwarsStats.getBedwarsFKDR();
        double bedwarswlr = bedwarsStats.getBedwarsWLR();
        double bedwarsbblr = bedwarsStats.getBedwarsBBLR();
        UChat.chat("§9------------------------------------------");
        UChat.chat(getFormattedRank(bedwarsstar) + " " + formattedName);
        UChat.chat("FKDR: " + formatColors(bedwarsfkdr, 15));
        UChat.chat("Final kills: " + formatColors(bedwarsfk, 25000));
        UChat.chat("WLR: " + formatColors(bedwarswlr, 5));
        UChat.chat("Wins: " + formatColors(bedwarsw, 20000));
        UChat.chat("BBLR: " + formatColors(bedwarsbblr, 5));
        UChat.chat("Beds: " + formatColors(bedwarsbb, 30000));
        if(bedwarsws != -1) UChat.chat("Winstreak: " + bedwarsws);
        UChat.chat("§9------------------------------------------");
    }

    static String formateName(PlayerProfile profile, String formattedName) {
        if(!profile.getRank().isEmpty() && !profile.getRank().equals("§7")) {
            formattedName = profile.getRank() + " " + formattedName;
        } else if (profile.getRank().equals("§7")) {
            formattedName = "§7" + formattedName;
        }

        return formattedName;
    }

    public String fetchPlayerData(String uuid) {
        return HypixelAPIUtils.fetchPlayerData(
                "http://api.abyssoverlay.com/player?uuid=" + uuid,
                "node-ao/2.0.3"
        );
    }

    public PlayerProfile fetchPlayerProfileData(String uuid)
            throws IOException {
        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return null;
        }
        return HypixelAPIUtils.parsePlayerProfilePlayerData(stjson);
    }

    public Bedwars fetchPlayerBedwarsStats(String uuid)
        throws IOException {
        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return null;
        }
        return HypixelAPIUtils.parseBedwarsPlayerData(stjson);
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
        else return Rank.FIVEZERO;
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
}
