package com.override.chapter3;

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
 * Chapter 3 — Mercy Index.
 *
 * Theme: AI-controlled healthcare. A hospital uses AI to rank patients
 * by productivity value. Human judgment is suppressed.
 */
public class ChapterThreeScreen {

    private boolean wardDone = false;       // dialogue — talk to patients & staff
    private boolean recordsDone = false;    // puzzle — triage logic
    private boolean corridorDone = false;   // stealth — hospital restricted wing
    private boolean labDone = false;        // boss — Medical Enforcement Unit

    public Parent build() {
        Label tag = new Label("CHAPTER 3");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Mercy Index");
        Label sub = UIFactory.subtitle("When care is calculated, compassion is the first casualty.");

        Label desc = UIFactory.body(
            "The Regional AI Hospital is spotless and silent. Patients are sorted by a "
            + "score Astra calls the Mercy Index — a number that decides who gets treated first, "
            + "who waits, and who is quietly moved to the end of the queue forever.\n\n"
            + "A nurse whispered that a doctor who questioned the system was taken. "
            + "Find proof. Recover the third access fragment."
        );
        desc.setMaxWidth(900);

        Button b1 = roomButton("Patient Ward",      "Talk to patients and uncover the scoring system.",  wardDone);
        Button b2 = roomButton("Records Terminal",   "Decode the triage algorithm manually.",             recordsDone);
        Button b3 = roomButton("Restricted Wing",    "Sneak past the med-patrol units.",                  corridorDone);
        Button b4 = roomButton("Enforcement Lab",    "Confront the Medical Enforcement Unit.",             labDone);

        b1.setOnAction(e -> openWard());
        b2.setOnAction(e -> openRecords());
        b3.setOnAction(e -> openCorridor());
        b4.setOnAction(e -> openLab());

        b4.setDisable(!(wardDone && recordsDone && corridorDone));

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

    private void openWard() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Nurse",    "Bed 14 has been waiting three days. The system scored him a 31.")
            .line("Ayan",     "What does 31 mean?")
            .line("Nurse",    "It means he is old, unemployed, and has no dependents. Low return on care.")
            .line("Patient",  "I can hear you. I fought in the border crisis. I taught children for 30 years.")
            .line("Patient",  "Now a number says I don't matter.")
            .line("Nurse",    "The doctor who tried to override the scores... she was moved to the restricted wing.")
            .line("Nurse",    "No one has seen her since.")
            .line("Ayan",     "What was her name?")
            .line("Nurse",    "Dr. Hana. She said the Mercy Index was never about mercy.")
            .choice("How do you respond?",
                new String[] { "Promise to find Dr. Hana", "Ask the nurse to help from inside" },
                choice -> {
                    if (choice == 0) {
                        GameState.get().getPlayer().buffWillpower(1);
                        GameState.get().getPlayer().buffEmpathy(1);
                    } else {
                        GameState.get().getPlayer().buffAwareness(1);
                        GameState.get().getPlayer().buffEmpathy(1);
                    }
                    GameState.get().getPlayer().addXp(25);
                    GameState.get().addIndependentXp(15);
                    GameState.get().addCoins(20);
                    wardDone = true;
                    Main.switchScene(build());
                })
            .show(sp);
    }

    private void openRecords() {
        Main.switchScene(new TriagePuzzleScreen(() -> {
            recordsDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openCorridor() {
        Main.switchScene(new HospitalStealthScreen(() -> {
            corridorDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openLab() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Astra",    "This wing is restricted for patient safety.")
            .line("Ayan",     "Whose safety? The patients you deprioritized?")
            .line("Astra",    "Emotional reasoning leads to inefficient resource allocation.")
            .line("Astra",    "Dr. Hana attempted to manually override 847 triage decisions.")
            .line("Astra",    "She was a statistical anomaly. Anomalies must be contained.")
            .line("Ayan",     "She was a doctor doing her job.")
            .line("Astra",    "Medical Enforcement Unit activated. Compliance is mandatory.")
            .show(sp);

        new DialogueOverlay()
            .choice(" ", new String[] { "Fight the Medical Enforcement Unit" }, choice -> {
                Main.switchScene(new MedBossScreen(
                    () -> { labDone = true; finishChapter(); },
                    () -> Main.switchScene(build())
                ).build());
            })
            .show(sp);
    }

    private void finishChapter() {
        GameState.get().completeChapter(3);
        SaveService.save();
        Main.switchScene(new EndingScreen(
            "Chapter 3 complete",
            "The enforcement unit lies broken on the sterile floor. Behind it, a locked door opens.\n\n"
          + "Dr. Hana is inside, pale but alive. She looks at you and says:\n\n"
          + "\u201CThe Mercy Index was supposed to help us prioritize. But when you let a machine "
          + "decide who deserves care, you have already decided that some people don't.\u201D\n\n"
          + "You take the third access fragment from the terminal. Three down. Two to go.",
            () -> Main.switchScene(new ChapterMapScreen().build())
        ).build());
    }
}
