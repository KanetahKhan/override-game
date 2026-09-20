package com.override.game.minigames;

import com.override.Main;
import com.override.shared.service.ChapterResultStore;
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
import javafx.application.Platform;
import java.util.Locale;

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
    private static volatile Runnable processExitHook;

    private GodotGameLauncher() {}

    public static boolean hasResult() {
        return ChapterResultStore.has(RESULT);
    }

    public static String readResult() {
        return ChapterResultStore.read(RESULT);
    }

    public static String resultEventId() { return ChapterResultStore.eventId(); }

    public static void clearResult() {
        try {
            ChapterResultStore.clear(RESULT);
        } catch (IOException ignored) {
        }
    }

    public static boolean launchGodotBackgroundStart() {
        if (!prepareResult()) return false;
        Path exported = PROJECT.resolve("build").resolve("Chapter2.exe");
        if (Files.isRegularFile(exported)) {
            // Fullscreen so the game covers exactly the screen the JavaFX
            // window was filling — the handoff happens behind a black fade,
            // so the player never sees a second window appear.
            godotProcess = spawn(exported.toAbsolutePath().toString(), "--fullscreen");
        } else {
            String godot = findGodot();
            if (godot == null) {
                showNoGodot();
                return false;
            }
            godotProcess = spawn(godot, "--path", PROJECT.toAbsolutePath().toString(), "--fullscreen");
        }
        if (godotProcess != null) {
            Process started = godotProcess;
            Thread watcher = new Thread(() -> {
                try {
                    started.waitFor();
                    Runnable hook = processExitHook;
                    if (hook != null) {
                        processExitHook = null;
                        Platform.runLater(hook);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "chapter2-process-watcher");
            watcher.setDaemon(true);
            watcher.start();
            return true;
        }
        return false;
    }

    public static void launchGodotAtWindowSize() {
        if (!prepareResult()) return;
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

    public static boolean isProcessAlive() {
        return godotProcess != null && godotProcess.isAlive();
    }

    public static boolean onProcessExit(Runnable action) {
        if (godotProcess != null && godotProcess.isAlive()) {
            processExitHook = action;
            return false;
        }
        if (action != null) {
            action.run();
        }
        return true;
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

    /**
     * Start Godot with its output going somewhere that cannot fill up.
     *
     * <p>The default {@code ProcessBuilder} pipes are the trap here: nothing on
     * this side reads them, so once Godot has printed a few kilobytes (it is
     * chatty — shader warnings, {@code print()} calls) the OS buffer fills, the
     * child blocks on its next write and never reaches {@code get_tree().quit()}.
     * The loading screen then polls a process that is alive but frozen and never
     * writes a result, so the player is stranded behind the game window. Sending
     * both streams to the null device removes the buffer entirely.</p>
     */
    private static Process spawn(String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            return pb.start();
        } catch (IOException e) {
            show("Could not start Chapter 2: " + e.getMessage());
            return null;
        }
    }

    private static boolean prepareResult() {
        try { ChapterResultStore.begin(RESULT); return true; }
        catch (IOException e) { show("Could not prepare this player's Chapter 2 run: " + e.getMessage()); return false; }
    }

    /**
     * Locate a Godot editor: the {@code override.godot} JVM flag, the {@code GODOT}
     * environment variable, the pointer file beside the project, then PATH.
     *
     * <p>The pointer file exists because environment variables only reach processes
     * started after they are set, so setting {@code GODOT} changes nothing until the
     * editor and every terminal under it are restarted. A file is read at the moment
     * the player presses Play.</p>
     */
    private static String findGodot() {
        String flag = System.getProperty("override.godot");
        if (flag != null && isFile(flag)) return flag;
        String env = System.getenv("GODOT");
        if (env != null && isFile(env)) return env;
        String pointed = readPointer();
        if (pointed != null && isFile(pointed)) return pointed;

        String path = System.getenv("PATH");
        if (path == null) return null;
        for (String dir : path.split(File.pathSeparator)) {
            for (String name : new String[] {"godot.exe", "godot4.exe", "godot", "godot4"}) {
                String candidate = dir + File.separator + name;
                if (isFile(candidate)) return candidate;
            }
            String scanned = scanForGodot(dir);
            if (scanned != null) return scanned;
        }
        return null;
    }

    /** First non-blank, non-comment line of the pointer file, or null. */
    private static String readPointer() {
        try {
            for (String line : Files.readAllLines(PROJECT.resolve(POINTER_FILE))) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) return trimmed;
            }
        } catch (IOException | RuntimeException ignored) {
            // No pointer on this machine; fall through to the remaining sources.
        }
        return null;
    }

    /**
     * Any {@code godot*} executable in one PATH directory. Official downloads keep
     * their version in the filename (Godot_v4.7.1-stable_win64.exe), so the exact
     * names above never match them. A console build is taken only if it is the one
     * thing there: it opens a second terminal window over the game.
     */
    private static String scanForGodot(String dir) {
        File[] hits = new File(dir).listFiles((d, name) -> {
            String n = name.toLowerCase(Locale.ROOT);
            return n.startsWith("godot") && (n.endsWith(".exe") || n.indexOf('.') < 0);
        });
        if (hits == null) return null;
        String console = null;
        for (File f : hits) {
            if (!f.isFile()) continue;
            if (f.getName().toLowerCase(Locale.ROOT).contains("console")) console = f.getAbsolutePath();
            else return f.getAbsolutePath();
        }
        return console;
    }

    private static void showNoGodot() {
        show("Chapter 2 isn't built yet, and no Godot editor was found.\n\n"
            + "Quickest fix: put the full path to your Godot executable on the first "
            + "line of chapter2-godot/" + POINTER_FILE + " — it takes effect straight "
            + "away, with no restart.\n\n"
            + "It is also picked up from the GODOT environment variable (only in "
            + "processes started after you set it), the -Doverride.godot JVM flag, or "
            + "any godot*.exe on PATH. An export to chapter2-godot/build/Chapter2.exe "
            + "is used ahead of all of them.");
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
