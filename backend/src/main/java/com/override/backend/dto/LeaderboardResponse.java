package com.override.backend.dto;

public class LeaderboardResponse {
    private String displayName;
    private int score;
    private String endingType;
    private long completionTimeSeconds;

    public LeaderboardResponse(String displayName, int score, String endingType, long completionTimeSeconds) {
        this.displayName = displayName;
        this.score = score;
        this.endingType = endingType;
        this.completionTimeSeconds = completionTimeSeconds;
    }

    public String getDisplayName() { return displayName; }
    public int getScore() { return score; }
    public String getEndingType() { return endingType; }
    public long getCompletionTimeSeconds() { return completionTimeSeconds; }
}
