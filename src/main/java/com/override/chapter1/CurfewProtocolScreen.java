package com.override.chapter1;

import com.override.Main;
import com.override.chapter1.CurfewNodeGames.NodeGame;
import com.override.chapter1.CurfewNodeGames.Outcome;
import com.override.game.minigames.ChiptuneSfx;
import com.override.game.minigames.HighScoreClient;
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.override.chapter1.CurfewNodeGames.BODY;
import static com.override.chapter1.CurfewNodeGames.MONO;
import static com.override.chapter1.CurfewNodeGames.clock;
import static com.override.chapter1.CurfewNodeGames.text;

/**
 * Chapter 1 — Curfew Protocol, played inside the game window.
 *
 * A 3D stealth escape across one floor: clear three hacking nodes, bank
 * credits hidden in the furniture, and reach the exit bay before curfew
 * closes, while a sentinel hunts you. The 3D floor lives in
 * {@link CurfewWorld}; this screen owns the HUD, run state, the node
 * mini-games and the hand-off to Chapter 2 (Harvest Protocol).
 *
 * Astra is always one key away — the scan (Q) and the ASK ASTRA button in
 * every node — and every use lands on the Dependency Meter straight away.
 * Difficulty and settings ({@link CurfewSettings}) and the top escapes,
 * achievements and read notes ({@link CurfewRecords}) persist between runs.
 */
public class CurfewProtocolScreen {

    private enum Phase { INTRO, PLAY, PAUSED, END }

    private static final int WIN_PLAYER_XP = 60;
    /** Independent XP for clearing the floor without Astra's help. */
    private static final int WIN_INDEPENDENT_XP = 20;

    /** Astra scan (Q): the unit shows on the minimap for a few seconds, for a dependency cost. */
    private static final int ASTRA_SCAN_DEPENDENCY = 5;
    private static final long ASTRA_SCAN_MS = 6000, ASTRA_SCAN_COOLDOWN_MS = 25000;
    private static final String ASTRA_COLOR = "#b388ff";
    private static final String GOLD = "#ffd166";

    /** Escaping faster than this earns "Before the Bell". */
    private static final int SPEEDRUN_SECONDS = 4 * 60;

    /** A toast stays up at least this long before a queued one replaces it. */
    private static final long TOAST_MIN_MS = 1100;

    private static final DateTimeFormatter RUN_DATE = DateTimeFormatter.ofPattern("d MMM");

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

    // persistent between runs
    private CurfewSettings settings;
    private CurfewRecords records;
    private CurfewDifficulty difficulty;

    // run state
    private Phase phase = Phase.INTRO;
    private int hp = 3, credits, secs;
    private final Set<String> tokens = new HashSet<>();
    private boolean hiddenNow, sentinelSeen;
    private double suspicion;
    private String alert = "PATROL", end, prompt, hideHint, activeNode;
    private int books, detections, astraUses, dependencyAdded, ledgersFound;
    private long scanEndsAt, scanReadyAt;
    private final List<String> newAchievements = new ArrayList<>();

    // result, settled the moment the floor is cleared
    private boolean rewardsApplied, firstClear, newBest;
    private int indepAwarded, runRank;
    private String finalGrade;
    private HighScoreClient.Best allTimeBest;

    private CurfewWorld world;
    private NodeGame nodeGame;

    // scene nodes
    private final StackPane root = new StackPane();
    private final StackPane overlayLayer = new StackPane();
    private StackPane pauseShade, noteView, settingsView;
    private Region integrityFill, staminaFill, suspicionFill, chaseFlash;
    private Label integrityLabel, alertLabel, dependencyLabel, clockLabel, creditsLabel, objectiveLabel,
        postureLabel, booksLabel, promptLabel, hideLabel, toastTag, toastText, mmLabel, hackAlert;
    private HBox promptBox, hideBox;
    private VBox toastBox, footer;
    private StackPane suspicionBar;
    private final Label[] nodeSlots = new Label[3];
    private Pane minimap;
    private Circle mmPlayer, mmSentinel;
    private StackPane crosshair;

