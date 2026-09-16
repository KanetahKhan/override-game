package com.override.chapter2;

import com.override.Main;
import com.override.game.minigames.GodotGameLauncher;
import com.override.shared.model.GameState;
import com.override.shared.ui.ChapterMapScreen;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Controller for {@code /menu/ch2_briefing.fxml} — the Chapter 2 briefing
 * screen. Owns the wiring (header bindings from GameState, optional last-run
 * verdict strip, READY → loading screen that launches Godot, BACK →
 * chapter map) plus a 5-second-cycle TV-jitter engine that disturbs the
 * title and spikes the faint static overlay into anime slashes before
 * reversing the text.
 */
public class ChapterTwoBriefingController {

    @FXML
    private Label playerHeader;

    @FXML
    private Label creditLabel;

    @FXML
    private Label mainTitle;

    @FXML
    private AnchorPane rootPane;

    private Pane staticOverlay;

    @FXML
    private HBox lastRunBox;

    /** Called by {@link ChapterTwoTutorialScreen} after FXML load. */
    void setStaticOverlay(Pane overlay) {
        this.staticOverlay = overlay;
        staticOverlay.setMinSize(0, 0);
        staticOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        staticOverlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // The overlay is a STACKPANE sibling of the FXML root, so it does NOT
        // inherit rootPane's stylesheets — attach the briefing CSS explicitly.
        staticOverlay.getStylesheets().add(
            String.valueOf(getClass().getResource(
                "/menu/ch2_briefing.css")));
    }

    @FXML
    private Label lastRunTag;

    @FXML
    private Label lastRunDetail;

    private boolean isReversed = false;
    private static final String NORMAL_TEXT = "HARVEST PROTOCOL";
    private static final String REVERSED_TEXT = "LOCOTORP TSEVRAH";

    @FXML
    private void initialize() {
        playerHeader.setText(
            GameState.get().getPlayer().getDisplayName() + " // SYS.OPTIMAL");
        creditLabel.setText("CREDITS // " + GameState.get().getCoins());
        fillLastRun();
        startGlitchCycle();
    }

    // ── TV-style jitter engine (5 s cycle) ───────────────────────────

    private void startGlitchCycle() {
        Timeline cycle = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> triggerIntenseGlitchTransition()));
        cycle.setCycleCount(Timeline.INDEFINITE);
        cycle.play();
    }

    /**
     * Subtle monochromatic CRT static pulse: a brief, repeating burst of
     * black-and-white noise with CRT scanlines — like a television idling
     * on a dead channel. No colour flashes, no violent tearing. The static
     * opacity rises gently, holds for a beat, then decays back to idle.
     */
    private void triggerIntenseGlitchTransition() {
        Timeline glitchAction = new Timeline(

            // IN: static fades in, screen jitters ±4 px
            new KeyFrame(Duration.millis(0), e -> {
                staticOverlay.setOpacity(0.07);
                setDistortion(rootPane, 0, 0, 1.0);
            }),
            new KeyFrame(Duration.millis(120), e -> {
                staticOverlay.setOpacity(0.35);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 8);
                setDistortion(rootPane, (Math.random() - 0.5) * 6, (Math.random() - 0.5) * 3, 1.0);
            }),
            new KeyFrame(Duration.millis(260), e -> {
                staticOverlay.setOpacity(0.45);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 6);
                setDistortion(rootPane, (Math.random() - 0.5) * 4, (Math.random() - 0.5) * 2, 1.0);
            }),
            new KeyFrame(Duration.millis(420), e -> {
                staticOverlay.setOpacity(0.30);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 4);
                setDistortion(rootPane, (Math.random() - 0.5) * 3, (Math.random() - 0.5) * 1.5, 1.0);
            }),

            // PEAK at the midpoint — flip the title here
            new KeyFrame(Duration.millis(500), e -> {
                isReversed = !isReversed;
                mainTitle.setText(isReversed ? REVERSED_TEXT : NORMAL_TEXT);
                staticOverlay.setOpacity(0.50);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 6);
                setDistortion(rootPane, (Math.random() - 0.5) * 5, (Math.random() - 0.5) * 2, 1.0);
            }),

            // OUT: static fades back down
            new KeyFrame(Duration.millis(640), e -> {
                staticOverlay.setOpacity(0.35);
                staticOverlay.setTranslateY((Math.random() - 0.5) * 4);
                setDistortion(rootPane, (Math.random() - 0.5) * 3, (Math.random() - 0.5) * 1.5, 1.0);
            }),
            new KeyFrame(Duration.millis(780), e -> {
                staticOverlay.setOpacity(0.15);
                staticOverlay.setTranslateY(0);
                setDistortion(rootPane, 0, 0, 1.0);
            }),
            new KeyFrame(Duration.millis(900), e -> {
                staticOverlay.setOpacity(0.0);
                staticOverlay.setTranslateY(0);
                setDistortion(rootPane, 0, 0, 1.0);
            }));

        glitchAction.play();
    }

    private void setDistortion(Node node, double x, double y, double opacity) {
        node.setTranslateX(x);
        node.setTranslateY(y);
        node.setOpacity(opacity);
    }

    // ── Previous-run verdict strip ────────────────────────────────────

    private void fillLastRun() {
        if (!GodotGameLauncher.hasResult()) {
            return;
        }
        String json = GodotGameLauncher.readResult();

        boolean win = "true".equals(extract(json, "\"win\""));
        double score = parseDouble(extract(json, "\"final_score_percent\""));
        double timeLeft = parseDouble(extract(json, "\"game_time_left\""));
        double progress = parseDouble(extract(json, "\"progress_percent\""));
        int badKilled = (int) parseDouble(extract(json, "\"bad_killed\""));
        int badSpawned = (int) parseDouble(extract(json, "\"bad_spawned\""));
        int goodKilled = (int) parseDouble(extract(json, "\"good_killed\""));

        String verdict = win ? "\u25B6 RUN SUCCESSFUL" : "\u25B6 RUN FAILED";
        int secs = (int) Math.max(0, Math.round(timeLeft));

        lastRunTag.setText(verdict);
        lastRunDetail.setText(String.format(
            "score %.0f%% \u00B7 progress %.0f%% \u00B7 %d/%d BAD down \u00B7 %d GOOD hit"
                + (win ? " \u00B7 %d s rem" : " \u00B7 clock expired"),
            score, progress, badKilled, badSpawned, goodKilled, secs));

        lastRunBox.setManaged(true);
        lastRunBox.setVisible(true);
    }

    // ── Actions ───────────────────────────────────────────────────────

    @FXML
    private void handleStartGame() {
        Main.switchScene(new ChapterTwoLoadingScreen().build());
    }

    @FXML
    private void handleBack() {
        Main.switchScene(new ChapterMapScreen().build());
    }

    // ── Small JSON helpers (Godot result file) ────────────────────────

    private static String extract(String json, String key) {
        if (json == null) {
            return "";
        }
        int i = json.indexOf(key);
        if (i < 0) {
            return "";
        }
        int colon = json.indexOf(':', i + key.length());
        if (colon < 0) {
            return "";
        }
        int start = colon + 1;
        while (start < json.length()
                && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }
        if (start < json.length() && json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        }
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']') {
                break;
            }
            end++;
        }
        return json.substring(start, end).trim();
    }

    private static double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}