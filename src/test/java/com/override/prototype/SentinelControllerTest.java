package com.override.prototype;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentinelControllerTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void followsPatrolSuspiciousChaseSearchReturnPatrolCycle() {
        SentinelController controller = new SentinelController();

        SentinelController.Output suspicious = controller.update(input(0.25, true, false, false, false));
        assertTrue(suspicious.entered(SentinelController.State.SUSPICIOUS));
        assertEquals(1.0 / 3.0, suspicious.detectionProgress(), EPSILON);
        assertFalse(suspicious.captureArmed());

        SentinelController.Output chase = controller.update(input(0.50, true, false, false, false));
        assertTrue(chase.entered(SentinelController.State.CHASE));
        assertEquals(1.0, chase.detectionProgress(), EPSILON);
        assertTrue(chase.captureArmed());

        SentinelController.Output search = controller.update(input(0.10, true, true, false, false));
        assertTrue(search.entered(SentinelController.State.SEARCH));
        assertEquals(SentinelController.SEARCH_SECONDS, search.searchSecondsRemaining(), EPSILON);
        assertFalse(search.captureArmed());

        SentinelController.Output searching = controller.update(input(3.5, false, false, false, false));
        assertEquals(SentinelController.State.SEARCH, searching.state());
        assertEquals(0.5, searching.searchSecondsRemaining(), EPSILON);

        SentinelController.Output returning = controller.update(input(0.5, false, false, false, false));
        assertTrue(returning.entered(SentinelController.State.RETURN));

        SentinelController.Output patrol = controller.update(input(0.0, false, false, true, false));
        assertTrue(patrol.entered(SentinelController.State.PATROL));
    }

    @Test
    void searchReacquiresAVisiblePlayerAndLostSuspicionReturnsToRoute() {
        SentinelController controller = new SentinelController();
        controller.update(input(0.75, true, false, false, false));
        controller.update(input(0.0, false, false, false, false));

        SentinelController.Output reacquired = controller.update(input(0.1, true, false, false, false));
        assertTrue(reacquired.entered(SentinelController.State.CHASE));
        assertTrue(reacquired.captureArmed());

        controller.reset(false);
        controller.update(input(0.25, true, false, false, false));
        SentinelController.Output lost = controller.update(input(0.1, false, false, false, false));
        assertTrue(lost.entered(SentinelController.State.RETURN));
        assertEquals(0.0, lost.detectionProgress(), EPSILON);
    }

    @Test
    void lockdownMultipliesMovementSpeedInEveryMovingState() {
        SentinelController controller = new SentinelController();
        double patrolLockdownSpeed = SentinelController.PATROL_SPEED_PIXELS_PER_SECOND
                * SentinelController.LOCKDOWN_SPEED_MULTIPLIER;
        double chaseLockdownSpeed = SentinelController.CHASE_SPEED_PIXELS_PER_SECOND
                * SentinelController.LOCKDOWN_SPEED_MULTIPLIER;

        SentinelController.Output patrol = controller.update(input(0.0, false, false, false, true));
        assertEquals(patrolLockdownSpeed, patrol.movementSpeed(), EPSILON);

        SentinelController.Output chase = controller.update(input(0.75, true, false, false, true));
        assertEquals(SentinelController.State.CHASE, chase.state());
        assertEquals(chaseLockdownSpeed, chase.movementSpeed(), EPSILON);

        SentinelController.Output search = controller.update(input(0.0, false, true, false, true));
        assertEquals(SentinelController.State.SEARCH, search.state());
        assertEquals(patrolLockdownSpeed, search.movementSpeed(), EPSILON);

        SentinelController.Output reset = controller.reset(true);
        assertEquals(SentinelController.State.PATROL, reset.state());
        assertEquals(patrolLockdownSpeed, reset.movementSpeed(), EPSILON);
    }

    private static SentinelController.Input input(
            double elapsed,
            boolean visible,
            boolean enteredRoom,
            boolean routeReached,
            boolean lockdown
    ) {
        return new SentinelController.Input(
                elapsed,
                visible,
                enteredRoom,
                routeReached,
                lockdown
        );
    }
}
