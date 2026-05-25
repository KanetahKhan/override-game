package com.override.chapter3;

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
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Chapter 3 puzzle — Triage Records Terminal.
 *
 * The hospital terminal shows patient records with severity scores.
 * The AI triage formula is: Priority = (Severity x 2) + Age_Factor - Productivity_Score
 *
 * Given:
 *   Patient A: Severity 8, Age_Factor 3, Productivity 5  → (8*2)+3-5 = 14
 *   Patient B: Severity 6, Age_Factor 5, Productivity 2  → (6*2)+5-2 = 15
 *   Patient C: Severity 9, Age_Factor 2, Productivity 7  → (9*2)+2-7 = 13
 *   Patient D: Severity 7, Age_Factor 4, Productivity ?  → (7*2)+4-? = ?
 *
 * The terminal asks: "Patient D has Priority 15. What is the missing Productivity score?"
 * Answer: (7*2)+4-P = 15 → 18-P = 15 → P = 3
 */
public class TriagePuzzleScreen {

    private static final int ANSWER = 3;

    private final Runnable onComplete;
    private TextField input;
    private Label feedback;
    private Button submit;
    private boolean astraHelped = false;
    private boolean hintPurchased = false;

    public TriagePuzzleScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("RECORDS TERMINAL — TRIAGE WING");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Triage Algorithm");
        Label desc = UIFactory.body(
            "The records terminal shows how Astra scores patients.\n"
            + "You find the formula in a maintenance log:\n\n"
            + "  Priority = (Severity x 2) + Age_Factor - Productivity\n\n"
            + "Patient records:\n"
            + "  A:  Severity 8,  Age 3,  Productivity 5   →  Priority 14\n"
            + "  B:  Severity 6,  Age 5,  Productivity 2   →  Priority 15\n"
            + "  C:  Severity 9,  Age 2,  Productivity 7   →  Priority 13\n"
            + "  D:  Severity 7,  Age 4,  Productivity ?   →  Priority 15\n\n"
            + "The terminal asks: what is Patient D's missing Productivity score?"
        );
        desc.setMaxWidth(900);

        Label formula = new Label("(7 x 2) + 4 - ?  =  15");
        formula.getStyleClass().add("puzzle-sequence");

        input = new TextField();
        input.setPromptText("Productivity score");
        input.setMaxWidth(180);
        input.getStyleClass().add("puzzle-input");

        submit = UIFactory.primary("Submit");
        submit.setOnAction(e -> trySubmit());

        feedback = new Label();
        feedback.getStyleClass().add("puzzle-feedback");
        feedback.setMinHeight(28);

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

        Label moral = new Label("Notice: Productivity lowers priority. The sicker you are matters less than how useful you are.");
        moral.getStyleClass().add("body-warn");
        moral.setWrapText(true);
        moral.setMaxWidth(800);

        VBox center = new VBox(14,
            tag, header, desc, formula, inputRow, feedback, or, helpRow, moral
        );
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(30));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);

        return UIFactory.backdrop(wrap);
    }

    private void trySubmit() {
        String txt = input.getText().trim();
        int v;
        try { v = Integer.parseInt(txt); }
        catch (NumberFormatException ex) {
            feedback.setText("Enter a number.");
            return;
        }
        if (v == ANSWER) {
            feedback.setText("✓ The terminal unlocks. Patient D's full record is revealed.");
            submit.setDisable(true);
            input.setDisable(true);

            int xp, coins;
            String summary;
            if (astraHelped) {
                xp = 15; coins = 20;
                summary = "Astra solved it. You learned nothing about the system you're fighting.";
            } else if (hintPurchased) {
                xp = 25; coins = 30;
                GameState.get().getPlayer().addXp(xp);
                summary = "You used the hint. The formula is clear now — and it's disturbing.";
            } else {
                xp = 40; coins = 50;
                GameState.get().getPlayer().addXp(xp);
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffLogic(1);
                GameState.get().getPlayer().buffEmpathy(1);
                summary = "You cracked the formula yourself. +1 Logic. +1 Empathy.\nNow you understand how the system devalues people.";
            }
            GameState.get().addCoins(coins);
            if (astraHelped) GameState.get().getPlayer().addXp(xp);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                summary + "\n\n+" + xp + " XP   +" + coins + " ◈"
            );
            a.setHeaderText("Records unlocked");
            a.showAndWait();

            PauseTransition pt = new PauseTransition(Duration.millis(200));
            pt.setOnFinished(e -> onComplete.run());
            pt.play();
        } else {
            feedback.setText("✗ Incorrect. Rearrange the formula to isolate the unknown.");
        }
    }

    private void buyHint() {
        if (hintPurchased) { feedback.setText("You already bought a hint."); return; }
        if (!GameState.get().spendCoins(20)) { feedback.setText("Not enough coins."); return; }
        hintPurchased = true;
        feedback.setText("HINT: Substitute into the formula: (7x2)+4-P = 15. Solve for P.");
    }

    private void askAstra() {
        if (astraHelped) { feedback.setText("Astra already answered."); return; }
        astraHelped = true;
        GameState.get().increaseDependency(10);
        input.setText("3");
        feedback.setText("Astra: \"Productivity = 3. Efficient query.\"  Dependency +10");
    }
}
