package com.override;

import com.override.game.minigames.ChiptuneMusic;
import com.override.shared.model.GameState;
import com.override.shared.ui.MainMenuScreen;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Entry point and global screen manager for Override.
 *
 * <p>Every screen is authored against a fixed 1280x720 design space. We mount
 * that design space inside a single persistent {@link Scene} whose root is a
 * {@link StackPane} viewport, and uniformly scale it to fill the actual window.
 * The app launches in auto full-screen mode; if the window is ever resized, the
 * whole design space (menus, 3D chapter 1, HUD, canvases) scales together.</p>
 */
public class Main extends Application {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static Stage stage;
    private static StackPane designPane;
    private static Scale designScale;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Override — The Last Real Mind");
        stage.setResizable(true);

        // Fixed-size design space in which every screen lays out.
        designPane = new StackPane();
        designPane.setMinSize(WIDTH, HEIGHT);
        designPane.setPrefSize(WIDTH, HEIGHT);
        designPane.setMaxSize(WIDTH, HEIGHT);

        // Scale the whole design space about its center to fit any window.
        designScale = new Scale(1.0, 1.0, WIDTH / 2.0, HEIGHT / 2.0);
        Group scaled = new Group(designPane);
        scaled.getTransforms().add(designScale);

        StackPane viewport = new StackPane(scaled);
        viewport.setStyle("-fx-background-color: #050a14;");

        Scene scene = new Scene(viewport);
        scene.getStylesheets().add(
            Main.class.getResource("/styles/main.css").toExternalForm()
        );
        stage.setScene(scene);

        // ESC must always leave full-screen, no matter which screen is up.
        // JavaFX's built-in full-screen exit needs focus; a scene-level key
        // handler makes it work regardless of screen or playing state.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && stage.isFullScreen()) {
                stage.setFullScreen(false);
                e.consume();
            }
        });

        // Keep the design space scaled to the current window size.
        viewport.widthProperty().addListener((o, a, b) -> applyScale(viewport));
        viewport.heightProperty().addListener((o, a, b) -> applyScale(viewport));

        stage.show();
        stage.setFullScreen(true);
        applyScale(viewport);

        GameState.init();
        ChiptuneMusic.start();
        stage.setOnCloseRequest(e -> ChiptuneMusic.stop());
        switchScene(new MainMenuScreen().build());

        System.out.println("[Main] Stage bounds: " + stage.getX() + "," + stage.getY()
            + " " + stage.getWidth() + "x" + stage.getHeight()
            + " | Scene: " + stage.getScene().getWidth() + "x" + stage.getScene().getHeight()
            + " | Fullscreen: " + stage.isFullScreen());
    }

    /** The window is gone: release the audio line rather than leaving it open. */
    @Override
    public void stop() {
        ChiptuneMusic.stop();
    }

    /** Uniformly scale the 1280x720 design space to fit the viewport. */
    private static void applyScale(StackPane viewport) {
        double w = viewport.getWidth();
        double h = viewport.getHeight();
        if (w <= 0 || h <= 0 || designScale == null) return;
        double s = Math.min(w / WIDTH, h / HEIGHT);
        designScale.setX(s);
        designScale.setY(s);
    }

    /** Replace the current screen inside the persistent scene with a fade-in. */
    public static void switchScene(Parent root) {
        designPane.getChildren().clear();
        designPane.getChildren().add(root);

        FadeTransition fade = new FadeTransition(Duration.millis(280), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    public static Stage getStage() {
        return stage;
    }
}
