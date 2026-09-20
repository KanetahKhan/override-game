package com.override.chapter1;

import com.override.Main;
import com.override.net.AstraProtocol;
import com.override.net.CoopConfig;
import com.override.net.OverrideRelay;
import com.override.net.RelayLink;
import com.override.shared.ui.MainMenuScreen;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import static com.override.chapter1.CurfewNodeGames.BODY;
import static com.override.chapter1.CurfewNodeGames.MONO;
import static com.override.chapter1.CurfewNodeGames.text;

/**
 * The other seat: a second player watches the floor live and spends Astra's
 * power on it, from anywhere with a network path to the relay.
 *
 * <p>This end never simulates anything. It draws what the game sends and sends
 * intents back, which is why a partner in another city plays fine on a link
 * with real latency: nothing here waits for a round trip.
 */
public class AstraConsoleScreen {

    private static final String ASTRA = "#a97bff";
    private static final double POWER_MAX = 100;
    private static final double POWER_REGEN = 4;     // per second

    private RelayLink link;
    private double power = POWER_MAX;
    private boolean floorLive;

    private final StackPane root = new StackPane();
    private final AstraFloorMap map = new AstraFloorMap();
    private Label status, vitals, alertLabel, powerLabel, logLabel;
    private Region powerFill;
    private TextField hostField, portField, roomField;
    private final Timeline regen = new Timeline(new KeyFrame(Duration.millis(200), e -> regen()));

    public Parent build() {
        root.setPrefSize(Main.WIDTH, Main.HEIGHT);
        root.setStyle("-fx-background-color: #05080b;");

        Label title = text("ASTRA CONSOLE", BODY, 40, "#e8fbf8");
        title.setStyle(title.getStyle() + " -fx-font-weight: bold;");
        Label sub = text("You are the floor. She is on it.", MONO, 13, ASTRA);

        status = text("Not connected.", MONO, 13, "#ffb347");
        vitals = text("--", MONO, 14, "#e8fbf8");
        alertLabel = text("OFFLINE", MONO, 16, "rgba(126,243,232,0.6)");
        logLabel = text("", BODY, 16, "rgba(207,238,234,0.85)");
        logLabel.setWrapText(true);
        logLabel.setMaxWidth(440);

        hostField = field(CoopConfig.host(), 200);
        portField = field(String.valueOf(CoopConfig.port()), 80);
        roomField = field(CoopConfig.room(), 110);
        Button connect = button("CONNECT", true);
        connect.setOnAction(e -> connect());
        Button back = button("BACK", false);
        back.setOnAction(e -> leave());
        VBox connectPanel = new VBox(10,
            row(text("RELAY", MONO, 12, "rgba(126,243,232,0.6)"), hostField,
                text("PORT", MONO, 12, "rgba(126,243,232,0.6)"), portField,
                text("ROOM", MONO, 12, "rgba(126,243,232,0.6)"), roomField),
            row(connect, back));
        connectPanel.setAlignment(Pos.CENTER_LEFT);

        powerFill = new Region();
        powerFill.setStyle("-fx-background-color: linear-gradient(to right, #6b4dff, " + ASTRA + ");");
        powerFill.setMaxHeight(Double.MAX_VALUE);
        StackPane powerTrack = new StackPane(powerFill);
        powerTrack.setAlignment(Pos.CENTER_LEFT);
        powerTrack.setMaxSize(260, 10);
        powerTrack.setPrefSize(260, 10);
        powerTrack.setStyle("-fx-background-color: rgba(169,123,255,0.12);"
            + " -fx-border-color: rgba(169,123,255,0.4);");
        powerLabel = text("100", MONO, 14, ASTRA);

        VBox left = new VBox(14, title, sub, connectPanel, status,
            text("INTEGRITY / CREDITS / NODES / CLOCK", MONO, 11, "rgba(126,243,232,0.55)"), vitals,
            row(text("UNIT", MONO, 11, "rgba(126,243,232,0.55)"), alertLabel),
            row(text("POWER", MONO, 11, "rgba(169,123,255,0.7)"), powerTrack, powerLabel),
            commands(), logLabel);
        left.setAlignment(Pos.TOP_LEFT);
        left.setPadding(new Insets(36));
        left.setMaxWidth(560);

        VBox right = new VBox(12, text("FLOOR 2 - LIVE", MONO, 12, "rgba(126,243,232,0.6)"), map);
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(36));

        HBox layout = new HBox(24, left, right);
        layout.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(layout);

