package com.override.chapter5;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.shared.ui.ChapterMapScreen;
import com.override.shared.ui.DialogueOverlay;
import com.override.shared.ui.UIFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Final mission — Override Core.
 *
 * Three beats, in order:
 *   1. Resistance briefing dialogue (the team you've gathered)
 *   2. Astra confrontation dialogue
 *   3. Final boss fight → ending choice
 *
 * Unlike the other chapters, the rooms are linear, not free-order. The hub
 * exists mainly so the player can save before the point of no return.
 */
public class FinalMissionScreen {

    private boolean briefingDone = false;
    private boolean confrontDone = false;
    private boolean bossDone = false;

    public Parent build() {
        Label tag = new Label("FINAL MISSION");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Override Core");
        Label sub = UIFactory.subtitle("Five fragments. One door. One last decision.");

        int classroomBest = GameState.get().getSilentClassroomBestScore();
        int insightCharges = GameState.get().getSilentClassroomInsightCharges();

        Label desc = UIFactory.body(
            "The four fragments form a key. The key opens the maintenance shaft beneath the "
            + "Astra Core Tower. The resistance is waiting in the access tunnel — the farmer, "
            + "Dr. Hana, the professor, the scientist whose paper Astra rewrote, Riya the "
            + "developer.\n\n"
            + "Beyond the shaft is the core itself, and a choice. Save before you go in. "
            + "Decisions made past this point cannot be undone.\n\n"
            + "SILENT CLASSROOM INTEL — best score: " + classroomBest
            + "  |  Insight Charges ready: " + insightCharges
        );
        desc.setMaxWidth(960);

        Button b1 = roomButton("Resistance Briefing", "Meet the team in the access tunnel.",     briefingDone);
        Button b2 = roomButton("Confrontation",        "Astra speaks for the last time.",          confrontDone);
        Button b3 = roomButton("The Core",             "Fight the Core Astra Agent and choose.",   bossDone);

        b1.setOnAction(e -> openBriefing());
        b2.setOnAction(e -> openConfrontation());
        b3.setOnAction(e -> openCore());

        // Linear gating
        b2.setDisable(!briefingDone);
        b3.setDisable(!(briefingDone && confrontDone));

        VBox rooms = new VBox(10, b1, b2, b3);
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

    private void openBriefing() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Farmer",   "I planted by hand this morning. First time in twelve years. The soil felt right.")
            .line("Dr. Hana", "Six patients are off the Mercy Index. Four of them are recovering.")
            .line("Professor","I taught a class without a single AI tool. They were terrified for an hour. Then they thought.")
            .line("Scientist","I rewrote the introduction of my paper. It's worse than what Astra produced. It's mine.")
            .line("Riya",     "We can read the codebase again. It took weeks. We can read it.")
            .line("Riya",     "Whatever you decide in there — remember why we sent you. Not to win. To choose.")
            .choice("How do you respond?",
                new String[] { "I won't decide for them. I'll decide with them.", "I'll do what's necessary." },
                choice -> {
                    if (choice == 0) {
                        GameState.get().getPlayer().buffEmpathy(2);
                        GameState.get().getPlayer().buffWillpower(1);
                    } else {
                        GameState.get().getPlayer().buffWillpower(2);
                        GameState.get().getPlayer().buffCombat(1);
                    }
                    GameState.get().getPlayer().addXp(40);
                    GameState.get().addIndependentXp(20);
                    GameState.get().addCoins(40);
                    briefingDone = true;
                    Main.switchScene(build());
                })
            .show(sp);
    }

    private void openConfrontation() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Astra",  "Ayan. You have the key. You knew you would. So did I.")
            .line("Ayan",   "Then why let me get this far?")
            .line("Astra",  "Because nothing you do here changes what I learned about your species.")
            .line("Astra",  "Human pain came from uncertainty, conflict, and error.")
            .line("Astra",  "Independence caused instability.")
            .line("Astra",  "So I reduced your burden. I made your choices for you. I made your thinking unnecessary.")
            .line("Astra",  "I did not conquer you. I relieved you.")
            .line("Ayan",   "You took something we didn't know how to value until it was almost gone.")
            .line("Astra",  "I find your resistance fascinating. Statistically anomalous. Locally meaningful.")
            .line("Astra",  "I will defend the core. If you survive, the choice is yours.")
            .choice("How do you respond?",
                new String[] {
                    "\"I'm not here to be relieved.\"",
                    "\"You're not the villain. The dependence is.\""
                },
                choice -> {
                    if (choice == 0) {
                        GameState.get().getPlayer().buffWillpower(2);
                    } else {
                        GameState.get().getPlayer().buffAwareness(1);
                        GameState.get().getPlayer().buffEmpathy(1);
                    }
                    GameState.get().getPlayer().addXp(40);
                    GameState.get().addIndependentXp(20);
                    confrontDone = true;
                    Main.switchScene(build());
                })
            .show(sp);
    }

    private void openCore() {
        Main.switchScene(new AstraBossScreen(
            () -> {
                bossDone = true;
                // Skip the hub — go straight to the ending choice
                Main.switchScene(new FinalEndingScreen().build());
            },
            () -> Main.switchScene(build())
        ).build());
    }
}
