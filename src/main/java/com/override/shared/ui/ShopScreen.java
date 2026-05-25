package com.override.shared.ui;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.BkashMockService;
import com.override.shared.service.BkashMockService.CoinPackage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Shop. Sells coin packages via the mock bKash flow.
 *
 * The clearly-labelled "MOCK PAYMENT" banner is intentional — for a
 * semester project we should never be ambiguous about whether real money
 * is being moved.
 */
public class ShopScreen {

    public Parent build() {
        Label title = UIFactory.title("Shop");
        Label sub = UIFactory.subtitle("Buy coins to unlock personas and hints.");

        Label warn = new Label("Payments are simulated. No real money is transferred.");
        warn.getStyleClass().add("body-warn");

        Label balance = new Label("Balance: ◈ " + GameState.get().getCoins());
        balance.getStyleClass().add("hud-coins");

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER);

        for (CoinPackage pkg : BkashMockService.packages()) {
            row.getChildren().add(buildTile(pkg));
        }

        Button back = UIFactory.secondary("← Back");
        back.setOnAction(e -> Main.switchScene(new MainMenuScreen().build()));

        Button toCharSelect = UIFactory.secondary("→ Character Select");
        toCharSelect.setOnAction(e -> Main.switchScene(new CharacterSelectScreen().build()));

        HBox actions = new HBox(12, back, toCharSelect);
        actions.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, title, sub, warn, balance, row, actions);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));

        return UIFactory.backdrop(root);
    }

    private VBox buildTile(CoinPackage pkg) {
        Label coinIcon = new Label("◈");
        coinIcon.setStyle("-fx-text-fill: #ffd66e; -fx-font-size: 56px; -fx-font-weight: 900;");

        Label coins = new Label(String.valueOf(pkg.coins()));
        coins.getStyleClass().add("shop-coins");

        Label label = new Label(pkg.label());
        label.getStyleClass().add("shop-label");

        Label price = new Label("৳ " + pkg.priceBdt());
        price.getStyleClass().add("shop-price");

        Button buy = UIFactory.primary("Buy via bKash");
        buy.setMinWidth(180);
        buy.setOnAction(e -> BkashMockService.purchase(pkg, () ->
            Main.switchScene(new ShopScreen().build())
        ));

        VBox tile = UIFactory.card();
        tile.getChildren().addAll(coinIcon, coins, label, price, buy);
        tile.setSpacing(8);
        tile.setAlignment(Pos.CENTER);
        tile.setMinWidth(200);
        tile.setPrefWidth(200);
        return tile;
    }
}
