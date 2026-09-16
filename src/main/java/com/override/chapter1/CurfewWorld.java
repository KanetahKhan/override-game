package com.override.chapter1;

import com.override.Main;
import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * The Curfew Protocol floor: six rooms, furniture you can open, hide in and
 * loot, and the sentinel that patrols them. A JavaFX 3D port of the original
 * Three.js chase-world.js, so the chapter runs inside the game window.
 *
 * Everything is authored in Three.js coordinates (Y up, the camera looks down
 * -Z) inside {@link #world}, which is rotated 180° about X to land in JavaFX's
 * Y-down space. That keeps the layout numbers identical to the original.
 *
 * JavaFX 3D has no fog or shadows and only a few lights per shape, so glowing
 * things use self-illumination instead of their own lights, and three moving
 * point lights (the player's lamp, the nearest ceiling panel, the sentinel)
 * carry the lighting.
 */
final class CurfewWorld {

    static final double HX = 25, HZ = 15, H = 3.6;
    private static final double RAY_FAR = 3.6;

    /** Callbacks into the chapter screen; all are invoked on the FX thread. */
    interface Listener {
        void onHover(String id, String label, String verb);
        void onUse(String id, String kind, boolean open);
        void onCoin(int amount, String source);
        void onCaught();
        void onAlert(String state);
        void onHide(boolean hidden, String label, String blockedReason);
        void onPosture(String posture, String message);
        void onTick(Tick tick);
    }

    record Tick(double px, double pz, double sx, double sz, String state, double dist,
                boolean hidden, double stamina, String posture, String hideHint) {}

    /* ------------------------------------------------------------ data types */

    private static final class Aabb {
        final double minX, minY, minZ, maxX, maxY, maxZ;

        Aabb(Bounds b, double grow) {
            minX = b.getMinX() - grow; maxX = b.getMaxX() + grow;
            minY = b.getMinY() - grow; maxY = b.getMaxY() + grow;
            minZ = b.getMinZ() - grow; maxZ = b.getMaxZ() + grow;
        }
    }

    private static final class Anim {
        double t, target;
        final double speed;
        final DoubleConsumer apply;

        Anim(double speed, DoubleConsumer apply) { this.speed = speed; this.apply = apply; }
    }

    private static final class Interactable {
        final Node node;
        final String id;
        String label, verb, kind;
        Anim anim;             // almirah doors, drawer slide
        Node stash;            // almirah credit stash
        Group shelf;           // books
        double sitX, sitZ;     // chairs
        boolean looted;        // drawers
        MeshView screen;       // terminals and lab workstations
        boolean powered = true;

        Interactable(Node node, String id, String label, String verb, String kind) {
            this.node = node; this.id = id; this.label = label; this.verb = verb; this.kind = kind;
        }
    }

    private static final class HideSpot {
        final double x, z;
        final String label;
        final Anim needsOpen;
        boolean ready;

        HideSpot(double x, double z, String label, Anim needsOpen) {
            this.x = x; this.z = z; this.label = label; this.needsOpen = needsOpen;
        }
    }

    private record Coin(Node node, Rotate spin, int value, double x, double z, double baseY) {}

    private static final class Panel {
        final double x, z;
        final Color color;
        final boolean flick;
        final Shape3D mesh;
        final PhongMaterial on, off;
        double level = 1;

        Panel(double x, double z, Color color, boolean flick, Shape3D mesh, PhongMaterial on, PhongMaterial off) {
            this.x = x; this.z = z; this.color = color; this.flick = flick;
            this.mesh = mesh; this.on = on; this.off = off;
        }
    }

    private record Led(Shape3D mesh, PhongMaterial on, PhongMaterial off) {}

    /* ----------------------------------------------------------- scene graph */

    private final Group world = new Group();
    private final SubScene subScene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate camYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate camPitch = new Rotate(0, Rotate.X_AXIS);
    private final PointLight lamp = new PointLight(Color.rgb(150, 172, 190));
    private final PointLight roomLight = new PointLight(Color.WHITE);
    private final PointLight sentinelLight = new PointLight(Color.web("#35e0d8"));

    private final Listener listener;
    private final Random rng = new Random();
    private final Map<Color, Image> solidImages = new HashMap<>();

    private final List<Aabb> colliders = new ArrayList<>();
    private final List<Aabb> sightBlockers = new ArrayList<>();
    private final List<Node> interactionWalls = new ArrayList<>();
    private final List<Interactable> interactables = new ArrayList<>();
    private final Map<String, Interactable> named = new HashMap<>();
    private final List<Anim> animators = new ArrayList<>();
    private final List<Panel> panels = new ArrayList<>();
    private final List<Led> leds = new ArrayList<>();
    private final List<Coin> coins = new ArrayList<>();
    private final List<HideSpot> hideSpots = new ArrayList<>();

    // shared materials
    private final PhongMaterial wallMat;
    private final PhongMaterial darkMat = mat(0x161b20);
    private final PhongMaterial metalMat = metal(0x39434b);
    private final PhongMaterial deskTopMat = woodMaterial();
    private final PhongMaterial legMat = metal(0x8b969e);
    private final PhongMaterial seatMat = mat(0x243b45);
    private final PhongMaterial handleMat = metal(0xa9b4bd);

    // exit bay
    private Anim exitAnim;
    private MeshView exitGlass;

    /* ------------------------------------------------------------- sentinel */

    private static final double[][] WAYPOINTS = {
        {-22, 0}, {-16.75, -9}, {-16.75, 0}, {-16.75, 9},
        {-4, 0}, {0, -9}, {0, 0}, {0, 9},
        {8, 0}, {16.75, -9}, {16.75, 0}, {16.75, 9},
        {22, 0}
    };

    private final Group sentinel = new Group();
    private final Rotate sentinelYaw = new Rotate(0, Rotate.Y_AXIS);
    private Shape3D visor, hem;
    private final PhongMaterial[] visorMats = new PhongMaterial[3];
    private final PhongMaterial[] hemMats = new PhongMaterial[3];
    private static final Color[] STATE_COLORS = {
        Color.web("#35e0d8"), Color.web("#ffb347"), Color.web("#ff3d5a")
    };

    private String aiState = "PATROL";
    private int aiWp = 6;
    private double aiYaw, aiLost, aiStun;
    private double lastSeenX, lastSeenZ;
    private double sx = WAYPOINTS[6][0], sz = WAYPOINTS[6][1];

    /* --------------------------------------------------------------- player */

    private static final double SPAWN_X = -22.4, SPAWN_Z = 1.6;
    private double px = SPAWN_X, pz = SPAWN_Z, yaw = -1.2, pitch = -0.04, bob;
    private double py, vy, stamina = 1, eye = Double.NaN;
    private boolean hidden, crouch, grounded = true;
    private Interactable sitting;
    private final Set<KeyCode> keys = EnumSet.noneOf(KeyCode.class);

    private boolean paused = true;
    private String hoverId;
    private double difficulty = 1;
    private double tickAcc;
    private long lastNanos = -1;
    private double clock;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastNanos < 0) lastNanos = now;
            double dt = Math.min(0.05, (now - lastNanos) / 1e9);
            lastNanos = now;
            frame(dt);
        }
    };

    CurfewWorld(Listener listener) {
        this.listener = listener;
        wallMat = new PhongMaterial(Color.WHITE);
        wallMat.setDiffuseMap(wallTexture());

        world.getTransforms().add(new Rotate(180, Rotate.X_AXIS));

        buildShell();
        buildLighting();
        buildRooms();
        buildSentinel();

        camera.setFieldOfView(74);
        camera.setNearClip(0.05);
        camera.setFarClip(120);
        camera.getTransforms().setAll(camYaw, camPitch, new Rotate(180, Rotate.X_AXIS));
        world.getChildren().add(camera);

        subScene = new SubScene(new Group(world), Main.WIDTH, Main.HEIGHT, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#05080b"));
        subScene.setCamera(camera);

        placeCamera(0);
    }

    /* ================================================================ API */

    SubScene view() { return subScene; }

    void start() { timer.start(); }

    void dispose() { timer.stop(); }

    void setPaused(boolean p) {
        paused = p;
        if (p) keys.clear();
    }

    void setDifficulty(double d) { difficulty = d; }

    void keyPressed(KeyCode k) {
        boolean fresh = keys.add(k);
        if (!fresh || paused) return;
        switch (k) {
            case E -> tryUse();
            case F -> toggleHide();
            case SPACE -> jump();
            default -> { }
        }
    }

    void keyReleased(KeyCode k) { keys.remove(k); }

    /** Mouse look, in screen pixels. */
    void look(double dx, double dy) {
        yaw -= dx * 0.0024;
        pitch = clamp(pitch - dy * 0.0024, -1.3, 1.3);
    }

    /** Left click in the 3D view acts like E. */
    void use() { if (!paused) tryUse(); }

    void unlockExit() {
        exitGlass.setMaterial(glow(Color.web("#4dff9e"), 0.9));
        exitAnim.target = 0;
    }

    void openExit() { exitAnim.target = 1; }

    void setTerminalDone(String id) {
        Interactable t = named.get(id);
        if (t == null) return;
        t.verb = "Cleared";
        t.kind = "done";
        t.screen.setMaterial(emissiveTexture(screenTexture(
            new String[] {"> NODE CLEARED", "> token extracted", "> credits banked", "> move."}, "#4dff9e"), 1.0));
        if ("node1".equals(id)) {
            PhongMaterial restored = workstationScreen(true);
            for (Interactable station : interactables)
                if ("workstation".equals(station.kind) && station.powered) station.screen.setMaterial(restored);
        }
    }

    private PhongMaterial workstationScreen(boolean restored) {
        return emissiveTexture(screenTexture(new String[] {"IUT // LAB NETWORK",
            restored ? "KERNEL RESTORED" : "STATION OFFLINE",
            restored ? "Manual access enabled" : "Restore the kernel node"},
            restored ? "#4dff9e" : "#7ef3e8"), 0.65);
    }

    void stunSentinel(double seconds) { aiStun = seconds; }

    void resetPlayer() {
        px = SPAWN_X; pz = SPAWN_Z; yaw = -1.2;
        hidden = false; sitting = null; crouch = false;
        py = 0; vy = 0; grounded = true;
        aiState = "PATROL"; aiStun = 3.2; aiWp = 6;
        sx = WAYPOINTS[6][0]; sz = WAYPOINTS[6][1];
    }

    /* ========================================================= world build */

    private void buildShell() {
        Image floorImg = floorTexture();
        PhongMaterial floorMat = new PhongMaterial(Color.WHITE);
        floorMat.setDiffuseMap(floorImg);
        floorMat.setSpecularColor(Color.gray(0.12));
        MeshView floor = quad(HX * 2, HZ * 2, floorMat);
        floor.getTransforms().add(new Rotate(-90, Rotate.X_AXIS));
        add(floor);

        MeshView ceil = quad(HX * 2, HZ * 2, mat(0x12161b));
        at(ceil, 0, H, 0);
        ceil.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        add(ceil);

        // outer shell
        wallSeg(-HX, -HZ, HX, -HZ); wallSeg(-HX, HZ, HX, HZ);
        wallSeg(-HX, -HZ, -HX, HZ); wallSeg(HX, -HZ, HX, HZ);
        // corridor walls with doorways at room centres
        double[] doorX = {-16.75, 0, 16.75};
        double gap = 1.7;
        for (double z : new double[] {-3, 3}) {
            double x = -HX;
            for (double d : doorX) { wallSeg(x, z, d - gap, z); x = d + gap; }
            wallSeg(x, z, HX, z);
        }
        // room dividers
        for (double x : new double[] {-8.5, 8.5}) { wallSeg(x, -HZ, x, -3); wallSeg(x, 3, x, HZ); }
    }

    private void wallSeg(double x1, double z1, double x2, double z2) {
        double len = Math.hypot(x2 - x1, z2 - z1);
        boolean horiz = Math.abs(x2 - x1) > Math.abs(z2 - z1);
        Box m = box(horiz ? len : 0.3, H, horiz ? 0.3 : len, wallMat);
        at(m, (x1 + x2) / 2, H / 2, (z1 + z2) / 2);
        add(m);
        Aabb b = new Aabb(worldBounds(m), 0.18);
        colliders.add(b);
        sightBlockers.add(b);
        interactionWalls.add(m);
        // Real skirting and a painted lower wall give the architecture scale.
        add(at(box(horiz ? len : 0.34, 0.14, horiz ? 0.34 : len, metalMat),
            (x1 + x2) / 2, 0.07, (z1 + z2) / 2));
        add(at(box(horiz ? len : 0.32, 0.035, horiz ? 0.32 : len, handleMat),
            (x1 + x2) / 2, 1.12, (z1 + z2) / 2));
    }

    private void buildLighting() {
        world.getChildren().add(new AmbientLight(Color.rgb(82, 91, 104)));

        lamp.setMaxRange(15);
        lamp.setLinearAttenuation(0.06);
        lamp.setQuadraticAttenuation(0.035);
        roomLight.setMaxRange(11);
        roomLight.setLinearAttenuation(0.05);
        roomLight.setQuadraticAttenuation(0.04);
        sentinelLight.setMaxRange(10);
        sentinelLight.setQuadraticAttenuation(0.06);
        world.getChildren().addAll(lamp, roomLight, sentinelLight);

        Color cool = Color.web("#cfe9ff"), warm = Color.web("#ffd2a0");
        double[] corridor = {-19, -11, -3, 5, 13, 21};
        for (int i = 0; i < corridor.length; i++) panel(corridor[i], 0, i == 2 || i == 5, cool);
        double[][] mid = {{-16.75, -6.5}, {0, -6.5}, {16.75, -6.5}, {-16.75, 6.5}, {0, 6.5}, {16.75, 6.5}};
        for (int i = 0; i < mid.length; i++) panel(mid[i][0], mid[i][1], i == 1 || i == 4, i == 4 ? warm : cool);
        double[][] back = {{-16.75, -12}, {0, -12}, {16.75, -12}, {-16.75, 12}, {0, 12}, {16.75, 12}};
        for (int i = 0; i < back.length; i++) panel(back[i][0], back[i][1], false, i == 2 ? warm : cool);

        neonStrip(-12, -2.55, 24, true, Color.web("#ff3d7f"));
        neonStrip(12, 2.55, 24, true, Color.web("#35e0d8"));
        neonStrip(-8.35, -9, 11, false, Color.web("#7a4dff"));
        neonStrip(8.35, 9, 11, false, Color.web("#ff9a3d"));

        sign("LAB 01", "#7ef3e8", -16.75, 2.02, -3.2, 0);
        sign("CLASS 2A", "#ffb347", 0, 2.02, -3.2, 0);
        sign("LAB 02", "#7ef3e8", 16.75, 2.02, -3.2, 0);
        sign("CLASS 1B", "#ffb347", -16.75, 2.02, 3.2, Math.PI);
        sign("SERVER", "#ff3d7f", 0, 2.02, 3.2, Math.PI);
        sign("EXIT BAY", "#4dff9e", 16.75, 2.02, 3.2, Math.PI);
    }

    private void panel(double x, double z, boolean flick, Color colour) {
        PhongMaterial on = glow(colour, 1.0), off = glow(colour, 0.22);
        Box p = box(2.4, 0.06, 0.5, on);
        at(p, x, H - 0.06, z);
        add(p);
        panels.add(new Panel(x, z, colour, flick, p, on, off));
    }

    private void neonStrip(double x, double z, double len, boolean horiz, Color colour) {
        Box m = box(horiz ? len : 0.08, 0.08, horiz ? 0.08 : len, glow(colour, 1.0));
        at(m, x, 2.62, z);
        add(m);
    }

    private void sign(String text, String accent, double x, double y, double z, double ry) {
        MeshView m = quad(2.2, 0.55, emissiveTexture(signTexture(text, accent), 0.9));
        rotY(at(m, x, y, z), ry);
        add(m);
    }

    /* -------------------------------------------------------- furniture */

    private int chairN, almirahN, workstationN;

    private Group deskProto() {
        Group g = new Group();
        g.getChildren().add(at(box(1.1, 0.07, 0.65, deskTopMat), 0, 0.72, 0));
        for (double[] p : new double[][] {{-0.48, -0.26}, {0.48, -0.26}, {-0.48, 0.26}, {0.48, 0.26}}) {
            g.getChildren().add(at(cyl(0.03, 0.72, legMat), p[0], 0.36, p[1]));
        }
        g.getChildren().add(at(box(0.5, 0.42, 0.06, seatMat), 0, 0.72, 0.92));
        for (double x : new double[] {-0.21, 0.21}) {
            g.getChildren().add(at(cyl(0.022, 0.88, legMat), x, 0.44, 0.9));
            g.getChildren().add(at(cyl(0.022, 0.45, legMat), x, 0.225, 0.5));
        }
        // Notebook, pages and binding: physical props, not flat painted silhouettes.
        g.getChildren().addAll(at(box(0.3, 0.035, 0.36, mat(0x365f6e)), -0.2, 0.772, 0),
            at(box(0.265, 0.018, 0.33, mat(0xd7d0b3)), -0.19, 0.797, 0),
            at(box(0.025, 0.043, 0.36, darkMat), -0.34, 0.776, 0));
        return g;
    }

    private void deskGrid(double cx, double cz, int rows, int cols) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            Group d = deskProto();
            double x = cx + (c - (cols - 1) / 2.0) * 2.2, z = cz + (r - (rows - 1) / 2.0) * 2.0;
            at(d, x, 0, z);
            add(d);
            addCollider(d, -0.08, false);
            // the seat is its own interactable so it can be picked on its own
            Box seat = box(0.5, 0.06, 0.45, seatMat);
            at(seat, x, 0.46, z + 0.7);
            add(seat);
            Interactable chair = reg(seat, "chair" + (++chairN), "Chair", "Sit down", "chair");
            chair.sitX = x;
            chair.sitZ = z + 1.32;
            hideSpots.add(new HideSpot(x, z + 1.25, "Crawl under the desk", null));
        }
    }

    /** Almirah: two doors that swing open and shut, and you can climb inside. */
    private void almirah(double x, double z, double ry, boolean contents) {
        int n = ++almirahN;
        Group g = new Group();
        // A hollow shell (back, sides, top, bottom) so the stash shows once the doors open.
        PhongMaterial bodyMat = metal(0x2b3a40);
        g.getChildren().addAll(
            at(box(1.9, 2.4, 0.06, bodyMat), 0, 1.2, -0.33),
            at(box(0.08, 2.4, 0.72, bodyMat), -0.91, 1.2, 0),
            at(box(0.08, 2.4, 0.72, bodyMat), 0.91, 1.2, 0),
            at(box(1.9, 0.08, 0.72, bodyMat), 0, 2.36, 0),
            at(box(1.9, 0.08, 0.72, bodyMat), 0, 0.04, 0),
            at(box(1.74, 2.24, 0.02, mat(0x090d10)), 0, 1.2, -0.29));
        for (int i = 0; i < 2; i++) g.getChildren().add(at(box(1.72, 0.05, 0.6, mat(0x1d272b)), 0, 0.75 + i * 0.78, 0.02));
        Node stash = null;
        if (contents) {
            stash = at(box(0.5, 0.06, 0.34, glow(Color.web("#4dff9e"), 1.0)), 0.2, 0.83, 0.1);
            g.getChildren().add(stash);
        }
        PhongMaterial doorMat = metal(0x35464d);
        PhongMaterial ventMat = glow(Color.web("#0d3a44"), 0.8);
        List<Rotate> hinges = new ArrayList<>();
        List<Integer> sides = new ArrayList<>();
        for (int s : new int[] {-1, 1}) {
            Group pivot = new Group();
            at(pivot, s * 0.93, 1.2, 0.37);
            Rotate hinge = new Rotate(0, Rotate.Y_AXIS);
            pivot.getTransforms().add(hinge);
            pivot.getChildren().addAll(
                at(box(0.92, 2.3, 0.07, doorMat), -s * 0.46, 0, 0),
                at(box(0.05, 0.3, 0.05, handleMat), -s * 0.86, 0, 0.07),
                at(box(0.52, 0.16, 0.02, ventMat), -s * 0.46, 0.82, 0.05));
            g.getChildren().add(pivot);
            hinges.add(hinge);
            sides.add(s);
        }
        rotY(at(g, x, 0, z), ry);
        add(g);
        addCollider(g, 0.05, true);
        Interactable it = reg(g, "almirah" + n, "Almirah " + n, "Open", "almirah");
        it.stash = stash;
        it.anim = anim(2.1, v -> {
            for (int i = 0; i < hinges.size(); i++) hinges.get(i).setAngle(Math.toDegrees(-sides.get(i) * v * 1.95));
        });
        hideSpots.add(new HideSpot(x + Math.sin(ry) * 1.2, z + Math.cos(ry) * 1.2, "Hide inside almirah " + n, it.anim));
    }

    /** Bookshelf with individually removable books; one may be a hollow ledger. */
    private void bookshelf(double x, double z, double ry, int keyIndex) {
        Group g = new Group();
        // An open frame rather than a solid block, so the books are actually visible.
        PhongMaterial frameMat = mat(0x3a2c22);
        g.getChildren().addAll(
            at(box(2.6, 2.2, 0.04, frameMat), 0, 1.1, -0.19),
            at(box(0.08, 2.2, 0.42, frameMat), -1.26, 1.1, 0),
            at(box(0.08, 2.2, 0.42, frameMat), 1.26, 1.1, 0),
            at(box(2.6, 0.08, 0.42, frameMat), 0, 2.16, 0),
            at(box(2.6, 0.08, 0.42, frameMat), 0, 0.04, 0),
            at(box(2.44, 2.04, 0.02, mat(0x120f0c)), 0, 1.1, -0.16));
        for (double y : new double[] {0.5, 1.12, 1.74}) g.getChildren().add(at(box(2.44, 0.05, 0.36, mat(0x2a2019)), 0, y, 0.02));
        rotY(at(g, x, 0, z), ry);
        add(g);
        addCollider(g, 0.08, true);

        int[] cols = {0x8a3b3b, 0x2f5f7a, 0x6a6a2f, 0x6b3b7a, 0x2f7a5f, 0x7a5a2f};
        PhongMaterial spineMat = mat(0xd9c9a0);
        double[] rowsY = {0.55, 1.17, 1.79};
        int k = 0;
        for (int row = 0; row < rowsY.length; row++) {
            for (int i = 0; i < 4; i++) {
                Group book = new Group(
                    box(0.16, 0.44, 0.3, mat(cols[(row * 4 + i) % cols.length])),
                    at(box(0.03, 0.3, 0.02, spineMat), 0, 0, 0.16));
                at(book, -1.0 + i * 0.3 + row * 0.06, rowsY[row] + 0.22, 0.06);
                g.getChildren().add(book);
                boolean isKey = k == keyIndex;
                Interactable it = reg(book, "book_" + Math.round(x) + "_" + k,
                    isKey ? "Heavy ledger" : "Book", "Take", isKey ? "keybook" : "book");
                it.shelf = g;
                k++;
            }
        }
    }

    /** Desk with a sliding drawer (which always holds a credit wedge). */
    private void deskWithDrawer(double x, double z, double ry, String id, String label) {
        Group g = new Group();
        g.getChildren().add(at(box(2.4, 0.1, 1.15, deskTopMat), 0, 0.78, 0));
        // Hollow pedestal lets the drawer tray slide out visibly.
        g.getChildren().addAll(
            at(box(0.08, 0.72, 1.0, deskTopMat), -1.06, 0.39, 0),
            at(box(0.08, 0.72, 1.0, deskTopMat), 1.06, 0.39, 0),
            at(box(2.12, 0.64, 0.06, deskTopMat), 0, 0.42, -0.47),
            at(box(0.08, 0.72, 1.0, deskTopMat), 0.03, 0.39, 0));
        Group drawer = new Group();
        Cylinder coinInside = cyl(0.11, 0.02, glow(Color.web("#ffb347"), 0.9));
        at(coinInside, 0, -0.06, -0.2).getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        drawer.getChildren().addAll(
            box(0.95, 0.34, 0.08, mat(0x5b4838)),
            at(box(0.5, 0.05, 0.05, handleMat), 0, 0, 0.06),
            at(box(0.88, 0.04, 0.55, deskTopMat), 0, -0.15, -0.25),
            at(box(0.04, 0.25, 0.55, deskTopMat), -0.44, -0.025, -0.25),
            at(box(0.04, 0.25, 0.55, deskTopMat), 0.44, -0.025, -0.25),
            at(box(0.88, 0.25, 0.04, deskTopMat), 0, -0.025, -0.51),
            coinInside);
        at(drawer, -0.5, 0.5, 0.52);
        g.getChildren().add(drawer);
        rotY(at(g, x, 0, z), ry);
        add(g);
        addCollider(g, 0.0, false);
        Interactable it = reg(drawer, id, label, "Pull open", "drawer");
        it.stash = coinInside;
        it.anim = anim(2.6, v -> drawer.setTranslateZ(0.52 + v * 0.44));
        hideSpots.add(new HideSpot(x + Math.sin(ry) * 1.25, z + Math.cos(ry) * 1.25, "Hide under desk", null));
    }

    private void lockerBank(double x, double z, double ry) {
        Group g = new Group();
        for (int i = 0; i < 5; i++) {
            g.getChildren().add(at(box(0.8, 2.0, 0.55, metal(i == 2 ? 0x2f5c63 : 0x35434b)), i * 0.84, 1.0, 0));
            g.getChildren().add(at(box(0.5, 0.22, 0.02, darkMat), i * 0.84, 1.72, 0.29));
        }
        rotY(at(g, x, 0, z), ry);
        add(g);
        addCollider(g, 0.08, true);
    }

    private void terminal(double x, double z, double ry, String id, String title, String accent, String[] lines) {
        Group g = new Group();
        g.getChildren().add(at(box(1.6, 1.0, 0.75, metal(0x1d242a)), 0, 0.5, 0));
        g.getChildren().add(at(box(1.5, 1.12, 0.1, metal(0x11181d)), 0, 1.58, -0.04));
        MeshView scr = quad(1.34, 0.96, emissiveTexture(screenTexture(lines, accent), 1.0));
        at(scr, 0, 1.58, 0.03);
        g.getChildren().add(scr);
        // A separate stand, input deck, raised keys and cooling vents ground the terminal.
        g.getChildren().addAll(at(box(0.22, 0.32, 0.16, legMat), 0, 1.12, -0.04),
            at(box(1.28, 0.05, 0.36, darkMat), 0, 1.04, 0.16));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 12; col++)
            g.getChildren().add(at(box(0.075, 0.023, 0.065, handleMat),
                -0.51 + col * 0.092, 1.077, 0.08 + row * 0.085));
        for (int i = 0; i < 7; i++)
            g.getChildren().add(at(box(0.7, 0.018, 0.014, darkMat), 0, 0.22 + i * 0.07, 0.382));
        g.getChildren().add(at(cyl(0.035, 0.018, glow(Color.web(accent), 0.9)), 0.62, 1.03, 0.22));
        rotY(at(g, x, 0, z), ry);
        add(g);
        addCollider(g, 0.12, false);
        Interactable it = reg(g, id, title, "Jack in", "terminal");
        it.screen = scr;
    }

    private void workstation(double x, double z) {
        Group pc = new Group();
        MeshView display = quad(0.82, 0.46, emissiveTexture(screenTexture(
            new String[] {"IUT // LAB NETWORK", "STATION OFFLINE", "Restore the kernel node"}, "#7ef3e8"), 0.65));
        at(display, 0, 1.37, -0.062);
        pc.getChildren().addAll(at(box(0.48, 0.035, 0.28, metalMat), 0, 0.925, 0),
            at(box(0.08, 0.2, 0.08, legMat), 0, 1.03, -0.1),
            at(box(0.9, 0.55, 0.065, darkMat), 0, 1.37, -0.1),
            display,
            at(box(0.6, 0.03, 0.2, metalMat), 0, 0.925, 0.27));
        at(pc, x, 0, z);
        add(pc);
        Interactable station = reg(pc, "workstation" + (++workstationN), "Lab workstation", "Power off", "workstation");
        station.screen = display;
    }

    private void coin(double x, double z, int value, double y) {
        Cylinder m = cyl(0.17, 0.035, glow(Color.web("#ffb347"), 1.0));
        Rotate spin = new Rotate(0, Rotate.Z_AXIS);
        at(m, x, y, z).getTransforms().addAll(new Rotate(90, Rotate.X_AXIS), spin);
        add(m);
        coins.add(new Coin(m, spin, value, x, z, y));
    }

    private void buildRooms() {
        // LAB 01 (north-west): kernel node
        terminal(-21.5, -12.5, 0.5, "node1", "KERNEL NODE", "#7ef3e8",
            new String[] {"> NODE 01 / KERNEL", "> integrity: degraded", "> minigame: KERNEL PANIC", "> reward: 40 CR + token"});
        PhongMaterial benchMat = mat(0x2a3238);
        PhongMaterial beakerMat = glow(Color.web("#4fb6cc"), 0.85);
        for (int i = 0; i < 3; i++) {
            Box bench = box(3.4, 0.9, 0.9, benchMat);
            at(bench, -19, 0.45, -10 + i * 2.4);
            add(bench);
            addCollider(bench, 0.05, false);
            add(at(cyl(0.12, 0.32, beakerMat), -18.2 + i * 0.6, 1.06, -10 + i * 2.4));
            workstation(-19.5, -10 + i * 2.4);
        }
        almirah(-23.6, -6.2, Math.PI / 2, true);
        deskWithDrawer(-13.4, -13.0, 0, "drawer1", "Lab drawer");
        coin(-16.5, -8.4, 5, 0.75);

        // CLASS 2A (north-middle): whiteboard, desks, bookshelf with the ledger
        add(at(box(5.4, 2.2, 0.12, mat(0x1b2426)), -1.2, 2.0, -14.78));
        add(at(quad(5.3, 2.1, emissiveTexture(boardTexture(), 0.75)), -1.2, 2.0, -14.71));
        deskGrid(0, -9.4, 3, 3);
        bookshelf(6.6, -13.2, 0, 5);
        deskWithDrawer(-4.6, -13.2, 0, "drawer2", "Teacher desk drawer");
        coin(2.4, -6.0, 5, 0.75);

        // LAB 02 (north-east): circuit node
        terminal(21.8, -12.4, 0.7, "node2", "CIRCUIT NODE", "#ff3d7f",
            new String[] {"> NODE 02 / CIRCUIT", "> bus: fragmented", "> minigame: CIRCUIT BREAKER", "> reward: 50 CR + token"});
        lockerBank(10.4, -13.4, 0);
        PhongMaterial spoolMat = mat(0x22303a);
        for (int i = 0; i < 4; i++) {
            Cylinder spool = cyl(0.42, 0.5, spoolMat);
            at(spool, 13 + (i % 2) * 1.2, 0.25, -7 - (i / 2) * 1.3).getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
            add(spool);
            addCollider(spool, 0.02, false);
        }
        bookshelf(23.4, -6.6, -Math.PI / 2, -1);
        coin(17.2, -9.2, 5, 0.75);

        // CLASS 1B (south-west)
        deskGrid(-16.75, 9.4, 3, 3);
        almirah(-23.6, 6.4, Math.PI / 2, false);
        bookshelf(-11.0, 13.0, Math.PI, 2);
        coin(-20.4, 12.6, 5, 0.75);

        // SERVER ROOM (south-middle): silent code node + racks
        terminal(-4.2, 13.6, Math.PI, "node3", "SILENT CODE NODE", "#4dff9e",
            new String[] {"> NODE 03 / SEQUENCE", "> order lost", "> minigame: SILENT CODE", "> reward: 35 CR + token"});
        PhongMaterial rackMat = metal(0x141a20);
        for (int i = 0; i < 5; i++) {
            Box rack = box(1.1, 2.4, 0.9, rackMat);
            at(rack, -6 + i * 2.8, 1.2, 8.6);
            add(rack);
            addCollider(rack, 0.1, true);
            Color c = Color.web(i % 2 == 1 ? "#35e0d8" : "#ff3d7f");
            PhongMaterial on = glow(c, 0.55), off = glow(c, 0.12);
            for (int slot = 0; slot < 7; slot++) {
                add(at(box(0.96, 0.23, 0.055, metalMat), -6 + i * 2.8, 0.35 + slot * 0.29, 8.125));
                for (int vent = 0; vent < 5; vent++)
                    add(at(box(0.42, 0.014, 0.015, darkMat), -6.12 + i * 2.8,
                        0.29 + slot * 0.29 + vent * 0.027, 8.09));
            }
            MeshView led = quad(0.035, 1.92, on);
            rotY(at(led, -5.63 + i * 2.8, 1.25, 8.09), Math.PI);
            add(led);
            leds.add(new Led(led, on, off));
        }
        coin(4.6, 12.2, 5, 0.75);

        // EXIT BAY (south-east): almirah, crates, exit door
        almirah(23.4, 12.6, -Math.PI / 2, true);
        PhongMaterial crateMat = mat(0x33404a);
        for (int i = 0; i < 5; i++) {
            Box c = box(1.0, 1.0, 1.0, crateMat);
            at(c, 11.5 + (i % 3) * 1.2, 0.5 + (i > 2 ? 1 : 0), 6.4 + (i / 3) * 1.2);
            add(c);
            addCollider(c, 0.02, false);
        }
        deskWithDrawer(13.6, 13.4, Math.PI, "drawer3", "Storage drawer");

        add(at(box(2.4, 2.9, 0.3, metalMat), 19.6, 1.45, 14.86));
        Group doorPivot = new Group();
        at(doorPivot, 18.5, 0, 14.74);
        Rotate doorHinge = new Rotate(0, Rotate.Y_AXIS);
        doorPivot.getTransforms().add(doorHinge);
        exitGlass = quad(1.3, 0.5, glow(Color.web("#ff5a4a"), 0.85));
        // on the corridor-facing side, so the lock colour is visible
        at(exitGlass, 1.05, 1.95, -0.07);
        doorPivot.getChildren().addAll(at(box(2.1, 2.6, 0.12, metal(0x384249)), 1.05, 1.3, 0), exitGlass);
        add(doorPivot);
        reg(doorPivot, "exit", "EXIT BAY DOOR", "Force open", "exit");
        exitAnim = anim(0.9, v -> doorHinge.setAngle(Math.toDegrees(-v * 1.6)));

        // loose corridor coins
        for (double[] c : new double[][] {{-22, 0}, {-7.5, 1.2}, {7.5, -1.2}, {22, 0.6}}) coin(c[0], c[1], 5, 0.55);
    }

    private void buildSentinel() {
        for (int i = 0; i < 3; i++) {
            visorMats[i] = glow(STATE_COLORS[i], 1.0);
            hemMats[i] = glow(STATE_COLORS[i], 0.7);
        }
        MeshView robe = new MeshView(cone(0.68, 2.0, 14));
        robe.setMaterial(mat(0x10141b));
        robe.setCullFace(CullFace.NONE);
        at(robe, 0, 1.0, 0);
        Sphere shoulders = new Sphere(0.44, 14);
        shoulders.setMaterial(mat(0x151a22));
        at(shoulders, 0, 1.92, 0);
        shoulders.setScaleX(1.1); shoulders.setScaleY(0.8); shoulders.setScaleZ(1.1);
        Sphere hood = new Sphere(0.36, 14);
        hood.setMaterial(mat(0x0b0e13));
        at(hood, 0, 2.16, 0);
        visor = at(box(0.3, 0.07, 0.05, visorMats[0]), 0, 2.12, 0.33);
        hem = at(cyl(0.7, 0.05, hemMats[0]), 0, 0.09, 0);
        // The original's translucent sight cone is left out: JavaFX 3D has no
        // reliable transparency, so it rendered as a solid wall of colour.

        sentinel.getChildren().addAll(robe, shoulders, hood, visor, hem);
        sentinel.getTransforms().add(sentinelYaw);
        add(sentinel);
    }

    /* ======================================================== interaction */

    private HideSpot currentHideSpot() {
        HideSpot best = null;
        double bd = 1.9;
        for (HideSpot s : hideSpots) {
            double d = Math.hypot(s.x - px, s.z - pz);
            if (d < bd) { bd = d; best = s; }
        }
        if (best != null) best.ready = best.needsOpen == null || best.needsOpen.t > 0.6;
        return best;
    }

    private void jump() {
        if (hidden || sitting != null || !grounded) return;
        vy = 4.7;
        grounded = false;
    }

    private void sit(Interactable chair) {
        sitting = chair;
        px = chair.sitX; pz = chair.sitZ;
        yaw = 0; py = 0; vy = 0;
        listener.onPosture("SEATED", "Seated — the desk hides your outline. E to stand.");
    }

    private void stand() {
        sitting = null;
        listener.onPosture(crouch ? "CROUCHED" : "STANDING", "On your feet.");
    }

    private void toggleHide() {
        if (hidden) {
            hidden = false;
            listener.onHide(false, null, null);
            return;
        }
        if (sitting != null) { stand(); return; }
        HideSpot s = currentHideSpot();
        if (s == null) return;
        if (!s.ready) { listener.onHide(false, null, "Open the almirah before you can climb in."); return; }
        hidden = true;
        px = s.x; pz = s.z; py = 0; vy = 0;
        listener.onHide(true, s.label, null);
    }

    private void tryUse() {
        if (hidden) { toggleHide(); return; }
        if (sitting != null) { stand(); return; }
        if (hoverId == null) {
            HideSpot s = currentHideSpot();
            if (s != null && s.ready) toggleHide();
            return;
        }
        Interactable obj = named.get(hoverId);
        if (obj == null) return;

        switch (obj.kind) {
            case "workstation" -> {
                obj.powered = !obj.powered;
                obj.verb = obj.powered ? "Power off" : "Power on";
                boolean restored = "done".equals(named.get("node1").kind);
                obj.screen.setMaterial(obj.powered ? workstationScreen(restored) : mat(0x070b10));
                listener.onUse(obj.id, obj.kind, obj.powered);
                listener.onHover(obj.id, obj.label, obj.verb);
            }
            case "chair" -> sit(obj);
            case "almirah" -> {
                Anim d = obj.anim;
                d.target = d.target > 0.5 ? 0 : 1;
                boolean open = d.target > 0.5;
                obj.verb = open ? "Close" : "Open";
                listener.onUse(obj.id, "almirah", open);
                if (open && obj.stash != null) {
                    ((Group) obj.node).getChildren().remove(obj.stash);
                    obj.stash = null;
                    listener.onCoin(15, "Almirah stash");
                }
                listener.onHover(obj.id, obj.label, obj.verb);
            }
            case "drawer" -> {
                Anim s = obj.anim;
                s.target = s.target > 0.5 ? 0 : 1;
                boolean open = s.target > 0.5;
                obj.verb = open ? "Push shut" : "Pull open";
                if (open && !obj.looted) {
                    obj.looted = true;
                    if (obj.stash != null) obj.stash.setVisible(false);
                    listener.onCoin(10, "Drawer");
                }
                listener.onUse(obj.id, "drawer", open);
                listener.onHover(obj.id, obj.label, obj.verb);
            }
            case "book", "keybook" -> {
                if (obj.shelf != null) obj.shelf.getChildren().remove(obj.node);
                interactables.remove(obj);
                named.remove(obj.id);
                hoverId = null;
                listener.onHover(null, null, null);
                listener.onUse(obj.id, obj.kind, false);
                if ("keybook".equals(obj.kind)) listener.onCoin(20, "Hollow ledger");
            }
            default -> listener.onUse(obj.id, obj.kind, false);
        }
    }

    /* ================================================== collision & sight */

    private boolean blocked(double nx, double nz) {
        for (Aabb b : colliders) {
            if (b.maxY < 0.6) continue;
            if (nx > b.minX && nx < b.maxX && nz > b.minZ && nz < b.maxZ) return true;
        }
        return false;
    }

    private boolean segBlocked(double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        for (Aabb b : sightBlockers) {
            if (b.maxY < 1.2) continue;
            double[] t = {0, 1};
            if (slab(ax, dx, b.minX, b.maxX, t) && slab(az, dz, b.minZ, b.maxZ, t)) return true;
        }
        return false;
    }

    /** One axis of a segment/box slab test; narrows t = [t0, t1]. */
    private static boolean slab(double o, double d, double mn, double mx, double[] t) {
        if (Math.abs(d) < 1e-6) return o >= mn && o <= mx;
        double a = (mn - o) / d, c = (mx - o) / d;
        if (a > c) { double tmp = a; a = c; c = tmp; }
        t[0] = Math.max(t[0], a);
        t[1] = Math.min(t[1], c);
        return t[0] <= t[1];
    }

    /** Distance along the ray to the box, or -1 when it misses within far. */
    private static double rayHit(double ox, double oy, double oz, double dx, double dy, double dz, Bounds b, double far) {
        double[] t = {0, far};
        if (!slab(ox, dx, b.getMinX(), b.getMaxX(), t)) return -1;
        if (!slab(oy, dy, b.getMinY(), b.getMaxY(), t)) return -1;
        if (!slab(oz, dz, b.getMinZ(), b.getMaxZ(), t)) return -1;
        return t[0];
    }

    /* ================================================================ loop */

    private void frame(double dt) {
        clock += dt;
        double tt = clock;

        // flicker + emissive life
        for (Panel p : panels) {
            if (!p.flick) continue;
            double n = (Math.sin(tt * 13.7 + 15) * Math.sin(tt * 4.1) + 1) / 2;
            p.level = n > 0.35 ? 1 : 0.14 + rng.nextDouble() * 0.2;
            p.mesh.setMaterial(p.level > 0.5 ? p.on : p.off);
        }
        double ledN = (Math.sin(tt * 13.7) * Math.sin(tt * 4.1) + 1) / 2;
        for (Led l : leds) l.mesh().setMaterial(ledN > 0.35 ? l.on() : l.off());
        for (Coin c : coins) {
            c.spin().setAngle(c.spin().getAngle() + Math.toDegrees(dt * 2.4));
            c.node().setTranslateY(c.baseY() + Math.sin(tt * 2 + c.x()) * 0.04);
        }
        for (Anim a : animators) {
            if (a.t != a.target) {
                a.t += Math.signum(a.target - a.t) * dt * a.speed;
                if (Math.abs(a.target - a.t) < 0.02) a.t = a.target;
                a.t = clamp(a.t, 0, 1);
                a.apply.accept(a.t);
            }
        }

        if (!paused) step(dt, tt);

        placeCamera(dt);
        placeLights(tt);
    }

    private void step(double dt, double tt) {
        // posture
        boolean wantCrouch = keys.contains(KeyCode.C) || keys.contains(KeyCode.CONTROL);
        if (wantCrouch != crouch && !hidden && sitting == null) {
            crouch = wantCrouch;
            listener.onPosture(crouch ? "CROUCHED" : "STANDING",
                crouch ? "Crouched — harder to spot, slower to run." : null);
        }

        // jump arc
        if (!grounded || py > 0) {
            vy -= 15.5 * dt;
            py += vy * dt;
            if (py <= 0) { py = 0; vy = 0; grounded = true; }
        }

        // movement
        boolean sprinting = false;
        if (!hidden && sitting == null) {
            sprinting = keys.contains(KeyCode.SHIFT) && stamina > 0.05 && !crouch;
            double sp = (sprinting ? 5.0 : crouch ? 1.45 : 2.9) * dt;
            stamina = clamp(stamina + (sprinting ? -dt * 0.28 : dt * 0.17), 0, 1);
            double fx = 0, fz = 0;
            if (keys.contains(KeyCode.W)) fz += 1;
            if (keys.contains(KeyCode.S)) fz -= 1;
            if (keys.contains(KeyCode.A)) fx -= 1;
            if (keys.contains(KeyCode.D)) fx += 1;
            if (fx != 0 || fz != 0) {
                double len = Math.hypot(fx, fz);
                double sin = Math.sin(yaw), cos = Math.cos(yaw);
                double dx = (-sin * fz / len + cos * fx / len) * sp;
                double dz = (-cos * fz / len - sin * fx / len) * sp;
                if (!blocked(px + dx, pz)) px += dx;
                if (!blocked(px, pz + dz)) pz += dz;
                px = clamp(px, -HX + 0.45, HX - 0.45);
                pz = clamp(pz, -HZ + 0.45, HZ - 0.45);
                bob += dt * (sprinting ? 13 : 8.5);
            }
        }
        double turn = 1.9 * dt;
        if (keys.contains(KeyCode.LEFT)) yaw += turn;
        if (keys.contains(KeyCode.RIGHT)) yaw -= turn;
        if (keys.contains(KeyCode.UP)) pitch = Math.min(1.3, pitch + turn * 0.6);
        if (keys.contains(KeyCode.DOWN)) pitch = Math.max(-1.3, pitch - turn * 0.6);

        // coins
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin c = coins.get(i);
            if (Math.hypot(c.x() - px, c.z() - pz) < 1.15) {
                world.getChildren().remove(c.node());
                coins.remove(i);
                listener.onCoin(c.value(), "Credit chip");
            }
        }

        stepSentinel(dt, tt);
        updateHover();

        // HUD tick
        tickAcc += dt;
        if (tickAcc > 0.08) {
            tickAcc = 0;
            HideSpot hs = hidden ? null : currentHideSpot();
            String posture = hidden ? "HIDDEN" : sitting != null ? "SEATED" : crouch ? "CROUCHED" : "STANDING";
            listener.onTick(new Tick(px, pz, sx, sz, aiState, Math.hypot(px - sx, pz - sz), hidden, stamina,
                posture, hs == null ? null : hs.ready ? hs.label : "Open the almirah first"));
        }
    }

    private void stepSentinel(double dt, double tt) {
        double dToPlayer = Math.hypot(px - sx, pz - sz);
        double fxs = Math.sin(aiYaw), fzs = Math.cos(aiYaw);
        double tx = px - sx, tz = pz - sz, tl = Math.hypot(tx, tz);
        if (tl > 1e-6) { tx /= tl; tz /= tl; }
        double angle = Math.acos(clamp(fxs * tx + fzs * tz, -1, 1));
        double sightRange = crouch || sitting != null ? 8.5 : 15;
        boolean visible = !hidden && aiStun <= 0 && dToPlayer < sightRange
            && (angle < 0.62 || dToPlayer < 3.2) && !segBlocked(sx, sz, px, pz);

        if (aiStun > 0) aiStun -= dt;

        if (visible) {
            if (!"CHASE".equals(aiState)) listener.onAlert("CHASE");
            aiState = "CHASE";
            aiLost = 0;
            lastSeenX = px; lastSeenZ = pz;
        } else if ("CHASE".equals(aiState)) {
            aiLost += dt;
            if (aiLost > 3.4) { aiState = "SEARCH"; aiLost = 0; listener.onAlert("SEARCH"); }
        } else if ("SEARCH".equals(aiState)) {
            aiLost += dt;
            if (aiLost > 5.5) { aiState = "PATROL"; aiLost = 0; listener.onAlert("PATROL"); }
        }

        double targetX, targetZ;
        if ("CHASE".equals(aiState)) { targetX = px; targetZ = pz; }
        else if ("SEARCH".equals(aiState)) { targetX = lastSeenX; targetZ = lastSeenZ; }
        else {
            targetX = WAYPOINTS[aiWp][0]; targetZ = WAYPOINTS[aiWp][1];
            if (Math.hypot(targetX - sx, targetZ - sz) < 1.0) {
                List<Integer> near = new ArrayList<>();
                for (int i = 0; i < WAYPOINTS.length; i++) {
                    double d = Math.hypot(WAYPOINTS[i][0] - sx, WAYPOINTS[i][1] - sz);
                    if (d > 1 && d < 11 && i != aiWp) near.add(i);
                }
                aiWp = near.isEmpty() ? rng.nextInt(WAYPOINTS.length) : near.get(rng.nextInt(near.size()));
            }
        }

        double spd = ("CHASE".equals(aiState) ? 3.45 : "SEARCH".equals(aiState) ? 2.5 : 1.95)
            * difficulty * (aiStun > 0 ? 0.25 : 1);
        double vx = targetX - sx, vz = targetZ - sz;
        double vl = Math.hypot(vx, vz);
        if (vl == 0) vl = 1;
        vx = vx / vl * spd * dt;
        vz = vz / vl * spd * dt;
        double step = spd * dt;
        double sgnZ = vz == 0 ? 1 : Math.signum(vz), sgnX = vx == 0 ? 1 : Math.signum(vx);
        if (!blocked(sx + vx, sz)) sx += vx;
        else if (!blocked(sx, sz + sgnZ * step)) sz += sgnZ * step;
        if (!blocked(sx, sz + vz)) sz += vz;
        else if (!blocked(sx + sgnX * step, sz)) sx += sgnX * step;
        double pad = 0.55;
        sx = clamp(sx, -HX + pad, HX - pad);
        sz = clamp(sz, -HZ + pad, HZ - pad);

        double wantYaw = Math.atan2(targetX - sx, targetZ - sz);
        double dy = wantYaw - aiYaw;
        while (dy > Math.PI) dy -= Math.PI * 2;
        while (dy < -Math.PI) dy += Math.PI * 2;
        aiYaw += dy * Math.min(1, dt * 4.5);

        int si = "CHASE".equals(aiState) ? 2 : "SEARCH".equals(aiState) ? 1 : 0;
        visor.setMaterial(visorMats[si]);
        hem.setMaterial(hemMats[si]);
        sentinelLight.setColor(scale(STATE_COLORS[si], si == 2 ? 0.85 + Math.sin(tt * 9) * 0.15 : 0.6));

        if (dToPlayer < 1.25 && !hidden && aiStun <= 0) {
            sitting = null;
            crouch = false;
            listener.onCaught();
            resetPlayer();
        }
    }

    private void updateHover() {
        double camY = eye + py;
        double cp = Math.cos(pitch);
        double dx = -Math.sin(yaw) * cp, dy = Math.sin(pitch), dz = -Math.cos(yaw) * cp;
        String id = null;
        double best = RAY_FAR;
        // Reject targets behind walls before testing furniture. Collision and picking
        // use the same world coordinate system, including rotated room dividers.
        for (Node wall : interactionWalls) {
            double t = rayHit(px, camY, pz, dx, dy, dz, worldBounds(wall), RAY_FAR);
            if (t >= 0) best = Math.min(best, t);
        }
        for (Interactable it : interactables) {
            double t = rayHit(px, camY, pz, dx, dy, dz, worldBounds(it.node), RAY_FAR);
            if (t >= 0 && t < best) { best = t; id = it.id; }
        }
        if (!java.util.Objects.equals(id, hoverId)) {
            hoverId = id;
            Interactable o = id == null ? null : named.get(id);
            listener.onHover(id, o == null ? null : o.label, o == null ? null : o.verb);
        }
    }

    private void placeCamera(double dt) {
        double want = hidden ? 1.0 : sitting != null ? 1.18 : crouch ? 0.95 : 1.62;
        eye = Double.isNaN(eye) ? want : eye + (want - eye) * Math.min(1, 0.18 * dt * 60);
        camera.setTranslateX(px);
        camera.setTranslateY(eye + py + Math.sin(bob) * 0.035);
        camera.setTranslateZ(pz);
        camYaw.setAngle(Math.toDegrees(yaw));
        camPitch.setAngle(Math.toDegrees(pitch));
    }

    private void placeLights(double tt) {
        at(lamp, px, eye + py + 0.25, pz);

        Panel nearest = panels.get(0);
        double nd = Double.MAX_VALUE;
        for (Panel p : panels) {
            double d = Math.hypot(p.x - px, p.z - pz);
            if (d < nd) { nd = d; nearest = p; }
        }
        at(roomLight, nearest.x, H - 0.45, nearest.z);
        roomLight.setColor(scale(nearest.color, 0.9 * nearest.level));

        at(sentinel, sx, Math.sin(tt * 2.6) * 0.045, sz);
        sentinelYaw.setAngle(Math.toDegrees(aiYaw));
        at(sentinelLight, sx, 2.1, sz);
    }

    /* ============================================================ helpers */

    private void add(Node n) { world.getChildren().add(n); }

    private Bounds worldBounds(Node n) {
        Bounds b = n.getBoundsInLocal();
        for (Node p = n; p != null && p != world; p = p.getParent()) b = p.localToParent(b);
        return b;
    }

    private void addCollider(Node n, double grow, boolean blocksSight) {
        Aabb b = new Aabb(worldBounds(n), grow);
        colliders.add(b);
        if (blocksSight) sightBlockers.add(b);
    }

    private Interactable reg(Node n, String id, String label, String verb, String kind) {
        Interactable it = new Interactable(n, id, label, verb, kind);
        interactables.add(it);
        named.put(id, it);
        return it;
    }

    private Anim anim(double speed, DoubleConsumer apply) {
        Anim a = new Anim(speed, apply);
        animators.add(a);
        return a;
    }

    private static <T extends Node> T at(T n, double x, double y, double z) {
        n.setTranslateX(x); n.setTranslateY(y); n.setTranslateZ(z);
        return n;
    }

    private static <T extends Node> T rotY(T n, double rad) {
        n.getTransforms().add(new Rotate(Math.toDegrees(rad), Rotate.Y_AXIS));
        return n;
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static Color rgb(int hex) { return Color.rgb((hex >> 16) & 0xff, (hex >> 8) & 0xff, hex & 0xff); }

    private static Color scale(Color c, double k) {
        return Color.color(clamp(c.getRed() * k, 0, 1), clamp(c.getGreen() * k, 0, 1), clamp(c.getBlue() * k, 0, 1));
    }

    private static Box box(double w, double h, double d, PhongMaterial m) {
        Box b = new Box(w, h, d);
        b.setMaterial(m);
        return b;
    }

    private static Cylinder cyl(double r, double h, PhongMaterial m) {
        Cylinder c = new Cylinder(r, h, 16);
        c.setMaterial(m);
        return c;
    }

    private static PhongMaterial mat(int hex) {
        PhongMaterial m = new PhongMaterial(rgb(hex));
        m.setSpecularColor(Color.gray(0.06));
        m.setSpecularPower(10);
        return m;
    }

    private static PhongMaterial metal(int hex) {
        PhongMaterial m = new PhongMaterial(rgb(hex));
        m.setSpecularColor(Color.gray(0.4));
        m.setSpecularPower(26);
        return m;
    }

    /** Unlit, self-illuminated colour: stands in for three.js emissive + its own light. */
    private PhongMaterial glow(Color c, double k) {
        Color e = scale(c, k);
        PhongMaterial m = new PhongMaterial(Color.BLACK);
        m.setSelfIlluminationMap(solidImages.computeIfAbsent(e, CurfewWorld::solid));
        return m;
    }

    private static Image solid(Color c) {
        WritableImage img = new WritableImage(2, 2);
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) img.getPixelWriter().setColor(x, y, c);
        return img;
    }

    private static PhongMaterial emissiveTexture(Image img, double k) {
        PhongMaterial m = new PhongMaterial(Color.gray(0.25));
        m.setDiffuseMap(img);
        m.setSelfIlluminationMap(k >= 1 ? img : dim(img, k));
        return m;
    }

    private static Image dim(Image src, double k) {
        int w = (int) src.getWidth(), h = (int) src.getHeight();
        WritableImage out = new WritableImage(w, h);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            Color c = src.getPixelReader().getColor(x, y);
            out.getPixelWriter().setColor(x, y, Color.color(c.getRed() * k, c.getGreen() * k, c.getBlue() * k, c.getOpacity()));
        }
        return out;
    }

    /** Plane facing +Z (like THREE.PlaneGeometry), texture upright when seen from the front. */
    private static MeshView quad(double w, double h, PhongMaterial m) {
        TriangleMesh mesh = new TriangleMesh();
        float hw = (float) (w / 2), hh = (float) (h / 2);
        mesh.getPoints().addAll(-hw, hh, 0, hw, hh, 0, hw, -hh, 0, -hw, -hh, 0);
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
        MeshView v = new MeshView(mesh);
        v.setCullFace(CullFace.NONE);
        v.setMaterial(m);
        return v;
    }

    /** Open cone along Y, apex at +h/2 (like THREE.ConeGeometry without its cap). */
    private static TriangleMesh cone(double r, double h, int seg) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(0, (float) (h / 2), 0);
        for (int i = 0; i < seg; i++) {
            double a = Math.PI * 2 * i / seg;
            mesh.getPoints().addAll((float) (Math.sin(a) * r), (float) (-h / 2), (float) (Math.cos(a) * r));
        }
        mesh.getTexCoords().addAll(0.5f, 0, 0, 1, 1, 1);
        for (int i = 0; i < seg; i++) {
            int a = 1 + i, b = 1 + (i + 1) % seg;
            mesh.getFaces().addAll(0, 0, a, 1, b, 2);
        }
        return mesh;
    }

    /* --------------------------------------------------- procedural textures */

    private static Image canvasTexture(int w, int h, Consumer<GraphicsContext> draw) {
        Canvas c = new Canvas(w, h);
        draw.accept(c.getGraphicsContext2D());
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    private static PhongMaterial woodMaterial() {
        PhongMaterial material = new PhongMaterial(Color.WHITE);
        material.setDiffuseMap(canvasTexture(256, 256, g -> {
            g.setFill(Color.web("#795c42"));
            g.fillRect(0, 0, 256, 256);
            Random grain = new Random(42);
            for (int i = 0; i < 180; i++) {
                g.setStroke(Color.color(0.18, 0.09, 0.04, 0.08 + grain.nextDouble() * 0.18));
                double y = grain.nextDouble() * 256;
                g.strokeLine(0, y, 256, y + grain.nextDouble() * 7);
            }
        }));
        material.setSpecularColor(Color.gray(0.18));
        material.setSpecularPower(24);
        return material;
    }

    private static Font mono(double size) { return Font.font("Consolas", FontWeight.BOLD, size); }

    /** The metre-scale floor tiles are baked once into one shared image. */
    private Image floorTexture() {
        int cellsX = 50, cellsY = 30, cell = 24;
        return canvasTexture(cellsX * cell, cellsY * cell, g -> {
            double w = cellsX * cell, h = cellsY * cell;
            g.setFill(Color.web("#1c2128"));
            g.fillRect(0, 0, w, h);
            Random grain = new Random(2048);
            for (int y = 0; y < cellsY; y++) for (int x = 0; x < cellsX; x++) {
                g.setFill(Color.web((x + y) % 2 == 0 ? "#4a565d" : "#434f57"));
                g.fillRect(x * cell + 1, y * cell + 1, cell - 2, cell - 2);
                g.setStroke(Color.color(0.7, 0.8, 0.85, 0.15));
                g.strokeLine(x * cell + 2, y * cell + 2, (x + 1) * cell - 2, y * cell + 2);
            }
            for (int i = 0; i < 9000; i++) {
                g.setFill(Color.color(1, 1, 1, grain.nextDouble() * 0.04));
                g.fillRect(grain.nextDouble() * w, grain.nextDouble() * h, 2, 2);
            }
            g.setStroke(Color.color(0, 0, 0, 0.55));
            g.setLineWidth(2);
            for (int i = 0; i <= cellsX; i++) g.strokeLine(i * cell, 0, i * cell, h);
            for (int i = 0; i <= cellsY; i++) g.strokeLine(0, i * cell, w, i * cell);
        });
    }

    private Image wallTexture() {
        return canvasTexture(256, 256, g -> {
            g.setFill(Color.web("#64757b"));
            g.fillRect(0, 0, 256, 256);
            g.setFill(Color.web("#354c55"));
            g.fillRect(0, 176, 256, 80);
            g.setFill(Color.web("#93a6a5"));
            g.fillRect(0, 173, 256, 3);
            for (int i = 0; i < 900; i++) {
                g.setFill(Color.color(0, 0, 0, rng.nextDouble() * 0.12));
                g.fillRect(rng.nextDouble() * 256, rng.nextDouble() * 256, 3, 3);
            }
        });
    }

    private static Image boardTexture() {
        Random r = new Random(3);
        return canvasTexture(1024, 512, g -> {
            g.setFill(Color.web("#1d2a2c"));
            g.fillRect(0, 0, 1024, 512);
            g.setStroke(Color.color(120 / 255.0, 220 / 255.0, 215 / 255.0, 0.10));
            g.setLineWidth(2);
            for (int i = 0; i < 18; i++) g.strokeLine(r.nextDouble() * 1024, r.nextDouble() * 512, r.nextDouble() * 1024, r.nextDouble() * 512);
            g.setFill(Color.web("#7ef3e8"));
            g.setFont(mono(54));
            g.fillText("CURFEW PROTOCOL — LESSON 01", 36, 100);
            g.setFont(mono(38));
            g.setFill(Color.web("#cfeeea"));
            g.fillText("the unit does not blink. it listens.", 36, 180);
            g.fillText("3 NODES MUST BE CLEARED BEFORE THE", 36, 250);
            g.fillText("EXIT BAY WILL RELEASE YOU.", 36, 300);
            g.setFill(Color.web("#ffb347"));
            g.setFont(mono(32));
            g.fillText("hide. the shelves are deeper than they look.", 36, 400);
        });
    }

    private static Image screenTexture(String[] lines, String accent) {
        return canvasTexture(512, 384, g -> {
            g.setFill(Color.web("#04100f"));
            g.fillRect(0, 0, 512, 384);
            g.setFill(Color.web(accent));
            g.setFont(mono(26));
            for (int i = 0; i < lines.length; i++) g.fillText(lines[i], 22, 52 + i * 40);
            g.setStroke(Color.color(126 / 255.0, 243 / 255.0, 232 / 255.0, 0.18));
            g.setLineWidth(1);
            for (int y = 0; y < 384; y += 4) g.strokeLine(0, y, 512, y);
        });
    }

    private static Image signTexture(String text, String accent) {
        return canvasTexture(512, 128, g -> {
            g.setFill(Color.web("#0a0f14"));
            g.fillRect(0, 0, 512, 128);
            g.setStroke(Color.web(accent));
            g.setLineWidth(8);
            g.strokeRect(10, 10, 492, 108);
            g.setFill(Color.web(accent));
            g.setFont(mono(52));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(text, 256, 64);
        });
    }
}
