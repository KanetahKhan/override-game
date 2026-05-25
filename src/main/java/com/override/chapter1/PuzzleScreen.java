package com.override.chapter1;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.ui.UIFactory;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Chapter 1's logic puzzle.
 *
 * Premise: a campus terminal demands the next number in a Fibonacci-like
 * sequence. The student NPCs in the room cannot solve it without AI help.
 *
 * Three resolution paths:
 *   1. Solve manually (full reward, +XP, +independentXp)
 *   2. Pay 20 coins for a hint (no dependency increase)
 *   3. Accept Astra's help (+10 dependency, smaller XP)
 *
 * The three-way choice is the centrepiece of the dependency mechanic.
 */
public class PuzzleScreen {

    // Sequence: 2, 3, 5, 8, 13, ?  (answer = 21)
    private static final int[] SEQUENCE = {2, 3, 5, 8, 13};
    private static final int ANSWER = 21;

    private final Runnable onComplete;
    private TextField input;
    private Label feedback;
    private Label sequenceLabel;
    private Button submit;
    private boolean astraHelped = false;
    private boolean hintPurchased = false;
    private int attempts = 0;

    public PuzzleScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("LOGIC TERMINAL — STUDENT WING B");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Sequence Lock");
        Label desc = UIFactory.body(
            "An old terminal blocks the door. The screen reads:\n\n" +
            "  \"PROVIDE THE NEXT NUMBER IN THE SEQUENCE.\"\n\n" +
            "Three students are huddled around it. None of them remember how. " +
            "One of them whispers: \"Just ask Astra. It's faster.\""
        );
        desc.setMaxWidth(900);

        sequenceLabel = new Label(formatSequence());
        sequenceLabel.getStyleClass().add("puzzle-sequence");

        input = new TextField();
        input.setPromptText("Your answer");
        input.setMaxWidth(160);
        input.getStyleClass().add("puzzle-input");

        submit = UIFactory.primary("Submit");
        submit.setOnAction(e -> trySubmit());

        feedback = new Label();
        feedback.getStyleClass().add("puzzle-feedback");
        feedback.setMinHeight(28);

        // Three resolution paths
        Button hint = UIFactory.secondary("Buy hint  (◈ 20)");
        hint.setOnAction(e -> buyHint());

        Button astra = UIFactory.danger("Ask Astra  (+10 dependency)");
        astra.setOnAction(e -> askAstra());

        HBox helpRow = new HBox(12, hint, astra);
        helpRow.setAlignment(Pos.CENTER);

        HBox inputRow = new HBox(10, input, submit);
        inputRow.setAlignment(Pos.CENTER);

        Label or = new Label("— or —");
        or.getStyleClass().add("body-dim");

        VBox center = new VBox(14,
            tag, header, desc, sequenceLabel, inputRow, feedback, or, helpRow
        );
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);

        return UIFactory.backdrop(wrap);
    }

    private String formatSequence() {
        StringBuilder sb = new StringBuilder();
        for (int n : SEQUENCE) sb.append(n).append("  →  ");
        sb.append("?");
        return sb.toString();
    }

    private void trySubmit() {
        attempts++;
        String txt = input.getText().trim();
        int v;
        try { v = Integer.parseInt(txt); }
        catch (NumberFormatException ex) {
            feedback.setText("Enter a number.");
            return;
        }
        if (v == ANSWER) {
            feedback.setText("✓ The terminal accepts. The door opens.");
            submit.setDisable(true);
            input.setDisable(true);

            // Reward depends on path taken
            int xp;
            int coins;
            String summary;
            if (astraHelped) {
                xp = 15; coins = 20;
                summary = "You took the easy path. Dependency rises.";
            } else if (hintPurchased) {
                xp = 25; coins = 30;
                GameState.get().getPlayer().addXp(xp);
                summary = "You bought the hint. You still solved it yourself.";
            } else {
                xp = 40; coins = 50;
                GameState.get().getPlayer().addXp(xp);
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffLogic(1);
                summary = "You solved it on your own. +1 Logic. The students stare at you.";
            }
            GameState.get().addCoins(coins);
            if (astraHelped) GameState.get().getPlayer().addXp(xp);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                summary + "\n\n+" + xp + " XP   +" + coins + " ◈"
            );
            a.setHeaderText("Puzzle solved");
            a.showAndWait();

            // Brief pause then return
            PauseTransition pt = new PauseTransition(Duration.millis(200));
            pt.setOnFinished(e -> onComplete.run());
            pt.play();
        } else {
            feedback.setText("✗ Wrong answer. The screen flashes red.");
        }
    }

    private void buyHint() {
        if (hintPurchased) {
            feedback.setText("You already bought a hint.");
            return;
        }
        if (!GameState.get().spendCoins(20)) {
            feedback.setText("Not enough coins.");
            return;
        }
        hintPurchased = true;
        feedback.setText("HINT: Each number is the sum of the two before it.");
        // Refresh HUD coin display by repeating subtle visual feedback
    }

    private void askAstra() {
        if (astraHelped) {
            feedback.setText("Astra has already shown you.");
            return;
        }
        astraHelped = true;
        GameState.get().increaseDependency(10);
        sequenceLabel.setText("2  →  3  →  5  →  8  →  13  →  21");
        input.setText("21");
        feedback.setText("Astra: \"The answer is 21. You're welcome.\"  Dependency +10");
    }
}