    // timers
    private final Timeline clockTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickClock()));
    private final PauseTransition toastTimer = new PauseTransition(Duration.millis(2600));
    private final PauseTransition toastNext = new PauseTransition();
    private final Deque<String[]> toastQueue = new ArrayDeque<>();
    private long toastShownAt;
    private FadeTransition flashPulse;

    // input
    private Robot robot;
    private boolean mouseLocked, mouseLockBroken, dragging;
    private final Set<KeyCode> held = EnumSet.noneOf(KeyCode.class);   // to ignore key repeat
    private double dragX, dragY, dragDist, lockX, lockY;
    private javafx.event.EventHandler<KeyEvent> keyPressHandler;
    private javafx.event.EventHandler<KeyEvent> keyReleaseHandler;
    private final ChangeListener<Boolean> focusListener = (o, was, focused) -> {
        if (!focused && phase == Phase.PLAY) pause();
    };

    public Parent build() {
        settings = CurfewSettings.load();
        records = CurfewRecords.load();
        difficulty = settings.difficulty;
        secs = difficulty.seconds;
        new HighScoreClient(difficulty.gameType()).refreshFromBackendAsync();

        world = new CurfewWorld(new WorldEvents());
        applySettings();

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

        // Shown over a node game: the unit keeps walking while you hack.
        hackAlert = text("", MONO, 13, "#35e0d8");
        hackAlert.setStyle(hackAlert.getStyle() + " -fx-background-color: rgba(3,8,10,0.9); -fx-padding: 7 16 7 16;"
            + " -fx-border-color: rgba(53,224,216,0.35);");
        hackAlert.setMouseTransparent(true);

        root.getChildren().addAll(world.view(), vignette, scan, chaseFlash, hud, overlayLayer);
        wireInput();

        clockTimer.setCycleCount(Animation.INDEFINITE);
        toastTimer.setOnFinished(e -> { if (toastQueue.isEmpty()) toastBox.setVisible(false); });
        toastNext.setOnFinished(e -> {
            String[] t = toastQueue.poll();
            if (t != null) showToast(t[0], t[1], t[2]);
        });

        showIntro();
        refreshHud();
        world.start();
        return root;
    }

    private void applySettings() {
        world.setLook(settings.sensitivity, settings.invertY);
        world.setFov(settings.fov);
        ChiptuneSfx.setMasterVolume(settings.volume);
    }

    /* =============================================================== HUD */

    private Pane buildHud() {
        AnchorPane hud = new AnchorPane();

        // top-left: integrity, stamina, dependency
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
        dependencyLabel = text("0%", MONO, 12, ASTRA_COLOR);
        HBox dependency = new HBox(12, fixedWidth(text("DEPENDENCY", MONO, 13, ASTRA_COLOR), 92), dependencyLabel);
        dependency.setAlignment(Pos.CENTER_LEFT);
        VBox topLeft = new VBox(11, integrity, stamina, dependency);
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
        AnchorPane.setTopAnchor(objective, 128.0);
        AnchorPane.setLeftAnchor(objective, 22.0);

        // minimap: the unit only shows while you can see it, or during an Astra scan
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
        mmSentinel = new Circle(4.5, Color.web("#ff3d5a"));
        mmLabel = text("FLOOR 2 — NO CONTACT", MONO, 9, "rgba(126,243,232,0.5)");
        mmLabel.setLayoutX(6);
        mmLabel.setLayoutY(116);
        minimap.getChildren().addAll(mmPlayer, mmSentinel, mmLabel);
        AnchorPane.setBottomAnchor(minimap, 22.0);
        AnchorPane.setRightAnchor(minimap, 22.0);

        // controls footer
        postureLabel = text("STANDING", MONO, 14, "rgba(126,243,232,0.55)");
        booksLabel = text("0", MONO, 14, "rgba(126,243,232,0.55)");
        Label booksTitle = text("BOOKS", MONO, 11, "rgba(126,243,232,0.5)");
        HBox.setMargin(booksTitle, new Insets(0, 0, 0, 18));
        HBox posture = new HBox(9, text("POSTURE", MONO, 11, "rgba(126,243,232,0.5)"), postureLabel, booksTitle, booksLabel);
        posture.setAlignment(Pos.CENTER_LEFT);
        FlowPane keys = new FlowPane(16, 4);
        keys.setPrefWrapLength(540);
        for (String k : new String[] {"WASD MOVE", "SHIFT RUN", "C CROUCH", "SPACE JUMP",
                MOUSE_LOCK ? "MOUSE LOOK" : "DRAG LOOK", "E / CLICK INTERACT", "F HIDE", "G THROW BOOK",
                "Q ASTRA SCAN", "ESC PAUSE"}) {
            keys.getChildren().add(text(k, MONO, 11, "rgba(126,243,232,0.5)"));
        }
        footer = new VBox(10, posture, keys);
        AnchorPane.setBottomAnchor(footer, 22.0);
        AnchorPane.setLeftAnchor(footer, 22.0);

        hud.getChildren().addAll(topLeft, topRight, objective, minimap, footer);

        // centred: crosshair, suspicion bar, prompts, toast
        crosshair = new StackPane(new Circle(1.5, Color.web("#7ef3e8")));
        crosshair.setMaxSize(18, 18);
        crosshair.setMinSize(18, 18);
        crosshair.setStyle("-fx-border-color: rgba(126,243,232,0.45); -fx-border-radius: 9; -fx-border-width: 1;");

        suspicionFill = new Region();
        suspicionBar = bar(suspicionFill, 5, "rgba(255,179,71,0.12)", "rgba(255,179,71,0.45)",
            "linear-gradient(to right, #ffb347, #ff5a4a)");
        suspicionBar.setTranslateY(-30);
        suspicionBar.setVisible(false);

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

        StackPane center = new StackPane(crosshair, suspicionBar, promptBox, hideBox, toastBox);
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

    private static void recolor(Label l, String color) {
        l.setStyle(l.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + color + ";"));
    }

    private boolean scanning() { return System.currentTimeMillis() < scanEndsAt; }

    private void refreshHud() {
        integrityFill.setMaxWidth(190 * hp / 3.0);
        integrityLabel.setText(hp + "/3");
        creditsLabel.setText(credits + " CR");
        dependencyLabel.setText(GameState.get().getDependency() + "%" + (astraUses > 0 ? "   ASTRA ×" + astraUses : ""));
        booksLabel.setText(String.valueOf(books));
        clockLabel.setText(clock(secs));
        recolor(clockLabel, secs < 60 ? "#ff5a4a" : "#e8fbf8");
        String[] ids = {"node1", "node2", "node3"};
        for (int i = 0; i < 3; i++) {
            boolean got = tokens.contains(ids[i]);
            nodeSlots[i].setText(got ? "◆" : String.valueOf(i + 1));
            nodeSlots[i].setStyle("-fx-font-family: " + MONO + "; -fx-font-size: 12px;"
                + " -fx-text-fill: " + (got ? "#4dff9e" : "rgba(126,243,232,0.5)") + ";"
                + " -fx-background-color: " + (got ? "rgba(77,255,158,0.16)" : "rgba(6,20,22,0.7)") + ";"
                + " -fx-border-color: " + (got ? "rgba(77,255,158,0.8)" : "rgba(53,224,216,0.32)") + ";");
        }
        boolean playing = phase == Phase.PLAY && nodeGame == null && noteView == null;
        crosshair.setVisible(playing);
        footer.setVisible(playing);
        minimap.setVisible(phase != Phase.INTRO && nodeGame == null && noteView == null);
        mmSentinel.setVisible(sentinelSeen || scanning());
        promptBox.setVisible(playing && prompt != null);
        String hide = hiddenNow ? "Step out" : hideHint;
        hideBox.setVisible(playing && hide != null);
        if (hide != null) hideLabel.setText(hide);
        boolean chasing = "CHASE".equals(alert) && !hiddenNow && phase == Phase.PLAY;
        suspicionBar.setVisible(playing && !chasing && !hiddenNow && suspicion > 0.02);
        suspicionFill.setMaxWidth(190 * suspicion);
        boolean flash = chasing && settings.chaseFlash;
        if (flash != chaseFlash.isVisible()) {
            chaseFlash.setVisible(flash);
            if (flash) flashPulse.play(); else flashPulse.stop();
        }
    }

    private void toast(String msg, String tag, String color) {
        // Two events often land together (open an almirah, find its stash):
        // let the first be read before the next one replaces it.
        long shown = System.currentTimeMillis() - toastShownAt;
        if (toastBox.isVisible() && shown < TOAST_MIN_MS) {
            if (toastQueue.size() < 3) toastQueue.add(new String[] {msg, tag, color});
            if (toastNext.getStatus() != Animation.Status.RUNNING) {
                toastNext.setDuration(Duration.millis(TOAST_MIN_MS - shown));
                toastNext.playFromStart();
            }
            return;
        }
        showToast(msg, tag, color);
    }

    private void showToast(String msg, String tag, String color) {
        toastTag.setText(tag);
        recolor(toastTag, color);
        toastText.setText(msg);
        toastBox.setVisible(true);
        toastShownAt = System.currentTimeMillis();
        toastTimer.playFromStart();
        if (!toastQueue.isEmpty()) {
            toastNext.setDuration(Duration.millis(TOAST_MIN_MS));
            toastNext.playFromStart();
        }
    }

    /* ======================================================= world events */

    private final class WorldEvents implements CurfewWorld.Listener {
        @Override public void onHover(String id, String label, String verb) {
            prompt = id == null ? null : verb + " — " + label;
            if (prompt != null) promptLabel.setText(prompt);
            crosshair.setStyle("-fx-border-color: " + (id == null ? "rgba(126,243,232,0.45)" : "#ffcf83")
                + "; -fx-border-radius: 9; -fx-border-width: " + (id == null ? "1" : "2") + ";");
            refreshHud();
        }

        @Override public void onUse(String id, String kind, boolean open) { handleUse(id, kind, open); }

        @Override public void onCoin(int amount, String source) {
            credits += amount;
            toast("+" + amount + " CR — " + source, "CREDITS", "#ffb347");
            refreshHud();
        }

        @Override public void onCaught(String how) { caught(how); }

        @Override public void onAlert(String state) {
            if ("CHASE".equals(state)) {
                detections++;
                toast("The unit has you. Break line of sight.", "DETECTED", "#ff3d5a");
            }
            alert = state;
            refreshHud();
        }

        @Override public void onNoise(String message) { toast(message, "NOISE", "#ffb347"); }

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
            suspicion = t.suspicion();
            sentinelSeen = t.sentinelSeen();
            books = t.books();
            staminaFill.setMaxWidth(190 * t.stamina());
            setPosture(t.posture());
            String alertColor = "CHASE".equals(alert) ? "#ff3d5a" : "SEARCH".equals(alert) ? "#ffb347" : "#35e0d8";
            boolean noticing = !hiddenNow && "PATROL".equals(alert) && suspicion > 0.05;
            alertLabel.setText(hiddenNow ? "CONCEALED" : "CHASE".equals(alert) ? "PURSUIT"
                : "SEARCH".equals(alert) ? "SWEEPING" : noticing ? "NOTICING" : "PATROLLING");
            recolor(alertLabel, noticing ? "#ffb347" : alertColor);
            mmPlayer.setCenterX((t.px() + CurfewWorld.HX) / (CurfewWorld.HX * 2) * 210);
            mmPlayer.setCenterY((t.pz() + CurfewWorld.HZ) / (CurfewWorld.HZ * 2) * 132);
            mmSentinel.setCenterX((t.sx() + CurfewWorld.HX) / (CurfewWorld.HX * 2) * 210);
            mmSentinel.setCenterY((t.sz() + CurfewWorld.HZ) / (CurfewWorld.HZ * 2) * 132);
            mmSentinel.setFill(Color.web("#ff3d5a"));
            mmSentinel.setStyle("-fx-effect: dropshadow(gaussian, #ff3d5a, 12, 0, 0, 0);");
            long dist = Math.round(t.dist());
            mmLabel.setText("FLOOR 2 — " + (scanning() ? "ASTRA FEED · " + dist + "M TO UNIT"
                : sentinelSeen ? dist + "M TO UNIT" : "NO CONTACT"));
            if (nodeGame != null) updateHackAlert(dist);
            refreshHud();
        }
    }

    private void setPosture(String p) {
        postureLabel.setText(p);
        recolor(postureLabel, "HIDDEN".equals(p) ? "#4dff9e" : "CROUCHED".equals(p) ? "#7ef3e8"
            : "SEATED".equals(p) ? "#ffb347" : "rgba(126,243,232,0.55)");
    }

    private void updateHackAlert(long dist) {
        boolean chase = "CHASE".equals(alert);
        hackAlert.setText(chase ? "THE UNIT HAS YOU — " + dist + "M · ESC TO DISCONNECT"
            : "JACKED IN · UNIT " + (sentinelSeen ? dist + "M" : "OUT OF SIGHT") + " · "
                + ("SEARCH".equals(alert) ? "SWEEPING" : "PATROLLING"));
        recolor(hackAlert, chase ? "#ff3d5a" : "SEARCH".equals(alert) ? "#ffb347" : "#35e0d8");
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
            case "keybook" -> {
                ledgersFound++;
                toast("Hollow ledger " + ledgersFound + "/" + CurfewWorld.LEDGERS + " — a credit wedge was taped inside.",
                    "LOOT", "#ffb347");
                if (ledgersFound >= CurfewWorld.LEDGERS) unlockAchievement("BOOKWORM");
            }
            case "book" -> toast("Book taken. G throws it — the thud pulls the unit away.", "ITEM", "#7ef3e8");
            case "note" -> openNote(id);
            case "almirah" -> toast(open ? "Almirah open — you can climb in with F." : "Almirah shut.", "FURNITURE", "#7ef3e8");
            case "workstation" -> toast(open ? "Workstation display on." : "Workstation display off.", "LAB", "#7ef3e8");
            case "drawer" -> toast(open ? "Drawer open." : "Drawer shut.", "FURNITURE", "#7ef3e8");
            default -> { }
        }
    }

    private void caught(String how) {
        boolean midHack = nodeGame != null;
        if (midHack) {
            closeNodeGameSilently();
            overlayLayer.getChildren().clear();
            world.setHacking(false);
        }
        hp--;
        hiddenNow = false;
        ChiptuneSfx.breach();
        refreshHud();
        if (hp <= 0) { finish(false, "caught"); return; }
        if (midHack) {
            world.setPaused(false);
            lockMouse();
        }
        toast(how + (midHack ? " The hack dropped." : "") + " Integrity down — back to the west stairwell.",
            "CAUGHT", "#ff3d5a");
    }

    /* ============================================================ Astra */

    /** Every Astra assist is a real choice: it lands on the Dependency Meter straight away. */
    private void useAstra(int dependency) {
        astraUses++;
        dependencyAdded += dependency;
        GameState.get().increaseDependency(dependency);
        ChiptuneSfx.emp();
    }

    private void astraScan() {
        long now = System.currentTimeMillis();
        if (now < scanReadyAt) {
            toast("Astra is recalibrating — " + ((scanReadyAt - now + 999) / 1000) + "s.", "ASTRA", ASTRA_COLOR);
            return;
        }
        useAstra(ASTRA_SCAN_DEPENDENCY);
        scanEndsAt = now + ASTRA_SCAN_MS;
        scanReadyAt = now + ASTRA_SCAN_COOLDOWN_MS;
        toast("\"I found it for you.\" The unit is on your minimap for " + ASTRA_SCAN_MS / 1000
            + " seconds. +" + ASTRA_SCAN_DEPENDENCY + " dependency.", "ASTRA SCAN", ASTRA_COLOR);
        refreshHud();
    }

    /* ===================================================== achievements */

    /** Unlocks it once, for good; returns true the first time. */
    private boolean unlockAchievement(String id) {
        if (!records.unlock(id)) return false;
        CurfewRecords.Achievement a = CurfewRecords.achievement(id);
        newAchievements.add(a.title());
        if (phase == Phase.PLAY) {
            toast(a.title() + " — " + a.description(), "ACHIEVEMENT UNLOCKED", GOLD);
            ChiptuneSfx.wave();
        }
        return true;
    }

    private void checkEscapeAchievements() {
        if (detections == 0) unlockAchievement("GHOST");
        if (astraUses == 0) unlockAchievement("LAST_REAL_MIND");
        if (difficulty.seconds - secs < SPEEDRUN_SECONDS) unlockAchievement("BEFORE_THE_BELL");
        if (hp == 3) unlockAchievement("UNTOUCHED");
        if (difficulty == CurfewDifficulty.HARD) unlockAchievement("NIGHT_SHIFT");
    }

    /* ============================================================ notes */

    private void openNote(String id) {
        CurfewLore.Note n = CurfewLore.byId(id);
        if (n == null || noteView != null) return;
        boolean fresh = records.markNoteRead(id);
        boolean archivist = fresh && records.notesRead() >= CurfewLore.NOTES.size() && unlockAchievement("ARCHIVIST");
        world.setPaused(true);
        unlockMouse();
        prompt = null;

        Label tag = text("NOTE · " + n.place() + (fresh
            ? "   ·   NEW — " + records.notesRead() + "/" + CurfewLore.NOTES.size() + " FOUND" : ""), MONO, 11, "#ffb347");
        Label title = text(n.title(), BODY, 26, "#e8fbf8");
        title.setStyle(title.getStyle() + " -fx-font-weight: bold;");
        Label body = text(n.body(), BODY, 17, "rgba(223,236,232,0.92)");
        body.setWrapText(true);
        body.setMaxWidth(560);
        body.setLineSpacing(3);
        VBox paper = new VBox(12, tag, title, body);
        if (archivist) paper.getChildren().add(text("ACHIEVEMENT UNLOCKED — Archivist: every note on the floor read.", MONO, 12, GOLD));
        paper.getChildren().add(text("E / ESC — PUT IT DOWN   ·   THE CURFEW CLOCK WAITS WHILE YOU READ", MONO, 11, "rgba(126,243,232,0.55)"));
        paper.setPadding(new Insets(26, 32, 22, 32));
        paper.setMaxSize(624, Region.USE_PREF_SIZE);
        paper.setStyle("-fx-background-color: linear-gradient(to bottom, #172225, #0d1618);"
            + " -fx-border-color: rgba(255,179,71,0.45); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 40, 0, 0, 8);");
        noteView = new StackPane(paper);
        noteView.setStyle("-fx-background-color: rgba(2,6,8,0.72);");
        overlayLayer.getChildren().add(noteView);
        refreshHud();
    }

    private void closeNote() {
        if (noteView == null) return;
        overlayLayer.getChildren().remove(noteView);
        noteView = null;
        if (phase == Phase.PLAY) {
            world.setPaused(false);
            lockMouse();
        }
        refreshHud();
    }

    /* ========================================================= settings */

    private void showSettings() {
        if (settingsView != null) return;
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);

        Label sensValue = text("", MONO, 13, "#e8fbf8");
        Slider sens = slider(0.3, 3.0, settings.sensitivity);
        sens.valueProperty().addListener((o, a, v) -> {
            settings.sensitivity = v.doubleValue();
            sensValue.setText(String.format("%.1f×", settings.sensitivity));
            applySettings();
        });
        sensValue.setText(String.format("%.1f×", settings.sensitivity));
        settingRow(grid, 0, "MOUSE SENSITIVITY", sens, sensValue);

        CheckBox invert = check(settings.invertY);
        invert.selectedProperty().addListener((o, a, v) -> { settings.invertY = v; applySettings(); });
        settingRow(grid, 1, "INVERT MOUSE Y", invert, null);

        Label fovValue = text("", MONO, 13, "#e8fbf8");
        Slider fov = slider(55, 100, settings.fov);
        fov.valueProperty().addListener((o, a, v) -> {
            settings.fov = v.doubleValue();
            fovValue.setText(Math.round(settings.fov) + "°");
            applySettings();
        });
        fovValue.setText(Math.round(settings.fov) + "°");
        settingRow(grid, 2, "FIELD OF VIEW", fov, fovValue);

        Label volValue = text("", MONO, 13, "#e8fbf8");
        Slider vol = slider(0, 1, settings.volume);
        vol.valueProperty().addListener((o, a, v) -> {
            settings.volume = v.doubleValue();
            volValue.setText(Math.round(settings.volume * 100) + "%");
            applySettings();
        });
        vol.setOnMouseReleased(e -> ChiptuneSfx.hit(4));   // a blip to hear the new level
        volValue.setText(Math.round(settings.volume * 100) + "%");
        settingRow(grid, 3, "VOLUME", vol, volValue);

        CheckBox flash = check(settings.chaseFlash);
        flash.selectedProperty().addListener((o, a, v) -> { settings.chaseFlash = v; refreshHud(); });
        settingRow(grid, 4, "RED PULSE WHEN CHASED", flash, null);

        Button done = overlayButton("DONE", true);
        done.setOnAction(e -> closeSettings());
        VBox box = new VBox(22, text("SETTINGS", MONO, 13, "#35e0d8"), grid, done);
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPadding(new Insets(28, 40, 28, 40));
        box.setStyle("-fx-background-color: rgba(5,16,18,0.97); -fx-border-color: rgba(53,224,216,0.45);");
        settingsView = new StackPane(box);
        settingsView.setStyle("-fx-background-color: rgba(2,6,8,0.6);");
        overlayLayer.getChildren().add(settingsView);
    }

    private void closeSettings() {
        if (settingsView == null) return;
        settings.save();
        overlayLayer.getChildren().remove(settingsView);
        settingsView = null;
    }

    private static void settingRow(GridPane grid, int row, String name, Node control, Label value) {
        grid.add(text(name, MONO, 12, "rgba(126,243,232,0.75)"), 0, row);
        grid.add(control, 1, row);
        if (value != null) grid.add(value, 2, row);
    }

    private static Slider slider(double min, double max, double value) {
        Slider s = new Slider(min, max, value);
        s.setPrefWidth(260);
        s.setFocusTraversable(false);
        return s;
    }

    private static CheckBox check(boolean on) {
        CheckBox c = new CheckBox();
        c.setSelected(on);
        c.setFocusTraversable(false);
        return c;
    }

    /* ========================================================= phases */

    private void startRun() {
        phase = Phase.PLAY;
        secs = difficulty.seconds;
        world.setDifficulty(difficulty.speed);
        world.setAwareness(difficulty.awareness);
        overlayLayer.getChildren().clear();
        world.setPaused(false);
        clockTimer.play();
        lockMouse();
        refreshHud();
    }

    private void tickClock() {
        // the curfew keeps counting during hacks, but waits while you read a note
        if (phase != Phase.PLAY || noteView != null) return;
        secs--;
        if (secs <= 0) { secs = 0; finish(false, "time"); }
        refreshHud();
    }

    private void pause() {
        if (phase != Phase.PLAY) return;
        phase = Phase.PAUSED;
        world.setPaused(true);
        world.setHacking(false);
        if (nodeGame != null) nodeGame.setPaused(true);
        unlockMouse();
        showPause();
        refreshHud();
    }

    private void resume() {
        if (phase != Phase.PAUSED) return;
        closeSettings();
        phase = Phase.PLAY;
        overlayLayer.getChildren().remove(pauseShade);
        pauseShade = null;
        if (nodeGame != null) {
            nodeGame.setPaused(false);
            world.setHacking(true);
        } else if (noteView == null) {
            world.setPaused(false);
            lockMouse();
        }
        refreshHud();
    }

    private void finish(boolean win, String why) {
        clockTimer.stop();
        closeNodeGameSilently();
        world.setHacking(false);
        world.setPaused(true);
        unlockMouse();
        phase = Phase.END;
        end = win ? "win" : why;
        toastQueue.clear();
        if (win) {
            applyRewards();
            recordRun();
            checkEscapeAchievements();
            ChiptuneSfx.wave();
        } else {
            ChiptuneSfx.gameOver();
            // Astra's help counts even on a failed run.
            if (astraUses > 0) SaveService.save();
        }
        showEnd();
        refreshHud();
    }

    /** Puts the escape on this difficulty's local top five and the all-time best (synced to the backend). */
    private void recordRun() {
        int score = runScore();
        runRank = records.addRun(difficulty, new CurfewRecords.Run(score, difficulty.seconds - secs, finalGrade,
            astraUses, System.currentTimeMillis()));
        int gradeRank = switch (finalGrade) { case "S" -> 4; case "A" -> 3; default -> 2; };
        // Best: score, then seconds left (most = fastest escape), grade rank, Astra used.
        allTimeBest = new HighScoreClient(difficulty.gameType())
            .submit(new HighScoreClient.Best(score, secs, gradeRank, astraUses > 0));
    }

    /* ======================================================= node games */

    private void openGame(String kind, String nodeId) {
        activeNode = nodeId;
        world.setPaused(true);
        world.setHacking(true);
        unlockMouse();
        nodeGame = switch (kind) {
            case "kp" -> CurfewNodeGames.kernelPanic(difficulty.kernelSpeed, this::closeGame);
            case "cb" -> CurfewNodeGames.circuitBreaker(difficulty.hackTime, this::closeGame);
            default -> CurfewNodeGames.silentCode(difficulty.hackTime, this::closeGame);
        };
        hackAlert.setText("JACKED IN · THE UNIT IS STILL WALKING");
        StackPane holder = new StackPane(nodeGame.view(), hackAlert);
        StackPane.setAlignment(hackAlert, Pos.TOP_CENTER);
        StackPane.setMargin(hackAlert, new Insets(22, 0, 0, 0));
        overlayLayer.getChildren().setAll(holder);
        prompt = null;
        refreshHud();
    }

    private void closeGame(Outcome outcome) {
        if (nodeGame == null) return;
        nodeGame.stop();
        nodeGame = null;
        world.setHacking(false);
        overlayLayer.getChildren().clear();
        String node = activeNode;
        activeNode = null;
        if ((outcome == Outcome.WON || outcome == Outcome.ASSISTED) && node != null) {
            boolean assisted = outcome == Outcome.ASSISTED;
            int reward = NODES.get(node).reward() / (assisted ? 2 : 1);
            if (assisted) useAstra(CurfewNodeGames.ASTRA_NODE_DEPENDENCY);
            else ChiptuneSfx.wave();
            tokens.add(node);
            credits += reward;
            world.setTerminalDone(node);
            int n = tokens.size();
            toast(assisted
                    ? "Astra cracked it for you. +" + reward + " CR, +" + CurfewNodeGames.ASTRA_NODE_DEPENDENCY + " dependency."
                    : "Node cleared. +" + reward + " CR, glitch token secured.",
                "NODE " + n + "/3", assisted ? ASTRA_COLOR : "#4dff9e");
            if (n >= 3) {
                world.unlockExit();
                world.lockdown();
                ChiptuneSfx.boss();
                objectiveLabel.setText("LOCKDOWN. The unit is faster now — get to the EXIT BAY door, south-east corner.");
                toast("The floor knows. Lights are red and the unit is sweeping for you.", "LOCKDOWN", "#ff3d5a");
            } else {
                objectiveLabel.setText((3 - n) + " node" + (n == 2 ? "" : "s") + " left. Sweep the labs and the server room.");
            }
            world.stunSentinel(2.2);
        } else if (outcome == Outcome.FAILED) {
            toast("Hack failed — the node reset itself. Try again.", "FAILED", "#ff3d5a");
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
        Label title = text("OVERRIDE", BODY, 64, "#e8fbf8");
        title.setStyle(title.getStyle() + " -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(53,224,216,0.45), 40, 0, 0, 0);");
        Label sub = text("CURFEW PROTOCOL", BODY, 24, "#ffb347");
        sub.setStyle(sub.getStyle() + " -fx-font-weight: bold;");
        Label story = para("Six rooms. One sentinel unit walking the floor, and it does not get tired. Clear the three hacking nodes, "
            + "bank the credits, and reach the exit bay before curfew closes. Half the credits are hidden in the furniture — "
            + "and it's never in the same place twice. Six notes left on this floor tell you what happened here.",
            18, "rgba(207,238,234,0.82)", 700);
        Label controls = text("WASD MOVE   SHIFT RUN   C CROUCH   SPACE JUMP   " + (MOUSE_LOCK ? "MOUSE" : "DRAG")
            + " / ARROWS LOOK   E INTERACT   F HIDE   G THROW   Q ASTRA", MONO, 12, "rgba(126,243,232,0.7)");
        Label hint = para("Hide at an open almirah or the front of a desk with F — but not while it's watching you. "
            + "The amber bar over your crosshair is the unit noticing you: break line of sight before it fills. "
            + "Running and landing jumps are loud; a thrown book pulls it away.",
            15, "rgba(255,179,71,0.85)", 700);
        Label astra = para("Astra is always one key away: Q scans for the unit, and every node has an ASK ASTRA button. "
            + "It never refuses. Every use raises your Dependency Meter.",
            15, ASTRA_COLOR, 700);

        HBox diffRow = new HBox(10, text("DIFFICULTY", MONO, 12, "rgba(126,243,232,0.6)"));
        diffRow.setAlignment(Pos.CENTER);
        for (CurfewDifficulty d : CurfewDifficulty.values()) {
            Button b = chip(d.label, d == difficulty);
            b.setOnAction(e -> {
                difficulty = d;
                settings.difficulty = d;
                settings.save();
                secs = d.seconds;
                new HighScoreClient(d.gameType()).refreshFromBackendAsync();
                showIntro();
                refreshHud();
            });
            diffRow.getChildren().add(b);
        }
        Label diffBlurb = text(difficulty.blurb, MONO, 12, "rgba(255,179,71,0.8)");
        Label recordsLine = text(recordsSummary(), MONO, 12, "rgba(126,243,232,0.65)");

        Button go = overlayButton("ENTER THE FLOOR", true);
        go.setOnAction(e -> startRun());
        Button back = overlayButton("← CHAPTER MAP", false);
        back.setOnAction(e -> leaveTo(new ChapterMapScreen().build()));
        Button gear = overlayButton("SETTINGS", false);
        gear.setOnAction(e -> showSettings());
        HBox actions = new HBox(14, back, gear, go);
        actions.setAlignment(Pos.CENTER);
        VBox.setMargin(actions, new Insets(6, 0, 0, 0));

        VBox box = new VBox(10, chapter, title, sub, story, controls, hint, astra, diffRow, diffBlurb, recordsLine, actions);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        showOverlay(box, "radial-gradient(center 50% 40%, radius 80%, rgba(10,32,36,0.95), rgba(3,6,9,0.99))");
    }

    private String recordsSummary() {
        List<CurfewRecords.Run> top = records.topRuns(difficulty);
        int fastest = records.fastest(difficulty);
        int allTime = new HighScoreClient(difficulty.gameType()).loadBest().score();
        return (top.isEmpty() ? "NO ESCAPES ON " + difficulty.label + " YET"
                : "YOUR BEST " + top.get(0).score() + " (" + top.get(0).grade() + ")   FASTEST " + clock(fastest))
            + (allTime > 0 ? "   ALL-TIME " + allTime : "")
            + "   ·   ACHIEVEMENTS " + records.achievementsUnlocked() + "/" + CurfewRecords.ACHIEVEMENTS.size()
            + "   ·   NOTES " + records.notesRead() + "/" + CurfewLore.NOTES.size();
    }

    private void showPause() {
        Button resume = overlayButton("RESUME", true);
        resume.setOnAction(e -> resume());
        Button gear = overlayButton("SETTINGS", false);
        gear.setOnAction(e -> showSettings());
        Button quit = overlayButton("QUIT TO CHAPTER MAP", false);
        quit.setOnAction(e -> leaveTo(new ChapterMapScreen().build()));
        VBox box = new VBox(18, text("SIMULATION PAUSED", MONO, 13, "#35e0d8"), resume, gear, quit);
        box.setAlignment(Pos.CENTER);
        // Stacked on top, so a node game or note underneath survives the pause.
        pauseShade = new StackPane(box);
        pauseShade.setStyle("-fx-background-color: rgba(3,6,9,0.82);");
        overlayLayer.getChildren().add(pauseShade);
    }

    private void showEnd() {
        boolean win = "win".equals(end);
        String tag, title, body, col;
        switch (end) {
            case "win" -> { tag = "FLOOR CLEARED · " + difficulty.label; title = "YOU GOT OUT"; col = "#4dff9e";
                body = "The exit bay door swung wide and the sentinel was two rooms behind you. Credits banked, tokens intact."
                    + (astraUses > 0 ? " Astra helped " + astraUses + "× — the Dependency Meter noted every one." : ""); }
            case "time" -> { tag = "CURFEW CLOSED"; title = "TIME RAN OUT"; col = "#ffb347";
                body = "The floor locked down around you. Faster sweeps next run — the furniture pays, but it costs seconds."; }
            default -> { tag = "INTEGRITY ZERO"; title = "THE UNIT TOOK YOU"; col = "#ff3d5a";
                body = "Three grabs and the sentinel had your pattern. Hide where it can't see you go in — and use the almirahs."; }
        }
        Label titleLabel = text(title, BODY, 56, "#e8fbf8");
        titleLabel.setStyle(titleLabel.getStyle() + " -fx-font-weight: bold;");
        HBox stats = new HBox(30,
            stat("CREDITS", String.valueOf(credits), "#ffb347"),
            stat("NODES", tokens.size() + "/3", "#7ef3e8"),
            stat(win ? "ESCAPE" : "TIME LEFT", clock(win ? difficulty.seconds - secs : secs), "#e8fbf8"),
            stat("DETECTED", detections + "×", "#ff5a4a"),
            stat("ASTRA", astraUses == 0 ? "NONE" : "×" + astraUses, ASTRA_COLOR),
            stat("GRADE", win ? finalGrade : grade(), col));
        stats.setAlignment(Pos.CENTER);

        Button again = overlayButton("RUN IT AGAIN", false);
        again.setOnAction(e -> leaveTo(new CurfewProtocolScreen().build()));
        Button next = overlayButton(win ? "CONTINUE ▶" : "CHAPTER MAP", true);
        next.setOnAction(e -> {
            if (win) completeChapter(); else leaveTo(new ChapterMapScreen().build());
        });
        HBox actions = new HBox(14, again, next);
        actions.setAlignment(Pos.CENTER);
        VBox.setMargin(actions, new Insets(10, 0, 0, 0));

        VBox box = new VBox(12, text(tag, MONO, 13, col), titleLabel, para(body, 18, "rgba(207,238,234,0.8)", 600), stats);
        if (win) {
            box.getChildren().add(text(scoreLine(), MONO, 12, "rgba(126,243,232,0.6)"));
            box.getChildren().add(leaderboard());
        }
        if (!newAchievements.isEmpty()) {
            box.getChildren().add(text("ACHIEVEMENT UNLOCKED: " + String.join(" · ", newAchievements)
                + "   (" + records.achievementsUnlocked() + "/" + CurfewRecords.ACHIEVEMENTS.size() + ")", MONO, 13, GOLD));
        }
        box.getChildren().add(actions);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        showOverlay(box, "radial-gradient(center 50% 45%, radius 80%, rgba(10,32,36,0.95), rgba(3,6,9,0.99))");
    }

    /** This difficulty's local top five, with this run highlighted, plus the all-time best. */
    private Node leaderboard() {
        VBox rows = new VBox(3);
        rows.getChildren().add(text("TOP ESCAPES · " + difficulty.label
            + (runRank > 0 ? "   ·   THIS RUN PLACED #" + runRank : ""), MONO, 11, "rgba(126,243,232,0.6)"));
        List<CurfewRecords.Run> top = records.topRuns(difficulty);
        for (int i = 0; i < top.size(); i++) {
            CurfewRecords.Run r = top.get(i);
            String date = LocalDate.ofInstant(Instant.ofEpochMilli(r.when()), ZoneId.systemDefault()).format(RUN_DATE);
            String line = String.format("%d.  %4d  %6s  %-2s  %-9s %s", i + 1, r.score(), clock(r.escapeSecs()),
                r.grade(), r.astra() == 0 ? "NO ASTRA" : "ASTRA ×" + r.astra(), date);
            rows.getChildren().add(text(line, MONO, 13, i + 1 == runRank ? "#4dff9e" : "#cfeeea"));
        }
        if (allTimeBest != null && allTimeBest.score() > 0) {
            rows.getChildren().add(text("ALL-TIME BEST " + allTimeBest.score()
                + (allTimeBest.combo() > 0 ? "   FASTEST " + clock(difficulty.seconds - allTimeBest.combo()) : "")
                + "   (shared through the game server when it's running)", MONO, 11, "rgba(126,243,232,0.6)"));
        }
        rows.setPadding(new Insets(10, 18, 10, 18));
        rows.setMaxWidth(Region.USE_PREF_SIZE);
        rows.setStyle("-fx-background-color: rgba(4,14,16,0.7); -fx-border-color: rgba(53,224,216,0.25);");
        return rows;
    }

    private static VBox stat(String name, String value, String color) {
        VBox v = new VBox(2, text(name, MONO, 11, "rgba(126,243,232,0.6)"), text(value, MONO, 30, color));
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

    /** A small toggle button, filled when selected. */
    private static Button chip(String label, boolean on) {
        Button b = new Button(label);
        b.setFocusTraversable(false);
        b.setStyle("-fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: #ffb347; -fx-cursor: hand;"
            + " -fx-font-family: " + MONO + "; -fx-font-size: 12px; -fx-padding: 7 22 7 22;"
            + " -fx-text-fill: " + (on ? "#05080b" : "#ffd9a0") + "; -fx-background-color: " + (on ? "#ffb347" : "transparent") + ";");
        return b;
    }

    private void showOverlay(VBox content, String background) {
        StackPane shade = new StackPane(content);
        shade.setStyle("-fx-background-color: " + background + ";");
        overlayLayer.getChildren().setAll(shade);
    }

    /* ======================================================= grading */

    /** Credits, time left and integrity, minus detections and Astra help. */
    private int runScore() {
        return credits + secs / 6 + hp * 10 - detections * 5 - astraUses * 20;
    }

    private String scoreLine() {
        return "SCORE " + runScore() + " = " + credits + " CR + " + secs / 6 + " TIME + " + hp * 10 + " INTEGRITY − "
            + detections * 5 + " DETECTED − " + astraUses * 20 + " ASTRA"
            + (astraUses > 0 ? "   ·   S NEEDS A RUN WITHOUT ASTRA" : "");
    }

    private String grade() {
        int n = tokens.size();
        if ("win".equals(end)) {
            int s = runScore();
            return s >= 280 && astraUses == 0 ? "S" : s >= 210 ? "A" : "B";
        }
        return n == 3 ? "C" : n > 0 ? "D" : "F";
    }

    /* ==================================================== chapter result */

    /** Pays out the moment the floor is cleared, so "Run it again" can't skip it. */
    private void applyRewards() {
        if (rewardsApplied) return;
        rewardsApplied = true;
        GameState state = GameState.get();
        // Campaign rewards are one-time, like the old Silent Classroom bank:
        // replays only improve the best score.
        firstClear = state.getChapterCompleted() < 1;
        indepAwarded = firstClear && astraUses == 0 ? WIN_INDEPENDENT_XP : 0;
        if (firstClear) {
            state.getPlayer().addXp(WIN_PLAYER_XP);
            state.addCoins(credits);
            state.addIndependentXp(indepAwarded);
        }
        finalGrade = grade();
        newBest = state.recordSilentClassroomScore(scoreForGrade(finalGrade));
        state.completeChapter(1);
        SaveService.save();
    }

    private void completeChapter() {
        GameState state = GameState.get();
        String summary = "Grade: " + finalGrade + (newBest ? "  (new best)" : "") + "   Difficulty: " + difficulty.label
            + "\nIntegrity left: " + hp + "/3   Times detected: " + detections
            + (firstClear
                ? "\nCredits banked: +" + credits + "\nXP: +" + WIN_PLAYER_XP + "   Independent XP: +" + indepAwarded
                : "\nReplay — campaign rewards were already claimed.")
            + "\nDependency: " + state.getDependency() + "%"
            + (astraUses > 0 ? "  (+" + dependencyAdded + " from Astra this run)" : "")
            + "\n\n" + (astraUses == 0
                ? "Ayan made it out of the exit bay before curfew closed — no Astra, just nerve and a few good hiding spots."
                : "Ayan made it out — but Astra opened the way " + astraUses + (astraUses == 1 ? " time" : " times")
                    + ". It will remember that you asked.")
            + "\nNext: Chapter 2 — Harvest Protocol.";

        leaveTo(new EndingScreen("Curfew Protocol complete", summary,
            () -> Main.switchScene(new ChapterMapScreen().build())).build());
    }

    /** Maps the grade onto the Chapter 1 best-score scale kept in GameState: S = 3000, A = 2300, B = 1500. */
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
        clockTimer.stop();
        toastTimer.stop();
        toastNext.stop();
        toastQueue.clear();
        flashPulse.stop();
        closeNodeGameSilently();
        if (settingsView != null) settings.save();
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
            if (e.getButton() != MouseButton.PRIMARY || phase != Phase.PLAY || nodeGame != null || noteView != null) return;
            if (MOUSE_LOCK && !mouseLockBroken && !mouseLocked) { lockMouse(); return; }
            if (dragDist <= 8) world.use();
        });
    }

    private void attach(Scene scene) {
        keyPressHandler = this::keyPressed;
        keyReleaseHandler = e -> {
            held.remove(e.getCode());
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
        boolean fresh = held.add(k);
        if (k == KeyCode.ESCAPE) {
            if (settingsView != null) closeSettings();
            else if (phase == Phase.PAUSED) resume();
            else if (noteView != null) closeNote();
            else if (nodeGame != null) closeGame(Outcome.QUIT);
            else if (phase == Phase.PLAY) pause();
            e.consume();
            return;
        }
        if (settingsView != null || phase == Phase.PAUSED) { e.consume(); return; }
        if (noteView != null) {
            if (fresh && k == KeyCode.E) closeNote();
            e.consume();
            return;
        }
        if (nodeGame != null) { nodeGame.onKey(k); e.consume(); return; }
        if (phase == Phase.PLAY) {
            if (k == KeyCode.Q) {
                if (fresh) astraScan();   // ignore key repeat
            } else {
                world.keyPressed(k);
            }
            e.consume();
        }
    }

    private void mouseMoved(MouseEvent e) {
        if (phase != Phase.PLAY || nodeGame != null || noteView != null) return;
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
