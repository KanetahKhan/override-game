package com.override.shared.ui;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.service.PlayerProfiles;
import com.override.shared.service.SaveService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;

/** Player access is deliberately name-only and local; no account server is required. */
public final class LoginScreen {
    private final Runnable signedIn, scoreboard, exit;

    public LoginScreen() {
        this(() -> Main.switchScene(new MainMenuScreen().build()),
            () -> Main.switchScene(new ScoreboardScreen(() -> Main.switchScene(new LoginScreen().build())).build()),
            Platform::exit);
    }

    public LoginScreen(Runnable signedIn, Runnable scoreboard, Runnable exit) {
        this.signedIn = signedIn; this.scoreboard = scoreboard; this.exit = exit;
    }

    public Parent build() {
        AnchorPane layout = new AnchorPane();
        place(layout, GameControls.label("[ O / R ]   OVERRIDE / PLAYER ACCESS", 12, "#89adae"), 56, 30);
        place(layout, GameControls.label("LOCAL TERMINAL   /   2556", 11, "#89adae"), 954, 32);

        Label title = GameControls.label("KEEP YOUR NAME.", 36, "#e4f5ef");
        title.setStyle(title.getStyle() + " -fx-font-weight: bold;");
        Label intro = GameControls.label("The system counts subjects. We remember players.", 12, "#91aebc");
        TextField name = new TextField();
        name.setId("player-name"); name.setPromptText("Enter your name");
        name.setAccessibleText("Player name, 2 to 24 characters");
        name.setPrefHeight(48);
        Label error = GameControls.label("", 12, "#ffa5a5");
        error.setId("login-error"); error.setWrapText(true); error.setMinHeight(32); error.setMaxWidth(420);
        Button submit = GameControls.button("LOG IN  →", 420, 52, true);
        submit.setId("login-submit");
        submit.setOnAction(e -> {
            try {
                PlayerProfiles.login(name.getText());
                GameState.reset();
                if (SaveService.saveExists() && !SaveService.load()) {
                    error.setText("Your save could not be read. It has been kept on disk.");
                    PlayerProfiles.logout(); GameState.reset();
                    return;
                }
                signedIn.run();
            } catch (IllegalArgumentException badName) { error.setText(badName.getMessage()); name.requestFocus(); }
            catch (IOException storage) { error.setText("Could not open this profile. Check storage and try again."); }
        });
        name.setOnAction(e -> { submit.fire(); e.consume(); });

        VBox form = new VBox(10, GameControls.label("01 / IDENTIFY YOURSELF", 11, "#6de6cc"),
            title, intro, GameControls.label("PLAYER NAME", 11, "#a1bfca"), name,
            GameControls.label("2–24 characters · same name resumes the same profile", 11, "#7e9caa"), submit, error);
        VBox.setMargin(name, new Insets(4, 0, 0, 0));
        VBox.setMargin(submit, new Insets(5, 0, 0, 0));
        form.setPrefWidth(468); form.setPadding(new Insets(24)); form.getStyleClass().add("player-panel");
        place(layout, form, 64, 122);

        FlowPane players = new FlowPane(8, 8);
        players.setPrefWrapLength(450);
        try {
            var profiles = PlayerProfiles.list();
            for (var profile : profiles) {
                Button pick = GameControls.button(profile.name(), 216, 38, false);
                pick.setOnAction(e -> { name.setText(profile.name()); submit.fire(); });
                players.getChildren().add(pick);
            }
            if (profiles.isEmpty()) players.getChildren().add(GameControls.label("No profiles yet. Your first run starts here.", 12, "#7995a5"));
        } catch (IOException e) { error.setText("Existing profiles could not be read. Check your storage."); }
        ScrollPane recent = new ScrollPane(players);
        recent.setFitToWidth(true); recent.setPrefSize(468, 94); recent.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox returning = new VBox(10, GameControls.label("RETURNING PLAYERS", 11, "#89aab4"), recent);
        place(layout, returning, 64, 515);

        Label side = GameControls.label("A NAME IS SOMETHING\nIT CANNOT CHOOSE FOR YOU.", 21, "#c0d9d7");
        VBox message = new VBox(16, GameControls.label("HUMAN INPUT REQUIRED", 11, "#6de6cc"), side,
            GameControls.label("Your progress. Your score. Your resistance.", 12, "#86a9b5"));
        place(layout, message, 740, 172);
        Button board = GameControls.button("SCOREBOARD", 218, 42, false);
        board.setId("login-scoreboard"); board.setOnAction(e -> scoreboard.run());
        Button quit = GameControls.button("QUIT", 142, 42, false);
        quit.setId("login-quit"); quit.setOnAction(e -> exit.run());
        HBox actions = new HBox(12, board, quit);
        place(layout, actions, 64, 642);
        place(layout, GameControls.label("PROFILES & SCORES STAY ON THIS COMPUTER", 10, "#6f929f"), 848, 673);
        Parent root = GameControls.page(layout, 0.12);
        root.setId("player-login");
        root.sceneProperty().addListener((o, old, scene) -> { if (scene != null) Platform.runLater(name::requestFocus); });
        return root;
    }

    private static void place(AnchorPane pane, javafx.scene.Node node, double x, double y) {
        AnchorPane.setLeftAnchor(node, x); AnchorPane.setTopAnchor(node, y); pane.getChildren().add(node);
    }
}
