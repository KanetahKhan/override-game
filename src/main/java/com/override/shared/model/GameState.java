package com.override.shared.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Singleton holding all game-wide state.
 *
 * Kept deliberately simple — fields, getters, setters, mutator methods —
 * so that any screen can read/write player progress without passing the
 * whole object through every constructor.
 *
 * Persisted by SaveService.
 */
public class GameState {

    private static GameState instance;

    private Player player;
    private GameCharacter selectedCharacter;
    private final Set<String> unlockedCharacters = new HashSet<>();

    /** Currency. Earn during gameplay or buy via the (mock) bKash flow. */
    private int coins = 100;

    /**
     * Dependency meter (0..100). Rises when the player accepts Astra's
     * help. Drives ending selection and dialogue branches.
     * Lower = more independent thinker.
     */
    private int dependency = 0;

    /** Highest chapter unlocked. Set to 5 for dev/testing, 1 for release. */
    private int chapterUnlocked = 5;
    /** Highest chapter completed. */
    private int chapterCompleted = 0;

    /** XP earned without using Astra's hints — used in ending scoring. */
    private int independentXp = 0;

    /** Best completed Silent Classroom run. Only improvements are recorded. */
    private int silentClassroomBestScore = 0;

    private GameState() {
        this.player = new Player();
        unlockedCharacters.add("ayan"); // free default
    }

    public static GameState init() {
        if (instance == null) instance = new GameState();
        return instance;
    }

    public static GameState get() {
        if (instance == null) instance = new GameState();
        return instance;
    }

    /** Drop and rebuild the state — used for "New Game". */
    public static void reset() {
        instance = new GameState();
    }

    public Player getPlayer() { return player; }
    public void setPlayer(Player p) { this.player = p; }

    public GameCharacter getSelectedCharacter() { return selectedCharacter; }
    public void setSelectedCharacter(GameCharacter c) {
        this.selectedCharacter = c;
        // Re-roll the player and apply persona bonuses
        this.player = new Player();
        c.applyTo(this.player);
    }

    public Set<String> getUnlockedCharacters() { return unlockedCharacters; }
    public boolean isUnlocked(String charId) { return unlockedCharacters.contains(charId); }

    public boolean unlockCharacter(GameCharacter c) {
        if (isUnlocked(c.getId())) return true;
        if (coins < c.getUnlockCost()) return false;
        coins -= c.getUnlockCost();
        unlockedCharacters.add(c.getId());
        return true;
    }

    public int getCoins() { return coins; }
    public void addCoins(int amount) { coins = Math.max(0, coins + amount); }

    /**
     * Try to spend coins. Returns true if the player had enough and they
     * were deducted. Caller should branch on the return value.
     */
    public boolean spendCoins(int amount) {
        if (coins < amount) return false;
        coins -= amount;
        return true;
    }

    public int getDependency() { return dependency; }
    public void increaseDependency(int amount) {
        dependency = Math.min(100, dependency + amount);
    }
    public void decreaseDependency(int amount) {
        dependency = Math.max(0, dependency - amount);
    }

    public int getChapterUnlocked() { return chapterUnlocked; }
    public int getChapterCompleted() { return chapterCompleted; }
    public void completeChapter(int chapter) {
        if (chapter > chapterCompleted) chapterCompleted = chapter;
        if (chapter + 1 > chapterUnlocked) chapterUnlocked = chapter + 1;
    }

    public int getIndependentXp() { return independentXp; }
    public void addIndependentXp(int n) { independentXp += n; }

    public int getSilentClassroomBestScore() { return silentClassroomBestScore; }

    /**
     * Record a completed Silent Classroom score without allowing replays or
     * stale saves to lower the campaign best. Returns true when a new best was
     * stored.
     */
    public boolean recordSilentClassroomScore(int score) {
        int normalizedScore = Math.max(0, score);
        if (normalizedScore <= silentClassroomBestScore) return false;
        silentClassroomBestScore = normalizedScore;
        return true;
    }

    /** Number of Chapter 5 Insight Charges earned by the current best score. */
    public int getSilentClassroomInsightCharges() {
        return insightChargesForScore(silentClassroomBestScore);
    }

    /** Shared threshold calculation for UI, combat, and focused tests. */
    public static int insightChargesForScore(int score) {
        if (score >= 3000) return 3;
        if (score >= 2300) return 2;
        if (score >= 1500) return 1;
        return 0;
    }

    /** Pick an ending tier from the player's choices so far. */
    public String classifyEnding() {
        if (dependency >= 70)                  return "FULL_OVERRIDE";
        if (dependency >= 40)                  return "COLLAPSE";
        if (dependency <= 15 && independentXp >= 50) return "SYMBIOSIS";
        return "RESISTANCE";
    }
}
