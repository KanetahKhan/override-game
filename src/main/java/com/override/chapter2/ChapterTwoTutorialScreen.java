package com.override.chapter2;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Pre-game briefing for Chapter 2 — Harvest Protocol.
 *
 * <p>Thin loading shim: the actual screen is authored in
 * {@code /menu/ch2_briefing.fxml} (layout) + {@code /menu/ch2_briefing.css}
 * (cyberpunk styling) + {@link ChapterTwoBriefingController} (wiring). The
 * CRT static overlay is created here and layered on TOP of the FXML root
 * so that it stays pinned to the window while rootPane jitters underneath.
 * </p>
 */
public class ChapterTwoTutorialScreen {

    public Parent build() {
        try {
            FXMLLoader loader = new FXMLLoader(
                ChapterTwoTutorialScreen.class.getResource("/menu/ch2_briefing.fxml"));
            Parent rootPane = loader.load();

            // Overlay lives OUTSIDE rootPane — sits in the StackPane wrapper,
            // stays fixed on the glass while rootPane translates beneath it.
            Pane staticOverlay = new Pane();
            staticOverlay.getStyleClass().add("static-overlay");
            staticOverlay.setMouseTransparent(true);
            StackPane.setAlignment(staticOverlay, javafx.geometry.Pos.CENTER);

            // Wire into the controller
            ChapterTwoBriefingController ctrl = loader.getController();
            ctrl.setStaticOverlay(staticOverlay);

            StackPane wrapper = new StackPane(rootPane, staticOverlay);
            return wrapper;
        } catch (IOException e) {
            throw new IllegalStateException(
                "Cannot load /menu/ch2_briefing.fxml from the classpath", e);
        }
    }
}
