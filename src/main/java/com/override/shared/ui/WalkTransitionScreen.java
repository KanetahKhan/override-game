package com.override.shared.ui;

import com.override.Main;
import javafx.animation.AnimationTimer;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Full-screen walking transition: Ayan walks across a neon-lit corridor for
 * ~2.5 seconds, then fires onComplete to hand off to the next scene.
 *
 * Uses the same AnimationTimer pattern as the mini-games. No assets needed —
 * everything is drawn procedurally on a Canvas.
 *
 * Typical usage:
 *   WalkTransitionScreen.walkTo("Leaving the Logic Lab...", () -> Main.switchScene(build()));
 */
public class WalkTransitionScreen {

    private static final double DURATION = 2.5;

    // Corridor geometry
    private static final int W  = 1280;
    private static final int H  = 720;
    private static final int VP_X = 640;   // vanishing point x
    private static final int VP_Y = 350;   // vanishing point y
    private static final int CEIL_Y = 200;
    private static final int FLOOR_Y = 500;

    // Character walk path
    private static final double WALK_START_X = 120;
    private static final double WALK_END_X   = 1160;
    private static final double WALK_Y       = 420;

    // Neon colours
    private static final Color BG_COLOR   = Color.web("#0a0a0f");
    private static final Color CYAN       = Color.web("#28e0c0");
    private static final Color CYAN_DIM   = Color.web("#28e0c0", 0.18);
    private static final Color CYAN_MID   = Color.web("#28e0c0", 0.55);
    private static final Color WALL_COLOR = Color.web("#111828");
    private static final Color RACK_COLOR = Color.web("#0d1f2d");

    private final String label;
    private final Runnable onComplete;
    private boolean fired = false;

    public WalkTransitionScreen(String label, Runnable onComplete) {
        this.label = label;
        this.onComplete = onComplete;
    }

    /** Convenience: switch to this screen immediately. */
    public static void walkTo(String label, Runnable onComplete) {
        Main.switchScene(new WalkTransitionScreen(label, onComplete).build());
    }

    public Parent build() {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        long[] lastNanos = {0};
        double[] time = {0};

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos[0] == 0) { lastNanos[0] = now; return; }
                double dt = (now - lastNanos[0]) / 1_000_000_000.0;
                if (dt > 0.05) dt = 0.05;
                lastNanos[0] = now;
                time[0] += dt;

                render(gc, time[0]);

