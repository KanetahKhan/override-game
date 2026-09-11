package com.override.chapter1;

import com.almasb.fxgl.app.FXGLPane;
import com.almasb.fxgl.app.GameApplication;
import com.override.Main;
import com.override.game.minigames.ComposeGame;
import com.override.game.minigames.ChiptuneSfx;
import com.override.game.minigames.KernelPanicGame;
import com.override.game.minigames.MiniGame;
import com.override.game.minigames.MiniGameLauncher;
import com.override.game.minigames.MiniGameResult;
import com.override.game.minigames.SnakeGame;
import com.override.prototype.SilentClassroomHost;
import com.override.prototype.SilentClassroomPreferences;
import com.override.prototype.SilentClassroomPrototypeApp;
import com.override.prototype.SilentClassroomResult;
import com.override.prototype.SilentClassroomSession;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.shared.ui.ChapterMapScreen;
import com.override.shared.ui.DialogueOverlay;
import com.override.shared.ui.EndingScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Chapter 1 host for the embedded FXGL Silent Classroom stealth floor.
 *
 * <p>The host deliberately owns campaign mutations and modal JavaFX windows;
 * the FXGL application owns only one run's hallway simulation.</p>
 */
public final class ChapterOneScreen implements SilentClassroomHost {

    private SilentClassroomSession session;
    private SilentClassroomPrototypeApp game;
    private StackPane root;
    private boolean embeddedRunning;
    private SilentClassroomRewardBank rewardBank;
    private final SilentClassroomPreferences preferences =
            SilentClassroomPreferences.shared();

    public Parent build() {
        // Replays may improve the persisted best score and Insight tier, but
        // must not repeatedly grant XP, coins, or independent-thinking XP.
        rewardBank = new SilentClassroomRewardBank(
                GameState.get().getChapterCompleted() < 1
        );
        ChiptuneSfx.setMasterVolume(preferences.masterVolume());
        // Exploration is safe until the Professor tutorial starts the mission.
        session = SilentClassroomSession.createPaused();

        game = new SilentClassroomPrototypeApp(session, this);
        FXGLPane gamePane = GameApplication.embeddedLaunch(game);
        embeddedRunning = true;

        gamePane.setMinSize(Main.WIDTH, Main.HEIGHT);
        gamePane.setPrefSize(Main.WIDTH, Main.HEIGHT);
        gamePane.setMaxSize(Main.WIDTH, Main.HEIGHT);

        root = new StackPane(gamePane);
        root.setAlignment(Pos.CENTER);
        root.setMinSize(Main.WIDTH, Main.HEIGHT);
        root.setPrefSize(Main.WIDTH, Main.HEIGHT);
        applyHostFontScale();
        return root;
    }

    @Override
    public void showTutorial(Runnable onBeginMission) {
        new DialogueOverlay()
                .line(
                        "Professor Rahman",
                        "Astra's tutors went silent. The students are waiting for answers "
                                + "they no longer know how to create."
                )
                .line(
                        "Ayan",
                        "I can restore the three manual lesson nodes — Kernel Panic, "
                                + "Syntax Snake, and Human Compose."
                )
                .line(
                        "Professor Rahman",
                        "The Sentinel owns the hallway. Break sight inside a numbered room, "
                                + "close doors behind you, and reach Room 301 after all three lessons."
                )
                .line(
                        "Controls",
                        "Move with " + preferences.movementSummary()
                                + ", interact with "
                                + SilentClassroomPreferences.display(preferences.interactKey())
                                + ", sprint with "
                                + SilentClassroomPreferences.display(preferences.sprintKey())
                                + ", and press Esc to pause or change accessibility settings."
                )
                .choice(
                        "Nine minutes until lockdown. Ready?",
                        new String[]{"Begin mission"},
                        ignored -> onBeginMission.run()
                )
                .show(root);
    }

