package com.override.backend.dto;

public class ProgressRequest {
    private int chapterNumber;
    private boolean completed;
    private int score;
    private long timeSpentSeconds;
    private int aiHelpUsed;

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int v) { this.chapterNumber = v; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean v) { this.completed = v; }

    public int getScore() { return score; }
    public void setScore(int v) { this.score = v; }

    public long getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(long v) { this.timeSpentSeconds = v; }

    public int getAiHelpUsed() { return aiHelpUsed; }
    public void setAiHelpUsed(int v) { this.aiHelpUsed = v; }
}
