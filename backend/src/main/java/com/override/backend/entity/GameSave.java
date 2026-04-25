package com.override.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_saves")
public class GameSave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "chapter_number")
    private int chapterNumber;

    private String checkpoint;

    @Column(name = "player_hp")
    private int playerHp = 100;

    @Column(name = "dependency_meter")
    private int dependencyMeter = 0;

    private int coins = 100;

    @Column(name = "choices_json", columnDefinition = "TEXT")
    private String choicesJson;

    @Column(name = "unlocked_skills_json", columnDefinition = "TEXT")
    private String unlockedSkillsJson;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public GameSave() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlayerProfile getPlayer() { return player; }
    public void setPlayer(PlayerProfile player) { this.player = player; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getCheckpoint() { return checkpoint; }
    public void setCheckpoint(String checkpoint) { this.checkpoint = checkpoint; }

    public int getPlayerHp() { return playerHp; }
    public void setPlayerHp(int playerHp) { this.playerHp = playerHp; }

    public int getDependencyMeter() { return dependencyMeter; }
    public void setDependencyMeter(int v) { this.dependencyMeter = v; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public String getChoicesJson() { return choicesJson; }
    public void setChoicesJson(String choicesJson) { this.choicesJson = choicesJson; }

    public String getUnlockedSkillsJson() { return unlockedSkillsJson; }
    public void setUnlockedSkillsJson(String v) { this.unlockedSkillsJson = v; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
