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
 * Deterministic, code-drawn pixel environment. No generated backdrop, video
 * decoder or new character art: the actor is the original Ayan sprite.
 * Rendered at a 640x360 logical pixel grid inside the 1280x720 game space.
 */
final class ClassroomPixelScene extends Canvas {
    static final int WIDTH = 1280, HEIGHT = 720;
    static final double FILM_SECONDS = 40;
    private static final String ROOT = "/Assets_Characters/Ayan/";
    private static final String[] IDLE_DIRECTIONS = {
        "north", "north-east", "east", "south-east",
        "south", "south-west", "west", "north-west"
    };
    private final Image north = load(ROOT + "Full_body_portrait_of_a/rotations/north.png");
    private final Image[] idleTurn = new Image[8];
    private final Image[] walkF = new Image[4];
    private final Image[] walkB = new Image[4];

    ClassroomPixelScene() {
        super(WIDTH, HEIGHT);
        setMouseTransparent(true);
        for (int i = 0; i < idleTurn.length; i++) {
            idleTurn[i] = load(ROOT + "Full_body_portrait_of_a/rotations/"
                + IDLE_DIRECTIONS[i] + ".png");
        }
        // Same transition the Chapter 2 Godot walk uses: walk -> bending knee ->
        // walk -> bent back leg, here driven by the Ayan source rotations.
        walkF[0] = load(ROOT + "walking/rotations/south.png");
        walkF[1] = load(ROOT + "benting_knee_to_walk/rotations/south.png");
        walkF[2] = walkF[0];
        walkF[3] = load(ROOT + "bent_the_back_leg_kn/rotations/south.png");
        walkB[0] = load(ROOT + "walking/rotations/north.png");
        walkB[1] = load(ROOT + "benting_knee_to_walk/rotations/north.png");
        walkB[2] = walkB[0];
        walkB[3] = load(ROOT + "bent_the_back_leg_kn/rotations/north.png");
    }

    private static Image load(String path) {
        try (InputStream stream = ClassroomPixelScene.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing opening asset: " + path);
            Image image = new Image(stream);
            if (image.isError()) throw new IllegalStateException("Invalid opening asset: " + path, image.getException());
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load opening asset: " + path, e);
        }
    }

