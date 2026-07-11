package com.override.prototype;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;

public final class SilentClassroomPrototypeApp extends GameApplication {

    private static final int VIEW_WIDTH = 1280;
    private static final int VIEW_HEIGHT = 720;

    /*
     * Tiled map:
     * 72 columns × 100 rows
     * Each tile is 32 × 32 pixels.
     */
    private static final int TILE_SIZE = 32;
    private static final int MAP_COLUMNS = 72;
    private static final int MAP_ROWS = 100;

 private static final int MAP_WIDTH =
        MAP_COLUMNS * TILE_SIZE;

private static final int MAP_HEIGHT =
        MAP_ROWS * TILE_SIZE;

    private static final double PLAYER_SIZE = 24;
    private static final double PLAYER_SPEED = 4;

    /*
     * Corresponds to the PlayerSpawn marker around tile 35,70.
     */
    private static final double PLAYER_START_X =
            35 * TILE_SIZE
                    + TILE_SIZE / 2.0
                    - PLAYER_SIZE / 2.0;

    private static final double PLAYER_START_Y =
            70 * TILE_SIZE
                    + TILE_SIZE / 2.0
                    - PLAYER_SIZE / 2.0;

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(VIEW_WIDTH);
        settings.setHeight(VIEW_HEIGHT);

        settings.setTitle("Silent Classroom - Collision Test");
        settings.setVersion("0.4");

        settings.setIntroEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);

        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);

        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(false);
    }

    @Override
    protected void initGame() {
        getGameScene().setBackgroundColor(
                Color.rgb(14, 17, 23)
        );

        /*
         * Register the factory before loading the TMX.
         */
        getGameWorld().addEntityFactory(
                new SilentClassroomFactory()
        );

        /*
         * Loads from:
         * src/main/resources/assets/levels/
         */
        setLevelFromMap(
                "academic-building-2-floor-2.tmx"
        );

        createPlayer();
        configureCamera();
    }

    private void createPlayer() {
        Rectangle temporaryPlayerView = new Rectangle(
                PLAYER_SIZE,
                PLAYER_SIZE,
                Color.CYAN
        );

        temporaryPlayerView.setStroke(Color.WHITE);
        temporaryPlayerView.setStrokeWidth(1.5);

        player = entityBuilder()
                .at(PLAYER_START_X, PLAYER_START_Y)
                .type(SilentClassroomType.PLAYER)
                .viewWithBBox(temporaryPlayerView)
                .collidable()
                .zIndex(100)
                .buildAndAttach();
    }

   private void configureCamera() {
    var viewport = getGameScene().getViewport();

    viewport.setBounds(
            0,
            0,
            MAP_WIDTH,
            MAP_HEIGHT
    );

    viewport.bindToEntity(
            player,
            getAppWidth() / 2.0,
            getAppHeight() / 2.0
    );

    viewport.setZoom(1.0);
}
    @Override
    protected void initInput() {
        // WASD movement
        onKey(
                KeyCode.W,
                () -> movePlayer(0, -PLAYER_SPEED)
        );

        onKey(
                KeyCode.S,
                () -> movePlayer(0, PLAYER_SPEED)
        );

        onKey(
                KeyCode.A,
                () -> movePlayer(-PLAYER_SPEED, 0)
        );

        onKey(
                KeyCode.D,
                () -> movePlayer(PLAYER_SPEED, 0)
        );

        // Arrow-key movement
        onKey(
                KeyCode.UP,
                () -> movePlayer(0, -PLAYER_SPEED)
        );

        onKey(
                KeyCode.DOWN,
                () -> movePlayer(0, PLAYER_SPEED)
        );

        onKey(
                KeyCode.LEFT,
                () -> movePlayer(-PLAYER_SPEED, 0)
        );

        onKey(
                KeyCode.RIGHT,
                () -> movePlayer(PLAYER_SPEED, 0)
        );
    }

    private void movePlayer(
            double movementX,
            double movementY
    ) {
        /*
         * Moving each axis separately allows the player
         * to slide along a wall instead of becoming stuck.
         */
        moveHorizontally(movementX);
        moveVertically(movementY);
    }

    private void moveHorizontally(double amount) {
        if (amount == 0) {
            return;
        }

        double previousX = player.getX();

        double nextX = clamp(
                previousX + amount,
                0,
                MAP_WIDTH - PLAYER_SIZE
        );

        player.setX(nextX);

        if (isTouchingWall()) {
            player.setX(previousX);
        }
    }

    private void moveVertically(double amount) {
        if (amount == 0) {
            return;
        }

        double previousY = player.getY();

        double nextY = clamp(
                previousY + amount,
                0,
                MAP_HEIGHT - PLAYER_SIZE
        );

        player.setY(nextY);

        if (isTouchingWall()) {
            player.setY(previousY);
        }
    }

    private boolean isTouchingWall() {
        return getGameWorld()
                .getEntitiesByType(
                        SilentClassroomType.WALL
                )
                .stream()
                .anyMatch(wall ->
                        player
                                .getBoundingBoxComponent()
                                .isCollidingWith(
                                        wall.getBoundingBoxComponent()
                                )
                );
    }

    private double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}