package com.override.game.minigames;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persists each mini-game's best run so the game-over screen can show "best so
 * far" and drive the chase.
 *
 * <p>Local-first: the best run is cached in {@code ~/.override/<gameType>.properties}
 * so {@link #loadBest} returns instantly and the game works fully offline. When the
 * Spring Boot backend is reachable it is synced best-effort in the background
 * (POST on submit, GET on refresh) — failures are swallowed and never block play.
 * Uses only {@code java.net.http}, so no extra dependency.
 *
 * <p>The canonical server-side store is the backend {@code HighScore} entity /
 * repository / service exposed at {@code /api/highscore}.
 */
public final class HighScoreClient {

    private static final String BACKEND = "http://localhost:8080";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(700))
            .build();

    private final String gameType;

    public HighScoreClient(String gameType) {
        this.gameType = gameType;
    }

    /** Immutable snapshot of a best (or current) run. */
    public record Best(int score, int combo, int wave, boolean assisted) {
        public static final Best ZERO = new Best(0, 0, 0, false);
    }

    // ----- Reads --------------------------------------------------------------

    /** The persisted best for this game (local cache); {@link Best#ZERO} if none. */
    public Best loadBest() {
        Properties p = new Properties();
        File f = file();
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) { p.load(in); }
            catch (Exception ignored) { }
        }
        return new Best(
                intProp(p, "bestScore"),
                intProp(p, "bestCombo"),
                intProp(p, "bestWave"),
                Boolean.parseBoolean(p.getProperty("bestRunWasAssisted", "false")));
    }

    /**
     * Pull the backend best in the background and merge anything higher into the
     * local cache for next session. Returns immediately.
     */
    public void refreshFromBackendAsync() {
        runAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND + "/api/highscore/" + gameType))
                        .timeout(Duration.ofMillis(900))
                        .GET().build();
                HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200 && res.body() != null && !res.body().isBlank()) {
                    Best remote = parse(res.body());
                    mergeIntoLocal(remote);
                }
            } catch (Exception ignored) { /* offline — keep local */ }
        });
    }

    // ----- Writes -------------------------------------------------------------

    /**
     * Record a finished run: update the local best if this beat it, then push to
     * the backend best-effort. Returns the (possibly updated) local best.
     */
    public Best submit(Best run) {
        Best merged = mergeIntoLocal(run);
        runAsync(() -> postToBackend(run));
        return merged;
    }

    // ----- Internals ----------------------------------------------------------

    private synchronized Best mergeIntoLocal(Best run) {
        Best cur = loadBest();
        Best best = new Best(
                Math.max(cur.score(), run.score()),
                Math.max(cur.combo(), run.combo()),
                Math.max(cur.wave(), run.wave()),
                // "assisted" follows whichever run holds the top score.
                run.score() >= cur.score() ? run.assisted() : cur.assisted());
        Properties p = new Properties();
        p.setProperty("bestScore", String.valueOf(best.score()));
        p.setProperty("bestCombo", String.valueOf(best.combo()));
        p.setProperty("bestWave", String.valueOf(best.wave()));
        p.setProperty("bestRunWasAssisted", String.valueOf(best.assisted()));
        File f = file();
        f.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            p.store(out, "Override mini-game best — " + gameType);
        } catch (Exception ignored) { }
        return best;
    }

    private void postToBackend(Best run) {
        try {
            String json = String.format(
                    "{\"gameType\":\"%s\",\"score\":%d,\"combo\":%d,\"wave\":%d,\"assisted\":%b}",
                    gameType, run.score(), run.combo(), run.wave(), run.assisted());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND + "/api/highscore"))
                    .timeout(Duration.ofMillis(900))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HTTP.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) { /* offline — local cache stands */ }
    }

    /** Minimal JSON field extraction — the response is a flat, known shape. */
    private Best parse(String json) {
        return new Best(
                jsonInt(json, "bestScore"),
                jsonInt(json, "bestCombo"),
                jsonInt(json, "bestWave"),
                json.matches("(?s).*\"bestRunWasAssisted\"\\s*:\\s*true.*"));
    }

    private static int jsonInt(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private File file() {
        File dir = new File(System.getProperty("user.home"), ".override");
        return new File(dir, gameType + ".properties");
    }

    private static int intProp(Properties p, String key) {
        try { return Integer.parseInt(p.getProperty(key, "0")); }
        catch (NumberFormatException e) { return 0; }
    }

    private static void runAsync(Runnable r) {
        Thread t = new Thread(r, "highscore-sync");
        t.setDaemon(true);
        t.start();
    }
}
