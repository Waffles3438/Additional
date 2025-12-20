package me.waffles.addition.util;

public class PlayerProfile {
    private final String rank, guildTag;

    public PlayerProfile(String rank, String guildTag) {
        this.rank = rank;
        this.guildTag = guildTag;
    }

    public String getRank() { return rank; }
    public String getGuildTag() { return guildTag; }
}
