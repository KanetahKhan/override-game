package com.override.chapter1;

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
 * Stealth section.
 *
 * Top-down corridor. The player (cyan dot) starts at the bottom; the goal
 * (green tile) is at the top. A campus sentinel patrols horizontally. Its
 * vision cone is a rectangle in front of it — entering it raises the
 * alarm and resets the player to start.
 *
 * Controls: WASD or arrow keys.
 *
 * On reaching the goal, fires onComplete. Stealth mechanic in action even
 * in this prototype scope: detection radius, alerted state, reset state.
 */
public class StealthScreen {

    private static final double W = 1000;
    private static final double H = 520;

    private static final double PLAYER_R = 10;
    private double px = W / 2;
    private double py = H - 30;

    // Bot patrols horizontally in the upper corridor
    private double bx = 80;
    private double by = 120;
    private double botSpeed = 2.5;
    private boolean botGoingRight = true;

    // Vision cone: rectangle 60 wide x 220 tall in the bot's facing direction
    private double coneW = 60;
    private double coneH = 220;

    private boolean[] keys = new boolean[256];
    private double mouseX = -1, mouseY = -1;
    private boolean mouseActive = false;

    private final Runnable onComplete;
    private Canvas canvas;
    private Label status;
    private AnimationTimer timer;
    private int alerts = 0;

    public StealthScreen(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public Parent build() {
        Label tag = new Label("STEALTH — CAMPUS CORRIDOR");
        tag.getStyleClass().add("scene-tag");

        Label header = UIFactory.title("Avoid the Sentinel");

        Label hint = UIFactory.body(
            "Reach the green door at the top.\nMove your mouse over the game area to guide the player.\nDo not enter the bot's vision cone."
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

        status = new Label("Stay quiet.");
        status.getStyleClass().add("puzzle-feedback");

        VBox center = new VBox(10, tag, header, hint, canvas, status);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);

        StackPane sp = UIFactory.backdrop(wrap);

        // Capture keys at the scene level so focus issues don't block movement
        // Use event filters — these fire regardless of which node has focus
        sp.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
            e -> { keys[e.getCode().getCode() & 0xFF] = true; e.consume(); });
        sp.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED,
            e -> { keys[e.getCode().getCode() & 0xFF] = false; e.consume(); });

        // Also listen at Scene level once attached
        sp.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> keys[e.getCode().getCode() & 0xFF] = true);
                newScene.setOnKeyReleased(e -> keys[e.getCode().getCode() & 0xFF] = false);
            }
        });

        // Start animation loop
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
        // Player movement — keyboard
        double speed = 3.2;
        if (keys[javafx.scene.input.KeyCode.W.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.UP.getCode() & 0xFF]) py -= speed;
        if (keys[javafx.scene.input.KeyCode.S.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.DOWN.getCode() & 0xFF]) py += speed;
        if (keys[javafx.scene.input.KeyCode.A.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.LEFT.getCode() & 0xFF]) px -= speed;
        if (keys[javafx.scene.input.KeyCode.D.getCode() & 0xFF] || keys[javafx.scene.input.KeyCode.RIGHT.getCode() & 0xFF]) px += speed;

        // Player movement — mouse (move toward cursor)
        if (mouseActive) {
            double dx = mouseX - px, dy = mouseY - py;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 4) {
                px += speed * dx / dist;
                py += speed * dy / dist;
            }
        }

        px = Math.max(PLAYER_R, Math.min(W - PLAYER_R, px));
        py = Math.max(PLAYER_R, Math.min(H - PLAYER_R, py));

        // Bot patrol
        if (botGoingRight) {
            bx += botSpeed;
            if (bx > W - 100) botGoingRight = false;
        } else {
            bx -= botSpeed;
            if (bx < 100) botGoingRight = true;
        }

        // Detection: vision cone is below the bot (the corridor is north of bot)
        double cx1 = bx - coneW / 2, cx2 = bx + coneW / 2;
        double cy1 = by, cy2 = by + coneH;
        if (px > cx1 && px < cx2 && py > cy1 && py < cy2) {
            alerts++;
            status.setText("⚠ DETECTED  ·  alerts: " + alerts + "  ·  reset to start");
            // soft penalty: reset position, +small dependency
            px = W / 2; py = H - 30;
            GameState.get().increaseDependency(2);
            if (alerts >= 3) {
                status.setText("Too many alerts. The corridor is locked. (Dependency +5)");
                GameState.get().increaseDependency(5);
            }
        }

        // Goal: top region
        if (py < 50 && px > W / 2 - 60 && px < W / 2 + 60) {
            timer.stop();
            int xp = alerts == 0 ? 30 : 15;
            GameState.get().getPlayer().addXp(xp);
            if (alerts == 0) {
                GameState.get().addIndependentXp(xp);
                GameState.get().getPlayer().buffAwareness(1);
            }
            int coinReward = alerts == 0 ? 40 : 20;
            GameState.get().addCoins(coinReward);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                (alerts == 0 ? "Clean infiltration. +1 Awareness.\n\n" : "You made it through.\n\n")
                + "+" + xp + " XP   +" + coinReward + " ◈"
            );
            a.setHeaderText("Door reached");
            a.showAndWait();
            onComplete.run();
        }
    }

    private void draw(GraphicsContext g) {
        g.setFill(Color.web("#08111a"));
        g.fillRect(0, 0, W, H);

        // Goal door at top
        g.setFill(Color.web("#28e0c044"));
        g.fillRect(W / 2 - 60, 0, 120, 30);
        g.setStroke(Color.web("#28e0c0"));
        g.setLineWidth(2);
        g.strokeRect(W / 2 - 60, 0, 120, 30);

        // Vision cone
        g.setFill(Color.web("#ff5e5e44"));
        g.fillRect(bx - coneW / 2, by, coneW, coneH);
        g.setStroke(Color.web("#ff5e5e88"));
        g.setLineWidth(1);
        g.strokeRect(bx - coneW / 2, by, coneW, coneH);

        // Bot
        g.setFill(Color.web("#ff5e5e"));
        g.fillOval(bx - 14, by - 14, 28, 28);
        g.setFill(Color.web("#1a0a0a"));
        g.fillOval(bx - 4, by - 4, 8, 8);

        // Player
        g.setFill(Color.web("#28e0c0"));
        g.fillOval(px - PLAYER_R, py - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);

        // Border
        g.setStroke(Color.web("#28e0c044"));
        g.setLineWidth(1);
        g.strokeRect(0, 0, W, H);
    }
}