    @Override
    public void launchMiniGame(
            String gameId,
            Consumer<MiniGameResult> onResult
    ) {
        MiniGame miniGame = switch (gameId) {
            case SilentClassroomSession.KERNEL_PANIC_GAME_ID ->
                    new KernelPanicGame(true, preferences.reducedFlashing());
            case SilentClassroomSession.SNAKE_GAME_ID ->
                    new SnakeGame(true, preferences.reducedFlashing());
            case SilentClassroomSession.COMPOSE_GAME_ID ->
                    new ComposeGame();
            default -> throw new IllegalArgumentException(
                    "Unknown Silent Classroom game: " + gameId
            );
        };

        MiniGameLauncher.launch(
                Main.getStage(),
                miniGame,
                onResult::accept
        );
    }

    @Override
    public void onMiniGameResolved(
            String gameId,
            MiniGameResult result,
            boolean firstClear
    ) {
        GameState state = GameState.get();

        // Assists always have a narrative cost, including on a failed attempt.
        state.increaseDependency(
                result.dependencyUsed()
                        * SilentClassroomSession.DEPENDENCY_PER_ASSIST
        );

        // Bank first-completion rewards locally. They are committed only at
        // Room 301, so restarting a partial run cannot farm campaign state.
        rewardBank.bank(result, firstClear);
    }

    @Override
    public void showPatrolClue(Runnable onClose) {
        new DialogueOverlay()
                .line(
                        "Patrol Archive",
                        "SENTINEL-01 is restricted to the central hallway. "
                                + "Its optical lock needs 0.75 seconds to confirm a target."
                )
                .line(
                        "Professor Rahman — archived note",
                        "We built the Sentinel to protect quiet study. Astra changed its order: "
                                + "protect the silence itself. Now it guards classrooms where "
                                + "students wait for answers instead of making them."
                )
                .line(
                        "Ayan",
                        "So the classrooms are blind spots. This route map will show "
                                + "whether it is north or south of me."
                )
                .choice(
                        "Patrol direction tracking enabled.",
                        new String[]{"Close archive"},
                        ignored -> onClose.run()
                )
                .show(root);
    }

    @Override
    public void showPauseMenu(
            Runnable onResume,
            Runnable onQuit
    ) {
        ButtonType resume = new ButtonType(
                "Resume",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );
        ButtonType quit = new ButtonType(
                "Restart later",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType settings = new ButtonType(
                "Controls & accessibility",
                ButtonBar.ButtonData.OTHER
        );

        while (true) {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "The current hallway run is not saved. Returning to the map will restart it.",
                    resume,
                    settings,
                    quit
            );
            alert.initOwner(Main.getStage());
            alert.setHeaderText("Silent Classroom paused");

            Optional<ButtonType> selection = alert.showAndWait();
            if (selection.isPresent() && selection.get() == settings) {
                showAccessibilitySettings();
                continue;
            }
            if (selection.isPresent() && selection.get() == quit) {
                onQuit.run();
                // Assist consequences survive a restart; uncommitted completion
                // rewards deliberately do not.
                SaveService.save();
                shutdownEmbedded();
                Main.switchScene(new ChapterMapScreen().build());
            } else {
                onResume.run();
            }
            return;
        }
    }

    private void showAccessibilitySettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(Main.getStage());
        dialog.setTitle("Silent Classroom settings");
        dialog.setHeaderText("Controls and accessibility (no score penalty)");

        ComboBox<KeyCode> up = keyChoice(
                SilentClassroomPreferences.UP_KEYS,
                preferences.upKey()
        );
        ComboBox<KeyCode> down = keyChoice(
                SilentClassroomPreferences.DOWN_KEYS,
                preferences.downKey()
        );
        ComboBox<KeyCode> left = keyChoice(
                SilentClassroomPreferences.LEFT_KEYS,
                preferences.leftKey()
        );
        ComboBox<KeyCode> right = keyChoice(
                SilentClassroomPreferences.RIGHT_KEYS,
                preferences.rightKey()
        );
        ComboBox<KeyCode> interact = keyChoice(
                SilentClassroomPreferences.INTERACT_KEYS,
                preferences.interactKey()
        );
        ComboBox<KeyCode> sprint = keyChoice(
                SilentClassroomPreferences.SPRINT_KEYS,
                preferences.sprintKey()
        );
        ComboBox<Double> fontScale = new ComboBox<>();
        fontScale.getItems().setAll(SilentClassroomPreferences.FONT_SCALES);
        fontScale.setValue(preferences.fontScale());

