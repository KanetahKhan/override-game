package com.override.chapter2;

import com.override.Main;
import com.override.shared.service.SaveService;
import com.override.shared.ui.ChapterMapScreen;
import com.override.shared.ui.UIFactory;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ChapterTwoScreen {

    private static final int W = 1280;
    private static final int H = 720;
    private static final double PLAYER_H = 150;
    private static final double PLAYER_W = 120;
    private static final double FEET_OFFSET = 20;
    private static final double CROUCH_Y_OFFSET = 8;
    private static final double WALK_SPEED = 3.0;
    private static final double PLAYER_SCREEN_X = 200;
    private static final double PLATFORM_H = 30;
    private static final long DODGE_NANOS = 300_000_000L;

    private Image bg, plat, spriteIdle, spriteWalk, spriteWalkBend, spriteWalkBack, spriteCrouch, spriteDodge;
    private double bgW, bgH, platW;

    private double cameraX = 0;
    private double groundY;
    private double playerY;
    private boolean walking = false;
    private boolean crouching = false;
    private boolean dodging = false;
    private boolean facingRight = true;
    private long dodgeEnd = 0;
    private double walkT = 0;

    private Canvas canvas;
    private AnimationTimer timer;

    public Parent build() {
        loadAssets();
        bgW = bg.getWidth();
        bgH = bg.getHeight();
        platW = plat.getWidth();
        groundY = H - PLATFORM_H;
        playerY = groundY - PLAYER_H + FEET_OFFSET;

        canvas = new Canvas(W, H);
        canvas.setFocusTraversable(true);

        canvas.setOnKeyPressed(e -> {
            onKey(e.getCode(), true);
            e.consume();
        });
        canvas.setOnKeyReleased(e -> {
            onKey(e.getCode(), false);
            e.consume();
        });

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                draw(canvas.getGraphicsContext2D());
            }
        };
        timer.start();

        StackPane gameLayer = new StackPane();
        gameLayer.setAlignment(Pos.TOP_LEFT);
        gameLayer.getChildren().add(canvas);

        VBox hudOverlay = new VBox(UIFactory.hud());
        hudOverlay.setAlignment(Pos.TOP_CENTER);
        gameLayer.getChildren().add(hudOverlay);

        StackPane sp = UIFactory.backdrop(gameLayer);
        sp.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            onKey(e.getCode(), true);
            e.consume();
        });
        sp.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, e -> {
            onKey(e.getCode(), false);
            e.consume();
        });

        return sp;
    }

    private void loadAssets() {
        ClassLoader cl = getClass().getClassLoader();
        bg          = new Image(cl.getResourceAsStream("Assets_Agri/field_1_bg.JPG"));
        plat        = new Image(cl.getResourceAsStream("Assets_Agri/field_2_platform.PNG"));
        spriteIdle    = new Image(cl.getResourceAsStream("Assets_Characters/Ayan/Full_body_portrait_of_a/rotations/east.png"));
        spriteWalk    = new Image(cl.getResourceAsStream("Assets_Characters/Ayan/walking/rotations/east.png"));
        spriteWalkBend= new Image(cl.getResourceAsStream("Assets_Characters/Ayan/benting_knee_to_walk/rotations/east.png"));
        spriteWalkBack= new Image(cl.getResourceAsStream("Assets_Characters/Ayan/bent_the_back_leg_kn/rotations/east.png"));
        spriteCrouch  = new Image(cl.getResourceAsStream("Assets_Characters/Ayan/crouching/rotations/east.png"));
        spriteDodge   = new Image(cl.getResourceAsStream("Assets_Characters/Ayan/dodging_attacks/rotations/east.png"));
    }

    private void onKey(KeyCode code, boolean pressed) {
        switch (code) {
            case RIGHT -> walking = pressed;
            case LEFT  -> walking = pressed;
            case SPACE -> { if (pressed) walking = false; }
            case DOWN  -> crouching = pressed;
            case D     -> {
                if (pressed && !dodging) {
                    dodging = true;
                    dodgeEnd = System.nanoTime() + DODGE_NANOS;
                }
            }
            case ESCAPE -> {
                timer.stop();
                SaveService.save();
                Main.switchScene(new ChapterMapScreen().build());
            }
        }
        if (pressed && (code == KeyCode.LEFT || code == KeyCode.RIGHT)) {
            facingRight = code == KeyCode.RIGHT;
        }
    }

    private void update(long now) {
        if (dodging && now >= dodgeEnd) {
            dodging = false;
        }

        if (walking && !dodging) {
            cameraX += facingRight ? WALK_SPEED : -WALK_SPEED;
            walkT += 0.06;
        }
    }

    private void draw(GraphicsContext g) {
        double bx = -cameraX % bgW;
        if (bx > 0) bx -= bgW;
        for (double x = bx; x < W; x += bgW) {
            g.drawImage(bg, x, 0, bgW, bgH);
        }

        g.setFill(Color.web("#3d2b1a"));
        g.fillRect(0, bgH, W, H - bgH);

        double px = -cameraX % platW;
        if (px > 0) px -= platW;
        for (double x = px; x < W; x += platW) {
            g.drawImage(plat, 0, 0, platW, PLATFORM_H, x, groundY, platW, PLATFORM_H);
        }

        Image sprite;
        if (dodging) {
            sprite = spriteDodge;
        } else if (crouching) {
            sprite = spriteCrouch;
        } else if (walking) {
            double phase = walkT % 1.2;
            if (phase < 0.4) sprite = spriteWalk;
            else if (phase < 0.6) sprite = spriteWalkBend;
            else if (phase < 0.8) sprite = spriteWalkBack;
            else sprite = spriteWalk;
        } else {
            sprite = spriteIdle;
        }

        double drawY = playerY;
        double drawX = PLAYER_SCREEN_X;

        if (crouching) {
            drawY += CROUCH_Y_OFFSET;
        }

        if (walking && !dodging) {
            drawY += Math.sin(walkT * 2 * Math.PI) * 2;
        }

        if (!facingRight) {
            g.save();
            g.translate(drawX + PLAYER_W / 2, drawY + PLAYER_H / 2);
            g.scale(-1, 1);
            g.drawImage(sprite, -PLAYER_W / 2, -PLAYER_H / 2, PLAYER_W, PLAYER_H);
            g.restore();
        } else {
            g.drawImage(sprite, drawX, drawY, PLAYER_W, PLAYER_H);
        }

        g.setFill(Color.web("#28e0c066"));
        g.setLineWidth(1);
        g.strokeText("← → Walk  |  SPACE Stop  |  ↓ Crouch  |  D Dodge  |  ESC Map", 20, H - 10);
    }
}
