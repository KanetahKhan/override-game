package com.override.game.minigames;

import javafx.scene.paint.Color;

/**
 * Colour palette for the mini-game framework.
 *
 * <p>Modeled on the old monochrome Nokia / Game Boy LCD look: a near-black
 * phosphor background with a bright green foreground, plus a handful of accent
 * colours so individual games can light things up (combos, danger, EMP juice).
 *
 * <p>Two factories are provided:
 * <ul>
 *   <li>{@link #nokia()} — the classic green LCD palette.</li>
 *   <li>{@link #chapter(Color)} — the same palette but with a custom accent so
 *       each chapter can tint its signature mini-game.</li>
 * </ul>
 *
 * Instances are immutable.
 */
public final class MiniGameTheme {

    private final Color background;
    private final Color foreground;
    private final Color dim;
    private final Color accent;
    private final Color danger;
    private final Color warning;
    private final Color glow;

    public MiniGameTheme(Color background, Color foreground, Color dim,
                         Color accent, Color danger, Color warning, Color glow) {
        this.background = background;
        this.foreground = foreground;
        this.dim = dim;
        this.accent = accent;
        this.danger = danger;
        this.warning = warning;
        this.glow = glow;
    }

    /** The classic green-on-black LCD palette. */
    public static MiniGameTheme nokia() {
        return new MiniGameTheme(
            Color.web("#0b120b"),   // background — very dark phosphor green/black
            Color.web("#8bdc6a"),   // foreground — LCD green
            Color.web("#274a23"),   // dim — faded grid / inactive pixels
            Color.web("#9bff66"),   // accent — bright lime (combos / EMP)
            Color.web("#ff5d5d"),   // danger — error red
            Color.web("#ffd166"),   // warning — amber
            Color.web("#eaffe0")    // glow — near-white highlight
        );
    }

    /** The LCD palette with a custom accent colour, for per-chapter tinting. */
    public static MiniGameTheme chapter(Color accent) {
        MiniGameTheme base = nokia();
        return new MiniGameTheme(
            base.background, base.foreground, base.dim,
            accent, base.danger, base.warning, base.glow
        );
    }

    public Color background() { return background; }
    public Color foreground() { return foreground; }
    public Color dim()        { return dim; }
    public Color accent()     { return accent; }
    public Color danger()     { return danger; }
    public Color warning()    { return warning; }
    public Color glow()       { return glow; }
}
