package com.override;

import com.override.shared.model.GameState;
import com.override.shared.ui.MainMenuScreen;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Entry point and global scene manager for Override.
 *
 * Holds a single Stage and swaps Scenes between menus, gameplay, dialogues,
 * puzzles, combat, etc. Every screen class returns a Parent root which we
 * wrap in a Scene here so styling stays consistent in one place.
 */
public class Main extends Application {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Override — The Last Real Mind");
        stage.setResizable(false);

        GameState.init();
        switchScene(new MainMenuScreen().build());

        stage.show();
    }

    /** Replace the current scene root with a fade-in transition. */
    public static void switchScene(Parent root) {
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add(
            Main.class.getResource("/styles/main.css").toExternalForm()
        );
        stage.setScene(scene);

        FadeTransition fade = new FadeTransition(Duration.millis(280), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    public static Stage getStage() {
        return stage;
    }
}
