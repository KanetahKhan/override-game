package com.override.chapter1;

import com.override.shared.model.GameState;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the real chapter HUD and its world-event path, including the hacking overlay. */
public final class MinimapSmoke {
    public static void main(String[] args) throws Exception {
        System.setProperty("override.mouseLock", "false");
        Path output = Path.of("target/minimap-preview");
        Files.createDirectories(output);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            Stage stage = new Stage();
            CurfewProtocolScreen screen = new CurfewProtocolScreen();
            try {
                GameState.init();
                Parent root = screen.build();
                Scene scene = new Scene(root, 1280, 720);
                scene.getStylesheets().add(MinimapSmoke.class.getResource("/styles/main.css").toExternalForm());
                stage.setScene(scene);
                stage.show();
                root.applyCss(); root.layout();
                CurfewMinimap map = (CurfewMinimap) root.lookup("#classroom-minimap");
                Circle player = (Circle) map.lookup("#minimap-player");
                Circle robot = (Circle) map.lookup("#minimap-robot");
                CurfewWorld world = field(screen, "world");
                CurfewWorld.Listener events = field(world, "listener");
                check(world.mapRooms().size() == 6, "All six rooms must be labelled");
                check(world.mapFootprints().stream().anyMatch(CurfewWorld.MapFootprint::wall)
                    && world.mapFootprints().stream().filter(f -> !f.wall()).count() > 30,
                    "The map must include the real walls and furniture");
                check(Color.web("#4b9bff").equals(player.getFill()), "Player must be blue");
                check(Color.web("#ff4058").equals(robot.getFill()), "Robot must be red");
                visible(map); visible(player); visible(robot);
                double spawnX = player.getCenterX(), spawnY = player.getCenterY();
                check(spawnX < map.getWidth() / 2 && spawnY > 100, "Real west-hall spawn must appear immediately");
                button(root, "ENTER THE FLOOR").fire();

                // Hidden player, unseen robot, no Astra: dots must still move into opposite rooms.
                events.onTick(tick(17, -10, -17, 10, 0));
                visible(robot); visible(player);
                check(player.getCenterX() > robot.getCenterX() && player.getCenterY() < robot.getCenterY(),
                    "North-east player and south-west robot must match the floor plan");
                double eastX = player.getCenterX(), northY = player.getCenterY();
                events.onTick(tick(7, 0, -17, 10, Math.PI));
                near(eastX - player.getCenterX(), player.getCenterY() - northY, "Map must preserve the X/Z scale");
                events.onTick(tick(-17, 10, 17, -10, Math.PI));
                check(player.getCenterX() < robot.getCenterX() && player.getCenterY() > robot.getCenterY(),
                    "Markers must follow both axes without rotating the north-up map");

                invoke(screen, "openGame", new Class<?>[] {String.class, String.class}, "sc", "node3");
                events.onTick(tick(-17, 10, 0, 0, 0));
                visible(map); visible(robot);
                StackPane overlays = field(screen, "overlayLayer");
                StackPane layers = (StackPane) root;
                check(layers.getChildren().indexOf(map.getParent()) > layers.getChildren().indexOf(overlays),
                    "Map must stay above the opaque hacking overlay");
                root.applyCss(); root.layout();
                check(map.localToScene(map.getBoundsInLocal()).getMinX() > 990,
                    "Map must fit beside the widest hacking panel");
                snapshot(map, output.resolve("classroom-minimap.png"));
                snapshot(root, output.resolve("minimap-during-hack.png"));
                invoke(screen, "pause", new Class<?>[0]);
                visible(map); visible(robot);
                invoke(screen, "resume", new Class<?>[0]);
                invoke(screen, "closeGame", new Class<?>[] {CurfewNodeGames.Outcome.class}, CurfewNodeGames.Outcome.QUIT);

                world.resetPlayer();
                events.onTick(world.snapshot());
                near(player.getCenterX(), spawnX, "Respawn X must update immediately");
                near(player.getCenterY(), spawnY, "Respawn Z must update immediately");
                int dependency = GameState.get().getDependency();
                invoke(screen, "astraScan", new Class<?>[0]);
                visible(map.lookup("#minimap-scan"));
                check(GameState.get().getDependency() == dependency + 5, "Optional Astra scan retains its existing cost");
                map.setScanning(false);
                check(!map.lookup("#minimap-scan").isVisible(), "Expired scan must remove only its heading overlay");
                visible(robot);
                snapshot(map, output.resolve("minimap-at-spawn.png"));
                System.out.println("PASS: real floor layout, spawn, blue/red markers, unseen/hidden tracking, movement, "
                    + "north-up scale, hacking overlay, pause/resume, respawn, optional scan, screenshots");
            } catch (Throwable error) { failure.set(error); }
            finally {
                try { invoke(screen, "dispose", new Class<?>[0]); }
                catch (ReflectiveOperationException ignored) { }
                stage.close();
                done.countDown();
            }
        });
        done.await();
        Platform.exit();
        if (failure.get() != null) throw new AssertionError("Minimap smoke test failed", failure.get());
    }

    private static CurfewWorld.Tick tick(double px, double pz, double sx, double sz, double yaw) {
        return new CurfewWorld.Tick(px, pz, sx, sz, "PATROL", Math.hypot(px - sx, pz - sz),
            true, 1, "HIDDEN", null, 0, false, 0, yaw, 0);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(object);
    }

    private static void invoke(Object object, String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(object, args);
    }

    private static Button button(Parent root, String text) {
        return root.lookupAll(".button").stream().map(n -> (Button) n).filter(b -> b.getText().equals(text))
            .findFirst().orElseThrow(() -> new AssertionError("Missing button: " + text));
    }

    private static void visible(Node node) {
        check(node != null, "Missing map node");
        for (Node n = node; n != null; n = n.getParent()) check(n.isVisible(), "Map marker was hidden");
    }

    private static void near(double a, double b, String message) { check(Math.abs(a - b) < 0.01, message); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static void snapshot(Node node, Path path) throws Exception {
        WritableImage image = node.snapshot(null, null);
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        BufferedImage png = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] pixels = ((DataBufferInt) png.getRaster().getDataBuffer()).getData();
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
        ImageIO.write(png, "png", path.toFile());
    }
}
