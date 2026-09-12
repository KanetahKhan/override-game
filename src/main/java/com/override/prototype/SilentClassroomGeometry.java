package com.override.prototype;

import javafx.geometry.Point2D;

import java.util.Objects;
import java.util.function.Predicate;

/** Pure movement and sight helpers shared by the hallway and headless tests. */
public final class SilentClassroomGeometry {

    private SilentClassroomGeometry() {
    }

    /** Returns a unit movement vector, or {@link Point2D#ZERO} for no input. */
    public static Point2D normalizedDirection(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("movement input must be finite");
        }
        Point2D direction = new Point2D(x, y);
        return direction.magnitude() == 0 ? Point2D.ZERO : direction.normalize();
    }

    private static final String[] COMPASS_DIRECTIONS = {
        "east", "south-east", "south", "south-west",
        "west", "north-west", "north", "north-east"
    };

    /**
     * Snaps a movement vector to one of 8 compass directions (screen
     * space: +x east, +y south), matching Ayan's directional sprite
     * rotation filenames.
     */
    public static String compassDirection8(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("movement input must be finite");
        }
        if (x == 0 && y == 0) {
            return "south";
        }
        double angle = Math.toDegrees(Math.atan2(y, x));
        if (angle < 0) {
            angle += 360;
        }
        int index = ((int) Math.round(angle / 45.0)) % 8;
        return COMPASS_DIRECTIONS[index];
    }

    /**
     * Samples a cone ray against an arbitrary wall/closed-door predicate.
     * The start and target themselves are not treated as blockers.
     */
    public static boolean hasLineOfSight(
            Point2D start,
            Point2D target,
            Point2D facing,
            double maximumRange,
            double halfAngleDegrees,
            double sampleSpacing,
            Predicate<Point2D> blocksSight
    ) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(facing, "facing");
        Objects.requireNonNull(blocksSight, "blocksSight");
        if (!Double.isFinite(maximumRange) || maximumRange <= 0
                || !Double.isFinite(halfAngleDegrees)
                || halfAngleDegrees <= 0 || halfAngleDegrees >= 180
                || !Double.isFinite(sampleSpacing) || sampleSpacing <= 0) {
            throw new IllegalArgumentException("invalid sight geometry");
        }

        Point2D delta = target.subtract(start);
        double distance = delta.magnitude();
        if (distance <= 0 || distance > maximumRange || facing.magnitude() == 0) {
            return false;
        }

        Point2D direction = delta.normalize();
        double coneCosine = Math.cos(Math.toRadians(halfAngleDegrees));
        if (facing.normalize().dotProduct(direction) < coneCosine) {
            return false;
        }

        int steps = Math.max(1, (int) Math.ceil(distance / sampleSpacing));
        for (int i = 1; i < steps; i++) {
            Point2D sample = start.add(delta.multiply(i / (double) steps));
            if (blocksSight.test(sample)) {
                return false;
            }
        }
        return true;
    }
}
