package com.override.shared.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/** Permanent run history for everyone on this computer; independent of campaign saves. */
public final class ScoreArchive {
    public enum Mode {
        CAMPAIGN("Campaign", true), HARVEST("Harvest Protocol", true),
        CLASSROOM_EASY("Classroom · Easy", false), CLASSROOM_NORMAL("Classroom · Normal", false),
        CLASSROOM_HARD("Classroom · Hard", false);
        public final String label;
        public final boolean percent;
        Mode(String label, boolean percent) { this.label = label; this.percent = percent; }
        public String format(double score) { return percent ? String.format(Locale.ROOT, "%.1f%%", score)
            : String.format(Locale.ROOT, "%,.0f", score); }
        @Override public String toString() { return label; }
    }

    public record Run(UUID id, UUID playerId, String playerName, Mode mode, double score,
                      String outcome, boolean assisted, long when) {
        public boolean ranked() { return !mode.name().startsWith("CLASSROOM_") || outcome.equals("CLEARED"); }
    }

    private static String lastError;
    private ScoreArchive() { }
    public static String lastError() { return lastError; }

    /** An event key is stable for one result. Reopening a result screen cannot duplicate a run. */
    public static synchronized boolean record(String eventKey, Mode mode, double score, String outcome, boolean assisted) {
        PlayerProfiles.Profile player = PlayerProfiles.active();
        if (player == null) return true; // Legacy/prototype entry points do not invent a player identity.
        if (!Double.isFinite(score)) { lastError = "An invalid score was not saved."; return false; }
        UUID id = UUID.nameUUIDFromBytes((player.id() + ":" + mode.name() + ":" + eventKey).getBytes(StandardCharsets.UTF_8));
        Path path = PlayerProfiles.root().resolve("score-history").resolve(id + ".properties");
        if (Files.isRegularFile(path)) return true;
        Properties p = new Properties();
        p.setProperty("playerId", player.id().toString());
        p.setProperty("playerName", player.name());
        p.setProperty("mode", mode.name());
        p.setProperty("score", Double.toString(mode.percent ? Math.max(0, Math.min(100, score)) : score));
        p.setProperty("outcome", outcome);
        p.setProperty("assisted", Boolean.toString(assisted));
        p.setProperty("when", Long.toString(System.currentTimeMillis()));
        try { PlayerProfiles.write(path, p); lastError = null; return true; }
        catch (IOException e) { lastError = "The latest score could not be saved. Check available storage and try again."; return false; }
    }

    public static List<Run> history() throws IOException {
        Path folder = PlayerProfiles.root().resolve("score-history");
        List<Run> runs = legacyHistory();
        if (!Files.isDirectory(folder)) return runs.stream().sorted(Comparator.comparingLong(Run::when).reversed()).toList();
        try (var files = Files.list(folder)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".properties")).toList()) {
                try {
                    Properties p = PlayerProfiles.read(path);
                    double score = Double.parseDouble(p.getProperty("score"));
                    if (!Double.isFinite(score) || p.getProperty("playerName") == null || p.getProperty("outcome") == null) continue;
                    runs.add(new Run(UUID.fromString(path.getFileName().toString().replace(".properties", "")),
                        UUID.fromString(p.getProperty("playerId")), p.getProperty("playerName"),
                        Mode.valueOf(p.getProperty("mode")), score, p.getProperty("outcome"),
                        Boolean.parseBoolean(p.getProperty("assisted")), Long.parseLong(p.getProperty("when"))));
                } catch (IllegalArgumentException | NullPointerException | IOException damaged) { lastError = "A damaged history entry could not be read; the other scores are shown."; }
            }
        }
        return runs.stream().sorted(Comparator.comparingLong(Run::when).reversed().thenComparing(r -> r.id().toString())).toList();
    }

    /** Retain pre-login scores without assigning them to a new player. */
    private static List<Run> legacyHistory() throws IOException {
        List<Run> runs = new ArrayList<>();
        Properties p = PlayerProfiles.read(PlayerProfiles.root().resolve("campaign-scoreboard.properties"));
        for (String key : p.stringPropertyNames()) {
            if (!key.startsWith("entry.")) continue;
            try {
                String[] f = p.getProperty(key).split("\\|");
                double score = Double.parseDouble(f[1]);
                if (!Double.isFinite(score)) continue;
                UUID player = UUID.nameUUIDFromBytes(("legacy-player:" + f[0]).getBytes(StandardCharsets.UTF_8));
                UUID id = UUID.nameUUIDFromBytes(("legacy-run:" + key + p.getProperty(key)).getBytes(StandardCharsets.UTF_8));
                runs.add(new Run(id, player, f[0] + " (legacy)", Mode.CAMPAIGN, score, f[2], false, Long.parseLong(f[3])));
            } catch (RuntimeException ignored) { }
        }
        return runs;
    }

    /** Best eligible run per player, with deterministic ties; never mix different score scales. */
    public static List<Run> topFive(List<Run> history, Mode mode) {
        Set<UUID> placed = new HashSet<>();
        return history.stream().filter(r -> r.mode() == mode && r.ranked())
            .sorted(Comparator.comparingDouble(Run::score).reversed().thenComparingLong(Run::when)
                .thenComparing(r -> r.playerName().toLowerCase(Locale.ROOT)).thenComparing(r -> r.id().toString()))
            .filter(r -> placed.add(r.playerId())).limit(5).toList();
    }
}
