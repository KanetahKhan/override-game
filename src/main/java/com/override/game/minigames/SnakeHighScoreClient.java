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
 * Persists the "Syntax Snake" mini-game's best score so the game-over screen
 * can show "BEST" instantly and drive the chase.
 *
 * <p>Local-first: the best is cached in
 * {@code ~/.override/syntax-snake.properties} so {@link #loadBest} returns
 * instantly and the game works fully offline. When the Spring Boot backend is
 * reachable it is synced best-effort in the background (POST on submit, GET on
 * refresh) — failures are swallowed and never block play.
 *
 * <p>Mirrors {@link HighScoreClient} but with the simpler {@code SnakeHighScore}
 * schema (score + date) exposed at {@code /api/snake-highscore}.
 */
public final class SnakeHighScoreClient {

    private static final String BACKEND = "http://localhost:8080";
    private static final String ENDPOINT = BACKEND + "/api/snake-highscore";
    private static final String LOCAL_FILE = "syntax-snake.properties";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(700))
            .build();

    /** Immutable snapshot of a best (or current) run. */
    public record Best(int score, String date) {
        public static final Best ZERO = new Best(0, "");
    }

    // ----- Reads --------------------------------------------------------------

    /** The persisted best (local cache); {@link Best#ZERO} if none. */
    public Best loadBest() {
        Properties p = new Properties();
        File f = file();
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) { p.load(in); }
            catch (Exception ignored) { }
        }
        return new Best(intProp(p, "bestScore"), p.getProperty("bestRunDate", ""));
    }

    /**
     * Pull the backend best in the background and merge anything higher into the
     * local cache for next session. Returns immediately.
     */
    public void refreshFromBackendAsync() {
        runAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT))
                        .timeout(Duration.ofMillis(900))
                        .GET().build();
                HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200 && res.body() != null && !res.body().isBlank()) {
                    mergeIntoLocal(parse(res.body()));
                }
            } catch (Exception ignored) { /* offline — keep local */ }
        });
    }

    // ----- Writes -------------------------------------------------------------

    /**
     * Record a finished run: update the local best if this beat it, then push to
     * the backend best-effort. Returns the (possibly updated) local best.
     */
    public Best submit(int score) {
        Best run = new Best(score, java.time.LocalDateTime.now().toString());
        Best merged = mergeIntoLocal(run);
        runAsync(() -> postToBackend(score));
        return merged;
    }

    // ----- Internals ----------------------------------------------------------

    private synchronized Best mergeIntoLocal(Best run) {
        Best cur = loadBest();
        Best best = run.score() > cur.score() ? run : cur;
        Properties p = new Properties();
        p.setProperty("bestScore", String.valueOf(best.score()));
        p.setProperty("bestRunDate", best.date() == null ? "" : best.date());
        File f = file();
        f.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            p.store(out, "Override mini-game best — syntax-snake");
        } catch (Exception ignored) { }
        return best;
    }

    private void postToBackend(int score) {
        try {
            String json = String.format("{\"score\":%d}", score);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofMillis(900))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HTTP.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) { /* offline — local cache stands */ }
    }

    private Best parse(String json) {
        int score = jsonInt(json, "bestScore");
        Matcher m = Pattern.compile("\"bestRunDate\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        String date = m.find() ? m.group(1) : "";
        return new Best(score, date);
    }

    private static int jsonInt(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private File file() {
        File dir = new File(System.getProperty("user.home"), ".override");
        return new File(dir, LOCAL_FILE);
    }

    private static int intProp(Properties p, String key) {
        try { return Integer.parseInt(p.getProperty(key, "0")); }
        catch (NumberFormatException e) { return 0; }
    }

    private static void runAsync(Runnable r) {
        Thread t = new Thread(r, "snake-highscore-sync");
        t.setDaemon(true);
        t.start();
    }
}
