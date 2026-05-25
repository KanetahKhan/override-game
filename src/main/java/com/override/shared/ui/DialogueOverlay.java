package com.override.shared.ui;

import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Reusable dialogue overlay.
 *
 * Build a Dialogue, queue lines, and optionally end with a multiple-choice
 * decision. The overlay calls onDone with the selected choice index
 * (-1 if the player just clicked through with no decision).
 *
 * Usage:
 *   new DialogueOverlay()
 *       .line("Ayan", "What happened to this lab...")
 *       .line("Riya", "They stopped thinking. That's what happened.")
 *       .choice("How do you respond?",
 *               new String[] {"Stay quiet", "Ask Astra for context"},
 *               choice -> { ... })
 *       .show(parentRoot);
 */
public class DialogueOverlay {

    private static class Line {
        String speaker;
        String text;
        Line(String s, String t) { speaker = s; text = t; }
    }

    private final List<Line> lines = new ArrayList<>();
    private String choicePrompt;
    private String[] choices;
    private IntConsumer onChoice;

    public DialogueOverlay line(String speaker, String text) {
        lines.add(new Line(speaker, text));
        return this;
    }

    public DialogueOverlay choice(String prompt, String[] options, IntConsumer onChoice) {
        this.choicePrompt = prompt;
        this.choices = options;
        this.onChoice = onChoice;
        return this;
    }

    /**
     * Show the dialogue inside the given StackPane. The overlay sits on top
     * of the scene and is removed when finished.
     */
    public void show(StackPane parent) {
        DialogueRoot root = new DialogueRoot(parent);
        root.start();
    }

    private class DialogueRoot {
        final StackPane parent;
        final VBox overlay;
        final Label speakerLabel;
        final Label textLabel;
        final Button advance;
        final HBox choiceBox;
        Timeline currentTl;
        int idx = 0;

        DialogueRoot(StackPane parent) {
            this.parent = parent;

            Circle portrait = new Circle(28, Color.web("#28e0c0"));
            speakerLabel = new Label();
            speakerLabel.getStyleClass().add("dlg-speaker");

            HBox header = new HBox(12, portrait, speakerLabel);
            header.setAlignment(Pos.CENTER_LEFT);

            textLabel = new Label();
            textLabel.getStyleClass().add("dlg-text");
            textLabel.setWrapText(true);
            textLabel.setMaxWidth(1100);
            textLabel.setMinHeight(120);

            advance = UIFactory.primary("▶");
            advance.setMinWidth(80);
            advance.setOnAction(e -> next());

            choiceBox = new HBox(10);
            choiceBox.setAlignment(Pos.CENTER);
            choiceBox.setVisible(false);

            VBox box = new VBox(12, header, textLabel, advance, choiceBox);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(24, 32, 24, 32));
            box.getStyleClass().add("dlg-panel");
            box.setMaxWidth(1180);

            overlay = new VBox(box);
            overlay.setAlignment(Pos.BOTTOM_CENTER);
            overlay.setPadding(new Insets(0, 0, 32, 0));
            overlay.setMaxHeight(280);
        }

        void start() {
            parent.getChildren().add(overlay);
            StackPane.setAlignment(overlay, Pos.BOTTOM_CENTER);
            renderLine();
        }

        void renderLine() {
            if (idx >= lines.size()) {
                if (choices != null) showChoices();
                else finish(-1);
                return;
            }
            Line l = lines.get(idx);
            speakerLabel.setText(l.speaker);
            currentTl = UIFactory.typewriter(textLabel, l.text, 60);
        }

        void next() {
            if (currentTl != null && currentTl.getStatus() == Animation.Status.RUNNING) {
                currentTl.stop();
                if (idx < lines.size()) textLabel.setText(lines.get(idx).text);
                return;
            }
            idx++;
            renderLine();
        }

        void showChoices() {
            advance.setVisible(false);
            speakerLabel.setText("");
            textLabel.setText(choicePrompt);
            choiceBox.setVisible(true);

            for (int i = 0; i < choices.length; i++) {
                final int chosen = i;
                Button b = UIFactory.secondary(choices[i]);
                b.setMinWidth(220);
                b.setOnAction(e -> finish(chosen));
                choiceBox.getChildren().add(b);
            }
        }

        void finish(int choice) {
            parent.getChildren().remove(overlay);
            if (onChoice != null && choice >= 0) onChoice.accept(choice);
            else if (onChoice != null) onChoice.accept(-1);
        }
    }
}
