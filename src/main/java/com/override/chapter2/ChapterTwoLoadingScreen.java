package com.override.chapter2;

import com.override.Main;
import com.override.game.minigames.ChiptuneMusic;
import com.override.game.minigames.GodotGameLauncher;
import com.override.shared.ui.ChapterMapScreen;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Pre-game loading screen for Chapter 2 — Harvest Protocol.
 * Same CRT static overlay + jitter as the briefing screen.
 */
public class ChapterTwoLoadingScreen {

    private static final String NEON = "#00ffff";

    private static final String[] STATUS = {
        "SPROUTING CROP ROWS",
        "CALIBRATING DRONE PATROLS",
        "LINKING SAFE ZONE BEACON",
        "SEALING THE ENFORCEMENT GRID",
        "HARVEST PROTOCOL ONLINE"
    };

    private Pane staticOverlay;
    private boolean isReversed = false;
    private Label titleLabel;
    private boolean wasFullScreen = false;
    private boolean wasMaximized = false;
    private Timeline processPoller;

    public Parent build() {
        // ── Title ──────────────────────────────────────────────────
        Label title = new Label("HARVEST PROTOCOL");
        titleLabel = title;
        title.getStyleClass().add("main-title");
        title.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);

        Label sub = new Label("ASSEMBLING THE FIELD");
        sub.setStyle("-fx-text-fill: " + NEON + "; -fx-font-size: 14px;"
            + " -fx-letter-spacing: 4px; -fx-font-weight: bold;"
            + " -fx-font-family: 'Monospaced';");

        // ── Status readout ─────────────────────────────────────────
        Label status = new Label("\u25B8 WAITING");
        status.setStyle("-fx-text-fill: #00ffaa; -fx-font-size: 14px;"
            + " -fx-font-family: 'Monospaced'; -fx-letter-spacing: 1px;");

        // ── Progress bar (neon fill + constant glow) ───────────────
        Region barFill = new Region();
        barFill.setStyle("-fx-background-color: linear-gradient(to right, "
            + "#00ffd0 0%, " + NEON + " 60%, #b3ffff 100%);"
            + " -fx-background-radius: 2;");

        Pane barTrack = new Pane(barFill);
        barTrack.setPrefSize(560, 14);
        barTrack.setStyle("-fx-background-color: rgba(0,255,255,0.08);"
            + " -fx-background-radius: 2; -fx-border-color: rgba(0,255,255,0.5);"
            + " -fx-border-width: 1; -fx-border-radius: 2;");
        barFill.setPrefSize(2, 12);
        barFill.setMinWidth(0);
        barFill.setMaxHeight(12);
        barFill.relocate(1, 1);

        StackPane barGlow = new StackPane(barTrack);
        barGlow.setEffect(new javafx.scene.effect.DropShadow(18.0,
            javafx.scene.paint.Color.CYAN));

        // ── Terminal frame ─────────────────────────────────────────
        VBox terminal = new VBox(22, title, sub, barGlow, status);
        terminal.setAlignment(Pos.CENTER);
        terminal.setPadding(new Insets(40, 60, 40, 60));
        terminal.setStyle("-fx-border-color: rgba(0,255,255,0.45); -fx-border-width: 1;"
            + " -fx-border-radius: 6; -fx-background-color: rgba(0,255,255,0.04);"
            + " -fx-background-radius: 6;");
        terminal.setMaxWidth(720);

        StackPane page = new StackPane(terminal);
        page.setStyle("-fx-background-color: radial-gradient("
            + "radius 120%, #080c12 0%, #0a1020 60%, #050810 100%);");
        page.getStylesheets().add(
            String.valueOf(getClass().getResource(
                "/menu/ch2_briefing.css")));

