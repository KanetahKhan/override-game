package com.override.game.minigames;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opens a {@link MiniGame} in a modal window over the owner, starts its loop, and
 * relays the {@link MiniGameResult} to the caller before closing.
 *
 * <p>Typical use from a chapter screen:
 * <pre>{@code
 * MiniGameLauncher.launch(Main.getStage(),
 *     new KernelPanicGame(),
 *     result -> {
 *         GameState.get().increaseDependency(result.dependencyUsed());
 *         GameState.get().getPlayer().addXp(result.xpEarned());
 *     });
 * }</pre>
 */
public final class MiniGameLauncher {

    private MiniGameLauncher() {}

    public static void launch(Window owner, MiniGame game, ResultListener listener) {
        Stage dialog = new Stage();
        if (owner != null) dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Override // Mini-Game");
        dialog.setResizable(false);

        StackPane view = game.getView();
        Scene scene = new Scene(view);
        // Scene-level key handling as a safety net in case canvas focus is lost.
        scene.setOnKeyPressed(e -> { game.dispatchKey(e.getCode()); e.consume(); });
        dialog.setScene(scene);

        // A window can finish through gameplay, ESC, the title-bar close button,
        // or an external hide. Funnel every path through one result callback.
        AtomicBoolean resultDelivered = new AtomicBoolean(false);
        game.setResultListener(result -> {
            if (!resultDelivered.compareAndSet(false, true)) return;
            if (dialog.isShowing()) dialog.close();
            if (listener != null) listener.onResult(result);
        });

        dialog.setOnCloseRequest(e -> {
            if (!resultDelivered.get()) {
                e.consume();
                game.cancel();
            }
        });
        dialog.setOnHidden(e -> {
            if (!resultDelivered.get()) game.cancel();
        });

        // Start the loop once the window is on screen so focus lands correctly.
        dialog.setOnShown(e -> game.start());
        dialog.show();
    }
}
