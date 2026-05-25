package com.override.shared.model;

/**
 * The player's living stats. Stats represent recovered human strength,
 * not fantasy powers — see Chapter system in the design doc.
 */
public class Player {

    private String displayName = "Ayan";
    private int level = 1;
    private int xp = 0;
    private int hp = 100;
    private int maxHp = 100;

    // Five core stats from the design
    private int logic = 5;
    private int awareness = 5;
    private int willpower = 5;
    private int combat = 5;
    private int empathy = 5;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getLevel() { return level; }
    public int getXp() { return xp; }

    /** Award XP, level up at every 100 XP threshold. */
    public void addXp(int amount) {
        xp += amount;
        while (xp >= level * 100) {
            xp -= level * 100;
            level++;
            maxHp += 10;
            hp = maxHp; // full heal on level up
        }
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public void damage(int amount) { hp = Math.max(0, hp - amount); }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public boolean isAlive() { return hp > 0; }
    public void fullHeal() { hp = maxHp; }

    public int getLogic() { return logic; }
    public int getAwareness() { return awareness; }
    public int getWillpower() { return willpower; }
    public int getCombat() { return combat; }
    public int getEmpathy() { return empathy; }

    public void buffLogic(int n)     { logic += n; }
    public void buffAwareness(int n) { awareness += n; }
    public void buffWillpower(int n) { willpower += n; }
    public void buffCombat(int n)    { combat += n; }
    public void buffEmpathy(int n)   { empathy += n; }
}