        // ── CRT static overlay (outside content, stays fixed) ──────
        staticOverlay = new Pane();
        staticOverlay.getStyleClass().add("static-overlay");
        staticOverlay.setMouseTransparent(true);
        staticOverlay.setMinSize(0, 0);
        staticOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        staticOverlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        staticOverlay.getStylesheets().add(
            String.valueOf(getClass().getResource(
                "/menu/ch2_briefing.css")));
        StackPane.setAlignment(staticOverlay, Pos.CENTER);

        page.getChildren().add(staticOverlay);

        // Launch the real game in background
        startProgress(barFill, status);
        startGlitchCycle();
        return page;
    }

    // ── Glitch engine (same as briefing) ───────────────────────────

    private void startGlitchCycle() {
        Timeline cycle = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> triggerIntenseGlitchTransition()));
        cycle.setCycleCount(Timeline.INDEFINITE);
        cycle.play();
    }

    private void triggerIntenseGlitchTransition() {
        Timeline glitchAction = new Timeline(
            new KeyFrame(Duration.millis(0), e -> {
                staticOverlay.setOpacity(0.07);
            }),
            new KeyFrame(Duration.millis(120), e -> {
                staticOverlay.setOpacity(0.35);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 8);
            }),
            new KeyFrame(Duration.millis(260), e -> {
                staticOverlay.setOpacity(0.45);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 6);
            }),
            new KeyFrame(Duration.millis(420), e -> {
                staticOverlay.setOpacity(0.30);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 4);
            }),
            new KeyFrame(Duration.millis(500), e -> {
                isReversed = !isReversed;
                titleLabel.setText(
                    isReversed ? "LOCOTORP TSEVRAH" : "HARVEST PROTOCOL");
                staticOverlay.setOpacity(0.50);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 6);
            }),
            new KeyFrame(Duration.millis(640), e -> {
                staticOverlay.setOpacity(0.35);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 4);
            }),
            new KeyFrame(Duration.millis(780), e -> {
                staticOverlay.setOpacity(0.15);
                staticOverlay.setTranslateY(0);
            }),
            new KeyFrame(Duration.millis(900), e -> {
                staticOverlay.setOpacity(0.07);
                staticOverlay.setTranslateY(0);
            }));
        glitchAction.play();
    }

    // ── Progress ───────────────────────────────────────────────────

    private void startProgress(Region barFill, Label status) {
        // Pin this screen above everything first, then start the game: it boots
        // (black splash and all) hidden behind the loading bar, so by the time
        // the bar finishes the game is ready and the swap has nothing to wait for.
        keepJavaWindowOnTop();
        boolean launched = GodotGameLauncher.launchGodotBackgroundStart();
        // The harvest run is a main game: drop the bed to its faint tone.
        if (launched) ChiptuneMusic.setDucked(true);

        double trackW = 560;
        Timeline tl = new Timeline();
        for (int i = 1; i <= 60; i++) {
            final double p = (double) i / 60;
            final int idx = Math.min(STATUS.length - 1, (int) (p * STATUS.length));
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(4200 * i / 60), e -> {
                barFill.setPrefWidth(Math.max(2, trackW * p));
                status.setText("\u25B8 " + STATUS[idx]);
            }));
        }
        tl.setOnFinished(e -> beginHandoff(status, launched));
        tl.play();
    }

    /**
     * The seam between chapters: fade this screen to black, and only then
     * start the game and move the JavaFX window out of the way. Every window
     * change happens while the screen is solid black, so the player sees one
     * continuous picture instead of a window swap.
     */
    /**
     * The seam between chapters. The game has been booting behind this screen
     * for the whole loading bar, so the swap is instant: this window steps out
     * of the way and the ready game window is already there. No black gap, and
     * this screen stays up behind it as the backdrop.
     */
    private void beginHandoff(Label status, boolean launched) {
        if (!launched) {
            releaseJavaWindow();
            Main.switchScene(new ChapterMapScreen().build());
            Alert a = new Alert(Alert.AlertType.ERROR,
                "Chapter 2 could not be launched.\n\n"
                + GodotGameLauncher.gameSource() + "\n\n"
                + "Export the game once with GodotGameLauncher.exportGame() so players can\n"
                + "open Chapter2.exe directly - no Godot install is needed.");
            a.showAndWait();
            return;
        }

        // Game already finished while the bar was running (very short run or a crash).
        if (!GodotGameLauncher.isProcessAlive()) {
            restoreJavaWindow();
            Main.switchScene(GodotGameLauncher.hasResult()
                ? new ChapterTwoResultScreen().build()
                : new ChapterMapScreen().build());
            return;
        }

        status.setText("▸ MISSION UNDERWAY — STAND BY");
        sendJavaWindowBehind();
        startProcessWatcher();
    }

    /**
     * FX-thread poller: checks every 600 ms whether the game has written its
     * result JSON (main.gd stores it the instant the run ends, immediately
     * before {@code get_tree().quit()}). On a result it restores the Java
     * window and shows the ResultScreen — this is the definitive end-of-game
     * signal because it survives flaky process-alive checks on Windows.
     *
     * <p>Runs entirely on the Application Thread, so {@code Main.switchScene}
     * is always safe. The Timeline is stored in a field so it cannot be
     * garbage-collected while playing.
     */
    private void startProcessWatcher() {
        processPoller = new Timeline();
        processPoller.getKeyFrames().add(new KeyFrame(Duration.millis(600), e -> {
            if (GodotGameLauncher.hasResult()) {
                processPoller.stop();
                restoreJavaWindow();
                Main.switchScene(new ChapterTwoResultScreen().build());
            } else if (!GodotGameLauncher.isProcessAlive()) {
                // Game closed without writing a result (manual quit / crash at
                // the OS level) — fall back to the map so the player is not stuck.
                processPoller.stop();
                restoreJavaWindow();
                Main.switchScene(new ChapterMapScreen().build());
            }
        }));
        processPoller.setCycleCount(Timeline.INDEFINITE);
        processPoller.play();
    }

    /** Store the stage's full-screen state, then minimize it off-screen. */
    /** Holds this screen above the booting game window so its splash never shows. */
    private void keepJavaWindowOnTop() {
        Stage s = Main.getStage();
        if (s != null && s.isShowing()) {
            s.setAlwaysOnTop(true);
        }
    }

    /** Undoes {@link #keepJavaWindowOnTop()} without moving the window. */
    private void releaseJavaWindow() {
        Stage s = Main.getStage();
        if (s != null) {
            s.setAlwaysOnTop(false);
        }
    }

    /** Drops the stage out of full screen and behind the game, without minimising. */
    private void sendJavaWindowBehind() {
        Stage s = Main.getStage();
        if (s == null || !s.isShowing()) {
            return;
        }
        wasFullScreen = s.isFullScreen();
        wasMaximized = s.isMaximized();
        s.setAlwaysOnTop(false);
        s.setFullScreen(false);
        s.setMaximized(true);   // still covers the screen, just no longer on top
        s.toBack();
    }

    private void restoreJavaWindow() {
        // Every path out of the Godot run comes through here; bring the music back.
        ChiptuneMusic.setDucked(false);
        Stage s = Main.getStage();
        if (s == null) {
            return;
        }
        s.setAlwaysOnTop(false);
        s.setIconified(false);
        s.toFront();
        s.requestFocus();
        if (!wasFullScreen) {
            s.setMaximized(wasMaximized);
            return;
        }
        // Windows ignores a full-screen request from a window that is not yet
        // in the foreground, so give the stage a beat and retry once it has focus.
        PauseTransition settle = new PauseTransition(Duration.millis(150));
        settle.setOnFinished(e -> {
            s.setFullScreen(true);
            if (!s.isFullScreen()) {
                s.focusedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> o, Boolean was, Boolean now) {
                        if (now) {
                            s.setFullScreen(true);
                            s.focusedProperty().removeListener(this);
                        }
                    }
                });
            }
        });
        settle.play();
    }
}