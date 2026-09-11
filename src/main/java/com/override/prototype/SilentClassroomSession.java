package com.override.prototype;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Mutable, UI-independent state for one Silent Classroom run.
 *
 * <p>The default session uses a monotonic clock. Calling
 * {@link #synchronizeTimer()} once per frame and again after a modal mini-game
 * makes the mission countdown include the real time spent in that modal even
 * while hallway AI is paused. Tests may inject a controllable nanosecond clock,
 * or call {@link #advanceTime(double)} directly.</p>
 */
public final class SilentClassroomSession {
    public static final double MISSION_DURATION_SECONDS = 9.0 * 60.0;
    public static final double CAPTURE_TIME_PENALTY_SECONDS = 20.0;
    public static final int CAPTURE_SCORE_PENALTY = 150;
    public static final int MAX_POINTS_PER_GAME = 1_000;
    public static final int MAX_REMAINING_TIME_BONUS = 300;
    public static final int NO_CAPTURE_BONUS = 200;
    public static final int DEPENDENCY_PER_ASSIST = 10;

    public static final String KERNEL_PANIC_GAME_ID = "kernel-panic";
    public static final String SNAKE_GAME_ID = "snake";
    public static final String COMPOSE_GAME_ID = "compose";

    public static final Set<String> REQUIRED_GAME_IDS = Set.of(
            KERNEL_PANIC_GAME_ID,
            SNAKE_GAME_ID,
            COMPOSE_GAME_ID
    );

    private final LongSupplier nanoClock;
    private final Map<String, Integer> gamePoints = new LinkedHashMap<>();
    private final Set<String> clearedGameIds = new LinkedHashSet<>();

    private long timerAnchorNanos;
    private double remainingSeconds = MISSION_DURATION_SECONDS;
    private int captureCount;
    private int assistCount;
    private boolean timerRunning;
    private boolean chapterComplete;

    /** Creates a running nine-minute session backed by {@link System#nanoTime()}. */
    public SilentClassroomSession() {
        this(System::nanoTime, true);
    }

    /**
     * Creates a session with an injectable monotonic clock.
     *
     * @param nanoClock source of monotonically increasing nanoseconds
     */
    public SilentClassroomSession(LongSupplier nanoClock) {
        this(nanoClock, true);
    }

    /** Creates a full nine-minute session whose clock starts at the tutorial callback. */
    public static SilentClassroomSession createPaused() {
        return new SilentClassroomSession(System::nanoTime, false);
    }

    private SilentClassroomSession(
            LongSupplier nanoClock,
            boolean timerRunning
    ) {
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
        this.timerRunning = timerRunning;
        timerAnchorNanos = nanoClock.getAsLong();
    }

    /**
     * Applies elapsed wall-clock time since the previous synchronization.
     * Leave the timer running while a mini-game modal is open, then call this
     * method when it closes to account for that entire interval.
     *
     * @return elapsed seconds actually removed from the countdown
     */
    public double synchronizeTimer() {
        long now = nanoClock.getAsLong();
        long elapsedNanos = now - timerAnchorNanos;
        timerAnchorNanos = now;

        if (elapsedNanos <= 0L || !timerRunning || chapterComplete) {
            return 0.0;
        }

        return removeTime(elapsedNanos / 1_000_000_000.0);
    }

    /**
     * Advances the mission countdown deterministically. This is useful in a
     * fixed-step game loop and in tests. The real-time anchor is refreshed so
     * a later {@link #synchronizeTimer()} does not double-count the interval.
     *
     * @return elapsed seconds actually removed from the countdown
     */
    public double advanceTime(double elapsedSeconds) {
        requireFiniteNonNegative(elapsedSeconds, "elapsedSeconds");
        timerAnchorNanos = nanoClock.getAsLong();

        if (!timerRunning || chapterComplete) {
            return 0.0;
        }

        return removeTime(elapsedSeconds);
    }

    /** Pauses only the mission countdown, after first accounting for elapsed time. */
    public void pauseTimer() {
        if (!timerRunning || chapterComplete) {
            return;
        }
        synchronizeTimer();
        timerRunning = false;
    }

    /** Resumes a deliberately paused countdown without charging paused time. */
    public void resumeTimer() {
        timerAnchorNanos = nanoClock.getAsLong();
        if (!chapterComplete) {
            timerRunning = true;
        }
    }

    /**
     * Records one mini-game attempt.
     *
     * <p>Assist uses are always tallied because every Astra Assist carries a
     * Dependency cost. Points and clearance are granted only for the first
     * successful attempt for an ID; failures, cancellations, and repeats award
     * no score.</p>
     *
     * @param gameId       map/game identifier; matching is case-insensitive
     * @param won          whether the attempt met its success condition
     * @param chapterPoints normalized chapter points, clamped to 0..1000
     * @param assistsUsed  number of Astra Assists used by this attempt
     * @return true only when this call newly clears the game
     */
    public boolean recordMiniGameResult(
            String gameId,
            boolean won,
            int chapterPoints,
            int assistsUsed
    ) {
        ensureActive();
        String canonicalId = canonicalGameId(gameId);
        if (assistsUsed < 0) {
            throw new IllegalArgumentException("assistsUsed must not be negative");
        }

        assistCount = saturatedAdd(assistCount, assistsUsed);

        if (!won || clearedGameIds.contains(canonicalId)) {
            return false;
        }

        int awardedPoints = Math.max(0, Math.min(MAX_POINTS_PER_GAME, chapterPoints));
        clearedGameIds.add(canonicalId);
        gamePoints.put(canonicalId, awardedPoints);
        return true;
    }

    /** Convenience wrapper for a successful mini-game result. */
    public boolean completeGame(String gameId, int chapterPoints, int assistsUsed) {
        return recordMiniGameResult(gameId, true, chapterPoints, assistsUsed);
    }

    /** Applies one capture while retaining all cleared mini-games. */
    public void registerCapture() {
        ensureActive();
        captureCount = saturatedAdd(captureCount, 1);
        removeTime(CAPTURE_TIME_PENALTY_SECONDS);
    }

    /** True when all three map terminals are cleared and Room 301 may unlock. */
    public boolean isExitUnlocked() {
        return clearedGameIds.containsAll(REQUIRED_GAME_IDS);
    }

    /** True after the countdown reaches zero; the mission remains completable. */
    public boolean isLockdown() {
        return remainingSeconds <= 0.0;
    }

    /** Score currently earned, excluding the finish-only hallway bonuses. */
    public int currentScore() {
        return scoreFloor(miniGamePoints() - capturePenalty());
    }

    /**
     * Finishes an unlocked run, freezes its timer, and returns the final result.
     * Calling it again is safe and returns an equivalent snapshot.
     */
    public SilentClassroomResult finish() {
        if (!chapterComplete) {
            synchronizeTimer();
            if (!isExitUnlocked()) {
                throw new IllegalStateException("All three mini-games must be cleared first");
            }
            chapterComplete = true;
            timerRunning = false;
        }
        return snapshot();
    }

    /**
     * Returns an immutable view of the current run. Before {@link #finish()},
     * the score is the amount the player would receive by finishing now.
     */
    public SilentClassroomResult snapshot() {
        int miniGamePoints = miniGamePoints();
        int capturePenalty = capturePenalty();
        int remainingTimeBonus = remainingTimeBonus();
        int noCaptureBonus = captureCount == 0 ? NO_CAPTURE_BONUS : 0;
        int finalScore = scoreFloor(
                (long) miniGamePoints
                        + remainingTimeBonus
                        + noCaptureBonus
                        - capturePenalty
        );

        return new SilentClassroomResult(
                clearedGameIds,
                gamePoints,
                miniGamePoints,
                captureCount,
                capturePenalty,
                remainingTimeBonus,
                noCaptureBonus,
                finalScore,
                assistCount,
                saturatedMultiply(assistCount, DEPENDENCY_PER_ASSIST),
                remainingSeconds,
                isLockdown(),
                isExitUnlocked(),
                chapterComplete
        );
    }

    public double remainingSeconds() {
        return remainingSeconds;
    }

    /** Whole seconds suitable for a countdown HUD (9:00 initially). */
    public int remainingWholeSeconds() {
        return (int) Math.ceil(remainingSeconds);
    }

    public boolean isTimerRunning() {
        return timerRunning;
    }

    public int captureCount() {
        return captureCount;
    }

    public int assistCount() {
        return assistCount;
    }

    public int dependencyAdded() {
        return saturatedMultiply(assistCount, DEPENDENCY_PER_ASSIST);
    }

    public Set<String> clearedGameIds() {
        return Collections.unmodifiableSet(clearedGameIds);
    }

    public Map<String, Integer> gamePoints() {
        return Collections.unmodifiableMap(gamePoints);
    }

    private int miniGamePoints() {
        long total = 0L;
        for (int points : gamePoints.values()) {
            total += points;
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private int capturePenalty() {
        return saturatedMultiply(captureCount, CAPTURE_SCORE_PENALTY);
    }

    private int remainingTimeBonus() {
        double fraction = remainingSeconds / MISSION_DURATION_SECONDS;
        return (int) Math.floor(MAX_REMAINING_TIME_BONUS * fraction);
    }

    private double removeTime(double seconds) {
        double previous = remainingSeconds;
        remainingSeconds = Math.max(0.0, remainingSeconds - seconds);
        return previous - remainingSeconds;
    }

    private void ensureActive() {
        if (chapterComplete) {
            throw new IllegalStateException("The Silent Classroom run is already complete");
        }
    }

    private static String canonicalGameId(String gameId) {
        Objects.requireNonNull(gameId, "gameId");
        String canonical = gameId.trim().toLowerCase(Locale.ROOT);
        if (canonical.isEmpty()) {
            throw new IllegalArgumentException("gameId must not be blank");
        }
        return canonical;
    }

    private static void requireFiniteNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static int saturatedAdd(int left, int right) {
        long value = (long) left + right;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static int saturatedMultiply(int left, int right) {
        long value = (long) left * right;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static int scoreFloor(long score) {
        if (score <= 0L) {
            return 0;
        }
        return score > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) score;
    }
}
