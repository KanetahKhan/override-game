package com.override.shared.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.io.InputStream;

/**
 * Deterministic, code-drawn preamble for Chapter 2 — Harvest Protocol.
 *
 * <p>Same narrated-letterbox language as {@link ClassroomPixelScene}, but the
 * set moves: Ren rides a fast hovercraft out of the Education Sector, only his
 * upper body showing inside the cockpit, then the craft flies off and the
 * farmland opens up beneath KK's crop drones. The farmland/drone art is the
 * original Chapter 2 asset pack. Rendered on the same 640x360 logical grid.</p>
 *
 * <p>Art direction note: the rider and drones are dark shapes, so every shot
 * is lit behind them (bright dusk sky, moon, horizon lamps) to keep strong
 * value contrast. Nothing important is placed dark-on-dark.</p>
 */
final class ChapterTwoPixelScene extends Canvas {
    static final int WIDTH = 1280, HEIGHT = 720;
    static final double FILM_SECONDS = 10;
    private static final double SHOT_SECONDS = FILM_SECONDS / 5;
    private static final String ROOT = "/Assets_Characters/REN/";
    private static final String ART = "/assets/chapter2/";

    private final Image rider = load(ROOT + "Full_body_portrait_of_a/rotations/south.png");
    private final Image field = load(ART + "field_bg.jpg");
    private final Image crops = load(ART + "crops.png");
    private final Image drone = load(ART + "drone.png");
    // The REN PNGs carry wide transparent margins; these are the drawn bounds of
    // the idle south sprite, used to frame the upper body without guessing.
    private static final double RX0 = 84, RX1 = 165, RY0 = 39, RY1 = 238;
    private static final double RX_MID = (RX0 + RX1) / 2, RY_TALL = RY1 - RY0;
    // When true the canvas stops drawing the code-drawn set and only lays the
    // narration rail/captions over whatever sits behind it (the live footage).
    private boolean overlayOnly;

    ChapterTwoPixelScene() {
        super(WIDTH, HEIGHT);
        setMouseTransparent(true);
    }

    /** Drops the drawn set so a {@code MediaView} can play underneath the rail. */
    void setOverlayOnly(boolean value) {
        this.overlayOnly = value;
    }

    private static Image load(String path) {
        try (InputStream stream = ChapterTwoPixelScene.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing harvest asset: " + path);
            Image image = new Image(stream);
            if (image.isError()) throw new IllegalStateException("Invalid harvest asset: " + path, image.getException());
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load harvest asset: " + path, e);
        }
    }

