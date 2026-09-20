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
    private static final Map<String, Image> tintCache = new HashMap<>();

    /**
     * The snake's colour ramp, darkest to brightest.
     *
     * <p>The shipped sprites are near-black teal — {@code body.png}'s opaque
     * pixels average RGB(19, 47, 52) — which all but disappears against the dark
     * playfield. Recolouring onto this ramp both moves the hue to purple and
     * lifts the sprite out of the background.</p>
     */
    private static final Color SNAKE_DARK  = Color.rgb(74, 22, 120);
    private static final Color SNAKE_LIGHT = Color.rgb(206, 140, 255);

    /** Preload every asset. Safe to call multiple times. */
    public static void preload() {
        for (String k : LAYOUTS.keySet()) get(k);
        // Build the recoloured snake up front: it is a per-pixel pass over a
        // 500x500 sprite, and doing it on the first frame would show as a hitch.
        tinted("body");
        tinted("cursor");
    }

    /**
     * Returns a purple, tightly cropped copy of a sprite, cached.
     *
     * <p>Two things are wrong with the sources for our purposes, and both are
     * fixed here. The art sits in a mostly empty 500x500 frame — {@code body.png}
     * fills only 182 of those 500 columns — so drawing the whole frame into a
     * 20px cell leaves a thin sliver with gaps around it. Cropping to the opaque
     * bounds lets the art fill its cell instead. And the pixels are so dark that
     * a flat hue rotation would still vanish against the playfield, so luminance
     * is expanded and gamma-lifted as well as recoloured.</p>
     */
    private static Image tinted(String key) {
        return tintCache.computeIfAbsent(key, k -> {
            Image src = get(k);
            PixelReader in = src.getPixelReader();
            if (in == null) return src;
            int w = (int) src.getWidth(), h = (int) src.getHeight();

            int minX = w, minY = h, maxX = -1, maxY = -1;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (in.getColor(x, y).getOpacity() <= 0.02) continue;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
            if (maxX < minX || maxY < minY) return src; // nothing opaque to crop to

            int cw = maxX - minX + 1, ch = maxY - minY + 1;
            WritableImage out = new WritableImage(cw, ch);
            var write = out.getPixelWriter();
            for (int y = 0; y < ch; y++) {
                for (int x = 0; x < cw; x++) {
                    Color c = in.getColor(minX + x, minY + y);
                    if (c.getOpacity() <= 0.02) { write.setColor(x, y, Color.TRANSPARENT); continue; }
                    double l = 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
                    // Expand the sprite's narrow dark range, then gamma-lift it.
                    double t = Math.pow(Math.min(1.0, l * 2.2), 0.65);
                    write.setColor(x, y, Color.color(
                        ramp(SNAKE_DARK.getRed(),   SNAKE_LIGHT.getRed(),   t),
                        ramp(SNAKE_DARK.getGreen(), SNAKE_LIGHT.getGreen(), t),
                        ramp(SNAKE_DARK.getBlue(),  SNAKE_LIGHT.getBlue(),  t),
                        c.getOpacity()));
                }
            }
            return out;
        });
    }

    private static double ramp(double from, double to, double t) {
        return Math.max(0.0, Math.min(1.0, from + (to - from) * t));
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

    /*
     * Both insets are negative, so each segment is drawn slightly larger than its
     * 20px cell. Segments then overlap their neighbours instead of leaving a gap,
     * which reads as one continuous thick snake rather than a dotted line.
     */

    /** The head overhangs its cell a little more than the body, so it leads clearly. */
    private static final double HEAD_INSET = -2.5;
    private static final double BODY_INSET = -1.5;

    /** Draw a single snake segment at grid cell (cx, cy) with cell size. */
    public static void drawHead(GraphicsContext g, double cx, double cy,
                                double cellSize, boolean blinkOn) {
        if (blinkOn) {
            g.setGlobalAlpha(0.85);
        }
        g.drawImage(tinted("cursor"),
                cx * cellSize + HEAD_INSET, cy * cellSize + HEAD_INSET,
                cellSize - 2 * HEAD_INSET, cellSize - 2 * HEAD_INSET);
        g.setGlobalAlpha(1);
    }

    /** Draw a body segment at grid cell (cx, cy). */
    public static void drawBody(GraphicsContext g, double cx, double cy,
                                double cellSize, double alpha) {
        g.setGlobalAlpha(alpha);
        g.drawImage(tinted("body"),
                cx * cellSize + BODY_INSET, cy * cellSize + BODY_INSET,
                cellSize - 2 * BODY_INSET, cellSize - 2 * BODY_INSET);
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

    /** Draw the KK Assist badge at (x, y) with given size. */
    public static void drawAssistBadge(GraphicsContext g, double x, double y, double size) {
        g.drawImage(get("badge"), x, y, size, size);
    }

    /** Draw the game-over splash screen, scaled to fill {@code w×h}. */
    public static void drawGameOver(GraphicsContext g, double w, double h) {
        g.drawImage(get("game_over"), 0, 0, w, h);
    }
}
