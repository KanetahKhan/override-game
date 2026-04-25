package com.override.backend.dto;

public class SaveRequest {
    private int chapterNumber;
    private String checkpoint;
    private int playerHp;
    private int dependencyMeter;
    private int coins;
    private String choicesJson;
    private String unlockedSkillsJson;

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int v) { this.chapterNumber = v; }

    public String getCheckpoint() { return checkpoint; }
    public void setCheckpoint(String v) { this.checkpoint = v; }

    public int getPlayerHp() { return playerHp; }
    public void setPlayerHp(int v) { this.playerHp = v; }

    public int getDependencyMeter() { return dependencyMeter; }
    public void setDependencyMeter(int v) { this.dependencyMeter = v; }

    public int getCoins() { return coins; }
    public void setCoins(int v) { this.coins = v; }

    public String getChoicesJson() { return choicesJson; }
    public void setChoicesJson(String v) { this.choicesJson = v; }

    public String getUnlockedSkillsJson() { return unlockedSkillsJson; }
    public void setUnlockedSkillsJson(String v) { this.unlockedSkillsJson = v; }
}
