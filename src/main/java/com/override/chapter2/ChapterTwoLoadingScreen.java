package com.override.chapter2;

import com.override.Main;
import com.override.game.minigames.GodotGameLauncher;
import com.override.shared.ui.ChapterMapScreen;
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
        startProgress(page, barFill, status);
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

private void startProgress(StackPane page, Region barFill, Label status) {
        boolean launched = GodotGameLauncher.launchGodotBackgroundStart();

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
        tl.setOnFinished(e -> finish(page, launched));
        tl.play();
    }

    private void finish(StackPane page, boolean launched) {
        if (launched) {
            // Chapter 2 is still running as its own window — hang the map screen
            // in the background, and as soon as the game process closes, hand over
            // to the Java result screen (if the game wrote a result) and bring the
            // Java stage back to the front.
            GodotGameLauncher.onProcessExit(() -> {
                restoreJavaWindow();
                if (GodotGameLauncher.hasResult()) {
                    Main.switchScene(new ChapterTwoResultScreen().build());
                }
            });

            // Give the game window a beat to reveal itself (it was booted hidden
            // under the fake loading bar), then drop the Java stage to the taskbar
            // so the game grabs focus automatically — no manual taskbar click.
            PauseTransition revealBreak = new PauseTransition(Duration.millis(1500));
            revealBreak.setOnFinished(e -> iconifyJavaWindow());
            revealBreak.play();
        }
        Main.switchScene(new ChapterMapScreen().build());
        if (!launched) {
            Alert a = new Alert(Alert.AlertType.ERROR,
                "Chapter 2 could not be launched.\n\n"
                + GodotGameLauncher.gameSource() + "\n\n"
                + "Export the game once with GodotGameLauncher.exportGame() so players can\n"
                + "open Chapter2.exe directly — no Godot install is needed.");
            a.showAndWait();
        }
    }

    /** Store the stage's full-screen state, then minimize it off-screen. */
    private void iconifyJavaWindow() {
        Stage s = Main.getStage();
        if (s == null || !s.isShowing()) {
            return;
        }
        wasFullScreen = s.isFullScreen();
        s.setFullScreen(false);
        s.setIconified(true);
    }

    /** Un-minimize the Java stage and pull it to the front of the screen. */
    private void restoreJavaWindow() {
        Stage s = Main.getStage();
        if (s == null) {
            return;
        }
        s.setIconified(false);
        s.toFront();
        s.requestFocus();
        if (wasFullScreen) {
            s.setFullScreen(true);
        }
    }
}