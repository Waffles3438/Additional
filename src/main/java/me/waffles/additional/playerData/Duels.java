package me.waffles.additional.playerData;

public class Duels {
    private final int Duelswins;
    private final int Duelskills;
    private final int Duelsdeaths;
    private final int Duelscws;
    private final int Duelsbws;
    private final double Duelswlr;
    private final double Duelskdr;
    private final String Level;
    public Duels(int Duelskills, int Duelsdeaths, int Duelswins, int Duelscws, int Duelsbws, double Duelswlr, double Duelskdr, String Level) {
        this.Duelskills = Duelskills;
        this.Duelsdeaths = Duelsdeaths;
        this.Duelswins = Duelswins;
        this.Duelscws = Duelscws;
        this.Duelsbws = Duelsbws;
        this.Duelskdr = Duelskdr;
        this.Duelswlr = Duelswlr;
        this.Level = Level;
    }

    public int getDuelsKills() { return Duelskills; }
    public int getDuelsDeaths() { return Duelsdeaths; }
    public int getDuelsWins() { return Duelswins; }
    public int getDuelsCWS() { return Duelscws; }
    public int getDuelsBWS() { return Duelsbws; }
    public double getDuelsKDR() { return Duelskdr; }
    public double getDuelsWLR() { return Duelswlr; }
    public String getLevel() { return Level; }

}
