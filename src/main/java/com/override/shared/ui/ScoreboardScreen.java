package com.override.shared.ui;

import com.override.Main;
import com.override.game.minigames.GodotGameLauncher;
import com.override.shared.model.GameState;
import com.override.shared.service.ScoreboardService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Campaign scoreboard — shows every completed two-chapter run ranked by final
 * campaign score, with "Replay" (start a fresh campaign) and "Main Menu".
 *
 * <p>Login is not built yet; entries use the local player display name.
 * When the login system lands this screen simply calls
 * {@link ScoreboardService#record} keyed by user id instead.</p>
 */
public class ScoreboardScreen {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private static final String NEON_CYAN  = "#00ffff";
    private static final String NEON_GREEN = "#00ffaa";
    private static final String NEON_AMBER = "#ffd24a";
    private static final String NEON_RED   = "#ff3366";

    public Parent build() {
        ScoreboardService board = ScoreboardService.load();
        List<ScoreboardService.CampaignRun> runs = board.top();

        // ── Title ─────────────────────────────────────────────────────
        Label title = new Label("SCOREBOARD");
        title.setStyle("-fx-text-fill: " + NEON_CYAN + ";"
            + " -fx-font-size: 40px; -fx-font-weight: 900;"
            + " -fx-font-family: 'Monospaced';");

        Label sub = new Label("CAMPAIGN RUNS — RANKED BY FINAL SCORE");
        sub.setStyle("-fx-text-fill: " + NEON_CYAN + ";"
            + " -fx-font-size: 13px; -fx-font-weight: bold;"
            + " -fx-font-family: 'Monospaced';"
            + " -fx-letter-spacing: 3px;");

        // ── Column headers ────────────────────────────────────────────
        Label hdrRank  = headerLabel("#");
        Label hdrName  = headerLabel("PLAYER");
        Label hdrScore = headerLabel("SCORE");
        Label hdrVerd  = headerLabel("VERDICT");
        Label hdrDate  = headerLabel("DATE");
        Region hdrSpacer = new Region();
        HBox.setHgrow(hdrSpacer, Priority.ALWAYS);
        HBox hdr = new HBox(18, hdrRank, hdrName, hdrSpacer,
            hdrScore, hdrVerd, hdrDate);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(6, 20, 6, 20));
        hdr.setMaxWidth(860);
        hdr.setMinWidth(860);

        // ── Score rows ────────────────────────────────────────────────
        VBox list = new VBox(4);
        list.setAlignment(Pos.CENTER);
        list.setPadding(new Insets(0, 0, 8, 0));

        if (runs.isEmpty()) {
            Label empty = new Label("NO CAMPAIGN RUNS RECORDED YET — FINISH BOTH CHAPTERS");
            empty.setStyle("-fx-text-fill: " + NEON_AMBER + ";"
                + " -fx-font-size: 13px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Monospaced';"
                + " -fx-letter-spacing: 1px;");
            list.getChildren().add(empty);
        } else {
            for (int i = 0; i < runs.size(); i++) {
                list.getChildren().add(buildRow(i + 1, runs.get(i)));
            }
        }

        // ── Buttons ───────────────────────────────────────────────────
        Button replay = new Button("REPLAY CAMPAIGN");
        replay.getStyleClass().add("asset-button");
        replay.setOnAction(e -> startNewCampaign());

        Button menu = new Button("MAIN MENU");
        menu.getStyleClass().add("asset-button");
        menu.getStyleClass().add("secondary");
        menu.setOnAction(e -> {
            GodotGameLauncher.clearResult();
            Main.switchScene(new MainMenuScreen().build());
        });

        HBox buttons = new HBox(20, replay, menu);
        buttons.setAlignment(Pos.CENTER);

        // ── Card ──────────────────────────────────────────────────────
        VBox card = new VBox(12, title, sub, hdr, list, buttons);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28, 20, 28, 20));
        card.setMaxWidth(900);
        card.setStyle("-fx-border-color: rgba(0,255,255,0.35); -fx-border-width: 1;"
            + " -fx-border-radius: 6; -fx-background-color: rgba(0,255,255,0.04);"
            + " -fx-background-radius: 6;");

        StackPane page = new StackPane(card);
        page.setStyle("-fx-background-color: radial-gradient("
            + "radius 120%, #080c12 0%, #0a1020 60%, #050810 100%);");
        page.getStylesheets().add(
            String.valueOf(getClass().getResource(
                "/menu/ch2_briefing.css")));

        return page;
    }

    /** Start a fresh campaign from Chapter 1 — same path as "Replay" on the result screen. */
    private void startNewCampaign() {
        GodotGameLauncher.clearResult();
        GameState.reset();
        Main.switchScene(new IntroStoryScreen(
            () -> Main.switchScene(new com.override.chapter1.CurfewProtocolScreen().build())
        ).build());
    }

    private HBox buildRow(int rank, ScoreboardService.CampaignRun run) {
        Label rankLbl = cellLabel(String.valueOf(rank), NEON_CYAN, 14);
        Label nameLbl = cellLabel(run.player(), "#e0f0f0", 14);
        Label scoreLbl = cellLabel(String.format("%.1f%%", run.scorePct()), NEON_GREEN, 14);
        String verdColor = "RESISTANCE".equals(run.verdict()) ? NEON_GREEN : NEON_RED;
        Label verdictLbl = cellLabel(run.verdict(), verdColor, 12);
        String date = LocalDate.ofInstant(
            Instant.ofEpochMilli(run.when()), ZoneId.systemDefault()).format(DATE_FMT);
        Label dateLbl = cellLabel(date, NEON_AMBER, 12);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(18, rankLbl, nameLbl, spacer,
            scoreLbl, verdictLbl, dateLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 20, 10, 20));
        row.setMinWidth(860);
        row.setMaxWidth(860);
        row.setStyle("-fx-border-color: rgba(0,255,255,0.15); -fx-border-width: 0 0 1 0;");
        return row;
    }

    private Label headerLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: rgba(0,255,255,0.45);"
            + " -fx-font-size: 11px; -fx-font-weight: bold;"
            + " -fx-font-family: 'Monospaced';"
            + " -fx-letter-spacing: 2px;");
        return l;
    }

    private Label cellLabel(String text, String color, double size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + ";"
            + " -fx-font-size: " + size + "px;"
            + " -fx-font-weight: bold;"
            + " -fx-font-family: 'Monospaced';");
        return l;
    }
}