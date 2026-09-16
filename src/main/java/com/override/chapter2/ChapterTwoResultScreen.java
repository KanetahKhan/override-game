package com.override.chapter2;

import com.override.Main;
import com.override.chapter1.CurfewRecords;
import com.override.game.minigames.GodotGameLauncher;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.shared.service.ScoreboardService;
import com.override.shared.ui.ScoreboardScreen;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Post-Chapter-2 result screen — a Java reimplementation of the end card
 * that used to live in Godot's main.gd (show_end_card). Reads the result
 * JSON saved by main.gd and renders the exact same text, styled with the
 * briefing aesthetic and CRT static + jitter engine.
 *
 * GOOD_DRONE_SCORE_PENALTY below mirrors main.gd line 55 (0.5).
 */
public class ChapterTwoResultScreen {

    private static final double GOOD_DRONE_SCORE_PENALTY = 0.5;

    private static final String NEON_CYAN  = "#00ffff";
    private static final String NEON_GREEN = "#00ffaa";
    private static final String NEON_RED   = "#ff3366";
    private static final String NEON_MAG   = "#ff86ff";
    private static final String NEON_AMBER = "#ffd24a";

    private Pane staticOverlay;

    public Parent build() {
        String json = GodotGameLauncher.readResult();

        boolean win          = "true".equals(extract(json, "\"win\""));
        double  score        = parseDouble(extract(json, "\"final_score_percent\""));
        int     badKilled    = (int) parseDouble(extract(json, "\"bad_killed\""));
        int     badSpawned   = (int) parseDouble(extract(json, "\"bad_spawned\""));
        int     goodKilled   = (int) parseDouble(extract(json, "\"good_killed\""));
        double  timeLeft     = parseDouble(extract(json, "\"game_time_left\""));
        double  progress     = parseDouble(extract(json, "\"progress_percent\""));

        double killPct = badSpawned > 0 ? badKilled * 100.0 / badSpawned : 100.0;
        boolean resistancePossible = win && killPct >= 50.0;

        // Chapter 2 counts as completed the moment its result screen is reached,
        // mirroring Chapter 1's completeChapter(1) in CurfewProtocolScreen.
        GameState.get().completeChapter(2);
        SaveService.save();

        // ── Headline (exact Godot show_end_card text) ─────────────
        String headerText = win
            ? "SAFE ZONE REACHED!"
            : "TIME UP - ENFORCEMENT CAUGHT YOU!";
        String headerColor = win ? NEON_GREEN : NEON_RED;

        Label header = new Label(headerText);
        header.setStyle("-fx-text-fill: " + headerColor + ";"
            + " -fx-font-size: 30px; -fx-font-weight: 900;"
            + " -fx-font-family: 'Monospaced';");
        header.setEffect(new DropShadow(
            javafx.scene.effect.BlurType.GAUSSIAN,
            Color.web(headerColor, 0.55), 24, 0.25, 0, 0));

        // ── Verdict sub-line ───────────────────────────────────────
        Label verdict = new Label(resistancePossible
            ? "RESISTANCE POSSIBLE"
            : "MISSION FAILED");
        verdict.setStyle("-fx-text-fill: "
            + (resistancePossible ? NEON_GREEN : NEON_RED) + ";"
            + " -fx-font-size: 16px; -fx-font-weight: bold;"
            + " -fx-font-family: 'Monospaced';"
            + " -fx-letter-spacing: 3px;");

        // ── Actions: see the scoreboard, or replay the whole campaign ──
        // Replay is a full restart (Chapter 1 → 2); Chapter 2 can only be
        // reached by playing the campaign again, never standalone.
        Button boardBtn = new Button("SEE SCOREBOARD");
        boardBtn.getStyleClass().add("asset-button");
        boardBtn.setOnAction(e -> {
            GodotGameLauncher.clearResult();
            Main.switchScene(new ScoreboardScreen().build());
        });

        Button replayBtn = new Button("REPLAY");
        replayBtn.getStyleClass().add("asset-button");
        replayBtn.getStyleClass().add("secondary");
        replayBtn.setOnAction(e -> {
            GodotGameLauncher.clearResult();
            GameState.reset();
            Main.switchScene(new com.override.shared.ui.IntroStoryScreen(
                () -> Main.switchScene(
                    new com.override.chapter1.CurfewProtocolScreen().build())
            ).build());
        });

        // ── Chapter 1 latest run (for the combined campaign verdict) ─
        CurfewRecords.Run ch1 = CurfewRecords.latestRun();

        // ── Combined campaign score: 50% chapter 1 + 50% chapter 2 ──
        double ch1Pct = chapterOnePercent(ch1);
        double ch2Pct = Math.max(0.0, Math.min(100.0, score));
        double finalPct = (ch1Pct + ch2Pct) / 2.0;
        boolean resistance = finalPct >= 50.0;

        // Record this finished run on the campaign scoreboard (local until
        // the login system lands; signature dedupes repeat viewings).
        ScoreboardService.load().record(
            new ScoreboardService.CampaignRun(
                GameState.get().getPlayer().getDisplayName(),
                finalPct,
                resistance ? "RESISTANCE" : "OVERRIDDEN",
                System.currentTimeMillis()),
            json);

        Label ch1Row = ch1 == null
            ? statRow("CHAPTER 1 · CURFEW PROTOCOL — NOT PLAYED · 0.0%", NEON_AMBER)
            : statRow(String.format(
                    "CHAPTER 1 · CURFEW PROTOCOL — GRADE %s · %.1f%% (50%% OF CAMPAIGN)",
                    ch1.grade(), ch1Pct),
                NEON_CYAN);

        Label ch2Row = statRow(String.format(
                "CHAPTER 2 · HARVEST PROTOCOL — %s · %.1f%% (50%% OF CAMPAIGN)",
                resistancePossible ? "RESISTANCE POSSIBLE" : "MISSION FAILED",
                ch2Pct),
            resistancePossible ? NEON_GREEN : NEON_RED);

        // ── Combined campaign verdict: RESISTANCE or OVERRIDDEN ─────
        Label mainResult = new Label(resistance ? "RESISTANCE" : "OVERRIDDEN");
        mainResult.setStyle("-fx-text-fill: " + (resistance ? NEON_GREEN : NEON_RED) + ";"
            + " -fx-font-size: 38px; -fx-font-weight: 900;"
            + " -fx-font-family: 'Monospaced';"
            + " -fx-letter-spacing: 5px;");
        mainResult.setEffect(new DropShadow(
            javafx.scene.effect.BlurType.GAUSSIAN,
            Color.web(resistance ? NEON_GREEN : NEON_RED, 0.6), 26, 0.25, 0, 0));

        Label mainSub = new Label(resistance
            ? "BOTH CHAPTERS HELD — THE HUMAN PATH STAYS OPEN"
            : (ch1 == null
                ? "COMPLETE CHAPTER 1 TO EARN FULL RESISTANCE"
                : "THE OVERRIDE WINS THIS ROUND — TRY AGAIN IN CHAPTER 2"));
        mainSub.setStyle("-fx-text-fill: " + (resistance ? NEON_GREEN : NEON_AMBER) + ";"
            + " -fx-font-size: 13px; -fx-font-weight: bold;"
            + " -fx-font-family: 'Monospaced';"
            + " -fx-letter-spacing: 1px;");

        // ── Stat list ─────────────────────────────────────────────
        VBox stats = new VBox(10);
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(16, 0, 4, 0));