                if (time[0] >= DURATION && !fired) {
                    fired = true;
                    stop();
                    onComplete.run();
                }
            }
        };
        timer.start();

        StackPane root = new StackPane(canvas);
        root.setMinSize(W, H);
        return root;
    }

    // ── Drawing ──────────────────────────────────────────────────────────────

    private void render(GraphicsContext gc, double t) {
        double progress = Math.min(t / DURATION, 1.0);

        drawBackground(gc);
        drawCorridor(gc);
        drawServerRacks(gc);
        drawFloorGrid(gc, t);
        drawAyan(gc, progress, t);
        drawLabel(gc, t);
        drawVignette(gc);
    }

    private void drawBackground(GraphicsContext gc) {
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, W, H);

        // Ceiling panel (slightly lighter)
        gc.setFill(WALL_COLOR);
        gc.fillPolygon(
            new double[]{0, W, VP_X},
            new double[]{0, 0, VP_Y},
            3
        );
        // Floor panel
        gc.fillPolygon(
            new double[]{0, W, VP_X},
            new double[]{H, H, VP_Y},
            3
        );
        // Left wall
        gc.fillPolygon(
            new double[]{0, VP_X, 0},
            new double[]{0, VP_Y, H},
            3
        );
        // Right wall
        gc.fillPolygon(
            new double[]{W, VP_X, W},
            new double[]{0, VP_Y, H},
            3
        );
    }

    private void drawCorridor(GraphicsContext gc) {
        gc.setStroke(CYAN_MID);
        gc.setLineWidth(1.5);

        // Ceiling line
        gc.strokeLine(0, CEIL_Y, W, CEIL_Y);
        // Floor line
        gc.strokeLine(0, FLOOR_Y, W, FLOOR_Y);

        // Perspective lines to vanishing point
        gc.setStroke(CYAN_DIM);
        gc.setLineWidth(1.0);
        // Top-left edge
        gc.strokeLine(0, CEIL_Y, VP_X, VP_Y);
        // Top-right edge
        gc.strokeLine(W, CEIL_Y, VP_X, VP_Y);
        // Bottom-left edge
        gc.strokeLine(0, FLOOR_Y, VP_X, VP_Y);
        // Bottom-right edge
        gc.strokeLine(W, FLOOR_Y, VP_X, VP_Y);

        // Left wall border
        gc.strokeLine(0, CEIL_Y, 0, FLOOR_Y);
        // Right wall border
        gc.strokeLine(W, CEIL_Y, W, FLOOR_Y);
    }

    private void drawServerRacks(GraphicsContext gc) {
        // Left wall racks
        drawRack(gc, 30,  220, 60, 240);
        drawRack(gc, 110, 220, 60, 240);
        drawRack(gc, 190, 220, 60, 240);
        drawRack(gc, 270, 220, 60, 240);

        // Right wall racks
        drawRack(gc, 950,  220, 60, 240);
        drawRack(gc, 1030, 220, 60, 240);
        drawRack(gc, 1110, 220, 60, 240);
        drawRack(gc, 1190, 220, 60, 240);
    }

    private void drawRack(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(RACK_COLOR);
        gc.fillRect(x, y, w, h);
        gc.setStroke(CYAN_DIM);
        gc.setLineWidth(1.0);
        gc.strokeRect(x, y, w, h);

        // LED dots on rack
        gc.setFill(CYAN_MID);
        for (int i = 0; i < 5; i++) {
            gc.fillOval(x + 8, y + 15 + i * 42, 5, 5);
        }
        // Horizontal slot lines
        gc.setStroke(Color.web("#28e0c0", 0.12));
        for (int i = 0; i < 6; i++) {
            gc.strokeLine(x + 2, y + 10 + i * 38, x + w - 2, y + 10 + i * 38);
        }
    }

    private void drawFloorGrid(GraphicsContext gc, double t) {
        // Scrolling perspective grid on the floor
        gc.setStroke(CYAN_DIM);
        gc.setLineWidth(0.8);

        double scroll = (t * 120) % 60; // moves toward viewer

        // Horizontal lines on floor (perspective-projected)
        for (int i = 0; i <= 8; i++) {
            double u = (i * 60 + scroll) / 480.0; // 0..1 from VP to bottom
            double y = VP_Y + u * (H - VP_Y);
            if (y < FLOOR_Y || y > H) continue;
            // X extents at this y
            double xLeft  = lerpFloorEdge(y, true);
            double xRight = lerpFloorEdge(y, false);
            gc.strokeLine(xLeft, y, xRight, y);
        }

        // Vertical dividers
        for (int col = -3; col <= 3; col++) {
            gc.strokeLine(VP_X + col * 40, VP_Y, VP_X + col * 240, H);
        }
    }

    /** Returns the left or right floor edge x at a given y. */
    private double lerpFloorEdge(double y, boolean left) {
        double u = (y - VP_Y) / (H - VP_Y);
        if (left)  return VP_X - u * VP_X;
        else       return VP_X + u * (W - VP_X);
    }

    private void drawAyan(GraphicsContext gc, double progress, double t) {
        double x = WALK_START_X + (WALK_END_X - WALK_START_X) * easeInOut(progress);
        double y = WALK_Y;

        double legSwing  =  Math.sin(t * 8) * Math.toRadians(30);
        double armSwing  = -Math.sin(t * 7) * Math.toRadians(25);

        double legLen = 38;
        double armLen = 30;

        // Shadow on floor
        gc.setFill(Color.web("#000000", 0.35));
        gc.fillOval(x - 18, y + 12, 36, 10);

        // Body
        gc.setFill(Color.web("#1a2040"));
        gc.fillRect(x - 9, y - 34, 18, 46);
        gc.setStroke(CYAN_MID);
        gc.setLineWidth(1.0);
        gc.strokeRect(x - 9, y - 34, 18, 46);

        // Head
        gc.setFill(Color.web("#0a0a0f"));
        gc.fillOval(x - 14, y - 64, 28, 28);
        gc.setStroke(CYAN);
        gc.setLineWidth(2.0);
        gc.strokeOval(x - 14, y - 64, 28, 28);

        // Eyes (two small cyan dots)
        gc.setFill(CYAN);
        gc.fillOval(x - 7, y - 55, 4, 4);
        gc.fillOval(x + 3, y - 55, 4, 4);

        gc.setStroke(CYAN_MID);
        gc.setLineWidth(2.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        // Left leg
        gc.strokeLine(x - 5, y + 12,
            x - 5 + Math.sin(legSwing) * legLen,
            y + 12 + Math.cos(legSwing) * legLen);
        // Right leg
        gc.strokeLine(x + 5, y + 12,
            x + 5 + Math.sin(-legSwing) * legLen,
            y + 12 + Math.cos(-legSwing) * legLen);

        // Left arm
        gc.strokeLine(x - 9, y - 22,
            x - 9 + Math.sin(armSwing) * armLen,
            y - 22 + Math.cos(armSwing) * armLen);
        // Right arm
        gc.strokeLine(x + 9, y - 22,
            x + 9 + Math.sin(-armSwing) * armLen,
            y - 22 + Math.cos(-armSwing) * armLen);

        gc.setLineCap(javafx.scene.shape.StrokeLineCap.SQUARE);
    }

    private void drawLabel(GraphicsContext gc, double t) {
        // Pulse the arrow slightly
        double alpha = 0.7 + 0.3 * Math.sin(t * 3);
        gc.setFill(Color.web("#28e0c0", alpha));
        gc.setFont(Font.font("Monospaced", 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("→   " + label, W / 2.0, 660);
    }

    private void drawVignette(GraphicsContext gc) {
        // Dark edges to frame the scene
        javafx.scene.paint.RadialGradient vignette = new javafx.scene.paint.RadialGradient(
            0, 0, 0.5, 0.5, 0.75, true,
            javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, Color.TRANSPARENT),
            new javafx.scene.paint.Stop(1, Color.web("#000000", 0.65))
        );
        gc.setFill(vignette);
        gc.fillRect(0, 0, W, H);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Smooth start/end easing. */
    private static double easeInOut(double t) {
        return t * t * (3 - 2 * t);
    }
}
