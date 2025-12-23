package me.waffles.addition.util;

public class PlayerProfile {
    private final String displayName, rank, guildTag;

    public PlayerProfile(String displayName, String rank, String guildTag) {
        this.rank = rank;
        this.guildTag = guildTag;
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
    public String getRank() { return rank; }
    public String getGuildTag() { return guildTag; }
}
