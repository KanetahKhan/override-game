package com.override.shared.ui;

import com.override.game.minigames.ChiptuneSfx;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

/** Session-wide presentation settings; never change gameplay or save data. */
final class OpeningPreferences {
    static final BooleanProperty REDUCED_MOTION = new SimpleBooleanProperty(false);

    /** Master sound level, 0..1, applied to every chiptune cue in the game. */
    static final DoubleProperty VOLUME = new SimpleDoubleProperty(ChiptuneSfx.getMasterVolume());

    static {
        VOLUME.addListener((o, was, now) -> ChiptuneSfx.setMasterVolume(now.doubleValue()));
    }

    private OpeningPreferences() { }
}
