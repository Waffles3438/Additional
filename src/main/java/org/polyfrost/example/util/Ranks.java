package org.polyfrost.example.util;

public class Ranks {
    private final String rank, special, monthly, MVPPlusPlusCheck, plusColor, admin;

    public Ranks(String ranks, String special, String monthly, String MVPPlusPlusCheck, String plusColor, String admin) {
        this.rank = ranks;
        this.special = special;
        this.monthly = monthly;
        this.MVPPlusPlusCheck = MVPPlusPlusCheck;
        this.plusColor = plusColor;
        this.admin = admin;
    }

    public String getRank() { return rank; }
    public String getSpecial() { return special; }
    public String getMonthly() { return monthly; }
    public String getMVPPlusPlusCheck() { return MVPPlusPlusCheck; }
    public String getplusColor() { return plusColor; }
    public String getAdmin() { return admin; }
}
