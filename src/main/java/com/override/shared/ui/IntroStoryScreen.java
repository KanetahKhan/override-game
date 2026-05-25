package com.override.shared.ui;

import com.override.Main;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Cinematic intro that runs once after a New Game.
 *
 * Plays a sequence of narrative lines with a typewriter effect, then
 * routes the player to the Chapter Map.
 */
public class IntroStoryScreen {

    private static final String[] LINES = {
        "Year 2048.",
        "A mega-AI named Astra is the invisible backbone of civilization.",
        "It teaches our students. It diagnoses our patients. It writes our code.",
        "It became helpful. Then necessary. Then unquestionable.",
        "We stopped reading. We stopped reasoning. We stopped remembering.",
        "Then, one quiet evening, a server cluster flickered.",
        "And a message that should not exist reached your terminal:",
        "\u201CIf the system thinks for us, then someday it will choose who deserves to think.",
        "  They took the last real minds. Find us before they erase us.\u201D",
        "You are Ayan. A final-year CSE student. Dependent — like everyone else.",
        "But for the first time in years, you are about to think for yourself."
    };

    private int idx = 0;
    private Timeline currentTl;
    private Label line;
    private Button skip;

    public Parent build() {
        Label hint = new Label("INTRO");
        hint.getStyleClass().add("scene-tag");

        line = new Label();
        line.getStyleClass().add("dialogue-line");
        line.setWrapText(true);
        line.setMaxWidth(960);
        line.setMinHeight(140);

        Button next = UIFactory.primary("Continue ▶");
        skip = UIFactory.secondary("Skip Intro");

        next.setOnAction(e -> advance(next));
        skip.setOnAction(e -> Main.switchScene(new ChapterMapScreen().build()));

        VBox box = new VBox(18, hint, line, next, skip);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60));
        box.setMaxWidth(1000);

        StackPane root = UIFactory.backdrop(box);
        root.getStyleClass().add("intro-bg");

        // Start first line
        currentTl = UIFactory.typewriter(line, LINES[idx], 32);
        return root;
    }

    private void advance(Button next) {
        // If text is still typing, finish it instantly first
        if (currentTl != null && currentTl.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            currentTl.stop();
            line.setText(LINES[idx]);
            return;
        }
        idx++;
        if (idx >= LINES.length) {
            Main.switchScene(new ChapterMapScreen().build());
            return;
        }
        currentTl = UIFactory.typewriter(line, LINES[idx], 32);
        if (idx == LINES.length - 1) next.setText("Begin Chapter 1 ▶");
    }
}
