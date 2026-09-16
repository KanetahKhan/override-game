package com.override.game.minigames;

import com.override.Main;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches Chapter 2 — Harvest Protocol, the Godot endless runner in
 * {@code chapter2-godot/}.
 *
 * <p>Prefers the exported build ({@code chapter2-godot/build/Chapter2.exe}, the
 * path in its export_presets.cfg). Without one it runs the project through a
 * Godot editor binary named by the {@code GODOT} environment variable or found
 * on the PATH. Either way the runner opens over the game window, at its size.
 * If neither exists the player gets a message instead of a crash.</p>
 */
public final class GodotGameLauncher {

    private static final Path PROJECT =
        Path.of(System.getProperty("override.chapter2Dir", "chapter2-godot"));

    private GodotGameLauncher() {}

    public static void launchGodotAtWindowSize() {
        List<String> cmd = new ArrayList<>();
        Path exported = PROJECT.resolve("build").resolve("Chapter2.exe");
        if (Files.isRegularFile(exported)) {
            cmd.add(exported.toAbsolutePath().toString());
        } else {
            String godot = findGodot();
            if (godot == null) {
                show("Chapter 2 isn't built yet.\n\n"
                    + "Export chapter2-godot (Project > Export > Windows Desktop) to "
                    + "chapter2-godot/build/Chapter2.exe, or set the GODOT environment "
                    + "variable to your Godot editor executable.");
                return;
            }
            cmd.add(godot);
            cmd.add("--path");
            cmd.add(PROJECT.toAbsolutePath().toString());
        }

        Stage stage = Main.getStage();
        if (stage != null) {
            cmd.add("--resolution");
            cmd.add(Math.round(stage.getWidth()) + "x" + Math.round(stage.getHeight()));
            cmd.add("--position");
            cmd.add(Math.round(stage.getX()) + "," + Math.round(stage.getY()));
        }

        try {
            new ProcessBuilder(cmd).inheritIO().start();
        } catch (IOException e) {
            show("Could not start Chapter 2: " + e.getMessage());
        }
    }

    private static String findGodot() {
        String env = System.getenv("GODOT");
        if (env != null && isFile(env)) return env;
        String path = System.getenv("PATH");
        if (path == null) return null;
        for (String dir : path.split(File.pathSeparator)) {
            for (String name : new String[] {"godot.exe", "godot4.exe", "godot", "godot4"}) {
                String candidate = dir + File.separator + name;
                if (isFile(candidate)) return candidate;
            }
        }
        return null;
    }

    private static boolean isFile(String p) {
        try {
            return Files.isRegularFile(Path.of(p));
        } catch (InvalidPathException e) {
            return false;   // odd PATH entries (quotes, stray characters)
        }
    }

    private static void show(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }
}
