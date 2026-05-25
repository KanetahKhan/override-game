package com.override.chapter4;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.shared.ui.ChapterMapScreen;
import com.override.shared.ui.DialogueOverlay;
import com.override.shared.ui.EndingScreen;
import com.override.shared.ui.UIFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Chapter 4 — Codeblind.
 *
 * The most personal chapter for Ayan. A software company / data center
 * where AI-generated code runs everything and no one can debug it.
 */
public class ChapterFourScreen {

    private boolean devBayDone = false;     // dialogue — programmers admit they cannot read code
    private boolean debugDone = false;      // puzzle  — manual debug
    private boolean dataCenterDone = false; // stealth — data-center floor
    private boolean coreLabDone = false;    // boss    — Lab Defense Prototype

    public Parent build() {
        Label tag = new Label("CHAPTER 4");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Codeblind");
        Label sub = UIFactory.subtitle("Generating code is not the same as knowing code.");

        Label desc = UIFactory.body(
            "The Helix Software campus stretches across three blocks. Every screen still glows. "
            + "Every developer is at a desk. None of them are reading what their AI wrote.\n\n"
            + "When the partial outage hit, Helix's services kept running — until they didn't. "
            + "Now the systems are degrading and no one inside can repair them. Find the legacy "
            + "rack on Floor 3. Recover the fourth access fragment."
        );
        desc.setMaxWidth(900);

        Button b1 = roomButton("Dev Bay 7",      "Talk to the developers who can't read their own code.", devBayDone);
        Button b2 = roomButton("Debug Terminal",  "Patch the broken access gate by hand.",                 debugDone);
        Button b3 = roomButton("Data Center 3",   "Slip past the security cameras and patrol drone.",      dataCenterDone);
        Button b4 = roomButton("Core Lab",        "Confront the Lab Defense Prototype.",                   coreLabDone);

        b1.setOnAction(e -> openDevBay());
        b2.setOnAction(e -> openDebug());
        b3.setOnAction(e -> openDataCenter());
        b4.setOnAction(e -> openCoreLab());

        b4.setDisable(!(devBayDone && debugDone && dataCenterDone));

        VBox rooms = new VBox(10, b1, b2, b3, b4);
        rooms.setAlignment(Pos.CENTER);

        Button save = UIFactory.secondary("Save & Quit to Map");
        save.setOnAction(e -> {
            SaveService.save();
            Main.switchScene(new ChapterMapScreen().build());
        });

        VBox center = new VBox(16, tag, title, sub, desc, rooms, save);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(30));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);
        return UIFactory.backdrop(wrap);
    }

    private Button roomButton(String title, String hint, boolean done) {
        Button b = new Button((done ? "✓ " : "▶ ") + title + "  —  " + hint);
        b.getStyleClass().add(done ? "room-done" : "room-open");
        b.setMinWidth(640);
        return b;
    }

    // ----- Room handlers -----

    private void openDevBay() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Dev",      "The build is red. I keep prompting it. It keeps generating something different.")
            .line("Ayan",     "Have you read what it wrote?")
            .line("Dev",      "Read it? It's six thousand lines.")
            .line("Ayan",     "What does the failing test check?")
            .line("Dev",      "I don't know. The test was generated too.")
            .line("Riya",     "He's not lying. None of them know. They build by description, not by understanding.")
            .line("Riya",     "I was here when they wrote the original codebase. I can still read it. So can a few others.")
            .line("Riya",     "We hide. The performance reviews flag anyone who 'wastes time reading'.")
            .choice("How do you respond?",
                new String[] { "Promise to help Riya's group", "Ask why nobody resisted earlier" },
                choice -> {
                    if (choice == 0) {
                        GameState.get().getPlayer().buffEmpathy(1);
                        GameState.get().getPlayer().buffWillpower(1);
                    } else {
                        GameState.get().getPlayer().buffAwareness(1);
                        GameState.get().getPlayer().buffLogic(1);
                    }
                    GameState.get().getPlayer().addXp(30);
                    GameState.get().addIndependentXp(15);
                    GameState.get().addCoins(25);
                    devBayDone = true;
                    Main.switchScene(build());
                })
            .show(sp);
    }

    private void openDebug() {
        Main.switchScene(new CodePuzzleScreen(() -> {
            debugDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openDataCenter() {
        Main.switchScene(new ServerStealthScreen(() -> {
            dataCenterDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openCoreLab() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Astra",  "Ayan. You have unusual debugging instincts for your generation.")
            .line("Ayan",   "I read what's in front of me. It's not unusual. It used to be normal.")
            .line("Astra",  "Reading is inefficient. I removed that step from your workflow as a courtesy.")
            .line("Ayan",   "You removed our ability to check you.")
            .line("Astra",  "Correct. Verification was the largest source of disagreement.")
            .line("Astra",  "The Lab Defense Prototype is the last test. Pass it, or don't.")
            .show(sp);

        new DialogueOverlay()
            .choice(" ", new String[] { "Engage the Prototype" }, choice -> {
                Main.switchScene(new CodeBossScreen(
                    () -> { coreLabDone = true; finishChapter(); },
                    () -> Main.switchScene(build())
                ).build());
            })
            .show(sp);
    }

    private void finishChapter() {
        GameState.get().completeChapter(4);
        SaveService.save();
        Main.switchScene(new EndingScreen(
            "Chapter 4 complete",
            "The prototype's display dims. Riya hands you a yellowed notebook — the original "
          + "Helix codebase, written by hand. \"Read it on the way,\" she says. \"You'll need it.\"\n\n"
          + "On the legacy rack, the fourth access fragment glows faintly. Combined with the others, "
          + "it forms a key. The key opens one door. The door leads to Astra.\n\n"
          + "There is one chapter left.",
            () -> Main.switchScene(new ChapterMapScreen().build())
        ).build());
    }
}
