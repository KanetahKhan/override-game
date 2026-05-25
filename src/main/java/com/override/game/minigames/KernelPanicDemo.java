package com.override.game.minigames;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/**
 * Standalone launcher to play / test "Kernel Panic" without booting the whole
 * game. Doubles as the smallest possible integration example.
 *
 * <pre>
 *   mvn javafx:run -Djavafx.mainClass=com.override.game.minigames.KernelPanicDemo
 * </pre>
 *
 * Inside the real game a chapter launches it instead via
 * {@link MiniGameLauncher#launch}, applying {@code result.dependencyUsed()} to the
 * global Dependency Meter.
 */
public final class KernelPanicDemo extends Application {

    @Override
    public void start(Stage stage) {
        KernelPanicGame game = new KernelPanicGame();
        game.setResultListener(r -> {
            System.out.printf("Kernel Panic result -> won=%s score=%d xp=%d dependencyUsed=%d%n",
                    r.won(), r.score(), r.xpEarned(), r.dependencyUsed());
            Platform.exit();
        });

        Scene scene = new Scene(game.getView());
        scene.setOnKeyPressed(e -> { game.dispatchKey(e.getCode()); e.consume(); });
        stage.setScene(scene);
        stage.setTitle("Kernel Panic — Demo");
        stage.setResizable(false);
        stage.setOnShown(e -> game.start());
        stage.show();

        // Self-test hook: -Dkernelpanic.selftest=true quits after ~2.5s so the
        // launch path can be verified in CI without manual input. No effect when
        // playing normally.
        if (Boolean.getBoolean("kernelpanic.selftest")) {
            Thread t = new Thread(() -> {
                try { Thread.sleep(2500); } catch (InterruptedException ignored) { }
                Platform.runLater(() -> game.dispatchKey(KeyCode.ESCAPE));
            }, "kp-selftest");
            t.setDaemon(true);
            t.start();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
