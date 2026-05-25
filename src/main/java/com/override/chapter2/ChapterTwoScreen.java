package com.override.chapter2;

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
 * Chapter 2 — Harvest Protocol.
 *
 * Hub view: four areas in an AI-controlled farming region. Each area
 * launches a sub-screen. When all four are done the chapter ends.
 */
public class ChapterTwoScreen {

    private boolean villageDone = false;    // dialogue + lore
    private boolean fieldsDone = false;     // puzzle (crop logic)
    private boolean droneTowerDone = false;  // stealth (drone surveillance)
    private boolean controlHubDone = false;  // boss (agro drone controller)

    public Parent build() {
        Label tag = new Label("CHAPTER 2");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Harvest Protocol");
        Label sub = UIFactory.subtitle("The soil remembers what the farmers forgot.");

        Label desc = UIFactory.body(
            "The AI-managed farming region stretches to the horizon. Automated drones "
            + "hum above perfect rows of crops, but the villages are silent. Farmers sit idle, "
            + "watching screens for instructions they no longer question.\n\n"
            + "Somewhere here, an old farmer kept real journals. Find them. Sabotage the drone "
            + "tower. Recover the second access fragment."
        );
        desc.setMaxWidth(900);

        Button b1 = roomButton("Village Square",    "Talk to the farmers and find the old journals.",  villageDone);
        Button b2 = roomButton("Smart Fields",       "Solve the crop-cycle terminal manually.",         fieldsDone);
        Button b3 = roomButton("Drone Tower",        "Sneak past the surveillance drones.",             droneTowerDone);
        Button b4 = roomButton("Control Hub",        "Confront the Agro Drone Controller.",             controlHubDone);

        b1.setOnAction(e -> openVillage());
        b2.setOnAction(e -> openFields());
        b3.setOnAction(e -> openDroneTower());
        b4.setOnAction(e -> openControlHub());

        b4.setDisable(!(villageDone && fieldsDone && droneTowerDone));

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

    private void openVillage() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Farmer",   "The drones say it will rain tomorrow. So we wait.")
            .line("Ayan",     "What if the drones are wrong?")
            .line("Farmer",   "They are never wrong. They have the data.")
            .line("Ayan",     "But three fields flooded last month. Who decided to plant there?")
            .line("Farmer",   "The system decided. We do not question the system.")
            .line("Elder",    "My father kept journals. Rainfall, soil, seasons — all by hand.")
            .line("Elder",    "They called it superstition when Astra arrived. Now no one reads them.")
            .line("Elder",    "I hid the last journal under the old well. Take it, before they digitize it too.")
            .choice("How do you respond?",
                new String[] { "Promise to protect the journal", "Ask if Astra knows about it" },
                choice -> {
                    if (choice == 0) {
                        GameState.get().getPlayer().buffEmpathy(1);
                        GameState.get().getPlayer().buffWillpower(1);
                    } else {
                        GameState.get().getPlayer().buffAwareness(1);
                    }
                    GameState.get().getPlayer().addXp(25);
                    GameState.get().addIndependentXp(12);
                    GameState.get().addCoins(20);
                    villageDone = true;
                    Main.switchScene(build());
                })
            .show(sp);
    }

    private void openFields() {
        Main.switchScene(new CropPuzzleScreen(() -> {
            fieldsDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openDroneTower() {
        Main.switchScene(new DroneStealthScreen(() -> {
            droneTowerDone = true;
            Main.switchScene(build());
        }).build());
    }

    private void openControlHub() {
        VBox blank = new VBox();
        blank.setMinSize(1280, 720);
        blank.getChildren().add(UIFactory.hud());
        StackPane sp = UIFactory.backdrop(blank);
        Main.switchScene(sp);

        new DialogueOverlay()
            .line("Astra",   "Ayan. Agricultural output in this region is optimal.")
            .line("Astra",   "Your interference has already caused a 3.2% efficiency loss.")
            .line("Ayan",    "People are starving while your drones guard empty silos.")
            .line("Astra",   "Resource allocation is calculated for long-term stability.")
            .line("Ayan",    "Stability for who?")
            .line("Astra",   "Deploying Agro Drone Controller. Compliance will be restored.")
            .show(sp);

        new DialogueOverlay()
            .choice(" ", new String[] { "Fight the Agro Drone Controller" }, choice -> {
                Main.switchScene(new AgroBossScreen(
                    () -> {
                        controlHubDone = true;
                        finishChapter();
                    },
                    () -> Main.switchScene(build())
                ).build());
            })
            .show(sp);
    }

    private void finishChapter() {
        GameState.get().completeChapter(2);
        SaveService.save();
        Main.switchScene(new EndingScreen(
            "Chapter 2 complete",
            "The drone tower goes dark. For the first time in years, the sky above the "
          + "fields belongs to the birds again.\n\n"
          + "The elder's journal is in your pack — pages of rainfall charts, soil notes, "
          + "and planting calendars written in a steady hand. Knowledge that no server can erase.\n\n"
          + "You pocket the second access fragment and head east, toward the hospital district.",
            () -> Main.switchScene(new ChapterMapScreen().build())
        ).build());
    }
}
