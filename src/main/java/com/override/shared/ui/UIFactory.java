package com.override.shared.ui;

import com.override.shared.model.GameState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Factory for shared UI building blocks: styled buttons, HUD bar with
 * coin/dependency display, panels, etc.
 *
 * Centralizing this here means screens stay short and the dark cyber theme
 * stays consistent.
 */
public class UIFactory {

    /** Primary action button (cyan). */
    public static Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-primary");
        b.setMinWidth(220);
        return b;
    }

    /** Secondary button (outlined). */
    public static Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-secondary");
        b.setMinWidth(220);
        return b;
    }

    /** Compact button used inside HUD or shop tiles. */
    public static Button compact(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-compact");
        return b;
    }

    /** Danger / risky-choice button (red glow). */
    public static Button danger(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-danger");
        return b;
    }

    /** Decorative title label. */
    public static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("title");
        return l;
    }

    public static Label subtitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("subtitle");
        return l;
    }

    public static Label body(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("body");
        l.setWrapText(true);
        return l;
    }

    /**
     * Build the persistent HUD shown at the top of every gameplay screen.
     *
     * Shows: character name, HP, dependency meter, coin balance.
     * Returns the root HBox; do not mutate after creation.
     */
    public static HBox hud() {
        GameState s = GameState.get();

        Circle dot = new Circle(6, Color.web("#28e0c0"));
        Label name = new Label(s.getPlayer().getDisplayName() + "  ·  Lv " + s.getPlayer().getLevel());
        name.getStyleClass().add("hud-name");

        ProgressBar hp = new ProgressBar((double) s.getPlayer().getHp() / s.getPlayer().getMaxHp());
        hp.getStyleClass().add("hp-bar");
        hp.setPrefWidth(160);

        Label depLabel = new Label("DEPENDENCY");
        depLabel.getStyleClass().add("hud-meta");

        ProgressBar dep = new ProgressBar(s.getDependency() / 100.0);
        dep.getStyleClass().add("dependency-bar");
        dep.setPrefWidth(140);

        Label depPct = new Label(s.getDependency() + "%");
        depPct.getStyleClass().add("hud-meta");

        Label coinIcon = new Label("◈");
        coinIcon.getStyleClass().add("coin-icon");
        Label coins = new Label(String.valueOf(s.getCoins()));
        coins.getStyleClass().add("hud-coins");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox left = new HBox(8, dot, name, hp);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox mid = new HBox(8, depLabel, dep, depPct);
        mid.setAlignment(Pos.CENTER);

        HBox right = new HBox(6, coinIcon, coins);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox bar = new HBox(20, left, spacer1, mid, spacer2, right);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 24, 10, 24));
        bar.getStyleClass().add("hud");
        bar.setMinWidth(1280);
        return bar;
    }

    /**
     * Animate a Label as if a person were typing — used everywhere a
     * dialogue line shows. Returns the Timeline so callers can skip it.
     */
    public static Timeline typewriter(Label target, String text, double charsPerSec) {
        target.setText("");
        Timeline tl = new Timeline();
        double per = 1000.0 / charsPerSec;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            sb.append(c);
            String snapshot = sb.toString();
            tl.getKeyFrames().add(new KeyFrame(
                Duration.millis(per * (i + 1)),
                e -> target.setText(snapshot)
            ));
        }
        tl.play();
        return tl;
    }

    /** Standard padded card panel for menus. */
    public static VBox card() {
        VBox v = new VBox();
        v.getStyleClass().add("card");
        v.setPadding(new Insets(20));
        return v;
    }

    /** Full-screen vertical layout with the dark gradient background. */
    public static StackPane backdrop(javafx.scene.Node... children) {
        StackPane sp = new StackPane(children);
        sp.getStyleClass().add("backdrop");
        sp.setMinSize(1280, 720);
        return sp;
    }
}
