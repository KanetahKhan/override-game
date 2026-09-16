package com.override.shared.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.net.URL;
import java.util.Objects;

/** Narrated, animated preamble shown before the Chapter 2 briefing/tutorial. */
public final class ChapterTwoIntroStoryScreen {
    private final Runnable onComplete;
    private final ChapterTwoPixelScene film = new ChapterTwoPixelScene();
    private final StackPane root = new StackPane();
    private final Rectangle progress = new Rectangle(0, 3, Color.web("#6ee7d0"));
    private Button pause;
    private double elapsed;
    private long previous;
    private boolean paused, finished;
    private final boolean animate;
    private MediaPlayer player;
    // The clip's opening is looped for the first LOOP_FRACTION of the film; the
    // rest of the clip then plays once so the payoff lands with the last shots.
    private static final double LOOP_SECONDS = 2.5;
    private static final double LOOP_FRACTION = 0.6;
    private double loopEnd = LOOP_SECONDS;
    private double tail;
    private boolean tailStarted;
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
            if (elapsed >= ChapterTwoPixelScene.FILM_SECONDS) finish();
        }
    };

    public ChapterTwoIntroStoryScreen(Runnable onComplete) { this(onComplete, true); }
    ChapterTwoIntroStoryScreen(Runnable onComplete, boolean animate) {
        this.onComplete = Objects.requireNonNull(onComplete);
        this.animate = animate;
    }

    public Parent build() {
        root.setId("harvest-intro");
        root.setMinSize(1280, 720); root.setPrefSize(1280, 720); root.setMaxSize(1280, 720);
        root.setStyle("-fx-background-color: #03070d;");
        pause = OpeningMenuView.option("PAUSE / SPACE", false); pause.setId("pause-intro");
        pause.setMinWidth(175); pause.setPrefWidth(175); pause.setMaxWidth(175);
        pause.setOnAction(e -> togglePause());
        Button skip = OpeningMenuView.option("ENTER FARM LEVEL >", true); skip.setId("skip-intro");
        skip.setMinWidth(255); skip.setPrefWidth(255); skip.setMaxWidth(255);
        skip.setOnAction(e -> finish());
        HBox controls = new HBox(10, pause, skip); controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setMaxSize(460, 44);
        StackPane.setAlignment(controls, Pos.TOP_RIGHT); StackPane.setMargin(controls, new Insets(76, 34, 0, 0));
        StackPane.setAlignment(progress, Pos.BOTTOM_LEFT);
        MediaView mediaView = animate ? createMediaView() : null;
        if (mediaView != null) {
            film.setOverlayOnly(true);
            root.getChildren().setAll(mediaView, film, controls, progress);
        } else {
            root.getChildren().setAll(film, controls, progress);
        }
        root.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE) { togglePause(); e.consume(); }
            else if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) { finish(); e.consume(); }
        });
        root.sceneProperty().addListener((o, oldScene, scene) -> {
            timer.stop(); previous = 0;
            if (scene != null && animate && !finished) { timer.start(); startMedia(); }
            else pauseMedia();
        });
        renderAt(0);
        return root;
    }

    void renderAt(double seconds) {
        film.film(seconds, OpeningPreferences.REDUCED_MOTION.get());
        progress.setWidth(1280 * Math.max(0, Math.min(1, seconds / ChapterTwoPixelScene.FILM_SECONDS)));
        if (player != null && !tailStarted && tail > 0.05
                && seconds >= ChapterTwoPixelScene.FILM_SECONDS * LOOP_FRACTION) {
            tailStarted = true;
            player.setCycleCount(1);
            player.setStartTime(Duration.seconds(loopEnd));
            player.setStopTime(Duration.seconds(loopEnd + tail));
            player.seek(Duration.seconds(loopEnd));
            player.play();
        }
    }

    private void togglePause() {
        if (finished) return;
        paused = !paused; previous = 0;
        pause.setText(paused ? "RESUME / SPACE" : "PAUSE / SPACE");
        if (paused) pauseMedia();
        else startMedia();
    }

    private void finish() {
        if (finished) return;
        finished = true; timer.stop();
        if (player != null) { player.stop(); player.dispose(); player = null; }
        onComplete.run();
    }

    /** The narrative footage sits behind the rail, faded so the text stays legible. */
    private MediaView createMediaView() {
        try {
            URL url = ChapterTwoIntroStoryScreen.class.getResource("/assets/chapter2/narrative.mp4");
            if (url == null) return null;
            Media media = new Media(url.toExternalForm());
            player = new MediaPlayer(media);
            player.setMute(true);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setStartTime(Duration.ZERO);
            player.setStopTime(Duration.seconds(LOOP_SECONDS));
            player.setOnReady(() -> {
                double total = media.getDuration().toSeconds();
                loopEnd = total > 0 ? Math.min(LOOP_SECONDS, total) : LOOP_SECONDS;
                tail = Math.max(0, total - loopEnd);
            });
            MediaView view = new MediaView(player);
            view.setFitWidth(1280); view.setFitHeight(720);
            view.setPreserveRatio(true);
            view.setOpacity(0.45);
            view.setMouseTransparent(true);
            return view;
        } catch (RuntimeException e) {
            player = null;
            return null;
        }
    }

    private void startMedia() {
        if (player == null || paused) return;
        if (OpeningPreferences.REDUCED_MOTION.get()) {
            player.seek(Duration.seconds(2));
            player.pause();
        } else if (player.getStatus() != MediaPlayer.Status.PLAYING) {
            player.play();
        }
    }

    private void pauseMedia() {
        if (player != null && player.getStatus() == MediaPlayer.Status.PLAYING) player.pause();
    }
}
