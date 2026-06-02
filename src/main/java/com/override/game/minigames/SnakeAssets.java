package com.override.game.minigames;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches all Syntax Snake sprite assets from
 * {@code /images/syntax-snake/}. Provides frame-slicing for sprite strips
 * and convenience draw methods for {@link SnakeGame}.
 *
 * <p>The original generation spec says to produce images at 2×–4× target
 * size and nearest-neighbour downscale; the loader handles the actual
 * on-disk size automatically.
 *
 * <p>All assets loaded lazily via {@link Class#getResourceAsStream}.
 */
public final class SnakeAssets {

    // =========================================================================
    //  Asset keys and sheet layouts (cols × rows, frame size from spec)
    // =========================================================================
    private static final Map<String, int[]> LAYOUTS = Map.ofEntries(
        Map.entry("playground",    new int[]{1, 1}),  // 288×384, single opaque bg
        Map.entry("cursor",        new int[]{1, 1}),  // 16×16, single
        Map.entry("body",          new int[]{1, 1}),  // 16×16, single
        Map.entry("food",          new int[]{3, 1}),  // 48×16 → 3 frames × 16×16
        Map.entry("eatBurstEffect", new int[]{4, 1}), // 64×16 → 4 frames × 16×16
        Map.entry("death",         new int[]{4, 1}),  // 64×16 → 4 frames × 16×16
        Map.entry("badge",         new int[]{1, 1}),  // 32×32, single
        Map.entry("game_over",     new int[]{1, 1})   // 288×384, single opaque
    );

    private static final Map<String, Image> cache = new HashMap<>();

    /** Preload every asset. Safe to call multiple times. */
    public static void preload() {
        for (String k : LAYOUTS.keySet()) get(k);
    }

    /** Load (or return cached) full image for the given asset key. */
    public static Image get(String key) {
        return cache.computeIfAbsent(key, k -> {
            var url = SnakeAssets.class.getResourceAsStream("/images/syntax-snake/" + k + ".png");
            if (url == null) throw new RuntimeException("Missing asset: /images/syntax-snake/" + k + ".png");
            return new Image(url);
        });
    }

    /**
     * Returns a single frame from a sprite sheet.
     * For single-frame layouts always returns the full image.
     */
    public static Image frame(String key, int index) {
        Image sheet = get(key);
        int[] colsRows = LAYOUTS.get(key);
        if (colsRows == null) return sheet;
        int cols = colsRows[0], rows = colsRows[1];
        int total = cols * rows;
        if (total <= 1) return sheet;

        int col = index % cols;
        int row = index / cols;
        double fw = sheet.getWidth() / cols;
        double fh = sheet.getHeight() / rows;
        int px = (int) Math.round(col * fw);
        int py = (int) Math.round(row * fh);
        int pw = (int) Math.round(fw);
        int ph = (int) Math.round(fh);

        PixelReader reader = sheet.getPixelReader();
        if (reader == null) return sheet;
        return new WritableImage(reader, px, py, pw, ph);
    }

    // =========================================================================
    //  Draw helpers
    // =========================================================================

    /** Draw the playfield background, scaled to fill {@code w×h}. */
    public static void drawBackground(GraphicsContext g, double w, double h) {
        g.drawImage(get("playground"), 0, 0, w, h);
    }

    /** Draw a single snake segment at grid cell (cx, cy) with cell size. */
    public static void drawHead(GraphicsContext g, double cx, double cy,
                                double cellSize, boolean blinkOn) {
        double inset = 1;
        if (blinkOn) {
            g.setGlobalAlpha(0.85);
        }
        g.drawImage(get("cursor"),
                cx * cellSize + inset, cy * cellSize + inset,
                cellSize - 2 * inset, cellSize - 2 * inset);
        g.setGlobalAlpha(1);
    }

    /** Draw a body segment at grid cell (cx, cy). */
    public static void drawBody(GraphicsContext g, double cx, double cy,
                                double cellSize, double alpha) {
        g.setGlobalAlpha(alpha);
        double inset = 2;
        g.drawImage(get("body"),
                cx * cellSize + inset, cy * cellSize + inset,
                cellSize - 2 * inset, cellSize - 2 * inset);
        g.setGlobalAlpha(1);
    }

    /**
     * Draw a knowledge-bit (food) variant at grid cell (cx, cy).
     * {@code variant} selects the 16×16 frame (0–2) from the sheet.
     */
    public static void drawFood(GraphicsContext g, double cx, double cy,
                                double cellSize, int variant, double alpha) {
        Image sheet = get("food");
        double fw = sheet.getWidth() / 3.0;
        double fh = sheet.getHeight();
        g.setGlobalAlpha(alpha);
        g.drawImage(sheet, variant * fw, 0, fw, fh,
                cx * cellSize + 2, cy * cellSize + 2,
                cellSize - 4, cellSize - 4);
        g.setGlobalAlpha(1);
    }

    /**
     * Draw a single frame of the eat burst effect centred at (px, py).
     */
    public static void drawEatBurst(GraphicsContext g, double px, double py,
                                    double size, int frame, double alpha) {
        Image sheet = get("eatBurstEffect");
        double fw = sheet.getWidth() / 4.0;
        double fh = sheet.getHeight();
        g.setGlobalAlpha(alpha);
        g.drawImage(sheet, frame * fw, 0, fw, fh,
                px - size / 2, py - size / 2, size, size);
        g.setGlobalAlpha(1);
    }

    /**
     * Draw a single frame of the death / glitch effect centred at (px, py).
     */
    public static void drawDeathEffect(GraphicsContext g, double px, double py,
                                       double size, int frame, double alpha) {
        Image sheet = get("death");
        double fw = sheet.getWidth() / 4.0;
        double fh = sheet.getHeight();
        g.setGlobalAlpha(alpha);
        g.drawImage(sheet, frame * fw, 0, fw, fh,
                px - size / 2, py - size / 2, size, size);
        g.setGlobalAlpha(1);
    }

    /** Draw the Astra Assist badge at (x, y) with given size. */
    public static void drawAssistBadge(GraphicsContext g, double x, double y, double size) {
        g.drawImage(get("badge"), x, y, size, size);
    }

    /** Draw the game-over splash screen, scaled to fill {@code w×h}. */
    public static void drawGameOver(GraphicsContext g, double w, double h) {
        g.drawImage(get("game_over"), 0, 0, w, h);
    }
}