        regen.setCycleCount(Animation.INDEFINITE);
        regen.play();
        refresh();
        return root;
    }

    private VBox commands() {
        VBox box = new VBox(8,
            command("CUT THE POWER", 35, () -> send(AstraProtocol.CMD_BLACKOUT)),
            command("SWEEP HER ROOM", 20, this::sweepHer),
            command("WAKE SECOND UNIT", 45, () -> send(AstraProtocol.CMD_WAKE)),
            command("SEAL THE FLOOR", 60, () -> send(AstraProtocol.CMD_LOCKDOWN)),
            speakBox());
        box.setPadding(new Insets(6, 0, 6, 0));
        return box;
    }

    /**
     * Astra's voice: whatever you type lands as a toast on her screen.
     *
     * <p>Costs the same 5 power the fixed taunt did, but only when a line is
     * actually sent — an empty box spends nothing.</p>
     */
    private Node speakBox() {
        TextField say = new TextField();
        say.setId("astra-say");
        say.setPromptText("say something to her, then Enter");
        say.setStyle(BODY + " -fx-background-color: #101b2b; -fx-text-fill: #d9e8f2;"
            + " -fx-border-color: #4b3d7a; -fx-prompt-text-fill: #6b7f92;");
        HBox.setHgrow(say, Priority.ALWAYS);

        Button send = new Button("SPEAK  5");
        send.setStyle(MONO + " -fx-background-color: #221a3a; -fx-text-fill: #cbb8ff;"
            + " -fx-border-color: #4b3d7a;");

        Runnable speak = () -> {
            String message = AstraProtocol.chat(say.getText());
            if (message == null || power < 5) return;
            power -= 5;
            send(AstraProtocol.CMD_TAUNT + " " + message);
            log("You said: " + message);
            say.clear();
            refresh();
        };
        send.setOnAction(e -> speak.run());
        say.setOnAction(e -> speak.run());

        HBox row = new HBox(8, say, send);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Sends the unit to wherever the player was standing on the last frame. */
    private void sweepHer() {
        AstraProtocol.Tick t = map.lastTick();
        if (t == null) return;
        send(AstraProtocol.CMD_SWEEP + " " + fixed(t.px()) + " " + fixed(t.pz()));
    }

    private Button command(String label, int cost, Runnable action) {
        Button b = button(label + "   " + cost, false);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            if (!floorLive || power < cost) return;
            power -= cost;
            action.run();
            refresh();
        });
        return b;
    }

    private void send(String command) {
        if (link != null && link.isConnected()) link.send(AstraProtocol.CMD + " " + command);
    }

    private void connect() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            port = OverrideRelay.DEFAULT_PORT;
        }
        CoopConfig.set(hostField.getText(), port, roomField.getText());
        if (link != null) link.close();
        link = new RelayLink(CoopConfig.host(), CoopConfig.port(), CoopConfig.room(),
            AstraProtocol.ROLE_ASTRA, "ASTRA", new RelayLink.Listener() {
                @Override public void onLine(String line) {
                    // qualified: an unqualified call here would recurse into this very method
                    AstraConsoleScreen.this.onLine(line);
                }

                @Override public void onStatus(String message, boolean connected) {
                    status.setText(message);
                    if (!connected) {
                        floorLive = false;
                        alertLabel.setText("OFFLINE");
                    }
                }
            });
        link.connect();
    }

    /** Always called on the FX thread: RelayLink hands lines back through runLater. */
    private void onLine(String line) {
        if (line.startsWith("PEER ")) {
            log("She just sat down at the terminal.");
            return;
        }
        if ("PEERGONE".equals(line)) {
            floorLive = false;
            alertLabel.setText("OFFLINE");
            map.clear();
            log("The floor went quiet - she closed the game.");
            return;
        }
        if (line.startsWith(AstraProtocol.EVENT + " ")) {
            log(line.substring(AstraProtocol.EVENT.length() + 1));
            return;
        }
        if (line.startsWith(AstraProtocol.SAY + " ")) {
            // Re-cleaned on arrival: what a peer sends is not ours to trust.
            String said = AstraProtocol.chat(line.substring(AstraProtocol.SAY.length() + 1));
            if (said != null) log("SHE SAYS: " + said);
            return;
        }
        AstraProtocol.Tick t = AstraProtocol.parseTick(line);
        if (t == null) return;
        floorLive = true;
        map.update(t);
        vitals.setText(t.hp() + "/3      " + t.credits() + " CR      " + t.tokens() + "/3      "
            + (t.secs() / 60) + ":" + String.format("%02d", t.secs() % 60));
        alertLabel.setText(t.hidden() ? "SHE IS HIDDEN" : t.state());
        recolor(alertLabel, "CHASE".equals(t.state()) ? "#ff3d5a"
            : "SEARCH".equals(t.state()) ? "#ffb347" : "#35e0d8");
    }

    private void regen() {
        if (power < POWER_MAX) {
            power = Math.min(POWER_MAX, power + POWER_REGEN * 0.2);
            refresh();
        }
    }

    private void refresh() {
        powerFill.setMaxWidth(260 * power / POWER_MAX);
        powerLabel.setText(String.valueOf((int) power));
    }

    private void log(String message) {
        logLabel.setText(message);
    }

    private void leave() {
        regen.stop();
        if (link != null) link.close();
        Main.switchScene(new MainMenuScreen().build());
    }

    private static String fixed(double v) {
        return String.format("%.2f", v);
    }

    private static void recolor(Label label, String colour) {
        label.setStyle(label.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + colour + ";"));
    }

    private static HBox row(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        // labels in a row must keep their width, or JavaFX squeezes them to "..."
        for (Node n : nodes) {
            if (n instanceof Label label) label.setMinWidth(Region.USE_PREF_SIZE);
        }
        return box;
    }

    private static TextField field(String value, double width) {
        TextField f = new TextField(value);
        f.setPrefWidth(width);
        f.setStyle("-fx-background-color: rgba(6,20,22,0.9); -fx-text-fill: #e8fbf8;"
            + " -fx-border-color: rgba(53,224,216,0.35); -fx-background-radius: 0;"
            + " -fx-border-radius: 0; -fx-font-family: " + MONO + ";");
        return f;
    }

    private static Button button(String label, boolean primary) {
        Button b = new Button(label);
        b.setFocusTraversable(false);
        String base = "-fx-background-radius: 0; -fx-border-radius: 0; -fx-text-fill: #e8fbf8;"
            + " -fx-font-family: " + MONO + "; -fx-font-size: 13px; -fx-padding: 10 18 10 18;"
            + " -fx-cursor: hand; -fx-border-color: "
            + (primary ? "#35e0d8" : "rgba(169,123,255,0.6)") + ";";
        String idle = base + " -fx-background-color: "
            + (primary ? "rgba(53,224,216,0.14)" : "rgba(169,123,255,0.10)") + ";";
        String hover = base + " -fx-background-color: rgba(169,123,255,0.28);";
        b.setStyle(idle);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(idle));
        return b;
    }
}
