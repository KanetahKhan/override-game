package com.override.chapter4;

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
 * Chapter 4 stealth — Data Center Floor.
 *
 * Two static sweeping cameras (rotating cones) plus a roaming security
 * drone in a figure-8 path. The corridors between server racks (drawn as
 * blocks) form a tighter grid, so the player has to time movements between
 * sweeps. Reaching the legacy server rack at the top finishes the room.
 */
public class ServerStealthScreen {

    private static final double W = 1000;
    private static final double H = 520;
    private static final double PLAYER_R = 10;

    private double px = W / 2, py = H - 30;

    // Sweeping camera 1 — top left, sweeps from -90° toward 0°
    private double cam1X = 200, cam1Y = 90;
    private double cam1Angle = -90; // facing down
    private double cam1Speed = 1.4;
    private double cam1Min = -135, cam1Max = -45;
    private boolean cam1Forward = true;

    // Sweeping camera 2 — top right
    private double cam2X = 800, cam2Y = 90;
    private double cam2Angle = -90;
    private double cam2Speed = 1.6;
    private double cam2Min = -135, cam2Max = -45;
    private boolean cam2Forward = false;

    // Security drone — figure-8 around the middle
    private double droneT = 0;
    private double droneRadius = 220;

    // Vision cone — forward arc, length 220, half-angle 22°
    private static final double CONE_LEN = 220;
    private static final double CONE_HALF_DEG = 22;

    private boolean[] keys = new boolean[256];

    private final Runnable onComplete;
    private Canvas canvas;
    private Label status;
    private AnimationTimer timer;
    private int alerts = 0;

    public ServerStealthScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("STEALTH — DATA CENTER FLOOR 3");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Reach the Legacy Rack");

        Label hint = UIFactory.body(
            "Slip past two sweeping cameras and the patrol drone.\n"
            + "WASD or arrow keys. The legacy rack is the green tile at the top.\n"
            + "Server cabinets (gray blocks) do not block movement, but they break line of sight visually."
        );
        hint.setMaxWidth(820);

        canvas = new Canvas(W, H);
        canvas.setFocusTraversable(true);
        canvas.setOnMouseClicked(e -> canvas.requestFocus());
        canvas.setOnKeyPressed(e -> { keys[e.getCode().getCode() & 0xFF] = true; e.consume(); });
        canvas.setOnKeyReleased(e -> { keys[e.getCode().getCode() & 0xFF] = false; e.consume(); });

        status = new Label("Two sweeping cameras. One drone. Stay quiet.");
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
        // Player movement
        double speed = 2.7;
        if (keys[javafx.scene.input.KeyCode.W.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.UP.getCode() & 0xFF]) py -= speed;
        if (keys[javafx.scene.input.KeyCode.S.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.DOWN.getCode() & 0xFF]) py += speed;
        if (keys[javafx.scene.input.KeyCode.A.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.LEFT.getCode() & 0xFF]) px -= speed;
        if (keys[javafx.scene.input.KeyCode.D.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.RIGHT.getCode() & 0xFF]) px += speed;
        px = Math.max(PLAYER_R, Math.min(W - PLAYER_R, px));
        py = Math.max(PLAYER_R, Math.min(H - PLAYER_R, py));

        // Sweep cam 1
        if (cam1Forward) { cam1Angle += cam1Speed; if (cam1Angle > cam1Max) cam1Forward = false; }
        else              { cam1Angle -= cam1Speed; if (cam1Angle < cam1Min) cam1Forward = true; }

        // Sweep cam 2
        if (cam2Forward) { cam2Angle += cam2Speed; if (cam2Angle > cam2Max) cam2Forward = false; }
        else              { cam2Angle -= cam2Speed; if (cam2Angle < cam2Min) cam2Forward = true; }

        // Drone figure-8 around center
        droneT += 0.018;
        double dxC = W / 2.0 + droneRadius * Math.sin(droneT * 2);
        double dyC = H / 2.0 + droneRadius * 0.45 * Math.sin(droneT * 4);

        // Detection — three cones
        boolean detected =
                inCone(cam1X, cam1Y, cam1Angle)
             || inCone(cam2X, cam2Y, cam2Angle)
             || inCircle(dxC, dyC, 70);

        if (detected) {
            alerts++;
            status.setText("⚠ DETECTED  ·  alerts: " + alerts + "  ·  reset to entrance");
            px = W / 2; py = H - 30;
            GameState.get().increaseDependency(2);
            if (alerts >= 4) {
                status.setText("Lockdown imminent. Security drones converge. (Dependency +5)");
                GameState.get().increaseDependency(5);
            }
        }

