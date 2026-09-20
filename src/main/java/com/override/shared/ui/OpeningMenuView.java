package com.override.shared.ui;

import com.override.Main;
import com.override.chapter1.AstraConsoleScreen;
import com.override.chapter1.CurfewProtocolScreen;
import com.override.game.minigames.ChiptuneSfx;
import com.override.net.CoopConfig;
import com.override.net.CoopRelayHost;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

/** Actual, keyboard-operable title menu; callbacks keep it independently testable. */
final class OpeningMenuView extends StackPane {
    record Actions(Runnable newGame, Runnable continueGame, Runnable shop, Runnable quit, Runnable scoreboard, Runnable switchPlayer) {
        Actions(Runnable newGame, Runnable continueGame, Runnable shop, Runnable quit) {
            this(newGame, continueGame, shop, quit, () -> {}, () -> {});
        }
        Actions {
            Objects.requireNonNull(newGame); Objects.requireNonNull(continueGame);
            Objects.requireNonNull(shop); Objects.requireNonNull(quit);
            Objects.requireNonNull(scoreboard); Objects.requireNonNull(switchPlayer);
        }
    }
    private final ClassroomPixelScene background = new ClassroomPixelScene();
    private final StackPane modal = new StackPane();
    private final List<Button> options;
    private double seconds;
    private long previous;
    private boolean lastReduced;
    private final AnimationTimer timer = new AnimationTimer() {
        @Override public void handle(long now) {
            boolean reduced = OpeningPreferences.REDUCED_MOTION.get();
            if (getScene() == null || getScene().getWindow() == null || !getScene().getWindow().isFocused()) {
                previous = 0; return;
            }
            if (previous == 0) previous = now;
            seconds += Math.min(0.05, (now - previous) / 1_000_000_000.0);
            previous = now;
            if (!reduced || reduced != lastReduced) background.menu(seconds, reduced);
            lastReduced = reduced;
        }
    };

    OpeningMenuView(boolean saveExists, Actions actions) { this(saveExists, actions, true); }

