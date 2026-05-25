package com.override.chapter2;

import com.override.shared.model.GameState;
import com.override.shared.ui.UIFactory;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Chapter 2 stealth — Drone Tower Infiltration.
 *
 * Two surveillance drones patrol the field. The player must reach
 * the tower control panel (top) without being spotted. Drones patrol
 * in opposite horizontal directions with wider vision cones than Ch1.
 */
public class DroneStealthScreen {

    private static final double W = 1000;
    private static final double H = 520;
    private static final double PLAYER_R = 10;

    private double px = W / 2, py = H - 30;

    // Drone 1 — patrols upper area
    private double d1x = 100, d1y = 100;
    private double d1Speed = 2.2;
    private boolean d1Right = true;

    // Drone 2 — patrols middle area
    private double d2x = W - 100, d2y = 280;
    private double d2Speed = 2.8;
    private boolean d2Right = false;

    private double coneW = 70, coneH = 180;

    private boolean[] keys = new boolean[256];
    private double mouseX = -1, mouseY = -1;
    private boolean mouseActive = false;

    private final Runnable onComplete;
    private Canvas canvas;
    private Label status;
    private AnimationTimer timer;
    private int alerts = 0;

    public DroneStealthScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("STEALTH — DRONE TOWER PERIMETER");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Drone Surveillance");

        Label hint = UIFactory.body(
            "Reach the tower entrance at the top.\nMove your mouse over the game area to guide the player.\n"
            + "Two drones patrol the area — avoid their scan zones."
        );
        hint.setMaxWidth(800);

        canvas = new Canvas(W, H);
        canvas.setFocusTraversable(true);
        canvas.setOnMouseClicked(e -> canvas.requestFocus());
        canvas.setOnMouseMoved(e -> { mouseX = e.getX(); mouseY = e.getY(); mouseActive = true; });
        canvas.setOnMouseDragged(e -> { mouseX = e.getX(); mouseY = e.getY(); mouseActive = true; });
        canvas.setOnMouseExited(e -> mouseActive = false);
        canvas.setOnKeyPressed(e -> { keys[e.getCode().getCode() & 0xFF] = true; e.consume(); });
        canvas.setOnKeyReleased(e -> { keys[e.getCode().getCode() & 0xFF] = false; e.consume(); });

        status = new Label("Stay low. Two drones active.");
        status.getStyleClass().add("puzzle-feedback");

        VBox center = new VBox(10, tag, header, hint, canvas, status);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);

        StackPane sp = UIFactory.backdrop(wrap);

        sp.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
            e -> { keys[e.getCode().getCode() & 0xFF] = true; e.consume(); });
        sp.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED,
            e -> { keys[e.getCode().getCode() & 0xFF] = false; e.consume(); });

        sp.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> keys[e.getCode().getCode() & 0xFF] = true);
                newScene.setOnKeyReleased(e -> keys[e.getCode().getCode() & 0xFF] = false);
            }
        });

        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                update();
                draw(canvas.getGraphicsContext2D());
            }
        };
        timer.start();

        return sp;
    }

    private void update() {
        double speed = 3.0;
        if (keys[javafx.scene.input.KeyCode.W.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.UP.getCode() & 0xFF]) py -= speed;
        if (keys[javafx.scene.input.KeyCode.S.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.DOWN.getCode() & 0xFF]) py += speed;
        if (keys[javafx.scene.input.KeyCode.A.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.LEFT.getCode() & 0xFF]) px -= speed;
        if (keys[javafx.scene.input.KeyCode.D.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.RIGHT.getCode() & 0xFF]) px += speed;

        if (mouseActive) {
            double dx = mouseX - px, dy = mouseY - py;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 4) { px += speed * dx / dist; py += speed * dy / dist; }
        }

        px = Math.max(PLAYER_R, Math.min(W - PLAYER_R, px));
        py = Math.max(PLAYER_R, Math.min(H - PLAYER_R, py));

        // Drone 1 patrol
        if (d1Right) { d1x += d1Speed; if (d1x > W - 80) d1Right = false; }
        else          { d1x -= d1Speed; if (d1x < 80) d1Right = true; }

        // Drone 2 patrol
        if (d2Right) { d2x += d2Speed; if (d2x > W - 80) d2Right = false; }
        else          { d2x -= d2Speed; if (d2x < 80) d2Right = true; }

        // Detection check for both drones
        if (inCone(d1x, d1y) || inCone(d2x, d2y)) {
            alerts++;
            status.setText("⚠ DRONE ALERT  ·  detections: " + alerts + "  ·  reset to start");
            px = W / 2; py = H - 30;
            GameState.get().increaseDependency(2);
            if (alerts >= 4) {
                status.setText("Multiple alerts. Perimeter lockdown triggered. (Dependency +5)");
                GameState.get().increaseDependency(5);
            }
        }

        // Goal
        if (py < 45 && px > W / 2 - 60 && px < W / 2 + 60) {
            timer.stop();
            int xp = alerts == 0 ? 35 : 18;
            GameState.get().getPlayer().addXp(xp);
            if (alerts == 0) {
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffAwareness(2);
            }
            int coinReward = alerts == 0 ? 45 : 25;
            GameState.get().addCoins(coinReward);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                (alerts == 0 ? "Ghost run. No drone saw you. +2 Awareness.\n\n"
                             : "You reached the tower. Drones spotted you " + alerts + " time(s).\n\n")
                + "+" + xp + " XP   +" + coinReward + " ◈"
            );
            a.setHeaderText("Tower reached");
            a.showAndWait();
            onComplete.run();
        }
    }

    private boolean inCone(double dx, double dy) {
        double cx1 = dx - coneW / 2, cx2 = dx + coneW / 2;
        double cy1 = dy, cy2 = dy + coneH;
        return px > cx1 && px < cx2 && py > cy1 && py < cy2;
    }

    private void draw(GraphicsContext g) {
        g.setFill(Color.web("#0a1208"));
        g.fillRect(0, 0, W, H);

        // Goal — tower entrance
        g.setFill(Color.web("#28e0c044"));
        g.fillRect(W / 2 - 60, 0, 120, 30);
        g.setStroke(Color.web("#28e0c0"));
        g.setLineWidth(2);
        g.strokeRect(W / 2 - 60, 0, 120, 30);

        // Draw field rows (decorative)
        g.setStroke(Color.web("#1a3010"));
        g.setLineWidth(1);
        for (int y = 60; y < H; y += 40) {
            g.strokeLine(0, y, W, y);
        }

        // Drone 1 vision cone + body
        drawDrone(g, d1x, d1y, "#ff8c00");

        // Drone 2 vision cone + body
        drawDrone(g, d2x, d2y, "#ff5e5e");

        // Player
        g.setFill(Color.web("#28e0c0"));
        g.fillOval(px - PLAYER_R, py - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);

        // Border
        g.setStroke(Color.web("#28e0c044"));
        g.setLineWidth(1);
        g.strokeRect(0, 0, W, H);
    }

    private void drawDrone(GraphicsContext g, double dx, double dy, String color) {
        // Vision cone
        g.setFill(Color.web(color + "33"));
        g.fillRect(dx - coneW / 2, dy, coneW, coneH);
        g.setStroke(Color.web(color + "66"));
        g.setLineWidth(1);
        g.strokeRect(dx - coneW / 2, dy, coneW, coneH);

        // Drone body (diamond shape)
        g.setFill(Color.web(color));
        double[] xpts = {dx, dx + 12, dx, dx - 12};
        double[] ypts = {dy - 14, dy, dy + 14, dy};
        g.fillPolygon(xpts, ypts, 4);
    }
}
