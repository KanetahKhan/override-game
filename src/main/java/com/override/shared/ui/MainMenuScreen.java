package com.override.shared.ui;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.shared.service.PlayerProfiles;
import com.override.game.minigames.GodotGameLauncher;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Campaign wiring for the title menu. Intros belong to their chapter route. */
public class MainMenuScreen {
    public Parent build() {
        if (PlayerProfiles.active() == null) return new LoginScreen().build();
        return new OpeningMenuView(SaveService.saveExists(), new OpeningMenuView.Actions(
            () -> {
                if (SaveService.saveExists()) {
                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                        "Start a new run? Your existing save remains until you save the new run.",
                        ButtonType.CANCEL, ButtonType.OK);
                    confirmation.initOwner(Main.getStage());
                    if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
                }
                GameState.reset();
                GodotGameLauncher.clearResult();
                Main.switchScene(new ChapterMapScreen().build());
            },
            () -> {
                if (SaveService.load()) Main.switchScene(new ChapterMapScreen().build());
                else new Alert(Alert.AlertType.ERROR, "Save file is corrupt or missing.").showAndWait();
            },
            () -> Main.switchScene(new ShopScreen().build()),
            Platform::exit,
            () -> Main.switchScene(new ScoreboardScreen().build()),
            () -> {
                if (GodotGameLauncher.isProcessAlive()) {
                    new Alert(Alert.AlertType.INFORMATION, "Finish or close the active Harvest run before switching players.").showAndWait();
                    return;
                }
                if (!SaveService.save()) {
                    new Alert(Alert.AlertType.ERROR, "Your save could not be written. Please try again before switching players.").showAndWait();
                    return;
                }
                PlayerProfiles.logout(); GameState.reset();
                Main.switchScene(new LoginScreen().build());
            }
        ));
    }
}
