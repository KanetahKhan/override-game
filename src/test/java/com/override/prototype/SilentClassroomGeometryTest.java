package com.override.prototype;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentClassroomGeometryTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void diagonalMovementIsNormalizedToTheSameSpeedAsCardinalMovement() {
        Point2D cardinal = SilentClassroomGeometry.normalizedDirection(1, 0);
        Point2D diagonal = SilentClassroomGeometry.normalizedDirection(1, 1);

        assertEquals(1.0, cardinal.magnitude(), EPSILON);
        assertEquals(1.0, diagonal.magnitude(), EPSILON);
        assertEquals(Math.sqrt(0.5), diagonal.getX(), EPSILON);
        assertEquals(Math.sqrt(0.5), diagonal.getY(), EPSILON);
        assertEquals(Point2D.ZERO, SilentClassroomGeometry.normalizedDirection(0, 0));
    }

    @Test
    void wallsAndClosedDoorsBlockSightWhileAnOpenRayRemainsVisible() {
        Point2D start = new Point2D(0, 0);
        Point2D target = new Point2D(300, 0);
        Point2D facing = new Point2D(1, 0);
        Rectangle2D wall = new Rectangle2D(140, -20, 20, 40);
        Rectangle2D closedDoor = new Rectangle2D(220, -20, 16, 40);

        assertTrue(canSee(start, target, point -> false));
        assertFalse(canSee(start, target, wall::contains));
        assertFalse(canSee(start, target, closedDoor::contains));
    }

    @Test
    void visionConeRejectsTargetsOutsideRangeOrFacing() {
        Point2D start = Point2D.ZERO;
        assertFalse(canSee(start, new Point2D(321, 0), point -> false));
        assertFalse(SilentClassroomGeometry.hasLineOfSight(
                start,
                new Point2D(-100, 0),
                new Point2D(1, 0),
                320,
                40,
                14,
                point -> false
        ));
    }

    private static boolean canSee(
            Point2D start,
            Point2D target,
            java.util.function.Predicate<Point2D> blocker
    ) {
        return SilentClassroomGeometry.hasLineOfSight(
                start,
                target,
                new Point2D(1, 0),
                320,
                40,
                14,
                blocker
        );
    }
}
