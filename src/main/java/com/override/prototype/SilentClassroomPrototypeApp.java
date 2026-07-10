package com.override.prototype;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGL.onKey;

public final class SilentClassroomPrototypeApp extends GameApplication {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;

    private static final double PLAYER_SIZE = 32;
    private static final double SPEED = 4;

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setTitle("Silent Classroom - FXGL Test");
        settings.setVersion("0.1");
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);
    }

    @Override
    protected void initGame() {
        createBackground();
        createWalls();
        createPlayer();
    }

    @Override
    protected void initInput() {
        onKey(KeyCode.W, () -> movePlayer(0, -SPEED));
        onKey(KeyCode.S, () -> movePlayer(0, SPEED));
        onKey(KeyCode.A, () -> movePlayer(-SPEED, 0));
        onKey(KeyCode.D, () -> movePlayer(SPEED, 0));
    }

    private void createBackground() {
        entityBuilder()
                .at(0, 0)
                .view(new Rectangle(
                        WIDTH,
                        HEIGHT,
                        Color.rgb(18, 21, 30)
                ))
                .buildAndAttach();
    }

    private void createPlayer() {
        player = entityBuilder()
                .at(80, 80)
                .viewWithBBox(new Rectangle(
                        PLAYER_SIZE,
                        PLAYER_SIZE,
                        Color.CYAN
                ))
                .buildAndAttach();
    }

    private void createWalls() {
        Color wallColor = Color.rgb(100, 55, 55);

        // Outer walls
        createWall(0, 0, WIDTH, 32, wallColor);
        createWall(0, HEIGHT - 32, WIDTH, 32, wallColor);
        createWall(0, 0, 32, HEIGHT, wallColor);
        createWall(WIDTH - 32, 0, 32, HEIGHT, wallColor);

        // Temporary classroom walls
        createWall(250, 130, 220, 32, wallColor);
        createWall(580, 280, 32, 170, wallColor);
    }

    private void createWall(
            double x,
            double y,
            double width,
            double height,
            Color color
    ) {
        entityBuilder()
                .at(x, y)
                .viewWithBBox(new Rectangle(width, height, color))
                .buildAndAttach();
    }

    private void movePlayer(double dx, double dy) {
        double nextX = player.getX() + dx;
        double nextY = player.getY() + dy;

        double minimumX = 32;
        double minimumY = 32;

        double maximumX = WIDTH - 32 - PLAYER_SIZE;
        double maximumY = HEIGHT - 32 - PLAYER_SIZE;

        player.setX(clamp(nextX, minimumX, maximumX));
        player.setY(clamp(nextY, minimumY, maximumY));
    }

    private double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static void main(String[] args) {
        launch(args);
    }
}