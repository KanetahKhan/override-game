package com.override.chapter4;

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
 * Chapter 4 puzzle — Manual Debug.
 *
 * A buggy snippet runs the campus access gate. The AI auto-completion that
 * wrote it is offline. The player has to read the code, find the off-by-one
 * in the loop bound, and supply the correct comparison value.
 *
 * Snippet (intentionally buggy):
 *   for (int i = 0; i &lt; tokens.length - 1; i++) {
 *       if (tokens[i] == EXPECTED[i]) ok++;
 *   }
 *   return ok == tokens.length;
 *
 * The bug is the &lt; bound: it stops one short. The terminal demands the
 * value the loop SHOULD compare against. Answer: tokens.length (so just
 * "tokens.length" — entering the literal length 5 also accepts).
 *
 * Three resolution paths same as previous puzzle screens.
 */
public class CodePuzzleScreen {

    // The "expected" token count baked into the puzzle. Players who enter
    // either the literal 5 or the symbolic answer get credit.
    private static final int EXPECTED_LENGTH = 5;

    private final Runnable onComplete;
    private TextField input;
    private Label feedback;
    private Label codeLabel;
    private Button submit;
    private boolean astraHelped = false;
    private boolean hintPurchased = false;

    public CodePuzzleScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("DEBUG TERMINAL — DEV BAY 7");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Manual Debug");
        Label desc = UIFactory.body(
            "The access gate is denying every valid token. The control panel was generated "
            + "by Astra's auto-complete two years ago. No one in the building remembers writing it.\n\n"
            + "The screen is open in a read-only editor. The bug is small. The cost of not "
            + "seeing it is large.\n\n"
            + "Look at the loop bound. What value should the comparison use so every token is checked?"
        );
        desc.setMaxWidth(900);

        codeLabel = new Label(formatCode());
        codeLabel.getStyleClass().add("puzzle-sequence");
        codeLabel.setStyle("-fx-font-family: 'Consolas','Courier New',monospace; -fx-font-size: 16px;");

        input = new TextField();
        input.setPromptText("Correct upper bound");
        input.setMaxWidth(220);
        input.getStyleClass().add("puzzle-input");

        submit = UIFactory.primary("Patch & Run");
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
            tag, header, desc, codeLabel, inputRow, feedback, or, helpRow
        );
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);

        return UIFactory.backdrop(wrap);
    }

    private String formatCode() {
        return  "int ok = 0;\n"
              + "for (int i = 0; i < tokens.length - 1; i++) {\n"
              + "    if (tokens[i] == EXPECTED[i]) ok++;\n"
              + "}\n"
              + "return ok == tokens.length;";
    }

    private void trySubmit() {
        String txt = input.getText().trim().toLowerCase();
        boolean correct =
                txt.equals(String.valueOf(EXPECTED_LENGTH))
             || txt.equals("tokens.length")
             || txt.equals("length");
        if (correct) {
            feedback.setText("✓ The loop now checks every token. Gate unlocks.");
            submit.setDisable(true);
            input.setDisable(true);

            int xp, coins;
            String summary;
            if (astraHelped) {
                xp = 18; coins = 25;
                summary = "Astra patched it for you. The terminal logs the assist.";
            } else if (hintPurchased) {
                xp = 30; coins = 40;
                GameState.get().getPlayer().addXp(xp);
                summary = "The hint pointed you in the right direction. You read the code yourself.";
            } else {
                xp = 50; coins = 60;
                GameState.get().getPlayer().addXp(xp);
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffLogic(2);
                summary = "You read the code. You found the bug. +2 Logic. The dev next to you stares.";
            }
            GameState.get().addCoins(coins);
            if (astraHelped) GameState.get().getPlayer().addXp(xp);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                summary + "\n\n+" + xp + " XP   +" + coins + " ◈"
            );
            a.setHeaderText("Patch accepted");
            a.showAndWait();

            PauseTransition pt = new PauseTransition(Duration.millis(200));
            pt.setOnFinished(e -> onComplete.run());
            pt.play();
        } else {
            feedback.setText("✗ Gate still rejects valid tokens. Check the loop bound — it stops too early.");
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
        feedback.setText("HINT: i < tokens.length - 1 means the LAST token is never checked. "
            + "Replace `tokens.length - 1` with the value that lets the loop check all of them.");
    }

    private void askAstra() {
        if (astraHelped) {
            feedback.setText("Astra has already produced the patch.");
            return;
        }
        astraHelped = true;
        GameState.get().increaseDependency(10);
        codeLabel.setText(
              "int ok = 0;\n"
            + "for (int i = 0; i < tokens.length; i++) {     // ← Astra's fix\n"
            + "    if (tokens[i] == EXPECTED[i]) ok++;\n"
            + "}\n"
            + "return ok == tokens.length;"
        );
        input.setText("tokens.length");
        feedback.setText("Astra: \"Bound was off by one. Fixed.\"  Dependency +10");
    }
}
