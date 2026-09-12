package com.override.chapter1;

import com.override.Main;
import com.override.chapter1.CurfewNodeGames.NodeGame;
import com.override.chapter1.CurfewNodeGames.Outcome;
import com.override.shared.model.GameState;
import com.override.shared.service.SaveService;
import com.override.shared.ui.ChapterMapScreen;
import com.override.shared.ui.EndingScreen;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.override.chapter1.CurfewNodeGames.BODY;
import static com.override.chapter1.CurfewNodeGames.MONO;
import static com.override.chapter1.CurfewNodeGames.text;

/**
 * Chapter 1 — Curfew Protocol, played inside the game window.
 *
 * A 3D stealth escape across one floor: clear three hacking nodes, bank
 * credits hidden in the furniture, and reach the exit bay before curfew
 * closes, while a sentinel hunts you. The 3D floor lives in
 * {@link CurfewWorld}; this screen owns the HUD, run state, the node
 * mini-games and the hand-off to Chapter 2 (Harvest Protocol).
 */
public class CurfewProtocolScreen {

    private enum Phase { INTRO, PLAY, PAUSED, END }

    private static final int CURFEW_SECONDS = 9 * 60;
    private static final double DIFFICULTY = 1.0;   // Normal
    private static final int WIN_PLAYER_XP = 60;
    /** Independent XP for clearing the floor without Astra's help. */
    private static final int WIN_INDEPENDENT_XP = 20;

    /** Mouse-look by capturing the pointer; -Doverride.mouseLock=false falls back to drag-to-look. */
    private static final boolean MOUSE_LOCK =
        Boolean.parseBoolean(System.getProperty("override.mouseLock", "true"));

    private record NodeInfo(String game, int reward) {}

    private static final Map<String, NodeInfo> NODES = new LinkedHashMap<>();
    static {
        NODES.put("node1", new NodeInfo("kp", 40));
        NODES.put("node2", new NodeInfo("cb", 50));
        NODES.put("node3", new NodeInfo("sc", 35));
    }

    // run state
    private Phase phase = Phase.INTRO;
    private int hp = 3, credits, secs = CURFEW_SECONDS;
    private final Set<String> tokens = new HashSet<>();
    private boolean hiddenNow;
    private String alert = "PATROL", end, prompt, hideHint, activeNode;
    private boolean rewardsApplied;

    private CurfewWorld world;
    private NodeGame nodeGame;

    // scene nodes
    private final StackPane root = new StackPane();
    private final StackPane overlayLayer = new StackPane();
    private Region integrityFill, staminaFill, chaseFlash;
    private Label integrityLabel, alertLabel, clockLabel, creditsLabel, objectiveLabel,
        postureLabel, promptLabel, hideLabel, toastTag, toastText, mmLabel;
    private HBox promptBox, hideBox;
    private VBox toastBox, footer;
    private final Label[] nodeSlots = new Label[3];
    private Pane minimap;
    private Circle mmPlayer, mmSentinel;
    private StackPane crosshair;

