package com.override.prototype;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentClassroomSessionTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void tutorialSessionStartsFullyPausedWithoutLosingATimeBonusPoint() {
        SilentClassroomSession session = SilentClassroomSession.createPaused();

        assertFalse(session.isTimerRunning());
        assertEquals(
                SilentClassroomSession.MISSION_DURATION_SECONDS,
                session.remainingSeconds(),
                EPSILON
        );
        assertEquals(300, session.snapshot().remainingTimeBonus());
        assertEquals(0.0, session.synchronizeTimer(), EPSILON);
    }

    @Test
    void timerUsesMonotonicElapsedTimeAndDoesNotChargeDeliberatePauses() {
        AtomicLong clock = new AtomicLong();
        SilentClassroomSession session = new SilentClassroomSession(clock::get);

        clock.addAndGet(5_500_000_000L);
        assertEquals(5.5, session.synchronizeTimer(), EPSILON);
        assertEquals(534.5, session.remainingSeconds(), EPSILON);

        clock.addAndGet(2_500_000_000L);
        session.pauseTimer();
        assertFalse(session.isTimerRunning());
        assertEquals(532.0, session.remainingSeconds(), EPSILON);

        clock.addAndGet(30_000_000_000L);
        assertEquals(0.0, session.synchronizeTimer(), EPSILON);
        assertEquals(532.0, session.remainingSeconds(), EPSILON);

        session.resumeTimer();
        clock.addAndGet(1_000_000_000L);
        assertEquals(1.0, session.synchronizeTimer(), EPSILON);
        assertEquals(531.0, session.remainingSeconds(), EPSILON);
    }

    @Test
    void onlyFirstSuccessfulClearAwardsPointsWhileEveryAssistIsCounted() {
        SilentClassroomSession session = new SilentClassroomSession(() -> 0L);

        assertFalse(session.recordMiniGameResult(" snake ", false, 900, 1));
        assertTrue(session.completeGame("SNAKE", 1_200, 2));
        assertFalse(session.completeGame("snake", 100, 3));

        SilentClassroomResult result = session.snapshot();
        assertEquals(1_000, result.miniGamePoints());
        assertEquals(1_000, result.gamePoints().get("snake"));
        assertEquals(1, result.clearedGameIds().size());
        assertEquals(6, result.assistCount());
        assertEquals(60, result.dependencyAdded());
        assertFalse(result.allGamesCleared());
    }

    @Test
    void captureKeepsClearsAndAppliesBothTimeAndScorePenalties() {
        SilentClassroomSession session = new SilentClassroomSession(() -> 0L);
        session.completeGame(SilentClassroomSession.COMPOSE_GAME_ID, 500, 0);
        session.advanceTime(10.0);

        session.registerCapture();

        SilentClassroomResult result = session.snapshot();
        assertTrue(result.clearedGameIds().contains(SilentClassroomSession.COMPOSE_GAME_ID));
        assertEquals(510.0, result.remainingSeconds(), EPSILON);
        assertEquals(1, result.captureCount());
        assertEquals(150, result.capturePenalty());
        assertEquals(0, result.noCaptureBonus());
        assertEquals(350, session.currentScore());
        assertEquals(633, result.finalScore());
    }

    @Test
    void lockdownStopsAtZeroButStillAllowsTheChapterToBeCompleted() {
        SilentClassroomSession session = new SilentClassroomSession(() -> 0L);

        assertEquals(540.0, session.advanceTime(600.0), EPSILON);
        assertEquals(0.0, session.advanceTime(10.0), EPSILON);
        assertTrue(session.isLockdown());
        assertEquals(0, session.remainingWholeSeconds());

        clearAllGames(session, 500);
        SilentClassroomResult result = session.finish();

        assertTrue(result.lockdown());
        assertTrue(result.allGamesCleared());
        assertTrue(result.chapterComplete());
        assertEquals(1_700, result.finalScore());
    }

    @Test
    void finishRequiresAllGamesAndFreezesTheFinalScoreBreakdown() {
        AtomicLong clock = new AtomicLong();
        SilentClassroomSession session = new SilentClassroomSession(clock::get);

        assertThrows(IllegalStateException.class, session::finish);

        session.completeGame(SilentClassroomSession.KERNEL_PANIC_GAME_ID, 1_000, 0);
        session.completeGame(SilentClassroomSession.SNAKE_GAME_ID, 900, 0);
        session.completeGame(SilentClassroomSession.COMPOSE_GAME_ID, 800, 0);
        session.advanceTime(54.0);

        SilentClassroomResult result = session.finish();
        assertEquals(2_700, result.miniGamePoints());
        assertEquals(270, result.remainingTimeBonus());
        assertEquals(200, result.noCaptureBonus());
        assertEquals(3_170, result.finalScore());
        assertEquals(3, result.insightCharges());
        assertTrue(result.chapterComplete());
        assertFalse(session.isTimerRunning());

        clock.addAndGet(60_000_000_000L);
        assertEquals(0.0, session.synchronizeTimer(), EPSILON);
        assertEquals(result, session.finish());
        assertThrows(
                IllegalStateException.class,
                () -> session.completeGame("another-game", 1_000, 0)
        );
    }

    @Test
    void requiredGamesUnlockTheExitInAnyOrderAndFailuresDoNotClearIt() {
        SilentClassroomSession session = new SilentClassroomSession(() -> 0L);

        assertFalse(session.recordMiniGameResult(
                SilentClassroomSession.KERNEL_PANIC_GAME_ID,
                false,
                1_000,
                0
        ));
        assertFalse(session.isExitUnlocked());
        assertTrue(session.completeGame(SilentClassroomSession.COMPOSE_GAME_ID, 700, 0));
        assertTrue(session.completeGame(SilentClassroomSession.SNAKE_GAME_ID, 800, 0));
        assertFalse(session.isExitUnlocked());
        assertTrue(session.completeGame(
                SilentClassroomSession.KERNEL_PANIC_GAME_ID,
                900,
                0
        ));
        assertTrue(session.isExitUnlocked());
    }

    private static void clearAllGames(SilentClassroomSession session, int pointsEach) {
        session.completeGame(SilentClassroomSession.KERNEL_PANIC_GAME_ID, pointsEach, 0);
        session.completeGame(SilentClassroomSession.SNAKE_GAME_ID, pointsEach, 0);
        session.completeGame(SilentClassroomSession.COMPOSE_GAME_ID, pointsEach, 0);
    }
}