        stats.getChildren().add(ch1Row);
        stats.getChildren().add(ch2Row);

        if (win) {
            stats.getChildren().add(
                statRow("Time left: " + String.format("%.2f s", timeLeft), NEON_CYAN));
        } else {
            stats.getChildren().add(
                statRow("Safe zone reached only "
                    + String.format("%.1f%%", progress)
                    + " of the way", NEON_MAG));
        }
        stats.getChildren().add(statRow(
            String.format("Bad drones killed: %d / %d (%.1f%%)",
                badKilled, badSpawned, killPct),
            killPct >= 50 ? NEON_GREEN : NEON_RED));
        stats.getChildren().add(statRow(
            String.format("Good drones hurt: %d x %.1f%% = -%.1f%%",
                goodKilled, GOOD_DRONE_SCORE_PENALTY,
                goodKilled * GOOD_DRONE_SCORE_PENALTY),
            NEON_AMBER));

        Label finalScore = new Label(String.format(
            "FINAL CAMPAIGN SCORE: %.1f%%", finalPct));
        finalScore.setStyle("-fx-text-fill: " + NEON_CYAN + ";"
            + " -fx-font-size: 22px; -fx-font-weight: 900;"
            + " -fx-font-family: 'Monospaced';");
        finalScore.setEffect(new DropShadow(
            javafx.scene.effect.BlurType.GAUSSIAN,
            Color.web(NEON_CYAN, 0.6), 18, 0.25, 0, 0));

        HBox actions = new HBox(18, boardBtn, replayBtn);
        actions.setAlignment(Pos.CENTER);

        // ── Assemble card ──────────────────────────────────────────
        VBox card = new VBox(10, header, verdict, mainResult, mainSub, stats, finalScore, actions);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 48, 24, 48));
        card.setMaxWidth(860);
        card.setStyle("-fx-border-color: rgba(0,255,255,0.35); -fx-border-width: 1;"
            + " -fx-border-radius: 6; -fx-background-color: rgba(0,255,255,0.04);"
            + " -fx-background-radius: 6;");

        StackPane page = new StackPane(card);
        page.setStyle("-fx-background-color: radial-gradient("
            + "radius 120%, #080c12 0%, #0a1020 60%, #050810 100%);");
        page.getStylesheets().add(
            String.valueOf(getClass().getResource(
                "/menu/ch2_briefing.css")));

        // ── CRT static overlay (sibling, stays fixed) ──────────────
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

        startGlitchCycle();
        return page;
    }

    // ── Jitter engine (same as briefing/loading) ───────────────────

    private void startGlitchCycle() {
        Timeline cycle = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> triggerIntenseGlitchTransition()));
        cycle.setCycleCount(Timeline.INDEFINITE);
        cycle.play();
    }

    private void triggerIntenseGlitchTransition() {
        Timeline glitchAction = new Timeline(
            new KeyFrame(Duration.millis(0), e ->
                staticOverlay.setOpacity(0.07)),
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

    private Label statRow(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + color + ";"
            + " -fx-font-size: 13px; -fx-font-weight: bold;"
            + " -fx-font-family: 'Monospaced';"
            + " -fx-letter-spacing: 1px;");
        return lbl;
    }

    /** Maps the latest Chapter 1 grade onto a 0-100 scale (50% of the campaign). */
    private double chapterOnePercent(CurfewRecords.Run ch1) {
        if (ch1 == null) return 0.0;
        return switch (ch1.grade()) {
            case "S" -> 100.0;
            case "A" -> 80.0;
            case "B" -> 60.0;
            default  -> 40.0;
        };
    }

    private String extract(String json, String key) {
        if (json == null) return "";
        int i = json.indexOf(key);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + key.length());
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && " \t\n\r".indexOf(json.charAt(start)) >= 0) start++;
        if (start >= json.length()) return "";
        char q = json.charAt(start);
        if (q == '"') {
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        }
        int end = start + 1;
        while (end < json.length() && ",\n\r} ".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(start, end);
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0; }
    }
}