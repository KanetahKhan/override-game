package com.override.game.minigames;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Base class for all Override mini-games.
 *
 * <p>A mini-game is a fixed-size pixel grid ({@code cols} × {@code rows} cells of
 * {@code cell} pixels each) drawn on a {@link Canvas}. {@link #start()} spins up a
 * 60fps {@link AnimationTimer}; each frame the subclass gets {@link #update(double)}
 * (with the elapsed seconds) followed by {@link #render()}. Key input arrives via
 * {@link #onKey(KeyCode)}.
 *
 * <p>When the run ends the subclass calls {@link #finish(boolean, int, int, int)}, which
 * stops the loop and reports a {@link MiniGameResult} (including {@link #dependencyUsed})
 * to the registered {@link ResultListener}.
 *
 * <p>Subclasses must implement {@link #init()}, {@link #update(double)},
 * {@link #render()} and {@link #onKey(KeyCode)}. They should not override the loop,
 * constructor contract, {@link #getView()} or the finish methods.
 */
public abstract class MiniGame {

    /** Grid dimensions. */
    protected final int cols;
    protected final int rows;
    protected final int cell;

    /** Pixel dimensions of the playfield (cols*cell × rows*cell). */
    protected final int width;
    protected final int height;

    protected final MiniGameTheme theme;

    protected final Canvas canvas;
    /** Shared graphics context — draw here from {@link #render()}. */
    protected final GraphicsContext g;
    private final StackPane view;

    /** Seconds elapsed since {@link #start()} — handy for timestamps/animation. */
    protected double time = 0;

    /**
     * Number of times the player chose an "Astra Assist" this run. Subclasses
     * increment it; it is reported back in the {@link MiniGameResult}.
     */
    protected int dependencyUsed = 0;

    private AnimationTimer timer;
    private long lastNanos = 0;
    private boolean finished = false;
    private ResultListener listener;

    protected MiniGame(int cols, int rows, int cell, MiniGameTheme theme) {
        this.cols = cols;
        this.rows = rows;
        this.cell = cell;
        this.theme = theme;
        this.width = cols * cell;
        this.height = rows * cell;

        this.canvas = new Canvas(width, height);
        this.g = canvas.getGraphicsContext2D();
        this.view = new StackPane(canvas);
        this.view.setStyle("-fx-background-color: black;");

        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(e -> { onKey(e.getCode()); e.consume(); });
    }

    // ----- Subclass hooks -----------------------------------------------------

    /** Called once before the loop starts. Set up game state here. */
    protected abstract void init();

    /** Advance simulation by {@code dt} seconds (clamped to avoid spikes). */
    protected abstract void update(double dt);

    /** Draw the current frame to {@link #g}. */
    protected abstract void render();

    /** Handle a key press. */
    protected abstract void onKey(KeyCode code);

    // ----- Lifecycle ----------------------------------------------------------

    /** Initialise and start the 60fps game loop. */
    public final void start() {
        if (finished || timer != null) return;
        time = 0;
        dependencyUsed = 0;
        init();
        if (finished) return;
        canvas.requestFocus();
        lastNanos = 0;
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNanos == 0) { lastNanos = now; }
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                if (dt > 0.05) dt = 0.05; // clamp huge frames (window drags, GC)
                time += dt;
                if (!finished) {
                    update(dt);
                    if (!finished) render();
                }
            }
        };
        timer.start();
    }

    /**
     * End the run: stop the loop and report the result. Safe to call once; later
     * calls are ignored.
     */
    protected final void finish(boolean won, int score, int xp, int chapterPoints) {
        if (finished) return;
        finished = true;
        if (timer != null) timer.stop();
        if (listener != null) {
            listener.onResult(new MiniGameResult(
                    won, score, xp, dependencyUsed, won ? chapterPoints : 0));
        }
    }

    /** Compatibility overload for arcade games that do not award chapter points. */
    protected final void finish(boolean won, int score, int xp) {
        finish(won, score, xp, 0);
    }

    /**
     * Abort the run from its owner (for example when the modal close button is
     * used). Cancellation is idempotent, stops the animation loop, and reports
     * a failed/no-reward result while preserving the assist count already used.
     */
    public final void cancel() {
        finish(false, 0, 0, 0);
    }

    /** The Canvas-backed view to drop into a Scene. */
    public final StackPane getView() {
        return view;
    }

    /** Register the callback fired when the run finishes or is cancelled. */
    public final void setResultListener(ResultListener listener) {
        this.listener = listener;
    }

    /** Forward a key press from an external owner (e.g. the Scene). */
    public final void dispatchKey(KeyCode code) {
        onKey(code);
    }

    protected final boolean isFinished() {
        return finished;
    }

    // ----- Drawing helpers ----------------------------------------------------

    /** Fill the whole playfield with the theme background. */
    protected final void clearScreen() {
        g.setFill(theme.background());
        g.fillRect(0, 0, width, height);
    }

    /** Fill a single grid cell at column {@code cx}, row {@code cy}. */
    protected final void px(int cx, int cy, Color color) {
        g.setFill(color);
        g.fillRect((double) cx * cell, (double) cy * cell, cell, cell);
    }
}
