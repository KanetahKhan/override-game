package com.override.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Best result for a single mini-game, keyed by {@code gameType} (one row per
 * game, e.g. "kernel-panic"). Tracks the best score, best combo and furthest
 * wave reached, plus whether the top-scoring run leaned on the Astra Assist.
 */
@Entity
@Table(name = "high_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = "game_type"))
public class HighScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_type", nullable = false, length = 50, unique = true)
    private String gameType;

    @Column(name = "best_score")
    private int bestScore;

    @Column(name = "best_combo")
    private int bestCombo;

    @Column(name = "best_wave")
    private int bestWave;

    @Column(name = "best_run_was_assisted")
    private boolean bestRunWasAssisted;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt = LocalDateTime.now();

    public HighScore() {}

    public HighScore(String gameType) {
        this.gameType = gameType;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }

    public int getBestScore() { return bestScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }

    public int getBestCombo() { return bestCombo; }
    public void setBestCombo(int bestCombo) { this.bestCombo = bestCombo; }

    public int getBestWave() { return bestWave; }
    public void setBestWave(int bestWave) { this.bestWave = bestWave; }

    public boolean isBestRunWasAssisted() { return bestRunWasAssisted; }
    public void setBestRunWasAssisted(boolean v) { this.bestRunWasAssisted = v; }

    public LocalDateTime getAchievedAt() { return achievedAt; }
    public void setAchievedAt(LocalDateTime achievedAt) { this.achievedAt = achievedAt; }
}
