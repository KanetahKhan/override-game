package com.override.backend.dto;

/** Payload for submitting a finished mini-game run to {@code POST /api/highscore}. */
public class HighScoreRequest {
    private String gameType;
    private int score;
    private int combo;
    private int wave;
    private boolean assisted;

    public String getGameType() { return gameType; }
    public void setGameType(String v) { this.gameType = v; }

    public int getScore() { return score; }
    public void setScore(int v) { this.score = v; }

    public int getCombo() { return combo; }
    public void setCombo(int v) { this.combo = v; }

    public int getWave() { return wave; }
    public void setWave(int v) { this.wave = v; }

    public boolean isAssisted() { return assisted; }
    public void setAssisted(boolean v) { this.assisted = v; }
}
