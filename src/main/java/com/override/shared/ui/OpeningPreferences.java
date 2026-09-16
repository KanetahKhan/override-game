package com.override.shared.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/** Session-wide presentation setting; never changes gameplay or save data. */
final class OpeningPreferences {
    static final BooleanProperty REDUCED_MOTION = new SimpleBooleanProperty(false);
    private OpeningPreferences() { }
}
