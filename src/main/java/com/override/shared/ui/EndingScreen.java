package com.override.shared.ui;

import com.override.shared.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Generic chapter-completion / ending screen.
 *
 * Used for both per-chapter outros and the four final endings. The header
 * and body change; the dependency-tier classification is shown at the
 * bottom so the player can see how their choices are scoring.
 */
public class EndingScreen {

    private final String header;
    private final String body;
    private final Runnable onContinue;

    public EndingScreen(String header, String body, Runnable onContinue) {
        this.header = header;
        this.body = body;
        this.onContinue = onContinue;
    }

    public Parent build() {
        Label tag = new Label("CHECKPOINT");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title(header);

        Label text = UIFactory.body(body);
        text.setMaxWidth(900);
        text.getStyleClass().add("ending-body");

        Label tierLabel = new Label("Current path: " + tierLabel(GameState.get().classifyEnding()));
        tierLabel.getStyleClass().add("ending-tier");

        Label stats = new Label(
            "Dependency: " + GameState.get().getDependency() + "%  ·  "
          + "Independent XP: " + GameState.get().getIndependentXp() + "  ·  "
          + "Level: " + GameState.get().getPlayer().getLevel()
        );
        stats.getStyleClass().add("body-dim");

        Button next = UIFactory.primary("Continue");
        next.setOnAction(e -> onContinue.run());

        VBox center = new VBox(16, tag, title, text, tierLabel, stats, next);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(60));

        return UIFactory.backdrop(center);
    }

    private String tierLabel(String tier) {
        return switch (tier) {
            case "FULL_OVERRIDE" -> "Full Override (you are leaning on Astra)";
            case "COLLAPSE"      -> "Collapse (mixed dependence)";
            case "RESISTANCE"    -> "Resistance (steady human path)";
            case "SYMBIOSIS"     -> "Symbiosis (independent and capable)";
            default              -> tier;
        };
    }
}
