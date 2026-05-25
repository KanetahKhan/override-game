package com.override.shared.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Selectable main characters. Default Ayan is free, the others must be
 * unlocked with coins (earned in-game or purchased via the mock bKash flow).
 *
 * Each character offers small stat tweaks so the choice feels meaningful
 * without breaking the story (you still play Ayan's story; these are
 * "personas" that change his starting strengths).
 */
public class GameCharacter {

    private final String id;
    private final String name;
    private final String role;
    private final String tagline;
    private final int unlockCost;          // coins required, 0 if default
    private final int bonusLogic;
    private final int bonusAwareness;
    private final int bonusWillpower;
    private final int bonusCombat;
    private final int bonusEmpathy;

    public GameCharacter(String id, String name, String role, String tagline,
                         int unlockCost,
                         int logic, int awareness, int willpower,
                         int combat, int empathy) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.tagline = tagline;
        this.unlockCost = unlockCost;
        this.bonusLogic = logic;
        this.bonusAwareness = awareness;
        this.bonusWillpower = willpower;
        this.bonusCombat = combat;
        this.bonusEmpathy = empathy;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getTagline() { return tagline; }
    public int getUnlockCost() { return unlockCost; }
    public int getBonusLogic() { return bonusLogic; }
    public int getBonusAwareness() { return bonusAwareness; }
    public int getBonusWillpower() { return bonusWillpower; }
    public int getBonusCombat() { return bonusCombat; }
    public int getBonusEmpathy() { return bonusEmpathy; }

    public boolean isFree() { return unlockCost == 0; }

    /** Apply this character's bonuses on top of a fresh Player. */
    public void applyTo(Player p) {
        p.setDisplayName(name);
        p.buffLogic(bonusLogic);
        p.buffAwareness(bonusAwareness);
        p.buffWillpower(bonusWillpower);
        p.buffCombat(bonusCombat);
        p.buffEmpathy(bonusEmpathy);
    }

    /** Hardcoded roster — five personas mapped to the design doc allies. */
    public static List<GameCharacter> roster() {
        List<GameCharacter> r = new ArrayList<>();
        r.add(new GameCharacter("ayan",     "Ayan",            "The Awakening Student",
            "A final-year CSE student. Dependent — but waking up.",
            0,   2, 2, 1, 1, 2));
        r.add(new GameCharacter("riya",     "Riya",            "The Hacker Sister",
            "Self-taught programmer. Trades empathy for raw logic.",
            300, 4, 2, 1, 2, 0));
        r.add(new GameCharacter("kabir",    "Dr. Kabir",       "The Defiant Doctor",
            "Refused to follow Astra's triage orders. Heals harder.",
            500, 1, 3, 3, 0, 4));
        r.add(new GameCharacter("akhter",   "Prof. Akhter",    "The Hidden Teacher",
            "Teaches without AI. Boosts willpower against Astra.",
            800, 3, 2, 4, 0, 3));
        r.add(new GameCharacter("nayeem",   "Nayeem",          "The Last Coder",
            "Remembers how to code without prompts. Pure problem-solver.",
            1200, 5, 3, 2, 2, 1));
        return r;
    }
}
