package com.override.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateSilentClassroomTest {

    @BeforeEach
    void resetState() {
        GameState.reset();
    }

    @Test
    void bestScoreOnlyImprovesAndDrivesInsightCharges() {
        GameState state = GameState.get();

        assertFalse(state.recordSilentClassroomScore(-1));
        assertEquals(0, state.getSilentClassroomBestScore());

        assertTrue(state.recordSilentClassroomScore(1_500));
        assertEquals(1, state.getSilentClassroomInsightCharges());
        assertFalse(state.recordSilentClassroomScore(1_500));
        assertFalse(state.recordSilentClassroomScore(1_499));
        assertEquals(1_500, state.getSilentClassroomBestScore());

        assertTrue(state.recordSilentClassroomScore(2_300));
        assertEquals(2, state.getSilentClassroomInsightCharges());
        assertTrue(state.recordSilentClassroomScore(3_000));
        assertEquals(3, state.getSilentClassroomInsightCharges());
    }

    @Test
    void insightChargeThresholdsIncludeTheirExactBoundaries() {
        int[][] cases = {
                {-1, 0},
                {0, 0},
                {1_499, 0},
                {1_500, 1},
                {2_299, 1},
                {2_300, 2},
                {2_999, 2},
                {3_000, 3},
                {Integer.MAX_VALUE, 3}
        };

        for (int[] testCase : cases) {
            assertEquals(
                    testCase[1],
                    GameState.insightChargesForScore(testCase[0]),
                    "score=" + testCase[0]
            );
        }
    }
}