        // Goal
        if (py < 45 && px > W / 2 - 60 && px < W / 2 + 60) {
            timer.stop();
            int xp = alerts == 0 ? 45 : 22;
            GameState.get().getPlayer().addXp(xp);
            if (alerts == 0) {
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffAwareness(2);
            }
            int coinReward = alerts == 0 ? 60 : 30;
            GameState.get().addCoins(coinReward);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                (alerts == 0 ? "Ghost run. The cameras never saw you. +2 Awareness.\n\n"
                             : "You reached the legacy rack. Alerts triggered: " + alerts + ".\n\n")
                + "+" + xp + " XP   +" + coinReward + " ◈"
            );
            a.setHeaderText("Legacy rack reached");
            a.showAndWait();
            onComplete.run();
        }
    }

    private boolean inCone(double ex, double ey, double angleDeg) {
        double dx = px - ex, dy = py - ey;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > CONE_LEN || dist < 1) return false;
        double pointAng = Math.toDegrees(Math.atan2(dy, dx));
        double diff = Math.abs(normalizeDeg(pointAng - angleDeg));
        return diff < CONE_HALF_DEG;
    }

    private boolean inCircle(double cx, double cy, double r) {
        double dx = px - cx, dy = py - cy;
        return dx * dx + dy * dy < r * r;
    }

    private double normalizeDeg(double d) {
        while (d > 180) d -= 360;
        while (d < -180) d += 360;
        return d;
    }

    private void draw(GraphicsContext g) {
        g.setFill(Color.web("#06090e"));
        g.fillRect(0, 0, W, H);

        // Floor grid
        g.setStroke(Color.web("#11181f"));
        g.setLineWidth(1);
        for (int y = 50; y < H; y += 50) g.strokeLine(0, y, W, y);
        for (int x = 50; x < W; x += 50) g.strokeLine(x, 0, x, H);

        // Server rack decorations
        g.setFill(Color.web("#1a232c"));
        g.fillRect(120, 200, 40, 110);
        g.fillRect(220, 200, 40, 110);
        g.fillRect(740, 200, 40, 110);
        g.fillRect(840, 200, 40, 110);
        g.fillRect(420, 350, 40, 110);
        g.fillRect(580, 350, 40, 110);
        g.setFill(Color.web("#28e0c022"));
        g.fillRect(140, 215, 4, 4);
        g.fillRect(240, 230, 4, 4);
        g.fillRect(760, 245, 4, 4);
        g.fillRect(860, 218, 4, 4);

        // Goal door
        g.setFill(Color.web("#28e0c044"));
        g.fillRect(W / 2 - 60, 0, 120, 30);
        g.setStroke(Color.web("#28e0c0"));
        g.setLineWidth(2);
        g.strokeRect(W / 2 - 60, 0, 120, 30);

        // Sweeping cone 1
        drawCone(g, cam1X, cam1Y, cam1Angle, "#ff5e5e");
        // Sweeping cone 2
        drawCone(g, cam2X, cam2Y, cam2Angle, "#ff8c00");

        // Camera bodies
        g.setFill(Color.web("#ff5e5e"));
        g.fillOval(cam1X - 8, cam1Y - 8, 16, 16);
        g.setFill(Color.web("#ff8c00"));
        g.fillOval(cam2X - 8, cam2Y - 8, 16, 16);

        // Drone (figure-8)
        double dxC = W / 2.0 + droneRadius * Math.sin(droneT * 2);
        double dyC = H / 2.0 + droneRadius * 0.45 * Math.sin(droneT * 4);
        g.setFill(Color.web("#ffd66e22"));
        g.fillOval(dxC - 70, dyC - 70, 140, 140);
        g.setStroke(Color.web("#ffd66e44"));
        g.strokeOval(dxC - 70, dyC - 70, 140, 140);
        g.setFill(Color.web("#ffd66e"));
        g.fillOval(dxC - 12, dyC - 12, 24, 24);
        g.setFill(Color.web("#1a1a1a"));
        g.fillOval(dxC - 4, dyC - 4, 8, 8);

        // Player
        g.setFill(Color.web("#28e0c0"));
        g.fillOval(px - PLAYER_R, py - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);

        // Border
        g.setStroke(Color.web("#28e0c044"));
        g.setLineWidth(1);
        g.strokeRect(0, 0, W, H);
    }

    private void drawCone(GraphicsContext g, double cx, double cy, double angDeg, String hex) {
        double a1 = Math.toRadians(angDeg - CONE_HALF_DEG);
        double a2 = Math.toRadians(angDeg + CONE_HALF_DEG);
        double x1 = cx + CONE_LEN * Math.cos(a1);
        double y1 = cy + CONE_LEN * Math.sin(a1);
        double x2 = cx + CONE_LEN * Math.cos(a2);
        double y2 = cy + CONE_LEN * Math.sin(a2);
        g.setFill(Color.web(hex + "33"));
        g.fillPolygon(new double[]{cx, x1, x2}, new double[]{cy, y1, y2}, 3);
        g.setStroke(Color.web(hex + "66"));
        g.strokePolygon(new double[]{cx, x1, x2}, new double[]{cy, y1, y2}, 3);
    }
}
