package com.override.prototype;

import java.util.Map;
import java.util.Set;

/**
 * Immutable score and progress snapshot for a Silent Classroom run.
 *
 * <p>The result contains both the score breakdown used by the chapter summary
 * and the campaign-facing values (Dependency and Insight Charges). Collections
 * are defensive, immutable copies, so a result remains stable while a session
 * continues.</p>
 */
public record SilentClassroomResult(
        Set<String> clearedGameIds,
        Map<String, Integer> gamePoints,
        int miniGamePoints,
        int captureCount,
        int capturePenalty,
        int remainingTimeBonus,
        int noCaptureBonus,
        int finalScore,
        int assistCount,
        int dependencyAdded,
        double remainingSeconds,
        boolean lockdown,
        boolean allGamesCleared,
        boolean chapterComplete
) {
    public static final int FIRST_INSIGHT_THRESHOLD = 1_500;
    public static final int SECOND_INSIGHT_THRESHOLD = 2_300;
    public static final int THIRD_INSIGHT_THRESHOLD = 3_000;

    public SilentClassroomResult {
        clearedGameIds = Set.copyOf(clearedGameIds);
        gamePoints = Map.copyOf(gamePoints);
    }

    /** Returns the combined hallway bonus shown on the chapter summary. */
    public int hallwayBonus() {
        return remainingTimeBonus + noCaptureBonus;
    }

    /**
     * Returns the Chapter 5 reward represented by this score. Each charge is
     * worth one 25-damage hit and one skipped Astra turn at the boss layer.
     */
    public int insightCharges() {
        if (finalScore >= THIRD_INSIGHT_THRESHOLD) {
            return 3;
        }
        if (finalScore >= SECOND_INSIGHT_THRESHOLD) {
            return 2;
        }
        if (finalScore >= FIRST_INSIGHT_THRESHOLD) {
            return 1;
        }
        return 0;
    }
}