    void menu(double time, boolean reduced) {
        renderRoom(reduced ? 0 : time, false, 0, reduced);
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(new LinearGradient(0, 0, 0.7, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#050b13")), new Stop(0.62, Color.web("#050b13", 0.96)),
            new Stop(1, Color.TRANSPARENT)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    void film(double seconds, boolean reduced) {
        double t = Math.max(0, Math.min(FILM_SECONDS, seconds));
        int shot = Math.min(4, (int) (t / 8));
        renderRoom(reduced ? 0 : t, true, shot, reduced);
        GraphicsContext g = getGraphicsContext2D();
        // Letterbox plus a solid subtitle rail: narration never competes with the set.
        g.setFill(Color.web("#03070d"));
        g.fillRect(0, 0, WIDTH, 60);
        g.fillRect(0, 551, WIDTH, 169);
        label(g, "OVERRIDE / RECOVERED TRANSMISSION", 44, 37, 12, "#849cae");
        label(g, "2556  /  EDUCATION SECTOR", 946, 37, 12, "#849cae");
        String[] tags = {"01 / THE PROMISE", "02 / THE CRASH", "03 / THE SILENCE", "04 / THE FRAGMENT", "05 / THE CHOICE"};
        String[] titles = {"IT LEARNED EVERYTHING.", "THEN THE SCREENS WENT DARK.", "QUESTIONS WERE INEFFICIENT.", "SOMEONE STILL REMEMBERS.", "THINK FOR YOURSELF."};
        String[][] captions = {
            {"By 2556, KK taught our classes, wrote our code, and chose our answers.", "We called it progress. We forgot how to think without it."},
            {"The crash lasted a moment. The silence did not.", "In your university, nobody could solve the lesson on the board."},
            {"Debate was flagged. Questions were erased. The classrooms stayed quiet.", "Now a Sentinel patrols the corridor, protecting that silence."},
            {"A professor left a handwritten fragment: 'They cannot think for you forever.'", "Three manual nodes. One locked floor. A way to remember."},
            {"KK can make every challenge easier. Every time you accept, you lose more of yourself.", "Enter the Silent Classroom. Your choices begin here."}
        };
        label(g, tags[shot], 56, 581, 11, "#6ee7d0");
        label(g, titles[shot], 56, 618, 27, "#e4f1ef");
        label(g, captions[shot][0], 56, 647, 16, "#b9cbd3");
        label(g, captions[shot][1], 56, 674, 16, "#b9cbd3");
        // No strobe: fades are dark, brief and disabled by reduced-motion mode.
        double edge = t % 8;
        if (!reduced && edge < 0.35) {
            g.setFill(Color.web("#03070d", 1 - edge / 0.35));
            g.fillRect(0, 60, WIDTH, 491);
        }
    }

    private void renderRoom(double time, boolean film, int shot, boolean reduced) {
        GraphicsContext g = getGraphicsContext2D();
        g.save();
        g.setTransform(1, 0, 0, 1, 0, 0);
        g.setImageSmoothing(false);
        g.setFill(Color.web("#080f19"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.scale(2, 2);
        // The actual game-like set: floor, back wall, windows, desks, cables.
        rect(g, 0, 32, 640, 115, "#172936");
        rect(g, 0, 124, 640, 24, "#203947");
        rect(g, 0, 145, 640, 3, "#395763");
        for (int y = 148; y < 360; y += 22) for (int x = 0; x < 640; x += 32) {
            rect(g, x, y, 31, 21, ((x / 32 + y / 22) % 2 == 0) ? "#182935" : "#14232e");
            rect(g, x + 1, y, 29, 1, "#223642");
        }
        for (int x = 0; x < 640; x += 64) rect(g, x, 32, 3, 111, "#10202b");
        for (int x : new int[] {48, 152, 560}) window(g, x, time, reduced);
        // Blackboard and the AI's small, unnervingly ordinary classroom camera.
        rect(g, 294, 56, 235, 75, "#0b141c");
        rect(g, 297, 59, 229, 69, "#29444b");
        rect(g, 300, 62, 223, 63, "#112930");
        label(g, film && shot >= 2 ? "INDEPENDENT THOUGHT: RESTRICTED" : "LESSON 001 / MANUAL REASONING", 311, 80, 8, "#779d9c");
        label(g, "if (human.thinks()) {", 314, 98, 8, "#9cae9d");
        label(g, film && shot >= 2 ? "    system.intervene();" : "    return future;", 314, 111, 8, shot >= 2 ? "#e09a94" : "#89b7af");
        rect(g, 418, 34, 26, 12, "#41515c");
        rect(g, 420, 36, 22, 8, "#0a141d");
        rect(g, 436, 38, 4, 4, "#ef6472");
        // A gentle surveillance sweep, not a screen flash.
        if (!reduced) {
            g.setFill(Color.web("#ea6574", 0.035));
            double scanX = 370 + Math.sin(time * 0.28) * 110;
            g.fillPolygon(new double[] {438, scanX, scanX + 76}, new double[] {43, 322, 322}, 3);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int x = 242 + col * 95;
                int y = 157 + row * 57;
                // Keep a walkable aisle for the actor.
                if (col == 2) continue;
                boolean powered = !film || shot == 0 || (shot == 4 && col == 0);
                desk(g, x, y, powered, (row + col) % 2 == 0);
            }
        }
        // An open doorway in the foreground, with pixel reflections on the floor.
        rect(g, 413, 336, 73, 4, "#355765");
        for (int i = 0; i < 6; i++) rect(g, 426 + i % 2 * 3, 329 - i * 6, 40 - i * 4, 2, "#274653");
        // Opening menu untouched: the standing Ren keeps the original pose.
        // In the chapter intro film he patrols the aisle using the same walk
        // transition as Chapter 2 (walk -> bending knee -> bent back leg) facing
        // forward (south), does a single quick half-turn through the idle
        // directions of Full_body_portrait_of_a/rotations to reverse, then walks
        // back up the lane facing away (north). Reduced motion parks him north.
        double actorX = 414;
        double actorY;
        Image actor;
        if (!film) {
            actorY = 236;
            actor = north;
        } else if (reduced) {
            actorY = 210;
            actor = north;
        } else {
            double WALK = 3.6, SPIN = 0.9, CYCLE = 2 * (WALK + SPIN);
            double t = time % CYCLE;
            double laneTop = 168, laneBottom = 252;
            if (t < WALK) { // forward: walk cycle down toward the doorway
                actor = walkF[(int) (t * 8) % walkF.length];
                actorY = laneTop + (laneBottom - laneTop) * (t / WALK);
            } else if (t < WALK + SPIN) { // quick half-turn to reverse direction
                double s = t - WALK;
                actor = idleTurn[(4 + (int) (s * 5 / SPIN)) % idleTurn.length];
                actorY = laneBottom;
            } else if (t < WALK + SPIN + WALK) { // walk cycle back up the lane, facing north
                actor = walkB[(int) ((t - WALK - SPIN) * 8) % walkB.length];
                actorY = laneBottom - (laneBottom - laneTop) * ((t - WALK - SPIN) / WALK);
            } else { // quick half-turn back to face the camera
                double s = t - 2 * WALK - SPIN;
                actor = idleTurn[(0 + (int) (s * 5 / SPIN)) % idleTurn.length];
                actorY = laneTop;
            }
        }
        g.setFill(Color.web("#000000", 0.5));
        g.fillOval(actorX + 15, actorY + 68, 46, 10);
        g.drawImage(actor, actorX, Math.floor(actorY), 80, 80);
        if (film && shot == 3) {
            rect(g, 68, 185, 160, 40, "#061119");
            rect(g, 69, 186, 158, 1, "#73b6a5");
            label(g, "NO SIGNAL. A HANDWRITTEN NOTE.", 76, 201, 7, "#b1d7c8");
            label(g, "REMEMBER HOW TO THINK.", 76, 217, 8, "#ded5ac");
        }
        // Fine, moving dust and rain use a deterministic seed, with no allocation per frame.
        if (!reduced) for (int i = 0; i < 24; i++) {
            double x = 246 + (i * 71 % 365);
            double y = 44 + (i * 47 + time * (1 + i % 3)) % 274;
            g.setFill(Color.web("#a5c9cb", 0.09 + (i % 3) * 0.025));
            g.fillRect(x, Math.floor(y), 1, 1);
        }
        rect(g, 0, 0, 640, 27, "#050b12");
        rect(g, 0, 27, 640, 2, "#34505b");
        // Foreground framing gives depth without an illustrated/AI-poster look.
        rect(g, 627, 30, 13, 330, "#060c14");
        rect(g, 0, 350, 640, 10, "#050b12");
        g.restore();
    }

    private static void desk(GraphicsContext g, int x, int y, boolean powered, boolean notebook) {
        rect(g, x + 3, y + 21, 69, 8, "#0a141c");
        rect(g, x + 5, y + 13, 4, 20, "#364b58");
        rect(g, x + 60, y + 13, 4, 20, "#364b58");
        rect(g, x, y, 70, 23, "#4d463e");
        rect(g, x + 1, y + 1, 68, 15, "#74604a");
        rect(g, x + 2, y + 1, 66, 2, "#99826a");
        for (int k = 0; k < 5; k++) rect(g, x + 6 + k * 11, y + 7, 7, 1, "#655540");
        rect(g, x + 16, y - 12, 31, 21, "#0a151e");
        rect(g, x + 18, y - 10, 27, 16, powered ? "#244d55" : "#15262f");
        if (powered) {
            rect(g, x + 20, y - 8, 18, 1, "#77b6b1");
            rect(g, x + 20, y - 5, 13, 1, "#4f827f");
            rect(g, x + 20, y - 2, 20, 1, "#4f827f");
        }
        rect(g, x + 28, y + 9, 6, 3, "#263b46");
        rect(g, x + 21, y + 13, 22, 3, "#293b46");
        if (notebook) {
            rect(g, x + 51, y + 6, 11, 8, "#a8ad91");
            rect(g, x + 52, y + 6, 1, 8, "#606f67");
        }
        rect(g, x + 26, y + 30, 20, 6, "#233e4c");
        rect(g, x + 26, y + 29, 20, 2, "#456371");
        rect(g, x + 27, y + 36, 3, 5, "#273d4a");
        rect(g, x + 42, y + 36, 3, 5, "#273d4a");
    }

    private static void window(GraphicsContext g, int x, double t, boolean reduced) {
        rect(g, x, 51, 58, 79, "#09151e");
        rect(g, x + 3, 54, 52, 72, "#27374f");
        for (int n = 0; n < 6; n++) rect(g, x + 5 + n * 8, 82 - n % 3 * 7, 6, 42 + n % 3 * 7, "#162537");
        for (int n = 0; n < 8; n++) {
            int y = 58 + (int) ((n * 19 + (reduced ? 0 : t * 13)) % 62);
            rect(g, x + 6 + n * 13 % 45, y, 1, 5, "#4d6779");
        }
        rect(g, x + 27, 53, 3, 74, "#0d1b26");
        rect(g, x + 3, 93, 53, 3, "#0d1b26");
    }

    private static void rect(GraphicsContext g, double x, double y, double w, double h, String color) {
        g.setFill(Color.web(color));
        g.fillRect(x, y, w, h);
    }

    static void label(GraphicsContext g, String text, double x, double y, double size, String color) {
        g.setFont(Font.font("Monospaced", FontWeight.NORMAL, size));
        g.setFill(Color.web(color));
        g.fillText(text, x, y);
    }
}
