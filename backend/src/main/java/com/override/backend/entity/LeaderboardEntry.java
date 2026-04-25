package com.override.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard_entries")
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    private int score;

    @Column(name = "ending_type", length = 30)
    private String endingType;

    @Column(name = "completion_time_seconds")
    private long completionTimeSeconds;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LeaderboardEntry() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public PlayerProfile getPlayer() { return player; }
    public void setPlayer(PlayerProfile player) { this.player = player; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getEndingType() { return endingType; }
    public void setEndingType(String endingType) { this.endingType = endingType; }

    public long getCompletionTimeSeconds() { return completionTimeSeconds; }
    public void setCompletionTimeSeconds(long v) { this.completionTimeSeconds = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
