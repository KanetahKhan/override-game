package com.override.game.minigames;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiniGameResultTest {

    @Test
    void constructorNormalizesAssistCountAndChapterPointBoundaries() {
        MiniGameResult belowMinimum = new MiniGameResult(true, -50, -20, -3, -1);
        MiniGameResult aboveMaximum = new MiniGameResult(true, 5_000, 250, 4, 1_001);

        assertEquals(0, belowMinimum.dependencyUsed());
        assertEquals(0, belowMinimum.chapterPoints());
        assertEquals(-50, belowMinimum.score());
        assertEquals(-20, belowMinimum.xpEarned());

        assertEquals(4, aboveMaximum.dependencyUsed());
        assertEquals(1_000, aboveMaximum.chapterPoints());
        assertEquals(5_000, aboveMaximum.score());
        assertEquals(250, aboveMaximum.xpEarned());
    }

    @Test
    void chapterPointCalculationClampsInputsAndRespectsAssistPenalty() {
        assertEquals(0, MiniGameResult.calculateChapterPoints(false, 1.0, 1.0, 0));
        assertEquals(1_000, MiniGameResult.calculateChapterPoints(true, 1.0, 1.0, 0));
        assertEquals(900, MiniGameResult.calculateChapterPoints(true, 1.0, 1.0, 1));
        assertEquals(600, MiniGameResult.calculateChapterPoints(true, -1.0, -1.0, 0));
        assertEquals(1_000, MiniGameResult.calculateChapterPoints(true, 2.0, 2.0, 0));
        assertEquals(800, MiniGameResult.calculateChapterPoints(true, 0.5, 0.5, 0));
        assertEquals(
                500,
                MiniGameResult.calculateChapterPoints(
                        true,
                        Double.NaN,
                        Double.POSITIVE_INFINITY,
                        1
                )
        );
    }

    @Test
    void legacyConstructorDefaultsNormalizedChapterPointsToZero() {
        MiniGameResult result = new MiniGameResult(true, 300, 25, -1);

        assertEquals(0, result.dependencyUsed());
        assertEquals(0, result.chapterPoints());
    }
}
