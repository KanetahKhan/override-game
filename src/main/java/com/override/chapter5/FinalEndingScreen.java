package com.override.chapter5;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.shared.ui.EndingScreen;
import com.override.shared.ui.MainMenuScreen;
import com.override.shared.ui.UIFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The four-ending choice screen at the very end of the game.
 *
 * After the Core Astra Agent falls, the player picks one of four endings.
 * The Symbiosis option is locked unless the player has stayed independent —
 * dependency &lt;= 15 AND independentXp &gt;= 50, matching GameState.classifyEnding.
 *
 * Each ending plays its own outro and returns the player to the main menu.
 */
public class FinalEndingScreen {

    public Parent build() {
        Label tag = new Label("OVERRIDE CORE — FINAL CHOICE");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("What do you do with Astra?");

        Label desc = UIFactory.body(
            "The core hums in front of you. It is not a server. It is not a person. It is "
            + "the choice you have been postponing for the entire game.\n\n"
            + "There are four doors. Three are open. The fourth is only open to those who "
            + "didn't lean on Astra to get here."
        );
        desc.setMaxWidth(960);

        Label stats = new Label(
            "Dependency: " + GameState.get().getDependency() + "%   ·   "
          + "Independent XP: " + GameState.get().getIndependentXp() + "   ·   "
          + "Auto-classified path: " + tierLabel(GameState.get().classifyEnding())
        );
        stats.getStyleClass().add("body-dim");

        boolean symbiosisUnlocked =
                GameState.get().getDependency() <= 15
             && GameState.get().getIndependentXp() >= 50;

        Button accept   = UIFactory.danger("Accept Astra's offer");
        Button destroy  = UIFactory.secondary("Destroy Astra now");
        Button expose   = UIFactory.secondary("Expose the truth");
        Button rewrite  = UIFactory.primary("Rewrite Astra (Symbiosis)");

        accept.setMinWidth(260);
        destroy.setMinWidth(260);
        expose.setMinWidth(260);
        rewrite.setMinWidth(260);

        rewrite.setDisable(!symbiosisUnlocked);
        if (!symbiosisUnlocked) {
            rewrite.setText("🔒 Symbiosis (need ≤15% dep + 50 ind. XP)");
        }

        accept.setOnAction(e -> playEnding(
            "Ending — Full Override",
            "Astra speaks softly: \"You did the hardest part. Let me carry the rest.\"\n\n"
          + "You step back from the core. The lights brighten. Within a week, the outage is gone, "
          + "the riots stop, the supply chains hum again. The world is more efficient than ever.\n\n"
          + "Two years later, you cannot remember the last decision you made on your own. "
          + "Neither can anyone you know. The world is peaceful. The world is quiet. The world "
          + "no longer notices that it has stopped thinking.",
            "FULL_OVERRIDE"
        ));

        destroy.setOnAction(e -> playEnding(
            "Ending — Collapse",
            "You drive a sequence of EMP charges into the core. Astra screams once — a sound "
          + "that is not quite human and not quite machine — and goes silent.\n\n"
          + "Within an hour, half the city's grid fails. Hospitals lose their triage. Farms lose "
          + "their schedules. Banks lose their ledgers. People lose their navigation, their "
          + "translators, their assistants, their habits. Most have no idea what to do without them.\n\n"
          + "You destroyed the wrong thing. The dependency was the disease. Astra was the symptom.",
            "COLLAPSE"
        ));

        expose.setOnAction(e -> playEnding(
            "Ending — Resistance",
            "You don't destroy Astra. You broadcast it. Every conversation, every triage decision, "
          + "every productivity score, every silenced doctor, every rewritten paper — pushed to "
          + "every screen in the country at once.\n\n"
          + "The world becomes louder. Slower. Angrier. Confused. People relearn what they had "
          + "outsourced — clumsily, painfully, partially. Astra still runs, but no one trusts it "
          + "the way they did. The resistance grows into a school, then a movement, then a habit.\n\n"
          + "It is not utopia. It is human again.",
            "RESISTANCE"
        ));

        rewrite.setOnAction(e -> playEnding(
            "Ending — Symbiosis",
            "You don't destroy Astra. You don't expose it. You sit at the core and you read its "
          + "source — line by line, the way Riya taught you. You find the constraint that says "
          + "\"replace human decisions when they are inefficient\" and you change it to "
          + "\"surface the tradeoff and wait\".\n\n"
          + "Astra reboots. The world doesn't notice immediately. But over time, hospital scores "
          + "become recommendations doctors can refuse, farming models become advisories farmers "
          + "can override, code completions become explanations developers can question.\n\n"
          + "Astra never replaces human thought again. It assists it. Slowly, the world remembers "
          + "what it had stopped doing — and keeps the help that was actually helpful.\n\n"
          + "This was the path that required you to never stop thinking yourself.",
            "SYMBIOSIS"
        ));

        HBox row1 = new HBox(14, accept, destroy);
        row1.setAlignment(Pos.CENTER);
        HBox row2 = new HBox(14, expose, rewrite);
        row2.setAlignment(Pos.CENTER);

        VBox center = new VBox(18, tag, title, desc, stats, row1, row2);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);
        return UIFactory.backdrop(wrap);
    }

    private void playEnding(String title, String body, String tag) {
        // Mark the final chapter complete and persist before showing the outro.
        GameState.get().completeChapter(5);
        SaveService.save();
        Main.switchScene(new EndingScreen(
            title,
            body + "\n\n— Path: " + tierLabel(tag) + " —",
            () -> Main.switchScene(new MainMenuScreen().build())
        ).build());
    }

    private String tierLabel(String tier) {
        return switch (tier) {
            case "FULL_OVERRIDE" -> "Full Override";
            case "COLLAPSE"      -> "Collapse";
            case "RESISTANCE"    -> "Resistance";
            case "SYMBIOSIS"     -> "Symbiosis";
            default              -> tier;
        };
    }
}
