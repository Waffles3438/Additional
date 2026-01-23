package me.waffles.additional.util;

public class Bedwars {
    private final int Bedwarsstar;
    private final int Bedwarsfk;
    private final int Bedwarsbb;
    private final int Bedwarsw;
    private final int Bedwarsws;
    private final double Bedwarsfkdr;
    private final double Bedwarswlr;
    private final double Bedwarsbblr;

    public Bedwars(int Bedwarsstar, int Bedwarsfk, int Bedwarsbb, int Bedwarsw, int Bedwarsws, double Bedwarsfkdr, double Bedwarswlr,
                  double Bedwarsbblr) {
        this.Bedwarsstar = Bedwarsstar;
        this.Bedwarsfk = Bedwarsfk;
        this.Bedwarsbb = Bedwarsbb;
        this.Bedwarsw = Bedwarsw;
        this.Bedwarsws = Bedwarsws;
        this.Bedwarsfkdr = Bedwarsfkdr;
        this.Bedwarswlr = Bedwarswlr;
        this.Bedwarsbblr = Bedwarsbblr;
    }

    public int getBedwarsStar() { return Bedwarsstar; }
    public int getBedwarsFinalKills() { return Bedwarsfk; }
    public int getBedwarsBedBreaks() { return Bedwarsbb; }
    public int getBedwarsWins() { return Bedwarsw; }
    public int getBedwarsWinStreak() { return Bedwarsws; }
    public double getBedwarsFKDR() { return Bedwarsfkdr; }
    public double getBedwarsWLR() { return Bedwarswlr; }
    public double getBedwarsBBLR() { return Bedwarsbblr; }

}
