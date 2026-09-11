package com.override.chapter1;

import com.override.game.minigames.MiniGameResult;
import com.override.shared.model.GameState;

/**
 * Defers one-time campaign rewards until a run reaches Room 301.
 * Abandoned banks are simply discarded, preventing partial-run farming.
 */
final class SilentClassroomRewardBank {

    private final boolean eligible;
    private int xp;
    private int coins;
    private int independentXp;
    private boolean committed;

    SilentClassroomRewardBank(boolean eligible) {
        this.eligible = eligible;
    }

    void bank(MiniGameResult result, boolean firstClear) {
        if (!eligible || committed || !firstClear || !result.won()) {
            return;
        }
        xp += Math.max(0, result.xpEarned());
        coins += Math.max(5, result.chapterPoints() / 100);
        if (result.dependencyUsed() == 0) {
            independentXp += 15;
        }
    }

    /** Commits at most once, and never after Chapter 1 was already completed. */
    boolean commit(GameState state) {
        if (!eligible || committed || state.getChapterCompleted() >= 1) {
            return false;
        }
        state.getPlayer().addXp(xp);
        state.addCoins(coins);
        state.addIndependentXp(independentXp);
        committed = true;
        return true;
    }
}
