package com.override.prototype;

import com.override.game.minigames.ChiptuneSfx;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentClassroomPreferencesTest {

    @Test
    void primaryBindingsCanBeRemappedAndArrowMovementAlwaysRemainsAvailable() {
        SilentClassroomPreferences preferences = SilentClassroomPreferences.shared();
        KeyCode originalUp = preferences.upKey();
        KeyCode originalInteract = preferences.interactKey();
        KeyCode originalSprint = preferences.sprintKey();
        try {
            preferences.setUpKey(KeyCode.I);
            preferences.setInteractKey(KeyCode.F);
            preferences.setSprintKey(KeyCode.CONTROL);

            assertTrue(preferences.isUp(KeyCode.I));
            assertTrue(preferences.isUp(KeyCode.UP));
            assertFalse(preferences.isUp(KeyCode.W));
            assertEquals(KeyCode.F, preferences.interactKey());
            assertEquals(KeyCode.CONTROL, preferences.sprintKey());
        } finally {
            preferences.setUpKey(originalUp);
            preferences.setInteractKey(originalInteract);
            preferences.setSprintKey(originalSprint);
        }
    }

    @Test
    void volumeIsClampedAndFontScaleRejectsUnsupportedValues() {
        SilentClassroomPreferences preferences = SilentClassroomPreferences.shared();
        double originalVolume = preferences.masterVolume();
        double originalScale = preferences.fontScale();
        try {
            preferences.setMasterVolume(-4);
            assertEquals(0.0, preferences.masterVolume());
            preferences.setMasterVolume(8);
            assertEquals(1.0, preferences.masterVolume());
            preferences.setFontScale(1.5);
            assertEquals(1.5, preferences.fontScale());
            assertThrows(
                    IllegalArgumentException.class,
                    () -> preferences.setFontScale(1.1)
            );

            ChiptuneSfx.setMasterVolume(0.35);
            assertEquals(0.35, ChiptuneSfx.getMasterVolume());
        } finally {
            preferences.setMasterVolume(originalVolume);
            preferences.setFontScale(originalScale);
            ChiptuneSfx.setMasterVolume(originalVolume);
        }
    }
}
