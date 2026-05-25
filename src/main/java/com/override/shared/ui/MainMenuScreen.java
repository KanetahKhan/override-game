package com.override.shared.ui;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The first screen the player sees.
 *
 * Layout: full-screen dark gradient with the title in centre and four
 * stacked menu buttons. Continue is disabled if no save exists.
 */
public class MainMenuScreen {

    public Parent build() {
        Label title = UIFactory.title("OVERRIDE");
        title.setStyle("-fx-font-size: 96px; -fx-font-weight: 900;");

        Label tagline = UIFactory.subtitle("the last real mind");
        tagline.setStyle("-fx-letter-spacing: 8px;");

        Label hint = new Label("A story-based game on the dark future of AI dependence");
        hint.getStyleClass().add("body-dim");

        Button newGame = UIFactory.primary("New Game");
        Button cont    = UIFactory.secondary("Continue");
        Button shop    = UIFactory.secondary("Shop");
        Button quit    = UIFactory.secondary("Quit");

        cont.setDisable(!SaveService.saveExists());

        newGame.setOnAction(e -> {
            GameState.reset();
            Main.switchScene(new CharacterSelectScreen().build());
        });

        cont.setOnAction(e -> {
            if (SaveService.load()) {
                Main.switchScene(new ChapterMapScreen().build());
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR, "Save file is corrupt or missing.");
                a.showAndWait();
            }
        });

        shop.setOnAction(e -> Main.switchScene(new ShopScreen().build()));
        quit.setOnAction(e -> System.exit(0));

        VBox buttons = new VBox(12, newGame, cont, shop, quit);
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(8, title, tagline, hint);
        center.setAlignment(Pos.CENTER);

        VBox root = new VBox(40, center, buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(80));

        Label footer = new Label("v0.1 prototype  ·  built for educational use  ·  SDG 4");
        footer.getStyleClass().add("footer");

        StackPane wrapper = UIFactory.backdrop(root, footer);
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(footer, new Insets(0, 0, 16, 0));
        return wrapper;
    }
}
