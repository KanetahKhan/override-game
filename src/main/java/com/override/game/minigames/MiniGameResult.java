package com.override.game.minigames;

/**
 * Immutable outcome of a single mini-game run, handed back to the chapter that
 * launched it via a {@link ResultListener}.
 *
 * @param won            whether the run met the game's success condition
 * @param score          final score
 * @param xpEarned       XP to award the player (already net of any penalty)
 * @param dependencyUsed how many times the player leaned on an "Astra Assist";
 *                       the chapter decides how that affects campaign state
 * @param chapterPoints  normalized Silent Classroom reward score (0..1000)
 */
public record MiniGameResult(
        boolean won,
        int score,
        int xpEarned,
        int dependencyUsed,
        int chapterPoints
) {

    /**
     * Keep result objects safe at their boundary. Raw score and XP retain their
     * game-specific meanings; only counts and normalized chapter points are
     * constrained here.
     */
    public MiniGameResult {
        dependencyUsed = Math.max(0, dependencyUsed);
        chapterPoints = clamp(chapterPoints, 0, 1_000);
    }

    /** Backward-compatible constructor for non-mission callers. */
    public MiniGameResult(boolean won, int score, int xpEarned, int dependencyUsed) {
        this(won, score, xpEarned, dependencyUsed, 0);
    }

    /**
     * Shared Silent Classroom scoring rule. Failed runs always award zero.
     * Performance and speed are normalized fractions and safely clamped.
     */
    public static int calculateChapterPoints(
            boolean won,
            double performance,
            double speed,
            int assistCount
    ) {
        if (!won) return 0;

        int completionPoints = 500;
        int performancePoints = (int) Math.round(300 * clamp01(performance));
        int speedPoints = (int) Math.round(100 * clamp01(speed));
        int independencePoints = assistCount <= 0 ? 100 : 0;
        return clamp(completionPoints + performancePoints + speedPoints + independencePoints,
                0, 1_000);
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0;
        return Math.max(0, Math.min(1, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