    OpeningMenuView(boolean saveExists, Actions actions, boolean animate) {
        setId("override-menu");
        setMinSize(1280, 720); setPrefSize(1280, 720); setMaxSize(1280, 720);
        setStyle("-fx-background-color: #050b13;");
        background.menu(0, OpeningPreferences.REDUCED_MOTION.get());
        AnchorPane layout = new AnchorPane();
        place(layout, text("[ O / R ]    OVERRIDE SYSTEMS", 12, "#8da6b4"), 56, 30);
        Label date = text("2556   /   HUMAN INPUT REQUIRED", 12, "#87a1ae");
        AnchorPane.setTopAnchor(date, 30.0); AnchorPane.setRightAnchor(date, 48.0);
        layout.getChildren().add(date);
        Button change = GameControls.button("SWITCH PLAYER", 160, 34, false);
        change.setId("switch-player"); change.setOnAction(e -> actions.switchPlayer().run());
        HBox identity = new HBox(16, text("PLAYER / " + com.override.shared.service.PlayerProfiles.name(), 12, "#a8dace"), change);
        identity.setAlignment(Pos.CENTER_LEFT); place(layout, identity, 70, 66);

        Label transmission = text("A SIGNAL THE SYSTEM COULDN'T ERASE", 11, "#6de6cc");
        Label title = text("OVERRIDE", 79, "#e6f5ee");
        title.setStyle(title.getStyle() + " -fx-font-weight: 900; -fx-padding: -4 0 -6 0;");
        Label sub = text("T H E   L A S T   R E A L   M I N D", 12, "#a8bebf");
        Label premise = text("It learned everything.\nWe forgot how to think.", 17, "#8fa5b4");
        VBox heading = new VBox(10, transmission, title, sub, premise);
        VBox.setMargin(premise, new Insets(14, 0, 12, 0));
        Button start = option("01   NEW GAME", true);
        start.setId("new-game"); start.setOnAction(e -> actions.newGame().run());
        Button resume = option("02   CONTINUE", false);
        resume.setId("continue-game"); resume.setDisable(!saveExists);
        resume.setOnAction(e -> actions.continueGame().run());
        Button shop = option("03   PERSONAS / SHOP", false);
        shop.setId("shop"); shop.setOnAction(e -> actions.shop().run());
        Button board = option("04   SCOREBOARD", false);
        board.setId("scoreboard-menu"); board.setOnAction(e -> actions.scoreboard().run());
        Button settings = option("05   DISPLAY OPTIONS", false);
        settings.setId("display-options"); settings.setOnAction(e -> showSettings(settings));
        Button coop = option("06   ASTRA CO-OP", false);
        coop.setId("astra-coop");
        coop.setOnAction(e -> showCoop(coop));
        Button quit = option("07   QUIT", false);
        quit.setId("quit"); quit.setOnAction(e -> actions.quit().run());
        options = List.of(start, resume, shop, board, settings, coop, quit);
        // Seven rows at the old 7px gap ran the QUIT button past the footer at
        // y=679. The gap absorbs the extra row rather than option(), whose 44px
        // height is shared with the intro screens and the modals.
        VBox choices = new VBox(3, start, resume, shop, board, settings, coop, quit);
        place(layout, new VBox(0, heading, choices), 70, 116);
        place(layout, text("CHAPTER 01 / THE SILENT CLASSROOM", 11, "#749a9f"), 768, 628);
        place(layout, text("KK IS STILL LISTENING.", 12, "#d9888f"), 768, 648);
        place(layout, text("UP / DOWN  SELECT     ENTER  CONFIRM", 10, "#657c8e"), 70, 679);
        Label footer = text("EDUCATION IS NOT OBEDIENCE.  /  SDG 4", 10, "#657c8e");
        AnchorPane.setBottomAnchor(footer, 26.0); AnchorPane.setRightAnchor(footer, 48.0);
        layout.getChildren().add(footer);
        modal.setVisible(false); modal.setManaged(false);
        getChildren().addAll(background, layout, modal);
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (!modal.isVisible() && e.getCode() == KeyCode.ENTER && getScene() != null
                    && getScene().getFocusOwner() instanceof Button focused
                    && !focused.isDisabled()) {
                focused.fire(); e.consume();
            }
        });
        setOnKeyPressed(e -> {
            if (modal.isVisible()) return;
            if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
                int index = 0;
                for (int i = 0; i < options.size(); i++) if (options.get(i).isFocused()) index = i;
                int direction = e.getCode() == KeyCode.UP ? -1 : 1;
                do index = Math.floorMod(index + direction, options.size()); while (options.get(index).isDisabled());
                options.get(index).requestFocus(); e.consume();
            }
        });
        sceneProperty().addListener((o, oldScene, scene) -> {
            timer.stop(); previous = 0;
            if (scene != null) { start.requestFocus(); if (animate) timer.start(); }
        });
    }

    /**
     * Two seats, one floor: one player runs the chapter while the other plays
     * Astra from anywhere that can reach the relay.
     */
    private void showCoop(Button source) {
        TextField host = coopField(CoopConfig.host(), 210);
        host.setId("coop-host");
        TextField port = coopField(String.valueOf(CoopConfig.port()), 80);
        TextField room = coopField(CoopConfig.room(), 110);
        Label relayNote = text("", 12, "#9bb1bd");

        Runnable remember = () -> {
            int chosen = CoopConfig.port();
            try {
                chosen = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException ignored) {
                // keep the previous port when the box holds nonsense
            }
            CoopConfig.set(host.getText(), chosen, room.getText());
        };

        Button relay = option("RUN THE RELAY HERE", false);
        relay.setId("coop-relay");
        relay.setOnAction(e -> {
            remember.run();
            relayNote.setText(CoopRelayHost.start(CoopConfig.port()));
        });
        Button asAyan = option("PLAY AS AYAN  (CHAPTER 1)", true);
        asAyan.setId("coop-ayan");
        asAyan.setOnAction(e -> {
            remember.run();
            CoopConfig.setLinked(true);
            Main.switchScene(new CurfewProtocolScreen().build());
        });
        Button asAstra = option("PLAY AS ASTRA  (CONSOLE)", false);
        asAstra.setId("coop-astra");
        asAstra.setOnAction(e -> {
            remember.run();
            Main.switchScene(new AstraConsoleScreen().build());
        });
        Button close = option("BACK", false);
        close.setId("close-coop");
        Runnable dismiss = () -> {
            modal.getChildren().clear();
            modal.setVisible(false);
            modal.setManaged(false);
            source.requestFocus();
        };
        close.setOnAction(e -> dismiss.run());

        HBox fields = new HBox(10, text("RELAY", 12, "#87a1ae"), host,
            text("PORT", 12, "#87a1ae"), port, text("ROOM", 12, "#87a1ae"), room);
        fields.setAlignment(Pos.CENTER_LEFT);
        VBox help = new VBox(3,
            text("Same Wi-Fi: one of you runs the relay and reads out the address.", 13, "#9bb1bd"),
            text("Different cities: run the relay on a cloud box, or join a Tailscale", 13, "#9bb1bd"),
            text("network and use that address. Both sides dial out - no router setup.", 13, "#9bb1bd"));

        VBox panel = new VBox(16, text("ASTRA CO-OP", 25, "#e6f5ee"), fields, help,
            relay, relayNote, asAyan, asAstra, close);
        panel.setMaxSize(660, VBox.USE_PREF_SIZE);
        panel.setPadding(new Insets(30));
        panel.setStyle("-fx-background-color: #0d1a27; -fx-border-color: #416b75; -fx-border-width: 1;");
        modal.setStyle("-fx-background-color: rgba(3,8,15,0.9);");
        modal.getChildren().setAll(panel);
        modal.setVisible(true);
        modal.setManaged(true);
        modal.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                dismiss.run();
                e.consume();
            }
        });
        host.requestFocus();
    }

    private static TextField coopField(String value, double width) {
        TextField f = new TextField(value);
        f.setPrefWidth(width);
        f.setStyle("-fx-background-color: #081722; -fx-text-fill: #d6e8e8;"
            + " -fx-border-color: #2c5563; -fx-background-radius: 0; -fx-border-radius: 0;"
            + " -fx-font-family: 'Monospaced';");
        return f;
    }

    private void showSettings(Button source) {
        CheckBox reduced = new CheckBox("Reduced motion"); reduced.setId("reduced-motion");
        reduced.setStyle("-fx-text-fill: #d6e8e8; -fx-font-size: 17px;");
        reduced.setSelected(OpeningPreferences.REDUCED_MOTION.get());
        reduced.setOnAction(e -> {
            OpeningPreferences.REDUCED_MOTION.set(reduced.isSelected());
            background.menu(seconds, reduced.isSelected());
        });
        Label help = text("Disables rain, camera sweeps and sprite motion\nin the menu and classroom opening.\nStory subtitles and controls stay available.", 13, "#9bb1bd");
        CheckBox fullscreen = new CheckBox("Fullscreen"); fullscreen.setId("fullscreen");
        fullscreen.setStyle("-fx-text-fill: #d6e8e8; -fx-font-size: 17px;");
        // Main scales the 1280x720 design space to the window, so this only resizes it.
        Stage window = com.override.Main.getStage();
        fullscreen.setDisable(window == null);
        fullscreen.setSelected(window != null && window.isFullScreen());
        fullscreen.setOnAction(e -> {
            if (window != null) window.setFullScreen(fullscreen.isSelected());
        });

        Label volumeValue = text(percent(OpeningPreferences.VOLUME.get()), 15, "#b9ffdf");
        Slider volume = new Slider(0, 1, OpeningPreferences.VOLUME.get());
        volume.setId("volume");
        volume.setPrefWidth(250);
        volume.setBlockIncrement(0.05);
        volume.getStyleClass().add("menu-slider");
        volume.valueProperty().addListener((o, was, now) -> {
            OpeningPreferences.VOLUME.set(now.doubleValue());
            volumeValue.setText(percent(now.doubleValue()));
        });
        // A cue on release, so the chosen level can actually be heard.
        volume.setOnMouseReleased(e -> ChiptuneSfx.door());
        HBox volumeRow = new HBox(14, text("Sound", 17, "#d6e8e8"), volume, volumeValue);
        volumeRow.setAlignment(Pos.CENTER_LEFT);

        Button close = option("BACK", false); close.setId("close-options");
        Runnable dismiss = () -> { modal.getChildren().clear(); modal.setVisible(false); modal.setManaged(false); source.requestFocus(); };
        close.setOnAction(e -> dismiss.run());
        VBox panel = new VBox(22, text("DISPLAY OPTIONS", 25, "#e6f5ee"), reduced, help, fullscreen, volumeRow, close);
        panel.setMaxSize(520, VBox.USE_PREF_SIZE); panel.setPadding(new Insets(32));
        panel.setStyle("-fx-background-color: #0d1a27; -fx-border-color: #416b75; -fx-border-width: 1;");
        modal.setStyle("-fx-background-color: rgba(3,8,15,0.9);");
        modal.getChildren().setAll(panel); modal.setVisible(true); modal.setManaged(true);
        modal.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) { dismiss.run(); e.consume(); } });
        reduced.requestFocus();
    }

    static Button option(String caption, boolean primary) {
        return GameControls.button(caption, 338, 44, primary);
    }

    private static String percent(double value) {
        return Math.round(value * 100) + "%";
    }

    static Label text(String value, double size, String color) {
        Label label = new Label(value);
        label.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: " + size + "px; -fx-text-fill: " + color + ";");
        return label;
    }

    private static void place(AnchorPane pane, javafx.scene.Node node, double x, double y) {
        AnchorPane.setLeftAnchor(node, x); AnchorPane.setTopAnchor(node, y); pane.getChildren().add(node);
    }
}
