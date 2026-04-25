package com.override.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chapter_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "chapter_number"}))
public class ChapterProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "chapter_number", nullable = false)
    private int chapterNumber;

    private boolean completed = false;

    private int score = 0;

    @Column(name = "time_spent_seconds")
    private long timeSpentSeconds = 0;

    @Column(name = "ai_help_used")
    private int aiHelpUsed = 0;

    public ChapterProgress() {}

    public ChapterProgress(PlayerProfile player, int chapterNumber) {
        this.player = player;
        this.chapterNumber = chapterNumber;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public PlayerProfile getPlayer() { return player; }
    public void setPlayer(PlayerProfile player) { this.player = player; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int v) { this.chapterNumber = v; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public long getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(long v) { this.timeSpentSeconds = v; }

    public int getAiHelpUsed() { return aiHelpUsed; }
    public void setAiHelpUsed(int v) { this.aiHelpUsed = v; }
}
