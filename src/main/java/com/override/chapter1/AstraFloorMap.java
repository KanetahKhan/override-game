package com.override.chapter1;

import com.override.net.AstraProtocol;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * The floor as Astra sees it: six rooms, a corridor, and live dots.
 *
 * <p>Deliberately not {@link CurfewMinimap} — that one reads its geometry from
 * a built 3D world, and this end of the link has no world to build. The layout
 * numbers mirror {@link CurfewWorld}: 50m by 30m, corridor between z = -3 and 3.
 */
final class AstraFloorMap extends Pane {

    private static final double W = 520, H = 312;
    private static final double HX = CurfewWorld.HX, HZ = CurfewWorld.HZ;

    private final Circle player = dot(6, Color.web("#7ef3e8"));
    private final Circle unit = dot(7, Color.web("#ff3d5a"));
    private final Circle escort = dot(7, Color.web("#ffb347"));
    private AstraProtocol.Tick last;

    AstraFloorMap() {
        setMinSize(W, H);
        setPrefSize(W, H);
        setMaxSize(W, H);
        setStyle("-fx-background-color: rgba(4,12,14,0.85); -fx-border-color: rgba(53,224,216,0.4);");

        // corridor band
        Region corridor = new Region();
        corridor.setLayoutX(0);
        corridor.setLayoutY(mapZ(-3));
        corridor.setPrefSize(W, mapZ(3) - mapZ(-3));
        corridor.setStyle("-fx-background-color: rgba(53,224,216,0.07);"
            + " -fx-border-color: rgba(53,224,216,0.25) transparent;");
        getChildren().add(corridor);

        room("LAB 01", -25, -15, -8.5, -3);
        room("CLASS 2A", -8.5, -15, 8.5, -3);
        room("LAB 02", 8.5, -15, 25, -3);
        room("CLASS 1B", -25, 3, -8.5, 15);
        room("SERVER", -8.5, 3, 8.5, 15);
        room("EXIT BAY", 8.5, 3, 25, 15);

        escort.setVisible(false);
        getChildren().addAll(unit, escort, player);
    }

    /** Draws one frame from the game; returns nothing, keeps the last tick for commands. */
    void update(AstraProtocol.Tick t) {
        last = t;
        player.setCenterX(mapX(t.px()));
        player.setCenterY(mapZ(t.pz()));
        player.setOpacity(t.hidden() ? 0.35 : 1);
        unit.setCenterX(mapX(t.sx()));
        unit.setCenterY(mapZ(t.sz()));
        unit.setFill(Color.web("CHASE".equals(t.state()) ? "#ff3d5a"
            : "SEARCH".equals(t.state()) ? "#ffb347" : "#35e0d8"));
        escort.setVisible(t.twoUnits());
        if (t.twoUnits()) {
            escort.setCenterX(mapX(t.ex()));
            escort.setCenterY(mapZ(t.ez()));
        }
    }

    /** The last frame received, or null before the game connects. */
    AstraProtocol.Tick lastTick() {
        return last;
    }

    void clear() {
        last = null;
        escort.setVisible(false);
    }

    private void room(String name, double x1, double z1, double x2, double z2) {
        Region r = new Region();
        r.setLayoutX(mapX(x1));
        r.setLayoutY(mapZ(z1));
        r.setPrefSize(mapX(x2) - mapX(x1), mapZ(z2) - mapZ(z1));
        r.setStyle("-fx-background-color: rgba(126,243,232,0.04);"
            + " -fx-border-color: rgba(53,224,216,0.22);");
        Text label = new Text(name);
        label.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        label.setFill(Color.web("rgba(126,243,232,0.55)"));
        label.setX(mapX(x1) + 8);
        label.setY(mapZ(z1) + 18);
        getChildren().addAll(r, label);
    }

    private static Circle dot(double radius, Color colour) {
        Circle c = new Circle(radius, colour);
        c.setStyle("-fx-effect: dropshadow(gaussian, " + toCss(colour) + ", 12, 0, 0, 0);");
        return c;
    }

    private static String toCss(Color c) {
        return String.format("#%02x%02x%02x", (int) (c.getRed() * 255),
            (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    private static double mapX(double x) {
        return (x + HX) / (HX * 2) * W;
    }

    private static double mapZ(double z) {
        return (z + HZ) / (HZ * 2) * H;
    }
}
