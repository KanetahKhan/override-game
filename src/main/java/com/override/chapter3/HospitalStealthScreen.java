package com.override.chapter3;

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
 * Chapter 3 stealth — Hospital Restricted Wing.
 *
 * Two med-patrol units and a stationary camera. The player navigates
 * a narrower corridor layout with cover objects (desks/beds).
 */
public class HospitalStealthScreen {

    private static final double W = 1000;
    private static final double H = 520;
    private static final double PLAYER_R = 10;

    private double px = W / 2, py = H - 30;

    // Patrol 1 — horizontal upper
    private double p1x = 150, p1y = 130;
    private double p1Speed = 2.0;
    private boolean p1Right = true;

    // Patrol 2 — vertical middle
    private double p2x = 700, p2y = 200;
    private double p2Speed = 1.8;
    private boolean p2Down = true;

    // Stationary camera at center
    private double camX = W / 2, camY = 260;
    private double camRadius = 90;

    private double coneW = 60, coneH = 170;

    private boolean[] keys = new boolean[256];
    private double mouseX = -1, mouseY = -1;
    private boolean mouseActive = false;

    private final Runnable onComplete;
    private Canvas canvas;
    private Label status;
    private AnimationTimer timer;
    private int alerts = 0;

    public HospitalStealthScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("STEALTH — RESTRICTED WING");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Hospital Infiltration");

        Label hint = UIFactory.body(
            "Reach Dr. Hana's holding room at the top.\nWASD or arrow keys to move.\n"
            + "Avoid two patrol units and the stationary camera (circle)."
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

        status = new Label("Two patrols. One camera. Stay silent.");
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
        double speed = 2.8;
        if (keys[javafx.scene.input.KeyCode.W.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.UP.getCode() & 0xFF]) py -= speed;
        if (keys[javafx.scene.input.KeyCode.S.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.DOWN.getCode() & 0xFF]) py += speed;
        if (keys[javafx.scene.input.KeyCode.A.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.LEFT.getCode() & 0xFF]) px -= speed;
        if (keys[javafx.scene.input.KeyCode.D.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.RIGHT.getCode() & 0xFF]) px += speed;
        px = Math.max(PLAYER_R, Math.min(W - PLAYER_R, px));
        py = Math.max(PLAYER_R, Math.min(H - PLAYER_R, py));

        // Patrol 1 — horizontal
        if (p1Right) { p1x += p1Speed; if (p1x > W - 100) p1Right = false; }
        else          { p1x -= p1Speed; if (p1x < 100) p1Right = true; }

        // Patrol 2 — vertical
        if (p2Down) { p2y += p2Speed; if (p2y > H - 100) p2Down = false; }
        else         { p2y -= p2Speed; if (p2y < 100) p2Down = true; }

        // Detection — patrol vision cones
        boolean detected = inConeLR(p1x, p1y) || inConeUD(p2x, p2y);

        // Detection — camera circle
        double dx = px - camX, dy = py - camY;
        if (Math.sqrt(dx * dx + dy * dy) < camRadius) detected = true;

        if (detected) {
            alerts++;
            status.setText("⚠ ALERT  ·  detections: " + alerts + "  ·  reset to entrance");
            px = W / 2; py = H - 30;
            GameState.get().increaseDependency(2);
            if (alerts >= 4) {
                status.setText("Lockdown triggered. Security notified. (Dependency +5)");
                GameState.get().increaseDependency(5);
            }
        }

        // Goal
        if (py < 45 && px > W / 2 - 60 && px < W / 2 + 60) {
            timer.stop();
            int xp = alerts == 0 ? 40 : 20;
            GameState.get().getPlayer().addXp(xp);
            if (alerts == 0) {
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffAwareness(2);
            }
            int coinReward = alerts == 0 ? 50 : 25;
            GameState.get().addCoins(coinReward);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                (alerts == 0 ? "Perfect infiltration. No alerts. +2 Awareness.\n\n"
                             : "You reached the holding room. Alerts triggered: " + alerts + ".\n\n")
                + "+" + xp + " XP   +" + coinReward + " ◈"
            );
            a.setHeaderText("Restricted wing accessed");
            a.showAndWait();
            onComplete.run();
        }
    }

    private boolean inConeLR(double ex, double ey) {
        double cx1 = ex - coneW / 2, cx2 = ex + coneW / 2;
        double cy1 = ey, cy2 = ey + coneH;
        return px > cx1 && px < cx2 && py > cy1 && py < cy2;
    }

    private boolean inConeUD(double ex, double ey) {
        // Vertical patrol — cone extends to the left
        double cx1 = ex - coneH, cx2 = ex;
        double cy1 = ey - coneW / 2, cy2 = ey + coneW / 2;
        return px > cx1 && px < cx2 && py > cy1 && py < cy2;
    }

    private void draw(GraphicsContext g) {
        g.setFill(Color.web("#0a0e14"));
        g.fillRect(0, 0, W, H);

        // Goal door
        g.setFill(Color.web("#28e0c044"));
        g.fillRect(W / 2 - 60, 0, 120, 30);
        g.setStroke(Color.web("#28e0c0"));
        g.setLineWidth(2);
        g.strokeRect(W / 2 - 60, 0, 120, 30);

        // Hospital floor lines
        g.setStroke(Color.web("#151e28"));
        g.setLineWidth(1);
        for (int y = 50; y < H; y += 50) g.strokeLine(0, y, W, y);
        for (int x = 50; x < W; x += 50) g.strokeLine(x, 0, x, H);

        // Camera circle
        g.setFill(Color.web("#ffd66e22"));
        g.fillOval(camX - camRadius, camY - camRadius, camRadius * 2, camRadius * 2);
        g.setStroke(Color.web("#ffd66e44"));
        g.strokeOval(camX - camRadius, camY - camRadius, camRadius * 2, camRadius * 2);
        // Camera icon
        g.setFill(Color.web("#ffd66e"));
        g.fillRect(camX - 6, camY - 6, 12, 12);

        // Patrol 1 — horizontal, cone below
        g.setFill(Color.web("#ff5e5e33"));
        g.fillRect(p1x - coneW / 2, p1y, coneW, coneH);
        g.setStroke(Color.web("#ff5e5e66"));
        g.strokeRect(p1x - coneW / 2, p1y, coneW, coneH);
        g.setFill(Color.web("#ff5e5e"));
        g.fillOval(p1x - 12, p1y - 12, 24, 24);

        // Patrol 2 — vertical, cone to the left
        g.setFill(Color.web("#ff8c0033"));
        g.fillRect(p2x - coneH, p2y - coneW / 2, coneH, coneW);
        g.setStroke(Color.web("#ff8c0066"));
        g.strokeRect(p2x - coneH, p2y - coneW / 2, coneH, coneW);
        g.setFill(Color.web("#ff8c00"));
        g.fillOval(p2x - 12, p2y - 12, 24, 24);

        // Player
        g.setFill(Color.web("#28e0c0"));
        g.fillOval(px - PLAYER_R, py - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);

        // Border
        g.setStroke(Color.web("#28e0c044"));
        g.setLineWidth(1);
        g.strokeRect(0, 0, W, H);
    }
}
