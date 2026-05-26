package com.override.chapter1;

import com.override.Main;
import com.override.game.minigames.KernelPanicGame;
import com.override.game.minigames.MiniGame;
import com.override.game.minigames.MiniGameLauncher;
import com.override.game.minigames.MiniGameResult;
import com.override.game.minigames.SnakeGame;
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
 * Chapter 1 — The Silent Classroom.
 *
 * Hub view of "rooms" the player enters in order:
 * <ol>
 *   <li>Lecture Hall A — dialogue + lore</li>
 *   <li>Logic Lab — <b>Kernel Panic</b> mini-game (Chapter 1, game #1)</li>
 *   <li>Practice Terminal — <b>Syntax Snake</b> mini-game (Chapter 1, game #2),
 *       unlocked after Kernel Panic so the two mini-games play in sequence</li>
 * </ol>
 * Recovering the access fragment requires all three; only chapter completion
 * persists in {@link GameState}, the room flags live for the screen's lifetime.
 */
public class ChapterOneScreen {

    private boolean lectureHallDone = false;   // dialogue + lore
    private boolean labDone = false;           // Kernel Panic
    private boolean terminalDone = false;      // Syntax Snake

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

        Button b1 = roomButton("Lecture Hall A", "Talk to students and a teacher.",        lectureHallDone, false);
        Button b2 = roomButton("Logic Lab",      "Kernel Panic — patch the failing kernel.", labDone, false);
        Button b3 = roomButton("Practice Terminal",
                labDone ? "Syntax Snake — drive the blinking cursor and harvest knowledge bits."
                        : "LOCKED — complete the Logic Lab first.",
                terminalDone, !labDone);
        b1.setOnAction(e -> openLectureHall());
        b2.setOnAction(e -> openLab());
        b3.setOnAction(e -> openTerminal());

        // Finishing the chapter is gated behind all three rooms above.
        Button leave = roomButton("Recover the Access Fragment", "Leave the building — ends the chapter.", false, false);
        boolean allDone = lectureHallDone && labDone && terminalDone;
        leave.setDisable(!allDone);
        leave.setOnAction(e -> finishChapter());

        VBox rooms = new VBox(10, b1, b2, b3, leave);
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

    private Button roomButton(String title, String hint, boolean done, boolean locked) {
        String prefix = locked ? "🔒 " : (done ? "✓ " : "▶ ");
        Button b = new Button(prefix + title + "  —  " + hint);
        b.getStyleClass().add(done ? "room-done" : "room-open");
        b.setMinWidth(640);
        if (locked) b.setDisable(true);
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
        // Logic Lab mini-game #1: Kernel Panic.
        launchMiniGame(new KernelPanicGame(), result -> {
            applyMiniGameResult(result);
            labDone = true;
            Main.switchScene(build());
        });
    }

    private void openTerminal() {
        // Practice Terminal mini-game #2: Syntax Snake. Only reachable once the
        // Logic Lab is done (the button is locked above), so the two mini-games
        // always play in sequence: Kernel Panic → Snake.
        if (!labDone) return;
        launchMiniGame(new SnakeGame(), result -> {
            applyMiniGameResult(result);
            terminalDone = true;
            Main.switchScene(build());
        });
    }

    private void launchMiniGame(MiniGame game, com.override.game.minigames.ResultListener listener) {
        MiniGameLauncher.launch(Main.getStage(), game, listener);
    }

    /**
     * Apply a mini-game result to the player and global state:
     * XP and coins for a strong run, dependency for using Astra, independence
     * bonus for solving it without leaning on the assist.
     */
    private void applyMiniGameResult(MiniGameResult result) {
        GameState gs = GameState.get();
        gs.getPlayer().addXp(result.xpEarned());
        gs.increaseDependency(result.dependencyUsed());
        gs.addCoins(Math.max(5, result.score() / 100));
        if (result.dependencyUsed() == 0) gs.addIndependentXp(15);
    }

    private void finishChapter() {
        GameState.get().completeChapter(1);
        SaveService.save();
        Main.switchScene(new EndingScreen(
            "Chapter 1 complete",
            "You leave the building with the first access fragment in your pocket. "
          + "On the way out, a student looks up from her terminal and asks, "
          + "“Wait — how did you know what to type?”\n\n"
          + "You don't answer. You're already thinking about the next sector.",
            () -> Main.switchScene(new ChapterMapScreen().build())
        ).build());
    }
}
