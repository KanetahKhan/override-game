package com.override.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persisted best run for Chapter 1's "Syntax Snake" mini-game.
 *
 * <p>This is a tiny single-row table: there is one global best score, plus the
 * timestamp of the run that produced it. The desktop client also caches the
 * best locally in {@code ~/.override/syntax-snake.properties} so the game-over
 * screen can show "BEST" instantly and works fully offline; the backend is the
 * canonical store and is synced best-effort.
 *
 * <p>See {@code com.override.backend.shared.service.SnakeHighScoreService} for
 * the load/submit logic — submits only ever raise the stored best.
 */
@Entity
@Table(name = "snake_high_scores")
public class SnakeHighScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "best_score", nullable = false)
    private int bestScore;

    @Column(name = "best_run_date")
    private LocalDateTime bestRunDate = LocalDateTime.now();

    public SnakeHighScore() {}

    public SnakeHighScore(int bestScore, LocalDateTime bestRunDate) {
        this.bestScore = bestScore;
        this.bestRunDate = bestRunDate;
    }

    public Long getId() { return id; }

    public int getBestScore() { return bestScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }

    public LocalDateTime getBestRunDate() { return bestRunDate; }
    public void setBestRunDate(LocalDateTime bestRunDate) { this.bestRunDate = bestRunDate; }
}
