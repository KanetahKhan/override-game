package com.override.shared.ui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Runs the actual presentation components, without stubbing missing campaign classes. */
public final class OpeningVisualSmoke {
    public static void main(String[] args) throws Exception {
        Path output = Path.of("target/opening-preview");
        Files.createDirectories(output);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                Stage stage = new Stage();
                int[] calls = new int[4];
                var actions = new OpeningMenuView.Actions(() -> calls[0]++, () -> calls[1]++, () -> calls[2]++, () -> calls[3]++);
                OpeningMenuView menu = new OpeningMenuView(false, actions, false);
                show(stage, menu);
                check(button(menu, "continue-game").isDisabled(), "Continue must require a save");
                button(menu, "new-game").fire(); button(menu, "shop").fire(); button(menu, "quit").fire();
                check(calls[0] == 1 && calls[2] == 1 && calls[3] == 1, "Menu callbacks");
                snapshot(menu, output.resolve("menu.png"));
                button(menu, "display-options").fire(); menu.applyCss(); menu.layout();
                CheckBox reduced = (CheckBox) menu.lookup("#reduced-motion");
                reduced.fire(); check(OpeningPreferences.REDUCED_MOTION.get(), "Reduced motion toggle");
                snapshot(menu, output.resolve("display-options.png"));
                button(menu, "close-options").fire();
                OpeningPreferences.REDUCED_MOTION.set(false);
                OpeningMenuView saved = new OpeningMenuView(true, actions, false);
                show(stage, saved);
                check(!button(saved, "continue-game").isDisabled(), "Continue must enable with a save");
                button(saved, "continue-game").fire(); check(calls[1] == 1, "Continue callback");

                int[] completions = {0};
                IntroStoryScreen intro = new IntroStoryScreen(() -> completions[0]++, false);
                Parent film = intro.build(); show(stage, film);
                button(film, "pause-intro").fire();
                check(button(film, "pause-intro").getText().startsWith("RESUME"), "Pause state");
                button(film, "pause-intro").fire();
                for (int shot = 0; shot < 5; shot++) {
                    intro.renderAt(shot * 8 + 3);
                    snapshot(film, output.resolve("story-" + (shot + 1) + ".png"));
                }
                Event.fireEvent(film, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
                button(film, "skip-intro").fire();
                check(completions[0] == 1, "Skip/finish callback must fire exactly once");
                ClassroomPixelScene pixels = new ClassroomPixelScene();
                pixels.menu(1, true); WritableImage stillA = pixels.snapshot(null, null);
                pixels.menu(19, true); WritableImage stillB = pixels.snapshot(null, null);
                check(equal(stillA, stillB), "Reduced-motion menu must be static");
                if (args.length > 0 && args[0].equals("--video")) exportFilm(pixels, output.resolve("silent-classroom-intro.mp4"));
                stage.close();
                System.out.println("PASS: menu actions, save gating, display options, pause, single completion, static reduced motion, seven real UI screenshots");
            } catch (Throwable error) { failure.set(error); }
            finally { done.countDown(); }
        });
        done.await(); Platform.exit();
        if (failure.get() != null) throw new AssertionError("Opening smoke test failed", failure.get());
    }

    private static void show(Stage stage, Parent root) {
        Scene scene = new Scene(root, 1280, 720);
        // Include the same global stylesheet as the game so CSS collisions are visible.
        scene.getStylesheets().add(OpeningVisualSmoke.class.getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene); stage.show(); root.applyCss(); root.layout();
    }

    private static Button button(Parent root, String id) {
        Button b = (Button) root.lookup("#" + id);
        if (b == null) throw new AssertionError("Missing button: " + id);
        return b;
    }

    private static boolean equal(WritableImage a, WritableImage b) {
        for (int y = 0; y < 720; y++) for (int x = 0; x < 1280; x++)
            if (a.getPixelReader().getArgb(x, y) != b.getPixelReader().getArgb(x, y)) return false;
        return true;
    }

    private static void snapshot(Parent root, Path path) throws Exception {
        WritableImage image = root.snapshot(null, null);
        check(image.getWidth() == 1280 && image.getHeight() == 720, "Preview must fit the design space");
        BufferedImage png = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] data = ((DataBufferInt) png.getRaster().getDataBuffer()).getData();
        image.getPixelReader().getPixels(0, 0, 1280, 720, PixelFormat.getIntArgbPreInstance(), data, 0, 1280);
        ImageIO.write(png, "png", path.toFile());
    }

    private static void exportFilm(ClassroomPixelScene canvas, Path path) throws Exception {
        // The MP4 is recorded from the same renderer as the in-game sequence, not a mockup.
        Process encoder = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo",
            "-pixel_format", "bgra", "-video_size", "1280x720", "-framerate", "24", "-i", "-",
            "-an", "-c:v", "libx264", "-preset", "fast", "-crf", "22", "-pix_fmt", "yuv420p",
            "-movflags", "+faststart", path.toString()).inheritIO().redirectInput(ProcessBuilder.Redirect.PIPE).start();
        byte[] frame = new byte[1280 * 720 * 4];
        WritableImage image = new WritableImage(1280, 720);
        try (OutputStream pipe = encoder.getOutputStream()) {
            for (int n = 0; n < 40 * 24; n++) {
                canvas.film(n / 24.0, false);
                canvas.snapshot(null, image);
                image.getPixelReader().getPixels(0, 0, 1280, 720, PixelFormat.getByteBgraPreInstance(), frame, 0, 1280 * 4);
                pipe.write(frame);
            }
        }
        check(encoder.waitFor() == 0, "Video encoding failed");
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