        Slider volume = new Slider(0, 100, preferences.masterVolume() * 100);
        volume.setShowTickLabels(true);
        volume.setMajorTickUnit(25);
        volume.setBlockIncrement(10);

        CheckBox reducedFlashing = new CheckBox("Reduced flashing and motion");
        reducedFlashing.setSelected(preferences.reducedFlashing());
        CheckBox colorBlindSafe = new CheckBox(
                "Patterned, color-blind-safe threat indicators"
        );
        colorBlindSafe.setSelected(preferences.colorBlindSafe());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(9);
        grid.setPadding(new Insets(8));
        int row = 0;
        grid.addRow(row++, new Label("Move up"), up);
        grid.addRow(row++, new Label("Move down"), down);
        grid.addRow(row++, new Label("Move left"), left);
        grid.addRow(row++, new Label("Move right"), right);
        grid.addRow(row++, new Label("Interact"), interact);
        grid.addRow(row++, new Label("Sprint"), sprint);
        grid.addRow(row++, new Label("HUD font scale"), fontScale);
        grid.addRow(row++, new Label("Master volume"), volume);
        grid.add(reducedFlashing, 0, row++, 2, 1);
        grid.add(colorBlindSafe, 0, row, 2, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType apply = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);

        if (dialog.showAndWait().filter(apply::equals).isEmpty()) {
            return;
        }

        preferences.setUpKey(up.getValue());
        preferences.setDownKey(down.getValue());
        preferences.setLeftKey(left.getValue());
        preferences.setRightKey(right.getValue());
        preferences.setInteractKey(interact.getValue());
        preferences.setSprintKey(sprint.getValue());
        preferences.setFontScale(fontScale.getValue());
        preferences.setMasterVolume(volume.getValue() / 100.0);
        preferences.setReducedFlashing(reducedFlashing.isSelected());
        preferences.setColorBlindSafe(colorBlindSafe.isSelected());
        ChiptuneSfx.setMasterVolume(preferences.masterVolume());
        applyHostFontScale();
    }

    private void applyHostFontScale() {
        if (root != null) {
            root.setStyle("-fx-font-size: " + (14 * preferences.fontScale()) + "px;");
        }
    }

    private static ComboBox<KeyCode> keyChoice(
            java.util.List<KeyCode> values,
            KeyCode selected
    ) {
        ComboBox<KeyCode> choice = new ComboBox<>();
        choice.getItems().setAll(values);
        choice.setValue(selected);
        return choice;
    }

    @Override
    public void finishChapter(SilentClassroomResult result) {
        GameState state = GameState.get();
        rewardBank.commit(state);
        state.recordSilentClassroomScore(result.finalScore());
        state.completeChapter(1);
        SaveService.save();

        int best = state.getSilentClassroomBestScore();
        int charges = state.getSilentClassroomInsightCharges();
        String summary = "Terminal points: " + result.miniGamePoints()
                + "\nHallway bonus: +" + result.hallwayBonus()
                + "\nCapture penalty: -" + result.capturePenalty()
                + "\nRun score: " + result.finalScore()
                + "\nBest score: " + best
                + "\nInsight Charges for the Astra boss: " + charges
                + "\n\nThe students watch Ayan leave without asking Astra what to do next.";

        shutdownEmbedded();
        Main.switchScene(new EndingScreen(
                "Silent Classroom complete",
                summary,
                () -> Main.switchScene(
                        new ChapterMapScreen().build()
                )
        ).build());
    }

    private void shutdownEmbedded() {
        if (!embeddedRunning) {
            return;
        }
        embeddedRunning = false;
        GameApplication.embeddedShutdown();
    }
}