    void film(double seconds, boolean reduced) {
        double t = Math.max(0, Math.min(FILM_SECONDS, seconds));
        int shot = Math.min(4, (int) (t / SHOT_SECONDS));
        GraphicsContext g = getGraphicsContext2D();
        if (overlayOnly) {
            g.setTransform(1, 0, 0, 1, 0, 0);
            g.clearRect(0, 0, WIDTH, HEIGHT);
        } else {
            renderShot(reduced ? 0 : t, shot, reduced);
        }
        // Same rail as the classroom film: narration never competes with the set.
        g.setFill(Color.web("#03070d"));
        g.fillRect(0, 0, WIDTH, 60);
        g.fillRect(0, 551, WIDTH, 169);
        label(g, "OVERRIDE / HARVEST PROTOCOL", 44, 37, 12, "#849cae");
        label(g, "2556  /  AGRI SECTOR 07", 958, 37, 12, "#849cae");
        String[] tags = {"01 / LAST PERIOD", "02 / NIGHT LINE", "03 / EDGE OF THE FIELDS", "04 / THE NUTRIENT WAR", "05 / STAY INVISIBLE"};
        String[] titles = {"THE CLASSROOM IS BEHIND HIM.", "FAST ENOUGH TO GO UNSEEN.", "A GLIMPSE OF GREEN.", "KK RATIONS THE HARVEST.", "HARVEST PROTOCOL."};
        String[][] captions = {
            {"The lesson nodes are done. The board is quiet — but KK's reach never sleeps.", "Ren rides the night line out of the Education Sector, toward the fields."},
            {"A hovercraft cuts the sky-lanes of a cyber future, running dark through the signal net.", "Only the top of him clears the cowling — a glimpse, a blur, then gone."},
            {"Past the last tower, the farmlands open in rows of glowing nutrient fields.", "He banks low and lets the craft carry him down to the soil."},
            {"KK's drones strip the nutrients, dose the crops, and decide who eats tonight.", "It is the yearly Nutrient Droning — and this time the fields are a battlefield."},
            {"Kill the drones. Free the harvest. Do not get caught by the AI.", "Harvest Protocol begins."}
        };
        label(g, tags[shot], 56, 581, 11, "#6ee7d0");
        label(g, titles[shot], 56, 618, 27, "#e4f1ef");
        label(g, captions[shot][0], 56, 647, 16, "#b9cbd3");
        label(g, captions[shot][1], 56, 674, 16, "#b9cbd3");
        double edge = t % SHOT_SECONDS;
        if (overlayOnly) {
            // Breathe the live footage into the rail so it reads as a faded memory.
            g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#03070d", 0.80)), new Stop(0.45, Color.web("#03070d", 0.12)),
                new Stop(1.00, Color.web("#03070d", 0.80))));
            g.fillRect(0, 60, WIDTH, 491);
        }
        if (!reduced && edge < 0.35) {
            g.setFill(Color.web("#03070d", 1 - edge / 0.35));
            g.fillRect(0, 60, WIDTH, 491);
        }
    }

    private void renderShot(double time, int shot, boolean reduced) {
        GraphicsContext g = getGraphicsContext2D();
        g.save();
        g.setTransform(1, 0, 0, 1, 0, 0);
        g.setImageSmoothing(false);
        g.setFill(Color.web("#050a12"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.scale(2, 2);
        if (shot <= 1) city(g, time, shot, reduced);
        else if (shot == 2) flyOff(g, time, reduced);
        else farm(g, time, shot, reduced);
        g.restore();
    }

    // ── Shots 01-02: the hovercraft running dark over the city ──────────────

    private void city(GraphicsContext g, double time, int shot, boolean reduced) {
        double rush = shot == 0 ? 1 : 1.7;
        // Bright dusk sky: the whole point is that the dark rider reads against it.
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.00, Color.web("#080822")), new Stop(0.40, Color.web("#2c1750")),
            new Stop(0.66, Color.web("#94306b")), new Stop(0.85, Color.web("#e8733a")),
            new Stop(1.00, Color.web("#ffc98a"))));
        g.fillRect(0, 30, 640, 245);
        stars(g, 46, 34, 152);
        glow(g, 330, 176, 250, "#ffd9a0", 0.55);
        rect(g, 0, 180, 640, 40, "#ef8a45");
        skyline(g, 190, 42, reduced ? 0 : time * 16 * rush, "#341a44", "#ffd9a0", 0.4);
        skyline(g, 214, 30, reduced ? 0 : time * 44 * rush, "#0d0820", "#6ee7d0", 0.65);
        rect(g, 0, 214, 640, 62, "#07040f");
        if (!reduced) for (int i = 0; i < 16; i++) { // speed haze through the lanes
            double y = 46 + (i * 71 % 150);
            double x = 640 - (time * 520 * rush + i * 131) % 800;
            rect(g, x, y, 40 + (i % 5) * 24, 1, i % 3 == 0 ? "#ffd9a0" : "#6ee7d0");
        }
        cockpit(g, time, shot, reduced);
    }

    /** Repeating silhouette of towers with lit windows, scrolling by parallax. */
    private static void skyline(GraphicsContext g, double baseline, int spacing,
                                double offset, String body, String window, double alpha) {
        int n = 640 / spacing + 3;
        int shift = (int) (offset % spacing);
        for (int i = -1; i < n; i++) {
            int x = i * spacing - shift;
            int h = 46 + (Math.floorMod(i * 53 + spacing * 7, 96));
            rect(g, x, baseline - h, spacing - 4, h, body);
            for (int wy = (int) baseline - h + 6; wy < baseline - 5; wy += 8) {
                for (int wx = x + 3; wx < x + spacing - 7; wx += 7) {
                    if (Math.floorMod(wx * 7 + wy * 13, 11) < 5) {
                        g.setFill(Color.web(window, alpha));
                        g.fillRect(wx, wy, 2, 2);
                    }
                }
            }
        }
    }

    /** The cockpit interior: rider's upper body silhouetted against the sky. */
    private void cockpit(GraphicsContext g, double time, int shot, boolean reduced) {
        double bob = reduced ? 0 : Math.sin(time * 3.1) * 2;
        riderUpper(g, 320, 34 + bob, 244, 202 + bob);
        // Canopy frame, dark, framing the bright sky.
        g.setFill(Color.web("#060a12"));
        g.fillPolygon(new double[] {0, 138, 178, 0}, new double[] {30, 30, 96, 156}, 4);
        g.fillPolygon(new double[] {640, 502, 462, 640}, new double[] {30, 30, 96, 156}, 4);
        rect(g, 0, 30, 640, 6, "#0e1624");
        // Instrument console.
        rect(g, 0, 198, 640, 162, "#0a141d");
        rect(g, 0, 198, 640, 3, "#12404a");
        rect(g, 18, 210, 168, 54, "#061019");
        rect(g, 20, 212, 164, 50, "#0d2531");
        label(g, "VELOCITY", 28, 228, 7, "#4f827f");
        label(g, shot == 0 ? "0 3 1 2" : "0 4 8 6", 28, 248, 15, "#7fe7d2");
        rect(g, 206, 210, 232, 54, "#061019");
        rect(g, 208, 212, 228, 50, "#0b1c28");
        label(g, "SIGNAL MESH", 216, 228, 7, "#4f827f");
        label(g, shot == 0 ? "DARK  /  UNSEEN" : "RUNNING SILENT", 216, 248, 12, "#7fb0e7");
        rect(g, 456, 210, 166, 54, "#061019");
        rect(g, 458, 212, 162, 50, "#0d2531");
        label(g, "DESTINATION", 464, 228, 7, "#4f827f");
        label(g, "AGRI 07 / FIELDS", 464, 248, 11, "#ded5ac");
        for (int i = 0; i < 5; i++) {
            double h = reduced ? 10 + i * 4 : 8 + Math.abs(Math.sin(time * 4 + i)) * 26;
            rect(g, 36 + i * 27, 300 - h, 18, h, "#2f9c8c");
        }
        rect(g, 0, 330, 640, 30, "#08111a");
        rect(g, 0, 330, 640, 1, "#20414c");
    }

    // ── Shot 03: the craft banks away and the fields open up ────────────────

    private void flyOff(GraphicsContext g, double time, boolean reduced) {
        groundField(g);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.00, Color.web("#070c22")), new Stop(0.46, Color.web("#263a66")),
            new Stop(0.74, Color.web("#b06a3a")), new Stop(0.92, Color.web("#ffd9a0")),
            new Stop(1.00, Color.web("#ffd9a0", 0))));
        g.fillRect(0, 30, 640, 190);
        stars(g, 30, 34, 122);
        glow(g, 200, 206, 230, "#ffd9a0", 0.45);
        rect(g, 0, 212, 640, 3, "#6ee7b0");
        for (int i = 0; i < 7; i++) rect(g, 0, 224 + i * 7, 640, 3, i % 2 == 0 ? "#233b28" : "#1a2c1e");
        double t = reduced ? 0 : time;
        double cx = 214 - Math.sin(t * 0.5) * 168;
        double cy = 150 - (t % 8) * 2.6;
        hovercraft(g, cx, cy, 1.25, t, reduced);
    }

    // ── Shots 04-05: the farm level and KK's nutrient drones ────────────────

    private void farm(GraphicsContext g, double time, int shot, boolean reduced) {
        double t = reduced ? 0 : time;
        groundField(g);
        // Dusk sky replaces the photo's sky; seam softened into the field.
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.00, Color.web("#1a2450")), new Stop(0.50, Color.web("#2f4a7a")),
            new Stop(0.82, Color.web("#5a7a6a")), new Stop(1.00, Color.web("#7a9a62"))));
        g.fillRect(0, 30, 640, 158);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#7a9a62", 0.0)), new Stop(1, Color.web("#7a9a62", 0.9))));
        g.fillRect(0, 150, 640, 48);
        stars(g, 30, 36, 150);
        // A low moon lights the drones from behind.
        glow(g, 508, 106, 190, "#eaf5d8", 0.5);
        g.setFill(Color.web("#f2f8e6")); g.fillOval(470, 68, 76, 76);
        rect(g, 0, 192, 640, 3, "#8ef0b0");
        // Foreground crop rows.
        for (int i = 0; i < 5; i++) g.drawImage(crops, 0 + i * 134, 196, 172, 200);
        // Push the crops into foreground silhouette.
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#061018", 0.0)), new Stop(1, Color.web("#04101a", 0.5))));
        g.fillRect(0, 200, 640, 160);
        // Each drone is backlit so its dark hull reads against the dusk sky,
        // with an extraction beam dropping through the crops into the soil.
        double[] dx = shot == 3 ? new double[] {86, 356} : new double[] {176};
        for (int i = 0; i < dx.length; i++) {
            double x = dx[i] + (reduced ? 0 : Math.sin(t * 0.7 + i) * 10);
            double y = 104 + (reduced ? 0 : Math.sin(t * 1.3 + i) * 6);
            glow(g, x + 100, y + 62, 300, "#e0556b", 0.42);
            droneBeam(g, x + 100, y + 88, t, reduced, i);
            g.drawImage(drone, x, y, 200, 136);
            if (shot == 4 && i == 0) {
                g.setStroke(Color.web("#e0556b", 0.95));
                g.setLineWidth(1);
                g.strokeOval(x - 9, y - 9, 220, 156);
                label(g, "TARGET LOCK", x + 58, y - 12, 9, "#ff6b7d");
            }
        }
        // Ren landing at the field edge, backlit so he does not vanish.
        glow(g, 96, 238, 96, "#8ef0b0", 0.5);
        double ry = 236 + (reduced ? 0 : Math.sin(t * 1.1) * 1.5);
        g.setFill(Color.web("#000000", 0.45));
        g.fillOval(70, ry + 4, 40, 8);
        riderFull(g, 92, ry + 8, 56);
        g.setFill(Color.web("#e0556b", shot == 4 ? 0.10 + 0.03 * Math.sin(t * 2) : 0.05));
        g.fillRect(0, 30, 640, 245);
        label(g, shot == 3 ? "NUTRIENT EXTRACTION  /  64% LOST" : "TARGET: NUTRIENT DRONE", 288, 268, 8,
            shot == 3 ? "#e0b06b" : "#e0556b");
    }

    private void droneBeam(GraphicsContext g, double x, double y, double time,
                           boolean reduced, int seed) {
        double pulse = reduced ? 0.20 : 0.16 + 0.07 * Math.abs(Math.sin(time * 2.3));
        g.setFill(Color.web("#e0556b", pulse));
        g.fillPolygon(new double[] {x - 4, x + 4, x + 30, x - 30}, new double[] {y, y, 300, 300}, 4);
        // Nutrients pulled up out of the soil.
        g.setFill(Color.web("#8ef0b0", reduced ? 0.62 : 0.5 + 0.3 * Math.abs(Math.sin(time * 3 + seed))));
        g.fillOval(x - 5, 196, 10, 10);
        g.setFill(Color.web("#8ef0b0", 0.18));
        g.fillOval(x - 13, 188, 26, 26);
    }

    private void groundField(GraphicsContext g) {
        double scale = Math.max(640.0 / field.getWidth(), 360.0 / field.getHeight());
        double w = field.getWidth() * scale, h = field.getHeight() * scale;
        g.drawImage(field, (640 - w) / 2, (360 - h) / 2, w, h);
    }

    // ── Shared lighting helpers ─────────────────────────────────────────────

    private static void stars(GraphicsContext g, int count, double y0, double y1) {
        for (int i = 0; i < count; i++) {
            double x = (i * 97 % 640);
            double y = y0 + (i * 137 % Math.max(1, (int) (y1 - y0)));
            g.setFill(Color.web("#e8f2ff", 0.20 + (i % 4) * 0.13));
            int s = i % 5 == 0 ? 2 : 1;
            g.fillRect(x, y, s, s);
        }
    }

    /** Stacked translucent discs: a cheap, predictable soft glow. */
    private static void glow(GraphicsContext g, double cx, double cy, double radius,
                             String color, double peak) {
        for (int i = 6; i >= 1; i--) {
            double r = radius * i / 6.0;
            g.setFill(Color.web(color, peak * (7 - i) / 21.0));
            g.fillOval(cx - r / 2, cy - r / 2, r, r);
        }
    }

    /** Draws the idle sprite at a given body height, clipped to a horizontal band. */
    private void riderUpper(GraphicsContext g, double centerX, double topY,
                            double bodyHeight, double bottomY) {
        double s = bodyHeight / RY_TALL;
        double drawW = 256 * s, drawH = 256 * s;
        g.save();
        g.beginPath();
        g.rect(0, topY, 640, Math.max(1, bottomY - topY));
        g.clip();
        g.drawImage(rider, centerX - RX_MID * s, topY - RY0 * s, drawW, drawH);
        g.restore();
    }

    /** Draws the whole idle figure with its feet on {@code baselineY}. */
    private void riderFull(GraphicsContext g, double centerX, double baselineY, double bodyHeight) {
        double s = bodyHeight / RY_TALL;
        g.drawImage(rider, centerX - RX_MID * s, baselineY - RY1 * s, 256 * s, 256 * s);
    }

    /** Small hovercraft seen from behind, with the rider a sliver in the canopy. */
    private void hovercraft(GraphicsContext g, double cx, double cy, double s,
                            double time, boolean reduced) {
        double w = 92 * s, h = 30 * s;
        glow(g, cx, cy + h * 0.7, 120 * s, "#3fd8c4", 0.55);
        g.setFill(Color.web("#050a12"));
        g.fillPolygon(new double[] {cx - w / 2, cx + w / 2, cx + w * 0.34, cx - w * 0.34},
            new double[] {cy + h, cy + h, cy - h * 0.1, cy - h * 0.1}, 4);
        rect(g, cx - w * 0.34, cy - h * 0.42, w * 0.68, h * 0.5, "#0e1b28");
        // Rider glimpsed above the cowling.
        riderUpper(g, cx, cy - h * 1.7, 52 * s, cy + h * 0.1);
        rect(g, cx - w * 0.42, cy + h * 0.5, w * 0.84, 3 * s, "#3fd8c4");
        if (!reduced) for (int i = 0; i < 3; i++) {
            double ty = cy + h * (0.35 + i * 0.22);
            rect(g, cx - w / 2 - 30 - (time * 90 + i * 20) % 60, ty, 34, 1,
                i == 1 ? "#3fd8c4" : "#e0556b");
        }
    }

    private static void rect(GraphicsContext g, double x, double y, double w, double h, String color) {
        g.setFill(Color.web(color));
        g.fillRect(x, y, w, h);
    }

    private static void label(GraphicsContext g, String text, double x, double y, double size, String color) {
        g.setFont(Font.font("Monospaced", FontWeight.NORMAL, size));
        g.setFill(Color.web(color));
        g.fillText(text, x, y);
    }
}
