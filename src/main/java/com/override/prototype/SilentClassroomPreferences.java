package com.override.prototype;

import javafx.scene.input.KeyCode;

import java.util.List;
import java.util.Objects;

/**
 * Runtime accessibility and control preferences for Silent Classroom.
 *
 * <p>The shared instance survives chapter restarts for the lifetime of the
 * application. Arrow keys remain universal movement alternatives, while the
 * primary movement, interaction, and sprint bindings can be changed from the
 * pause menu. None of these options participate in scoring.</p>
 */
public final class SilentClassroomPreferences {

    public static final List<KeyCode> UP_KEYS =
            List.of(KeyCode.W, KeyCode.UP, KeyCode.I);
    public static final List<KeyCode> DOWN_KEYS =
            List.of(KeyCode.S, KeyCode.DOWN, KeyCode.K);
    public static final List<KeyCode> LEFT_KEYS =
            List.of(KeyCode.A, KeyCode.LEFT, KeyCode.J);
    public static final List<KeyCode> RIGHT_KEYS =
            List.of(KeyCode.D, KeyCode.RIGHT, KeyCode.L);
    public static final List<KeyCode> INTERACT_KEYS =
            List.of(KeyCode.E, KeyCode.F, KeyCode.SPACE, KeyCode.ENTER);
    public static final List<KeyCode> SPRINT_KEYS =
            List.of(KeyCode.SHIFT, KeyCode.CONTROL);
    public static final List<Double> FONT_SCALES =
            List.of(1.0, 1.25, 1.5);

    private static final SilentClassroomPreferences SHARED =
            new SilentClassroomPreferences();

    private KeyCode upKey = KeyCode.W;
    private KeyCode downKey = KeyCode.S;
    private KeyCode leftKey = KeyCode.A;
    private KeyCode rightKey = KeyCode.D;
    private KeyCode interactKey = KeyCode.E;
    private KeyCode sprintKey = KeyCode.SHIFT;
    private double masterVolume = 1.0;
    private double fontScale = 1.0;
    private boolean reducedFlashing;
    private boolean colorBlindSafe = true;

    private SilentClassroomPreferences() {
    }

    public static SilentClassroomPreferences shared() {
        return SHARED;
    }

    public KeyCode upKey() {
        return upKey;
    }

    public void setUpKey(KeyCode key) {
        upKey = requireChoice(key, UP_KEYS, "upKey");
    }

    public KeyCode downKey() {
        return downKey;
    }

    public void setDownKey(KeyCode key) {
        downKey = requireChoice(key, DOWN_KEYS, "downKey");
    }

    public KeyCode leftKey() {
        return leftKey;
    }

    public void setLeftKey(KeyCode key) {
        leftKey = requireChoice(key, LEFT_KEYS, "leftKey");
    }

    public KeyCode rightKey() {
        return rightKey;
    }

    public void setRightKey(KeyCode key) {
        rightKey = requireChoice(key, RIGHT_KEYS, "rightKey");
    }

    public KeyCode interactKey() {
        return interactKey;
    }

    public void setInteractKey(KeyCode key) {
        interactKey = requireChoice(key, INTERACT_KEYS, "interactKey");
    }

    public KeyCode sprintKey() {
        return sprintKey;
    }

    public void setSprintKey(KeyCode key) {
        sprintKey = requireChoice(key, SPRINT_KEYS, "sprintKey");
    }

    public double masterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(double volume) {
        if (!Double.isFinite(volume)) {
            throw new IllegalArgumentException("volume must be finite");
        }
        masterVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public double fontScale() {
        return fontScale;
    }

    public void setFontScale(double scale) {
        if (!FONT_SCALES.contains(scale)) {
            throw new IllegalArgumentException("Unsupported font scale: " + scale);
        }
        fontScale = scale;
    }

    public boolean reducedFlashing() {
        return reducedFlashing;
    }

    public void setReducedFlashing(boolean reducedFlashing) {
        this.reducedFlashing = reducedFlashing;
    }

    public boolean colorBlindSafe() {
        return colorBlindSafe;
    }

    public void setColorBlindSafe(boolean colorBlindSafe) {
        this.colorBlindSafe = colorBlindSafe;
    }

    public boolean isUp(KeyCode key) {
        return key == upKey || key == KeyCode.UP;
    }

    public boolean isDown(KeyCode key) {
        return key == downKey || key == KeyCode.DOWN;
    }

    public boolean isLeft(KeyCode key) {
        return key == leftKey || key == KeyCode.LEFT;
    }

    public boolean isRight(KeyCode key) {
        return key == rightKey || key == KeyCode.RIGHT;
    }

    public String movementSummary() {
        return display(upKey) + display(leftKey) + display(downKey) + display(rightKey)
                + " or arrow keys";
    }

    public static String display(KeyCode key) {
        return switch (key) {
            case CONTROL -> "Ctrl";
            case SHIFT -> "Shift";
            case SPACE -> "Space";
            case ENTER -> "Enter";
            default -> key.getName();
        };
    }

    private static KeyCode requireChoice(
            KeyCode key,
            List<KeyCode> choices,
            String name
    ) {
        Objects.requireNonNull(key, name);
        if (!choices.contains(key)) {
            throw new IllegalArgumentException("Unsupported " + name + ": " + key);
        }
        return key;
    }
}
