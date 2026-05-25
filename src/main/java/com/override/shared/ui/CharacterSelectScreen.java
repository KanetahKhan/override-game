package com.override.shared.ui;

import com.override.Main;
import com.override.shared.model.GameCharacter;
import com.override.shared.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Character selection screen.
 *
 * Each tile shows: avatar dot, name, role, tagline, stat bonuses, and
 * either a "Select" or "Unlock for N coins" button. If the player can't
 * afford to unlock, the button routes to the Shop screen.
 */
public class CharacterSelectScreen {

    public Parent build() {
        Label title = UIFactory.title("Choose your persona");
        Label sub = UIFactory.subtitle("Each persona alters Ayan's starting strengths.");

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER);

        for (GameCharacter c : GameCharacter.roster()) {
            row.getChildren().add(buildTile(c));
        }

        Button back = UIFactory.secondary("← Back to menu");
        back.setOnAction(e -> Main.switchScene(new MainMenuScreen().build()));

        Button shop = UIFactory.secondary("Shop / Buy Coins");
        shop.setOnAction(e -> Main.switchScene(new ShopScreen().build()));

        HBox actions = new HBox(12, back, shop);
        actions.setAlignment(Pos.CENTER);

        Label coinLabel = new Label("Your balance: ◈ " + GameState.get().getCoins());
        coinLabel.getStyleClass().add("hud-coins");

        VBox root = new VBox(20, title, sub, row, coinLabel, actions);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));

        return UIFactory.backdrop(root);
    }

    private VBox buildTile(GameCharacter c) {
        boolean unlocked = GameState.get().isUnlocked(c.getId());

        // Avatar – simple coloured ring stand-in (no asset files in prototype)
        Circle avatar = new Circle(38);
        avatar.setFill(Color.web(unlocked ? "#0a1f2b" : "#2a1010"));
        avatar.setStroke(Color.web(unlocked ? "#28e0c0" : "#6b3a3a"));
        avatar.setStrokeWidth(2.5);

        Label letter = new Label(c.getName().substring(0, 1));
        letter.setStyle("-fx-text-fill: " + (unlocked ? "#28e0c0" : "#a05a5a")
            + "; -fx-font-size: 36px; -fx-font-weight: 800;");

        StackPane avatarBox = new StackPane(avatar, letter);

        Label name = new Label(c.getName());
        name.getStyleClass().add("char-name");

        Label role = new Label(c.getRole());
        role.getStyleClass().add("char-role");

        Label tag = new Label(c.getTagline());
        tag.setWrapText(true);
        tag.getStyleClass().add("char-tag");
        tag.setMaxWidth(200);

        Label stats = new Label(
            "+L" + c.getBonusLogic() +
            "  +A" + c.getBonusAwareness() +
            "  +W" + c.getBonusWillpower() +
            "  +C" + c.getBonusCombat() +
            "  +E" + c.getBonusEmpathy()
        );
        stats.getStyleClass().add("char-stats");

        Button action;
        if (unlocked) {
            action = UIFactory.primary("Select");
            action.setMinWidth(180);
            action.setOnAction(e -> {
                GameState.get().setSelectedCharacter(c);
                Main.switchScene(new IntroStoryScreen().build());
            });
        } else {
            action = UIFactory.danger("Unlock — ◈ " + c.getUnlockCost());
            action.setMinWidth(180);
            action.setOnAction(e -> tryUnlock(c));
        }

        VBox tile = UIFactory.card();
        tile.getChildren().addAll(avatarBox, name, role, tag, stats, action);
        tile.setSpacing(8);
        tile.setAlignment(Pos.CENTER);
        tile.setMinWidth(220);
        tile.setPrefWidth(220);
        if (!unlocked) tile.getStyleClass().add("card-locked");
        return tile;
    }

    private void tryUnlock(GameCharacter c) {
        GameState s = GameState.get();
        if (s.getCoins() < c.getUnlockCost()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Not enough coins");
            a.setHeaderText("You need ◈ " + (c.getUnlockCost() - s.getCoins()) + " more.");
            a.setContentText("You can earn coins by completing chapters, or buy a coin pack from the Shop.");
            a.showAndWait();
            Main.switchScene(new ShopScreen().build());
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unlock " + c.getName() + "?");
        confirm.setHeaderText("Spend ◈ " + c.getUnlockCost() + " to unlock " + c.getName() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn.getButtonData().isDefaultButton()) {
                s.unlockCharacter(c);
                Main.switchScene(new CharacterSelectScreen().build());
            }
        });
    }
}
