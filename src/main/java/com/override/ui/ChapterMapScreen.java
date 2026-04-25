package com.override.ui;

import com.override.Main;
import com.override.model.GameState;
import com.override.service.SaveService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Chapter selection / progress hub.
 *
 * Shows the four major chapters plus the final mission. Only the current
 * unlocked chapter is selectable; completed chapters get a checkmark.
 *
 * In the prototype only Chapter 1 is fully implemented — the others stub
 * out to a "coming soon" dialog so the menu structure is wired correctly.
 */
public class ChapterMapScreen {

    private static final String[][] CHAPTERS = {
        { "1", "The Silent Classroom",  "Education dependency",   "1" },
        { "2", "Harvest Protocol",      "Agricultural dependency","0" },
        { "3", "Mercy Index",           "AI-controlled healthcare","0" },
        { "4", "Codeblind",             "Loss of real coding skill","0" },
        { "F", "Override Core",         "Final rescue mission",   "0" }
    };

    public Parent build() {
        Label title = UIFactory.title("Chapter Map");
        Label sub = UIFactory.subtitle("The world is bigger than you remember.");

        VBox list = new VBox(12);
        list.setAlignment(Pos.CENTER);

        int unlocked = GameState.get().getChapterUnlocked();
        int completed = GameState.get().getChapterCompleted();

        for (String[] ch : CHAPTERS) {
            int chNum = "F".equals(ch[0]) ? 5 : Integer.parseInt(ch[0]);
            boolean isUnlocked = chNum <= unlocked;
            boolean isDone = chNum <= completed;
            list.getChildren().add(buildRow(ch[0], ch[1], ch[2], isUnlocked, isDone, chNum));
        }

        Button save = UIFactory.secondary("Save");
        save.setOnAction(e -> {
            SaveService.save();
            new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "Game saved."
            ).showAndWait();
        });
        Button menu = UIFactory.secondary("Main Menu");
        menu.setOnAction(e -> Main.switchScene(new MainMenuScreen().build()));

        HBox actions = new HBox(12, save, menu);
        actions.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, title, sub, list, actions);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));

        VBox wrap = new VBox(UIFactory.hud(), root);
        wrap.setAlignment(Pos.TOP_CENTER);

        return UIFactory.backdrop(wrap);
    }

    private HBox buildRow(String label, String name, String theme,
                          boolean unlocked, boolean done, int chNum) {
        Label num = new Label(label);
        num.getStyleClass().add("ch-num");

        Label nm = new Label(name);
        nm.getStyleClass().add("ch-name");

        Label th = new Label(theme);
        th.getStyleClass().add("ch-theme");

        VBox text = new VBox(4, nm, th);

        Label status = new Label(done ? "✓ COMPLETED" : (unlocked ? "▶ AVAILABLE" : "🔒 LOCKED"));
        status.getStyleClass().add(
            done ? "ch-done" : (unlocked ? "ch-open" : "ch-locked")
        );

        Button play = UIFactory.compact(done ? "Replay" : "Play");
        play.setDisable(!unlocked);
        play.setOnAction(e -> {
            if (chNum == 1) {
                Main.switchScene(new ChapterOneScreen().build());
            } else {
                new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Chapter " + label + ": " + name + "\n\n" +
                    "Not implemented in this prototype build.\n" +
                    "The chapter map and progression logic are wired — the gameplay screens for Chapters 2-4 + final mission are scaffolded for the team to fill in next."
                ).showAndWait();
            }
        });

        HBox row = new HBox(20, num, text, status, play);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 24, 14, 24));
        row.getStyleClass().add("ch-row");
        if (!unlocked) row.getStyleClass().add("ch-row-locked");
        row.setMinWidth(820);
        row.setMaxWidth(820);
        return row;
    }
}
