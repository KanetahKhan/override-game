package com.override.shared.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import java.util.Objects;

/** 40-second, captioned classroom-only cinematic, animated by the game itself. */
public final class IntroStoryScreen {
    private final Runnable onComplete;
    private final ClassroomPixelScene film = new ClassroomPixelScene();
    private final StackPane root = new StackPane();
    private final Rectangle progress = new Rectangle(0, 3, Color.web("#6ee7d0"));
    private Button pause;
    private double elapsed;
    private long previous;
    private boolean paused, finished;
    private final boolean animate;
    private final AnimationTimer timer = new AnimationTimer() {
        @Override public void handle(long now) {
            if (root.getScene() == null || root.getScene().getWindow() == null
                    || !root.getScene().getWindow().isFocused() || paused) {
                previous = 0; return;
            }
            if (previous == 0) previous = now;
            elapsed += Math.min(0.1, (now - previous) / 1_000_000_000.0);
            previous = now;
            renderAt(elapsed);
            if (elapsed >= ClassroomPixelScene.FILM_SECONDS) finish();
        }
    };

    public IntroStoryScreen(Runnable onComplete) { this(onComplete, true); }
    IntroStoryScreen(Runnable onComplete, boolean animate) {
        this.onComplete = Objects.requireNonNull(onComplete);
        this.animate = animate;
    }

    public Parent build() {
        root.setId("classroom-intro");
        root.setMinSize(1280, 720); root.setPrefSize(1280, 720); root.setMaxSize(1280, 720);
        root.setStyle("-fx-background-color: #03070d;");
        pause = OpeningMenuView.option("PAUSE / SPACE", false); pause.setId("pause-intro");
        pause.setMinWidth(175); pause.setPrefWidth(175); pause.setMaxWidth(175);
        pause.setOnAction(e -> togglePause());
        Button skip = OpeningMenuView.option("ENTER CLASSROOM >", true); skip.setId("skip-intro");
        skip.setMinWidth(230); skip.setPrefWidth(230); skip.setMaxWidth(230);
        skip.setOnAction(e -> finish());
        HBox controls = new HBox(10, pause, skip); controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setMaxSize(415, 44);
        StackPane.setAlignment(controls, Pos.TOP_RIGHT); StackPane.setMargin(controls, new Insets(76, 34, 0, 0));
        StackPane.setAlignment(progress, Pos.BOTTOM_LEFT);
        root.getChildren().setAll(film, controls, progress);
        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE) { togglePause(); e.consume(); }
            else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) { finish(); e.consume(); }
        });
        root.sceneProperty().addListener((o, oldScene, scene) -> {
            timer.stop(); previous = 0;
            if (scene != null && animate && !finished) timer.start();
        });
        renderAt(0);
        return root;
    }

    void renderAt(double seconds) {
        film.film(seconds, OpeningPreferences.REDUCED_MOTION.get());
        progress.setWidth(1280 * Math.max(0, Math.min(1, seconds / ClassroomPixelScene.FILM_SECONDS)));
    }

    private void togglePause() {
        if (finished) return;
        paused = !paused; previous = 0;
        pause.setText(paused ? "RESUME / SPACE" : "PAUSE / SPACE");
    }

    private void finish() {
        if (finished) return;
        finished = true; timer.stop(); onComplete.run();
    }
}
