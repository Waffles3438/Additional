package me.waffles.addition.util;

public class PlayerProfile {
    private final String displayName, rank;

    public PlayerProfile(String displayName, String rank) {
        this.rank = rank;
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
    public String getRank() { return rank; }
}
