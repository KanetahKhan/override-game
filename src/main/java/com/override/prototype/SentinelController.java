package com.override.prototype;

/**
 * Pure state machine for the Silent Classroom hallway Sentinel.
 *
 * <p>The application owns navigation, collision, field-of-view tests, and the
 * last known player position. It feeds those observations into
 * {@link #update(Input)} and uses the returned state, speed, and transition
 * flags to drive movement, animation, captions, and audio.</p>
 */
public final class SentinelController {
    public static final double VISION_RANGE_PIXELS = 320.0;
    public static final double DETECTION_SECONDS = 0.75;
    public static final double SEARCH_SECONDS = 4.0;
    public static final double PATROL_SPEED_PIXELS_PER_SECOND = 95.0;
    public static final double CHASE_SPEED_PIXELS_PER_SECOND = 180.0;
    public static final double LOCKDOWN_SPEED_MULTIPLIER = 1.15;

    public enum State {
        PATROL,
        SUSPICIOUS,
        CHASE,
        SEARCH,
        RETURN
    }

    /**
     * One frame of observations supplied by the hallway application.
     *
     * @param elapsedSeconds    frame duration
     * @param playerVisible     line of sight and range test result
     * @param playerEnteredRoom one-frame event used to begin a doorway search
     * @param patrolRouteReached whether RETURN has reached its patrol route
     * @param lockdown          whether the mission countdown has expired
     */
    public record Input(
            double elapsedSeconds,
            boolean playerVisible,
            boolean playerEnteredRoom,
            boolean patrolRouteReached,
            boolean lockdown
    ) {
        public Input {
            if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0.0) {
                throw new IllegalArgumentException(
                        "elapsedSeconds must be finite and non-negative"
                );
            }
        }
    }

    /**
     * Integration-facing decision for the current frame.
     *
     * @param previousState         state before the update
     * @param state                 state after the update
     * @param movementSpeed         recommended absolute movement speed
     * @param detectionProgress     normalized 0..1 detection meter
     * @param searchSecondsRemaining remaining doorway-search duration
     * @param captureArmed          whether contact may capture the player
     */
    public record Output(
            State previousState,
            State state,
            double movementSpeed,
            double detectionProgress,
            double searchSecondsRemaining,
            boolean captureArmed
    ) {
        public boolean stateChanged() {
            return previousState != state;
        }

        public boolean entered(State expected) {
            return stateChanged() && state == expected;
        }
    }

    private State state = State.PATROL;
    private double detectionElapsed;
    private double searchElapsed;

    public State state() {
        return state;
    }

    /** Applies one frame of perception and returns the resulting decision. */
    public Output update(Input input) {
        if (input == null) {
            throw new NullPointerException("input");
        }

        State previous = state;

        switch (state) {
            case PATROL -> updatePatrol(input);
            case SUSPICIOUS -> updateSuspicious(input);
            case CHASE -> updateChase(input);
            case SEARCH -> updateSearch(input);
            case RETURN -> updateReturn(input);
        }

        return output(previous, input.lockdown());
    }

    /** Resets the Sentinel after a capture or when starting a fresh run. */
    public Output reset(boolean lockdown) {
        State previous = state;
        state = State.PATROL;
        detectionElapsed = 0.0;
        searchElapsed = 0.0;
        return output(previous, lockdown);
    }

    public double detectionProgress() {
        if (state == State.CHASE) {
            return 1.0;
        }
        if (state != State.SUSPICIOUS) {
            return 0.0;
        }
        return Math.min(1.0, detectionElapsed / DETECTION_SECONDS);
    }

    public double searchSecondsRemaining() {
        if (state != State.SEARCH) {
            return 0.0;
        }
        return Math.max(0.0, SEARCH_SECONDS - searchElapsed);
    }

    private void updatePatrol(Input input) {
        detectionElapsed = 0.0;
        searchElapsed = 0.0;
        if (input.playerVisible()) {
            state = State.SUSPICIOUS;
            continueDetection(input.elapsedSeconds());
        }
    }

    private void updateSuspicious(Input input) {
        if (!input.playerVisible()) {
            state = State.RETURN;
            detectionElapsed = 0.0;
            return;
        }
        continueDetection(input.elapsedSeconds());
    }

    private void continueDetection(double elapsedSeconds) {
        detectionElapsed = Math.min(DETECTION_SECONDS, detectionElapsed + elapsedSeconds);
        if (detectionElapsed >= DETECTION_SECONDS) {
            state = State.CHASE;
        }
    }

    private void updateChase(Input input) {
        detectionElapsed = DETECTION_SECONDS;
        if (input.playerEnteredRoom() || !input.playerVisible()) {
            state = State.SEARCH;
            searchElapsed = 0.0;
        }
    }

    private void updateSearch(Input input) {
        detectionElapsed = 0.0;
        if (input.playerVisible() && !input.playerEnteredRoom()) {
            state = State.CHASE;
            detectionElapsed = DETECTION_SECONDS;
            searchElapsed = 0.0;
            return;
        }

        searchElapsed = Math.min(SEARCH_SECONDS, searchElapsed + input.elapsedSeconds());
        if (searchElapsed >= SEARCH_SECONDS) {
            state = State.RETURN;
            searchElapsed = 0.0;
        }
    }

    private void updateReturn(Input input) {
        detectionElapsed = 0.0;
        searchElapsed = 0.0;

        if (input.playerVisible()) {
            state = State.SUSPICIOUS;
            continueDetection(input.elapsedSeconds());
        } else if (input.patrolRouteReached()) {
            state = State.PATROL;
        }
    }

    private Output output(State previous, boolean lockdown) {
        return new Output(
                previous,
                state,
                movementSpeed(state, lockdown),
                detectionProgress(),
                searchSecondsRemaining(),
                state == State.CHASE
        );
    }

    private static double movementSpeed(State state, boolean lockdown) {
        double speed = switch (state) {
            case SUSPICIOUS -> 0.0;
            case CHASE -> CHASE_SPEED_PIXELS_PER_SECOND;
            case PATROL, SEARCH, RETURN -> PATROL_SPEED_PIXELS_PER_SECOND;
        };
        return lockdown ? speed * LOCKDOWN_SPEED_MULTIPLIER : speed;
    }
}
