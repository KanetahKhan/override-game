package com.override.shared.ui;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;

/** Reuses the supplied sci-fi atlas as a subdued metal frame, with live game typography. */
public final class GameControls {
    private static final Image ATLAS = new Image(GameControls.class.getResourceAsStream("/ui/scifi-controls.jpg"),
        3000, 1500, true, true);
    private GameControls() { }

    public static Button button(String caption, double width, double height, boolean primary) {
        Button b = new Button(caption);
        ImageView frame = new ImageView(ATLAS);
        // The blank third frame: no baked-in text, icons or unrelated parts of the source sheet.
        frame.setViewport(new Rectangle2D(161, 755, 558, 179));
        frame.setFitWidth(width); frame.setFitHeight(height); frame.setSmooth(true);
        frame.setMouseTransparent(true);
        frame.fitWidthProperty().bind(b.widthProperty());
        frame.fitHeightProperty().bind(b.heightProperty());
        Polygon clip = new Polygon(); frame.setClip(clip);
        Runnable resize = () -> {
            double w = b.getWidth(), h = b.getHeight(), corner = Math.min(16, h / 3);
            clip.getPoints().setAll(corner, 0.0, w-corner, 0.0, w, h/2, w-corner, h, corner, h, 0.0, h/2);
        };
        b.widthProperty().addListener((o, old, value) -> resize.run());
        b.heightProperty().addListener((o, old, value) -> resize.run());
        b.setGraphic(frame);
        b.setContentDisplay(ContentDisplay.CENTER);
        b.setAlignment(Pos.CENTER);
        b.setMinSize(width, height); b.setPrefSize(width, height); b.setMaxSize(width, height);
        b.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 0;"
            + " -fx-font-family: 'Monospaced'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;"
            + " -fx-text-fill: " + (primary ? "#d9fff1" : "#d1e0e5") + ";");
        Runnable style = () -> {
            boolean highlighted = b.isHover() || b.isFocused();
            double brightness = b.isPressed() ? -0.48 : highlighted ? -0.05 : primary ? -0.20 : -0.38;
            frame.setEffect(new ColorAdjust(-0.19, -0.58, brightness, 0.12));
            b.setOpacity(b.isDisabled() ? 0.38 : 1);
        };
        b.hoverProperty().addListener((o, a, v) -> style.run());
        b.focusedProperty().addListener((o, a, v) -> style.run());
        b.pressedProperty().addListener((o, a, v) -> style.run());
        b.disabledProperty().addListener((o, a, v) -> style.run());
        style.run();
        return b;
    }

    static Label label(String text, double size, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: " + size + "px; -fx-text-fill: " + color + ";");
        return l;
    }

    static StackPane page(Node content, double darkness) {
        ClassroomPixelScene room = new ClassroomPixelScene();
        room.menu(0, true);
        Region shade = new Region();
        shade.setStyle("-fx-background-color: rgba(3,9,16," + darkness + ");");
        shade.setMouseTransparent(true);
        StackPane root = new StackPane(room, shade, content);
        root.setMinSize(1280, 720); root.setPrefSize(1280, 720); root.setMaxSize(1280, 720);
        root.getStylesheets().add(GameControls.class.getResource("/styles/player-pages.css").toExternalForm());
        return root;
    }
}
