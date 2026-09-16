package com.override.game.minigames;

import com.override.Main;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches Chapter 2 – Harvest Protocol, the Godot endless runner in
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

    private static final Path RESULT =
        PROJECT.resolve("build").resolve("chapter2_result.json");

    /** Machine-local pointer at a Godot binary, beside the project. Gitignored. */
    private static final String POINTER_FILE = "godot-path.txt";

    private static Process godotProcess;
    private static Runnable processExitHook;

    private GodotGameLauncher() {}

    public static boolean hasResult() {
        return Files.isRegularFile(RESULT);
    }

    public static String readResult() {
        try {
            return Files.readString(RESULT, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static void clearResult() {
        try {
            Files.deleteIfExists(RESULT);
        } catch (IOException ignored) {
        }
    }

    public static boolean launchGodotBackgroundStart() {
        if (hasResult()) {
            clearResult();
        }
        Path exported = PROJECT.resolve("build").resolve("Chapter2.exe");
        if (Files.isRegularFile(exported)) {
            godotProcess = spawn(exported.toAbsolutePath().toString());
        } else {
            String godot = findGodot();
            if (godot == null) {
                showNoGodot();
                return false;
            }
            godotProcess = spawn(godot, "--path", PROJECT.toAbsolutePath().toString());
        }
        if (godotProcess != null) {
            new Thread(() -> {
                try {
                    int code = godotProcess.waitFor();
                    if (processExitHook != null) {
                        processExitHook.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "chapter2-process-watcher").setDaemon(true);
            new Thread(() -> {
                try {
                    godotProcess.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "chapter2-exit-watcher").setDaemon(true);
            return true;
        }
        return false;
    }

    public static void launchGodotAtWindowSize() {
        List<String> cmd = new ArrayList<>();
        Path exported = PROJECT.resolve("build").resolve("Chapter2.exe");
        if (Files.isRegularFile(exported)) {
            cmd.add(exported.toAbsolutePath().toString());
        } else {
            String godot = findGodot();
            if (godot == null) {
                showNoGodot();
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

    public static void onProcessExit(Runnable action) {
        if (godotProcess != null && godotProcess.isAlive()) {
            processExitHook = action;
        } else if (action != null) {
            action.run();
        }
    }

    public static Path gameSource() {
        return PROJECT;
    }

    public static void exportGame() {
        show("Export Chapter 2 once:\n\n"
            + "Open chapter2-godot/project.godot, pick Project > Export > Windows "
            + "Desktop, and export to chapter2-godot/build/Chapter2.exe.\n\n"
            + "No Godot install is needed to play after that."
        );
    }

    private static Process spawn(String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            return pb.start();
        } catch (IOException e) {
            show("Could not start Chapter 2: " + e.getMessage());
            return null;
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
            return false;
        }
    }

    private static void show(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }
}
