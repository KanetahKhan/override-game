package com.override.chapter1;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/** North-up floor plan. Geometry and live positions both come from the 3D world. */
final class CurfewMinimap extends Pane {
    private static final double WIDTH = 264, HEIGHT = 224;
    private static final double LEFT = 12, TOP = 32;
    private static final double SCALE = (WIDTH - LEFT * 2) / (CurfewWorld.HX * 2);
    private static final Color PLAYER = Color.web("#4b9bff");
    private static final Color ROBOT = Color.web("#ff4058");
    private static final Color INK = Color.web("#061019");

    private final List<CurfewWorld.MapRoom> rooms;
    private final Circle sentinel = marker("minimap-robot", 5, ROBOT);
    /** The Nightmare escort; hidden on every other difficulty. */
    private final Circle escort = marker("minimap-robot-2", 5, ROBOT);
    private final Circle player = marker("minimap-player", 4, PLAYER);
    private final Line facing = new Line();
    private final Polygon scanCone = new Polygon();
    private final Label detail = CurfewNodeGames.text("", CurfewNodeGames.MONO, 10, "#b8cbd9");
    private CurfewWorld.Tick position;
    private boolean scanning;

    CurfewMinimap(CurfewWorld world) {
        rooms = world.mapRooms();
        setId("classroom-minimap");
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setMouseTransparent(true);
        setStyle("-fx-background-color: rgba(5,13,21,0.96); -fx-border-color: #426170; -fx-border-radius: 6;"
            + " -fx-background-radius: 6;");

        Canvas plan = new Canvas(WIDTH, HEIGHT);
        drawFloor(plan.getGraphicsContext2D(), world.mapFootprints());
        facing.setStroke(PLAYER);
        facing.setStrokeWidth(2);
        scanCone.setFill(Color.web("#ff4058", 0.22));
        scanCone.setStroke(Color.web("#ff4058", 0.6));
        scanCone.setId("minimap-scan");
        // A direction cone can touch the edge, but must not spill over the HUD text.
        scanCone.setClip(new javafx.scene.shape.Rectangle(LEFT, TOP,
            CurfewWorld.HX * 2 * SCALE, CurfewWorld.HZ * 2 * SCALE));
        detail.setLayoutX(LEFT);
        detail.setLayoutY(182);
        detail.setPrefWidth(WIDTH - LEFT * 2);
        escort.setVisible(false);
        getChildren().addAll(plan, scanCone, facing, sentinel, escort, player, detail);
        update(world.snapshot());
    }

    private void drawFloor(GraphicsContext g, List<CurfewWorld.MapFootprint> footprints) {
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        g.setFill(Color.web("#d5e7ee"));
        g.fillText("FLOOR 2 / LIVE MAP", LEFT, 20);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText("N ↑", WIDTH - LEFT, 20);
        g.setTextAlign(TextAlignment.LEFT);

        g.setFill(Color.web("#152632"));
        g.fillRect(LEFT, TOP, CurfewWorld.HX * 2 * SCALE, CurfewWorld.HZ * 2 * SCALE);
        g.setFill(Color.web("#263c48"));
        g.fillRect(LEFT, mapZ(-3), CurfewWorld.HX * 2 * SCALE, 6 * SCALE);
        // Furniture first, then walls, so the door gaps stay easy to read.
        for (boolean walls : new boolean[] {false, true}) {
            g.setFill(Color.web(walls ? "#90aab8" : "#4a606e"));
            for (CurfewWorld.MapFootprint f : footprints) {
                if (f.wall() != walls) continue;
                g.fillRect(mapX(f.minX()), mapZ(f.minZ()),
                    Math.max(1.5, f.width() * SCALE), Math.max(1.5, f.depth() * SCALE));
            }
        }
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        g.setTextAlign(TextAlignment.CENTER);
        for (CurfewWorld.MapRoom room : rooms) {
            g.setFill(Color.web("#08151e", 0.9));
            g.fillRect(mapX(room.x()) - 33, mapZ(room.z()) - 5, 66, 11);
            g.setFill(Color.web(room.name().equals("EXIT BAY") ? "#8ce6b2" : "#d1e3eb"));
            g.fillText(room.name(), mapX(room.x()), mapZ(room.z()) + 3);
        }
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font("Monospaced", 10));
        g.setFill(PLAYER);
        g.fillOval(LEFT, 206, 8, 8);
        g.setFill(Color.web("#d1e3eb"));
        g.fillText("YOU", LEFT + 15, 214);
        g.setFill(ROBOT);
        g.fillOval(95, 206, 8, 8);
        g.setFill(Color.web("#d1e3eb"));
        g.fillText("ROBOT", 110, 214);
        g.setTextAlign(TextAlignment.RIGHT);
        g.setFill(Color.web("#90aab8"));
        g.fillText("TOP DOWN", WIDTH - LEFT, 214);
    }

    /** No visibility, hiding, range, or Astra condition may remove either dot. */
    void update(CurfewWorld.Tick tick) {
        position = tick;
        double x = mapX(tick.px()), y = mapZ(tick.pz());
        player.setCenterX(x);
        player.setCenterY(y);
        sentinel.setCenterX(mapX(tick.sx()));
        sentinel.setCenterY(mapZ(tick.sz()));
        escort.setVisible(tick.twoUnits());
        if (tick.twoUnits()) {
            escort.setCenterX(mapX(tick.ex()));
            escort.setCenterY(mapZ(tick.ez()));
        }
        facing.setStartX(x);
        facing.setStartY(y);
        facing.setEndX(x - Math.sin(tick.yaw()) * 10);
        facing.setEndY(y - Math.cos(tick.yaw()) * 10);
        updateScan();
        setAccessibleText("Floor 2 map. You: blue dot. Robot: red dot. You are in " + roomAt(tick.px(), tick.pz()) + ".");
    }

    void setScanning(boolean enabled) {
        if (scanning == enabled) return;
        scanning = enabled;
        updateScan();
    }

    private void updateScan() {
        if (position == null) return;
        scanCone.setVisible(scanning);
        if (scanning) {
            double x = sentinel.getCenterX(), y = sentinel.getCenterY(), heading = position.sentinelYaw();
            scanCone.getPoints().setAll(x, y,
                x + Math.sin(heading - 0.5) * 28, y + Math.cos(heading - 0.5) * 28,
                x + Math.sin(heading + 0.5) * 28, y + Math.cos(heading + 0.5) * 28);
        }
        detail.setText(scanning ? "ASTRA · " + Math.round(position.dist()) + "M · " + position.state()
            : "YOU · " + roomAt(position.px(), position.pz()));
    }

    private String roomAt(double x, double z) {
        if (Math.abs(z) <= 3) return "HALLWAY";
        CurfewWorld.MapRoom nearest = null;
        for (CurfewWorld.MapRoom room : rooms) {
            if (Math.signum(room.z()) != Math.signum(z)) continue;
            if (nearest == null || Math.abs(room.x() - x) < Math.abs(nearest.x() - x)) nearest = room;
        }
        return nearest == null ? "FLOOR 2" : nearest.name();
    }

    private static Circle marker(String id, double radius, Color color) {
        Circle dot = new Circle(radius, color);
        dot.setId(id);
        dot.setStroke(INK);
        dot.setStrokeWidth(1.5);
        return dot;
    }

    private static double mapX(double x) { return LEFT + (x + CurfewWorld.HX) * SCALE; }
    private static double mapZ(double z) { return TOP + (z + CurfewWorld.HZ) * SCALE; }
}
