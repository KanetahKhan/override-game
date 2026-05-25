package com.override.chapter2;

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
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Chapter 2 puzzle — Crop Cycle Terminal.
 *
 * The smart-field terminal controls irrigation. The AI schedule has failed
 * and the player must manually calculate the correct watering day based
 * on a simple pattern in the crop rotation data.
 *
 * Pattern: crops rotate on a 7-day cycle. The terminal shows the last
 * five irrigation days (3, 10, 17, 24, 31) and asks for the next.
 * Answer: 38 (each interval is +7).
 *
 * Three resolution paths (same as Chapter 1 puzzle).
 */
public class CropPuzzleScreen {

    private static final int[] SCHEDULE = {3, 10, 17, 24, 31};
    private static final int ANSWER = 38;

    private final Runnable onComplete;
    private TextField input;
    private Label feedback;
    private Label scheduleLabel;
    private Button submit;
    private boolean astraHelped = false;
    private boolean hintPurchased = false;

    public CropPuzzleScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("CROP TERMINAL — SMART FIELD SECTOR 4");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Irrigation Lock");
        Label desc = UIFactory.body(
            "The field irrigation terminal is flashing red. Astra's automated schedule "
            + "crashed during the server disturbance, and the crops are drying out.\n\n"
            + "The screen shows the last five scheduled watering days and demands the next one.\n\n"
            + "A farmer stands behind you: \"We used to know this by heart. "
            + "Seven-day rotation, my grandfather said. But I never learned it myself.\""
        );
        desc.setMaxWidth(900);

        scheduleLabel = new Label(formatSchedule());
        scheduleLabel.getStyleClass().add("puzzle-sequence");

        input = new TextField();
        input.setPromptText("Next watering day");
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

        VBox center = new VBox(14,
            tag, header, desc, scheduleLabel, inputRow, feedback, or, helpRow
        );
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);

        return UIFactory.backdrop(wrap);
    }

    private String formatSchedule() {
        StringBuilder sb = new StringBuilder("Day ");
        for (int n : SCHEDULE) sb.append(n).append("  →  ");
        sb.append("?");
        return sb.toString();
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
            feedback.setText("✓ The irrigation system restarts. Water flows into the fields.");
            submit.setDisable(true);
            input.setDisable(true);

            int xp, coins;
            String summary;
            if (astraHelped) {
                xp = 15; coins = 20;
                summary = "Astra solved it for you. The farmer looks away.";
            } else if (hintPurchased) {
                xp = 25; coins = 30;
                GameState.get().getPlayer().addXp(xp);
                summary = "The hint helped. You figured it out.";
            } else {
                xp = 40; coins = 50;
                GameState.get().getPlayer().addXp(xp);
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffLogic(1);
                summary = "You solved it like the old farmers did. +1 Logic. The farmer nods with respect.";
            }
            GameState.get().addCoins(coins);
            if (astraHelped) GameState.get().getPlayer().addXp(xp);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                summary + "\n\n+" + xp + " XP   +" + coins + " ◈"
            );
            a.setHeaderText("Irrigation restored");
            a.showAndWait();

            PauseTransition pt = new PauseTransition(Duration.millis(200));
            pt.setOnFinished(e -> onComplete.run());
            pt.play();
        } else {
            feedback.setText("✗ Incorrect. The terminal buzzes. Think about the interval.");
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
        feedback.setText("HINT: Look at the gap between each day. It's always the same.");
    }

    private void askAstra() {
        if (astraHelped) {
            feedback.setText("Astra has already answered.");
            return;
        }
        astraHelped = true;
        GameState.get().increaseDependency(10);
        scheduleLabel.setText("Day 3  →  10  →  17  →  24  →  31  →  38");
        input.setText("38");
        feedback.setText("Astra: \"Day 38. A 7-day rotation. Trivial.\"  Dependency +10");
    }
}