    // timers
    private final Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickClock()));
    private final PauseTransition toastTimer = new PauseTransition(Duration.millis(2600));
    private FadeTransition flashPulse;

    // input
    private Robot robot;
    private boolean mouseLocked, mouseLockBroken, dragging;
    private double dragX, dragY, dragDist, lockX, lockY;
    private javafx.event.EventHandler<KeyEvent> keyPressHandler;
    private javafx.event.EventHandler<KeyEvent> keyReleaseHandler;
    private final ChangeListener<Boolean> focusListener = (o, was, focused) -> {
        if (!focused && phase == Phase.PLAY && nodeGame == null) pause();
    };

    public Parent build() {
        world = new CurfewWorld(new WorldEvents());
        world.setDifficulty(DIFFICULTY);

        root.setPrefSize(Main.WIDTH, Main.HEIGHT);
        root.setStyle("-fx-background-color: #05080b;");

        Region vignette = new Region();
        vignette.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 75%, transparent 55%, rgba(2,5,8,0.72) 100%);");
        Region scan = new Region();
        scan.setOpacity(0.05);
        scan.setStyle("-fx-background-color: linear-gradient(from 0px 0px to 0px 3px, repeat,"
            + " rgba(126,243,232,0.7) 0%, rgba(126,243,232,0.7) 33%, transparent 33%, transparent 100%);");
        chaseFlash = new Region();
        chaseFlash.setVisible(false);
        chaseFlash.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 80%, transparent 55%, rgba(255,61,90,0.42) 100%);");
        flashPulse = new FadeTransition(Duration.millis(500), chaseFlash);
        flashPulse.setFromValue(0.1);
        flashPulse.setToValue(1);
        flashPulse.setAutoReverse(true);
        flashPulse.setCycleCount(Animation.INDEFINITE);

        Pane hud = buildHud();
        for (Region r : new Region[] {vignette, scan, chaseFlash, hud}) r.setMouseTransparent(true);

        root.getChildren().addAll(world.view(), vignette, scan, chaseFlash, hud, overlayLayer);
        wireInput();

        clock.setCycleCount(Animation.INDEFINITE);
        toastTimer.setOnFinished(e -> toastBox.setVisible(false));

        showIntro();
        refreshHud();
        world.start();
        return root;
    }

    /* =============================================================== HUD */

    private Pane buildHud() {
        AnchorPane hud = new AnchorPane();

        // top-left: integrity + stamina
        integrityFill = new Region();
        integrityLabel = text("3/3", MONO, 14, "#e8fbf8");
        HBox integrity = new HBox(12, fixedWidth(text("INTEGRITY", MONO, 13, "#7ef3e8"), 92),
            bar(integrityFill, 9, "rgba(126,243,232,0.12)", "rgba(126,243,232,0.35)", "linear-gradient(to right, #35e0d8, #7ef3e8)"),
            integrityLabel);
        integrity.setAlignment(Pos.CENTER_LEFT);
        staminaFill = new Region();
        alertLabel = text("PATROLLING", MONO, 12, "#35e0d8");
        HBox stamina = new HBox(12, fixedWidth(text("STAMINA", MONO, 13, "#ffb347"), 92),
            bar(staminaFill, 6, "rgba(255,179,71,0.12)", "rgba(255,179,71,0.35)", "linear-gradient(to right, #ff7a4a, #ffb347)"),
            alertLabel);
        stamina.setAlignment(Pos.CENTER_LEFT);
        VBox topLeft = new VBox(11, integrity, stamina);
        AnchorPane.setTopAnchor(topLeft, 20.0);
        AnchorPane.setLeftAnchor(topLeft, 22.0);

        // top-right: clock, nodes, credits
        clockLabel = text("9:00", MONO, 40, "#e8fbf8");
        HBox slots = new HBox(7);
        slots.setAlignment(Pos.CENTER_RIGHT);
        for (int i = 0; i < 3; i++) {
            nodeSlots[i] = text(String.valueOf(i + 1), MONO, 12, "rgba(126,243,232,0.5)");
            nodeSlots[i].setAlignment(Pos.CENTER);
            nodeSlots[i].setMinSize(30, 30);
            nodeSlots[i].setMaxSize(30, 30);
            slots.getChildren().add(nodeSlots[i]);
        }
        creditsLabel = text("0 CR", MONO, 15, "#ffb347");
        VBox topRight = new VBox(4, text("CURFEW ENDS IN", MONO, 12, "rgba(126,243,232,0.6)"), clockLabel, slots, creditsLabel);
        topRight.setAlignment(Pos.TOP_RIGHT);
        VBox.setMargin(creditsLabel, new Insets(5, 0, 0, 0));
        AnchorPane.setTopAnchor(topRight, 20.0);
        AnchorPane.setRightAnchor(topRight, 22.0);

        // objective
        objectiveLabel = text("Find the three hacking nodes. Clear all three to release the exit bay.", BODY, 18, "#e8fbf8");
        objectiveLabel.setWrapText(true);
        objectiveLabel.setStyle(objectiveLabel.getStyle() + " -fx-font-weight: bold;");
        VBox objective = new VBox(2, text("OBJECTIVE", MONO, 11, "rgba(126,243,232,0.55)"), objectiveLabel);
        objective.setMaxWidth(330);
        objective.setPrefWidth(330);
        AnchorPane.setTopAnchor(objective, 104.0);
        AnchorPane.setLeftAnchor(objective, 22.0);

        // minimap
        minimap = new Pane();
        minimap.setPrefSize(210, 132);
        minimap.setStyle("-fx-background-color: rgba(4,12,14,0.8); -fx-border-color: rgba(53,224,216,0.4);");
        Region corridor = new Region();
        corridor.setLayoutY(132 * 0.41);
        corridor.setPrefSize(210, 132 * 0.18);
        corridor.setStyle("-fx-background-color: rgba(53,224,216,0.07); -fx-border-color: rgba(53,224,216,0.2) transparent;");
        minimap.getChildren().add(corridor);
        for (double f : new double[] {0.33, 0.67}) {
            Region div = new Region();
            div.setLayoutX(210 * f);
            div.setPrefSize(1, 132);
            div.setStyle("-fx-background-color: rgba(53,224,216,0.2);");
            minimap.getChildren().add(div);
        }
        mmPlayer = new Circle(3.5, Color.web("#7ef3e8"));
        mmPlayer.setStyle("-fx-effect: dropshadow(gaussian, #7ef3e8, 9, 0, 0, 0);");
        mmSentinel = new Circle(4.5, Color.web("#35e0d8"));
        mmLabel = text("FLOOR 2 — SCANNING", MONO, 9, "rgba(126,243,232,0.5)");
        mmLabel.setLayoutX(6);
        mmLabel.setLayoutY(116);
        minimap.getChildren().addAll(mmPlayer, mmSentinel, mmLabel);
        AnchorPane.setBottomAnchor(minimap, 22.0);
        AnchorPane.setRightAnchor(minimap, 22.0);

        // controls footer
        postureLabel = text("STANDING", MONO, 14, "rgba(126,243,232,0.55)");
        HBox posture = new HBox(9, text("POSTURE", MONO, 11, "rgba(126,243,232,0.5)"), postureLabel);
        posture.setAlignment(Pos.CENTER_LEFT);
        FlowPane keys = new FlowPane(16, 4);
        keys.setPrefWrapLength(540);
        for (String k : new String[] {"WASD MOVE", "SHIFT RUN", "C CROUCH", "SPACE JUMP",
                MOUSE_LOCK ? "MOUSE LOOK" : "DRAG LOOK", "E / CLICK INTERACT", "F HIDE", "ESC PAUSE"}) {
            keys.getChildren().add(text(k, MONO, 11, "rgba(126,243,232,0.5)"));
        }
        footer = new VBox(10, posture, keys);
        AnchorPane.setBottomAnchor(footer, 22.0);
        AnchorPane.setLeftAnchor(footer, 22.0);

        hud.getChildren().addAll(topLeft, topRight, objective, minimap, footer);

        // centred: crosshair, prompts, toast
        crosshair = new StackPane(new Circle(1.5, Color.web("#7ef3e8")));
        crosshair.setMaxSize(18, 18);
        crosshair.setMinSize(18, 18);
        crosshair.setStyle("-fx-border-color: rgba(126,243,232,0.45); -fx-border-radius: 9; -fx-border-width: 1;");

        promptLabel = text("", BODY, 18, "#e8fbf8");
        promptLabel.setStyle(promptLabel.getStyle() + " -fx-font-weight: bold;");
        promptBox = keyPrompt("E", "#35e0d8", "rgba(53,224,216,0.45)", promptLabel);
        promptBox.setTranslateY(44);
        hideLabel = text("", BODY, 16, "#ffd9a0");
        hideLabel.setStyle(hideLabel.getStyle() + " -fx-font-weight: bold;");
        hideBox = keyPrompt("F", "#ffb347", "rgba(255,179,71,0.5)", hideLabel);
        hideBox.setTranslateY(84);

        toastTag = text("", MONO, 11, "#35e0d8");
        toastText = text("", BODY, 22, "#e8fbf8");
        toastText.setStyle(toastText.getStyle() + " -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 14, 0, 0, 2);");
        toastBox = new VBox(5, toastTag, toastText);
        toastBox.setAlignment(Pos.CENTER);
        toastBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        toastBox.setVisible(false);
        toastBox.setTranslateY(Main.HEIGHT / 2.0 - 150);

        StackPane center = new StackPane(crosshair, promptBox, hideBox, toastBox);
        AnchorPane.setTopAnchor(center, 0.0);
        AnchorPane.setBottomAnchor(center, 0.0);
        AnchorPane.setLeftAnchor(center, 0.0);
        AnchorPane.setRightAnchor(center, 0.0);
        hud.getChildren().add(0, center);
        return hud;
    }

    private static Label fixedWidth(Label l, double w) {
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    private static StackPane bar(Region fill, double h, String track, String border, String fillStyle) {
        StackPane b = new StackPane(fill);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPrefSize(190, h);
        b.setMinSize(190, h);
        b.setMaxSize(190, h);
        b.setStyle("-fx-background-color: " + track + "; -fx-border-color: " + border + ";");
        fill.setStyle("-fx-background-color: " + fillStyle + ";");
        fill.setMaxHeight(Double.MAX_VALUE);
        return b;
    }

    private static HBox keyPrompt(String key, String keyBg, String border, Label label) {
        Label k = text(key, MONO, 13, "#05080b");
        k.setStyle(k.getStyle() + " -fx-background-color: " + keyBg + "; -fx-padding: 2 7 2 7;");
        HBox box = new HBox(10, k, label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(7, 16, 7, 16));
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: rgba(5,16,18,0.85); -fx-border-color: " + border + ";");
        box.setVisible(false);
        return box;
    }

    private void refreshHud() {
        integrityFill.setMaxWidth(190 * hp / 3.0);
        integrityLabel.setText(hp + "/3");
        creditsLabel.setText(credits + " CR");
        clockLabel.setText(secs / 60 + ":" + String.format("%02d", secs % 60));
        clockLabel.setStyle(clockLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + (secs < 60 ? "#ff5a4a" : "#e8fbf8") + ";"));
        String[] ids = {"node1", "node2", "node3"};
        for (int i = 0; i < 3; i++) {
            boolean got = tokens.contains(ids[i]);
            nodeSlots[i].setText(got ? "◆" : String.valueOf(i + 1));
            nodeSlots[i].setStyle("-fx-font-family: " + MONO + "; -fx-font-size: 12px;"
                + " -fx-text-fill: " + (got ? "#4dff9e" : "rgba(126,243,232,0.5)") + ";"
                + " -fx-background-color: " + (got ? "rgba(77,255,158,0.16)" : "rgba(6,20,22,0.7)") + ";"
                + " -fx-border-color: " + (got ? "rgba(77,255,158,0.8)" : "rgba(53,224,216,0.32)") + ";");
        }
        boolean playing = phase == Phase.PLAY && nodeGame == null;
        crosshair.setVisible(playing);
        footer.setVisible(playing);
        minimap.setVisible(phase != Phase.INTRO && nodeGame == null);
        promptBox.setVisible(playing && prompt != null);
        String hide = hiddenNow ? "Step out" : hideHint;
        hideBox.setVisible(playing && hide != null);
        if (hide != null) hideLabel.setText(hide);
        boolean chasing = "CHASE".equals(alert) && !hiddenNow && phase == Phase.PLAY;
        if (chasing != chaseFlash.isVisible()) {
            chaseFlash.setVisible(chasing);
            if (chasing) flashPulse.play(); else flashPulse.stop();
        }
    }

    private void toast(String msg, String tag, String color) {
        toastTag.setText(tag);
        toastTag.setStyle(toastTag.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + color + ";"));
        toastText.setText(msg);
        toastBox.setVisible(true);
        toastTimer.playFromStart();
    }

    /* ======================================================= world events */

    private final class WorldEvents implements CurfewWorld.Listener {
        @Override public void onHover(String id, String label, String verb) {
            prompt = id == null ? null : verb + " — " + label;
            if (prompt != null) promptLabel.setText(prompt);
            refreshHud();
        }

        @Override public void onUse(String id, String kind, boolean open) { handleUse(id, kind, open); }

        @Override public void onCoin(int amount, String source) {
            credits += amount;
            toast("+" + amount + " CR — " + source, "CREDITS", "#ffb347");
            refreshHud();
        }

        @Override public void onCaught() { caught(); }

        @Override public void onAlert(String state) {
            if ("CHASE".equals(state)) toast("The unit has you. Break line of sight.", "DETECTED", "#ff3d5a");
            alert = state;
            refreshHud();
        }

        @Override public void onHide(boolean on, String label, String blocked) {
            if (blocked != null) { toast(blocked, "BLOCKED", "#ffb347"); return; }
            hiddenNow = on;
            toast(on ? "Hidden — " + label : "Out in the open again",
                on ? "CONCEALED" : "EXPOSED", on ? "#4dff9e" : "#ffb347");
            refreshHud();
        }

        @Override public void onPosture(String p, String msg) {
            setPosture(p);
            if (msg != null) toast(msg, p, "CROUCHED".equals(p) ? "#7ef3e8" : "SEATED".equals(p) ? "#ffb347" : "#35e0d8");
        }

        @Override public void onTick(CurfewWorld.Tick t) {
            alert = t.state();
            hiddenNow = t.hidden();
            hideHint = t.hideHint();
            staminaFill.setMaxWidth(190 * t.stamina());
            setPosture(t.posture());
            String alertColor = "CHASE".equals(alert) ? "#ff3d5a" : "SEARCH".equals(alert) ? "#ffb347" : "#35e0d8";
            alertLabel.setText(hiddenNow ? "CONCEALED" : "CHASE".equals(alert) ? "PURSUIT" : "SEARCH".equals(alert) ? "SWEEPING" : "PATROLLING");
            alertLabel.setStyle(alertLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + alertColor + ";"));
            mmPlayer.setCenterX((t.px() + CurfewWorld.HX) / (CurfewWorld.HX * 2) * 210);
            mmPlayer.setCenterY((t.pz() + CurfewWorld.HZ) / (CurfewWorld.HZ * 2) * 132);
            mmSentinel.setCenterX((t.sx() + CurfewWorld.HX) / (CurfewWorld.HX * 2) * 210);
            mmSentinel.setCenterY((t.sz() + CurfewWorld.HZ) / (CurfewWorld.HZ * 2) * 132);
            mmSentinel.setFill(Color.web(alertColor));
            mmSentinel.setStyle("-fx-effect: dropshadow(gaussian, " + alertColor + ", 12, 0, 0, 0);");
            mmLabel.setText("FLOOR 2 — " + (t.dist() > 90 ? "SCANNING" : Math.round(t.dist()) + "M TO UNIT"));
            refreshHud();
        }
    }

    private void setPosture(String p) {
        postureLabel.setText(p);
        String c = "HIDDEN".equals(p) ? "#4dff9e" : "CROUCHED".equals(p) ? "#7ef3e8" : "SEATED".equals(p) ? "#ffb347" : "rgba(126,243,232,0.55)";
        postureLabel.setStyle(postureLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + c + ";"));
    }

    private void handleUse(String id, String kind, boolean open) {
        switch (kind) {
            case "terminal" -> {
                NodeInfo node = NODES.get(id);
                if (node == null) return;
                if (tokens.contains(id)) { toast("This node is already stripped.", "NODE", "#4dff9e"); return; }
                openGame(node.game(), id);
            }
            case "done" -> toast("Nothing left in this node.", "NODE", "#4dff9e");
            case "exit" -> {
                if (tokens.size() < 3) {
                    toast("Exit bay sealed — " + tokens.size() + "/3 nodes cleared.", "LOCKED", "#ff5a4a");
                    return;
                }
                world.openExit();
                finish(true, "win");
            }
            case "keybook" -> toast("Hollow ledger — a credit wedge was taped inside.", "LOOT", "#ffb347");
            case "book" -> toast("Just a textbook. Put it wherever.", "ITEM", "#7ef3e8");
            case "almirah" -> toast(open ? "Almirah open — you can climb in with F." : "Almirah shut.", "FURNITURE", "#7ef3e8");
            case "drawer" -> toast(open ? "Drawer open." : "Drawer shut.", "FURNITURE", "#7ef3e8");
            default -> { }
        }
    }

    private void caught() {
        hp--;
        hiddenNow = false;
        refreshHud();
        if (hp <= 0) { finish(false, "caught"); return; }
        toast("Grabbed. Integrity down — reset to the west stairwell.", "CAUGHT", "#ff3d5a");
    }

    /* ========================================================= phases */

    private void startRun() {
        phase = Phase.PLAY;
        secs = CURFEW_SECONDS;
        overlayLayer.getChildren().clear();
        world.setPaused(false);
        clock.play();
        lockMouse();
        refreshHud();
    }

    private void tickClock() {
        if (phase != Phase.PLAY || nodeGame != null) return;
        secs--;
        if (secs <= 0) { secs = 0; finish(false, "time"); }
        refreshHud();
    }

    private void pause() {
        if (phase != Phase.PLAY) return;
        phase = Phase.PAUSED;
        world.setPaused(true);
        unlockMouse();
        showPause();
        refreshHud();
    }

    private void resume() {
        phase = Phase.PLAY;
        overlayLayer.getChildren().clear();
        world.setPaused(false);
        lockMouse();
        refreshHud();
    }

    private void finish(boolean win, String why) {
        clock.stop();
        closeNodeGameSilently();
        world.setPaused(true);
        unlockMouse();
        phase = Phase.END;
        end = win ? "win" : why;
        showEnd();
        refreshHud();
    }

    /* ======================================================= node games */

    private void openGame(String kind, String nodeId) {
        activeNode = nodeId;
        world.setPaused(true);
        unlockMouse();
        nodeGame = switch (kind) {
            case "kp" -> CurfewNodeGames.kernelPanic(DIFFICULTY, this::closeGame);
            case "cb" -> CurfewNodeGames.circuitBreaker(this::closeGame);
            default -> CurfewNodeGames.silentCode(this::closeGame);
        };
        overlayLayer.getChildren().setAll(nodeGame.view());
        prompt = null;
        refreshHud();
    }

    private void closeGame(Outcome outcome) {
        if (nodeGame == null) return;
        nodeGame.stop();
        nodeGame = null;
        overlayLayer.getChildren().clear();
        String node = activeNode;
        activeNode = null;
        if (outcome == Outcome.WON && node != null) {
            int reward = NODES.get(node).reward();
            tokens.add(node);
            credits += reward;
            world.setTerminalDone(node);
            int n = tokens.size();
            toast("Node cleared. +" + reward + " CR, glitch token secured.", "NODE " + n + "/3", "#4dff9e");
            if (n >= 3) {
                world.unlockExit();
                objectiveLabel.setText("All three nodes cleared. Get to the EXIT BAY door, south-east corner.");
            } else {
                objectiveLabel.setText((3 - n) + " node" + (n == 2 ? "" : "s") + " left. Sweep the labs and the server room.");
            }
            world.stunSentinel(2.2);
        } else if (outcome == Outcome.FAILED) {
            toast("Kernel collapsed. Try the node again.", "FAILED", "#ff3d5a");
        } else {
            toast("Disconnected. The node is still live.", "ABORT", "#ffb347");
        }
        if (phase == Phase.PLAY) {
            world.setPaused(false);
            lockMouse();
        }
        refreshHud();
    }

    private void closeNodeGameSilently() {
        if (nodeGame != null) {
            nodeGame.stop();
            nodeGame = null;
            activeNode = null;
        }
    }

    /* ========================================================= overlays */

    private void showIntro() {
        Label chapter = text("CHAPTER 01", MONO, 13, "#35e0d8");
        Label title = text("OVERRIDE", BODY, 84, "#e8fbf8");
        title.setStyle(title.getStyle() + " -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(53,224,216,0.45), 40, 0, 0, 0);");
        Label sub = text("CURFEW PROTOCOL", BODY, 25, "#ffb347");
        sub.setStyle(sub.getStyle() + " -fx-font-weight: bold;");
        Label story = para("Six rooms. One sentinel unit walking the floor, and it does not get tired. Clear the three hacking nodes, "
            + "bank the credits, and reach the exit bay before curfew closes. Open the almirahs, pull the drawers, pull books off "
            + "the shelves — half the credits on this floor are hidden inside furniture. And when the visor turns red, get inside something.",
            20, "rgba(207,238,234,0.82)", 600);
        Label controls = text("WASD MOVE    SHIFT RUN    C CROUCH    SPACE JUMP    " + (MOUSE_LOCK ? "MOUSE" : "DRAG") + " / ARROWS LOOK    E INTERACT / SIT    F HIDE",
            MONO, 12, "rgba(126,243,232,0.7)");
        Label hint = para("Hiding: stand at an open almirah or at the front of a desk and press F — the prompt appears bottom-centre. "
            + "Crouching under a desk or sitting in a chair also cuts the unit's sight range almost in half.",
            16, "rgba(255,179,71,0.85)", 600);
        Button go = overlayButton("ENTER THE FLOOR", true);
        go.setOnAction(e -> startRun());
        Button back = overlayButton("← CHAPTER MAP", false);
        back.setOnAction(e -> leaveTo(new ChapterMapScreen().build()));
        HBox actions = new HBox(14, back, go);
        actions.setAlignment(Pos.CENTER);
        VBox.setMargin(actions, new Insets(8, 0, 0, 0));

        VBox box = new VBox(14, chapter, title, sub, story, controls, hint, actions);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        showOverlay(box, "radial-gradient(center 50% 40%, radius 80%, rgba(10,32,36,0.95), rgba(3,6,9,0.99))");
    }

    private void showPause() {
        Button resume = overlayButton("RESUME", true);
        resume.setOnAction(e -> resume());
        Button quit = overlayButton("QUIT TO CHAPTER MAP", false);
        quit.setOnAction(e -> leaveTo(new ChapterMapScreen().build()));
        VBox box = new VBox(18, text("SIMULATION PAUSED", MONO, 13, "#35e0d8"), resume, quit);
        box.setAlignment(Pos.CENTER);
        showOverlay(box, "rgba(3,6,9,0.82)");
    }

    private void showEnd() {
        boolean win = "win".equals(end);
        String tag, title, body, col;
        switch (end) {
            case "win" -> { tag = "FLOOR CLEARED"; title = "YOU GOT OUT"; col = "#4dff9e";
                body = "The exit bay door swung wide and the sentinel was two rooms behind you. Credits banked, tokens intact."; }
            case "time" -> { tag = "CURFEW CLOSED"; title = "TIME RAN OUT"; col = "#ffb347";
                body = "The floor locked down around you. Faster sweeps next run — the furniture pays, but it costs seconds."; }
            default -> { tag = "INTEGRITY ZERO"; title = "THE UNIT TOOK YOU"; col = "#ff3d5a";
                body = "Three grabs and the sentinel had your pattern. Use the almirahs — it cannot see through a closed shell."; }
        }
        Label titleLabel = text(title, BODY, 66, "#e8fbf8");
        titleLabel.setStyle(titleLabel.getStyle() + " -fx-font-weight: bold;");
        HBox stats = new HBox(34, stat("CREDITS", String.valueOf(credits), "#ffb347"),
            stat("NODES", tokens.size() + "/3", "#7ef3e8"), stat("GRADE", grade(), col));
        stats.setAlignment(Pos.CENTER);

        Button again = overlayButton("RUN IT AGAIN", false);
        again.setOnAction(e -> leaveTo(new CurfewProtocolScreen().build()));
        Button next = overlayButton(win ? "CONTINUE ▶" : "CHAPTER MAP", true);
        next.setOnAction(e -> {
            if (win) completeChapter(); else leaveTo(new ChapterMapScreen().build());
        });
        HBox actions = new HBox(14, again, next);
        actions.setAlignment(Pos.CENTER);
        VBox.setMargin(actions, new Insets(14, 0, 0, 0));

        VBox box = new VBox(14, text(tag, MONO, 13, col), titleLabel, para(body, 20, "rgba(207,238,234,0.8)", 540), stats, actions);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        showOverlay(box, "radial-gradient(center 50% 45%, radius 80%, rgba(10,32,36,0.95), rgba(3,6,9,0.99))");
    }

    private static VBox stat(String name, String value, String color) {
        VBox v = new VBox(2, text(name, MONO, 11, "rgba(126,243,232,0.6)"), text(value, MONO, 34, color));
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private static Label para(String s, double size, String color, double width) {
        Label l = text(s, BODY, size, color);
        l.setWrapText(true);
        l.setMaxWidth(width);
        l.setTextAlignment(TextAlignment.CENTER);
        return l;
    }

    private static Button overlayButton(String label, boolean primary) {
        Button b = new Button(label);
        b.setFocusTraversable(false);
        String base = "-fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: #35e0d8; -fx-text-fill: #e8fbf8;"
            + " -fx-font-family: " + MONO + "; -fx-font-size: 14px; -fx-padding: 13 38 13 38; -fx-cursor: hand;";
        String idle = base + " -fx-background-color: " + (primary ? "rgba(53,224,216,0.14)" : "transparent") + ";";
        String hover = base + " -fx-background-color: rgba(53,224,216,0.24);";
        b.setStyle(idle);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(idle));
        return b;
    }

    private void showOverlay(VBox content, String background) {
        StackPane shade = new StackPane(content);
        shade.setStyle("-fx-background-color: " + background + ";");
        overlayLayer.getChildren().setAll(shade);
    }

    private String grade() {
        int n = tokens.size();
        if ("win".equals(end)) return credits > 180 ? "S" : credits > 130 ? "A" : "B";
        return n == 3 ? "C" : n > 0 ? "D" : "F";
    }

    /* ==================================================== chapter result */

    private void completeChapter() {
        if (rewardsApplied) return;
        rewardsApplied = true;
        GameState state = GameState.get();
        // Campaign rewards are one-time, like the old Silent Classroom bank:
        // replays only improve the best score.
        boolean firstClear = state.getChapterCompleted() < 1;
        if (firstClear) {
            state.getPlayer().addXp(WIN_PLAYER_XP);
            state.addCoins(credits);
            state.addIndependentXp(WIN_INDEPENDENT_XP);
        }
        String g = grade();
        boolean newBest = state.recordSilentClassroomScore(scoreForGrade(g));
        state.completeChapter(1);
        SaveService.save();

        String summary = "Grade: " + g + (newBest ? "  (new best)" : "")
            + "\nIntegrity left: " + hp + "/3"
            + (firstClear
                ? "\nCredits banked: +" + credits + "\nXP: +" + WIN_PLAYER_XP + "   Independent XP: +" + WIN_INDEPENDENT_XP
                : "\nReplay — campaign rewards were already claimed.")
            + "\nInsight Charges for the Astra boss: " + state.getSilentClassroomInsightCharges()
            + "\n\nAyan made it out of the exit bay before curfew closed — no Astra, just nerve and a few good hiding spots."
            + "\nNext: Chapter 2 — Harvest Protocol.";

        leaveTo(new EndingScreen("Curfew Protocol complete", summary,
            () -> Main.switchScene(new ChapterMapScreen().build())).build());
    }

    /**
     * Maps the grade onto the Chapter 1 score scale, so it still feeds
     * GameState.insightChargesForScore for the Astra boss: S = 3, A = 2, B = 1.
     */
    private static int scoreForGrade(String grade) {
        return switch (grade) {
            case "S" -> 3000;
            case "A" -> 2300;
            case "B" -> 1500;
            default -> 0;
        };
    }

    private void leaveTo(Parent next) {
        dispose();
        Main.switchScene(next);
    }

    private void dispose() {
        clock.stop();
        toastTimer.stop();
        flashPulse.stop();
        closeNodeGameSilently();
        unlockMouse();
        detach();
        world.dispose();
        if (Main.getStage() != null) Main.getStage().focusedProperty().removeListener(focusListener);
    }

    /* ============================================================ input */

    private void wireInput() {
        root.sceneProperty().addListener((o, old, scene) -> {
            if (scene != null) attach(scene);
            else detach();
        });
        if (Main.getStage() != null) Main.getStage().focusedProperty().addListener(focusListener);

        var view = world.view();
        view.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dragging = true;
            dragDist = 0;
            dragX = e.getScreenX();
            dragY = e.getScreenY();
        });
        view.setOnMouseReleased(e -> dragging = false);
        view.setOnMouseDragged(this::mouseMoved);
        view.setOnMouseMoved(this::mouseMoved);
        view.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || phase != Phase.PLAY || nodeGame != null) return;
            if (MOUSE_LOCK && !mouseLockBroken && !mouseLocked) { lockMouse(); return; }
            if (dragDist <= 8) world.use();
        });
    }

    private void attach(Scene scene) {
        keyPressHandler = this::keyPressed;
        keyReleaseHandler = e -> {
            world.keyReleased(e.getCode());
            if (phase == Phase.PLAY) e.consume();
        };
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleaseHandler);
    }

    private void detach() {
        // Keep the persistent Scene clean: the event filters we added to the
        // Scene itself must be removed explicitly when this screen goes away.
        Scene scene = root == null ? null : root.getScene();
        if (scene != null) {
            if (keyPressHandler != null) scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressHandler);
            if (keyReleaseHandler != null) scene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleaseHandler);
        }
        keyPressHandler = null;
        keyReleaseHandler = null;
    }

    private void keyPressed(KeyEvent e) {
        KeyCode k = e.getCode();
        if (k == KeyCode.ESCAPE) {
            if (nodeGame != null) closeGame(Outcome.QUIT);
            else if (phase == Phase.PLAY) pause();
            else if (phase == Phase.PAUSED) resume();
            e.consume();
            return;
        }
        if (nodeGame != null) { nodeGame.onKey(k); e.consume(); return; }
        if (phase == Phase.PLAY) { world.keyPressed(k); e.consume(); }
    }

    private void mouseMoved(MouseEvent e) {
        if (phase != Phase.PLAY || nodeGame != null) return;
        if (mouseLocked) {
            double dx = e.getScreenX() - lockX, dy = e.getScreenY() - lockY;
            if (Math.abs(dx) >= 0.5 || Math.abs(dy) >= 0.5) {
                // ignore the jump an alt-tab or window move can produce
                if (Math.abs(dx) < 300 && Math.abs(dy) < 300) world.look(dx, dy);
                robot.mouseMove(lockX, lockY);
            }
            return;
        }
        if (!dragging) return;
        double dx = e.getScreenX() - dragX, dy = e.getScreenY() - dragY;
        dragX = e.getScreenX();
        dragY = e.getScreenY();
        dragDist += Math.abs(dx) + Math.abs(dy);
        world.look(dx, dy);
    }

    private void lockMouse() {
        if (!MOUSE_LOCK || mouseLockBroken || mouseLocked || root.getScene() == null) return;
        try {
            if (robot == null) robot = new Robot();
            Bounds b = world.view().localToScreen(world.view().getBoundsInLocal());
            lockX = Math.round(b.getCenterX());
            lockY = Math.round(b.getCenterY());
            robot.mouseMove(lockX, lockY);
            // If the pointer did not land where we put it (odd display scaling),
            // re-centring would spin the view, so fall back to drag-to-look.
            if (Math.abs(robot.getMouseX() - lockX) > 2 || Math.abs(robot.getMouseY() - lockY) > 2) {
                mouseLockBroken = true;
                return;
            }
            // Measure from where the pointer really rests (it can land a sub-pixel off).
            lockX = robot.getMouseX();
            lockY = robot.getMouseY();
            root.setCursor(Cursor.NONE);
            mouseLocked = true;
        } catch (Exception ex) {
            mouseLockBroken = true;   // no robot on this platform: drag-to-look still works
        }
    }

    private void unlockMouse() {
        mouseLocked = false;
        root.setCursor(Cursor.DEFAULT);
    }
}
