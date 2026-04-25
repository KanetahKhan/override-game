package com.override.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_profiles")
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", length = 50)
    private String displayName = "Ayan";

    private int level = 1;
    private int xp = 0;

    @Column(name = "logic_stat")
    private int logicStat = 5;
    @Column(name = "awareness_stat")
    private int awarenessStat = 5;
    @Column(name = "willpower_stat")
    private int willpowerStat = 5;
    @Column(name = "combat_stat")
    private int combatStat = 5;
    @Column(name = "empathy_stat")
    private int empathyStat = 5;

    @Column(name = "dependency_meter")
    private int dependencyMeter = 0;

    @Column(name = "current_chapter")
    private int currentChapter = 1;

    private int coins = 100;

    @Column(name = "independent_xp")
    private int independentXp = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PlayerProfile() {}

    public PlayerProfile(User user) {
        this.user = user;
        this.displayName = user.getUsername();
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getLogicStat() { return logicStat; }
    public void setLogicStat(int v) { this.logicStat = v; }

    public int getAwarenessStat() { return awarenessStat; }
    public void setAwarenessStat(int v) { this.awarenessStat = v; }

    public int getWillpowerStat() { return willpowerStat; }
    public void setWillpowerStat(int v) { this.willpowerStat = v; }

    public int getCombatStat() { return combatStat; }
    public void setCombatStat(int v) { this.combatStat = v; }

    public int getEmpathyStat() { return empathyStat; }
    public void setEmpathyStat(int v) { this.empathyStat = v; }

    public int getDependencyMeter() { return dependencyMeter; }
    public void setDependencyMeter(int v) { this.dependencyMeter = v; }

    public int getCurrentChapter() { return currentChapter; }
    public void setCurrentChapter(int v) { this.currentChapter = v; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public int getIndependentXp() { return independentXp; }
    public void setIndependentXp(int v) { this.independentXp = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
