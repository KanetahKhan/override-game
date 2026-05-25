package com.override.shared.ui;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.chapter1.ChapterOneScreen;
import com.override.chapter2.ChapterTwoScreen;
import com.override.chapter3.ChapterThreeScreen;
import com.override.chapter4.ChapterFourScreen;
import com.override.chapter5.FinalMissionScreen;
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
 * All five entries (Ch 1-4 + Final) route to real gameplay screens.
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
            switch (chNum) {
                case 1 -> Main.switchScene(new ChapterOneScreen().build());
                case 2 -> Main.switchScene(new ChapterTwoScreen().build());
                case 3 -> Main.switchScene(new ChapterThreeScreen().build());
                case 4 -> Main.switchScene(new ChapterFourScreen().build());
                case 5 -> Main.switchScene(new FinalMissionScreen().build());
                default -> new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "Chapter " + label + ": " + name + "\n\nUnknown chapter."
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
