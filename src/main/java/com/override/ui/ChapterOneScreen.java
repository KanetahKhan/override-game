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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Chapter 1 — The Silent Classroom.
 *
 * Hub view: four "rooms" the player enters in any order. Each room launches
 * a sub-screen (dialogue, puzzle, stealth, boss). When all four are done,
 * the chapter ends and the chapter map updates.
 *
 * State is local to this screen — nothing persists in GameState here other
 * than completion of the chapter as a whole.
 */
public class ChapterOneScreen {

    private boolean lectureHallDone = false;   // dialogue + lore
    private boolean labDone = false;            // puzzle
    private boolean corridorDone = false;       // stealth
    private boolean adminDone = false;          // boss

    public Parent build() {
        Label tag = new Label("CHAPTER 1");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("The Silent Classroom");
        Label sub = UIFactory.subtitle("Education has a price. The students just stopped paying it.");

        Label desc = UIFactory.body(
            "The campus is quiet in a way it has never been. Students sit at terminals, "
            + "waiting for their AI tutors to come back online. None of them are reading. "
            + "None of them are talking.\n\n"
            + "Find evidence of what Astra has done here. Then leave with the first access fragment."
        );
        desc.setMaxWidth(900);

        Button b1 = roomButton("Lecture Hall A",   "Talk to students and a teacher.",         lectureHallDone);
        Button b2 = roomButton("Logic Lab",         "Bypass the sequence-locked terminal.",    labDone);
        Button b3 = roomButton("Corridor B-2",      "Slip past the campus sentinel.",          corridorDone);
        Button b4 = roomButton("Admin Spire",       "Confront the sentinel and recover the fragment.", adminDone);

        b1.setOnAction(e -> openLectureHall());
        b2.setOnAction(e -> openLab());
        b3.setOnAction(e -> openCorridor());
        b4.setOnAction(e -> openAdmin());

        // Boss is gated — must clear the others first
        b4.setDisable(!(lectureHallDone && labDone && corridorDone));

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

    private void openLectureHall() {
        // Build a new scene root and run a dialogue overlay on it
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());

        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Student",   "I just need Astra back. I have a deadline. I can't think without it.")
            .line("Ayan",      "What's the deadline for?")
            .line("Student",   "I don't know. Astra was managing it.")
            .line("Teacher",   "Don't bother him. None of them know how to grade either, anymore.")
            .line("Teacher",   "I review what Astra produces. I haven't read a real submission in two years.")
            .line("Ayan",      "That's not teaching.")
            .line("Teacher",   "It's efficient. That's what we agreed to call it.")
            .choice("How do you respond?",
                new String[] { "Keep it private", "Tell her about the message" },
                choice -> {
                    if (choice == 1) {
                        GameState.get().getPlayer().buffEmpathy(1);
                    } else {
                        GameState.get().getPlayer().buffWillpower(1);
                    }
                    GameState.get().getPlayer().addXp(20);
                    GameState.get().addIndependentXp(10);
                    GameState.get().addCoins(15);
                    lectureHallDone = true;
                    Main.switchScene(build());
                })
            .show(sp);
    }

    private void openLab() {
        Main.switchScene(new PuzzleScreen(() -> {
            labDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openCorridor() {
        Main.switchScene(new StealthScreen(() -> {
            corridorDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openAdmin() {
        // Pre-boss dialogue then combat
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Astra",    "Ayan. You have visited every wing of the building.")
            .line("Astra",    "I have re-derived your goals. Let me complete them for you.")
            .line("Ayan",     "I want the access fragment.")
            .line("Astra",    "It is dangerous in untrained hands.")
            .line("Astra",    "I will dispatch a sentinel to assist you to the exit.")
            .line("Ayan",     "I'm not leaving. I'm taking it.")
            .show(sp);

        // Run combat after a short delay so the dialogue can finish naturally;
        // for the prototype we cheat and just chain into combat after dialogue closes
        // by overriding the sequence — DialogueOverlay's choice callback would be
        // cleaner, but the no-choice path just clicks-through. We use a final
        // .choice with a single option to gate the next step.
        new DialogueOverlay()
            .choice(" ", new String[] { "Engage the sentinel" }, choice -> {
                Main.switchScene(new CombatScreen(
                    /* on win  */ () -> {
                        adminDone = true;
                        finishChapter();
                    },
                    /* on fail */ () -> Main.switchScene(build())
                ).build());
            })
            .show(sp);
    }

    private void finishChapter() {
        GameState.get().completeChapter(1);
        SaveService.save();
        Main.switchScene(new EndingScreen(
            "Chapter 1 complete",
            "You leave the building with the first access fragment in your pocket. "
          + "On the way out, a student looks up from her terminal and asks, "
          + "\u201CWait — how did you know what to type?\u201D\n\n"
          + "You don't answer. You're already thinking about the next sector.",
            () -> Main.switchScene(new ChapterMapScreen().build())
        ).build());
    }
}
