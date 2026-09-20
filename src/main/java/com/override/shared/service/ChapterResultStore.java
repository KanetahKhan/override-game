package com.override.shared.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

/** Keeps the Godot result transport separate from each named player's pending result. */
public final class ChapterResultStore {
    private static final String RESULT_FILE = "chapter2-result.properties";
    private static final Pattern SCORE = Pattern.compile("\"final_score_percent\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\s*[,}]");
    private static final Pattern WIN = Pattern.compile("\"win\"\\s*:\\s*(?:true|false)\\s*[,}]");
    private ChapterResultStore() { }
    private static Path ownerFile() { return PlayerProfiles.root().resolve("chapter2-owner.properties"); }

    public static void begin(Path transport) throws IOException {
        capture(transport);
        if (Files.isRegularFile(transport)) {
            Files.createDirectories(PlayerProfiles.root());
            Files.copy(transport, PlayerProfiles.root().resolve("unassigned-chapter2-result.json"), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(transport);
        }
        Properties owner = new Properties();
        if (PlayerProfiles.active() != null) owner.setProperty("playerId", PlayerProfiles.active().id().toString());
        owner.setProperty("eventId", UUID.randomUUID().toString());
        String grade = com.override.shared.model.GameState.get().getCampaignChapterOneGrade();
        owner.setProperty("chapter1Grade", grade == null ? "" : grade);
        PlayerProfiles.write(ownerFile(), owner);
        if (PlayerProfiles.active() != null) Files.deleteIfExists(PlayerProfiles.dataFile(RESULT_FILE));
    }

    public static boolean has(Path transport) {
        if (PlayerProfiles.active() == null) return Files.isRegularFile(transport);
        try { capture(transport); return Files.isRegularFile(PlayerProfiles.dataFile(RESULT_FILE)); }
        catch (IOException e) { System.err.println("Could not read Chapter 2 result: " + e.getMessage()); return false; }
    }

    public static String read(Path transport) {
        try {
            if (PlayerProfiles.active() == null) return Files.isRegularFile(transport) ? Files.readString(transport) : null;
            capture(transport);
            return PlayerProfiles.read(PlayerProfiles.dataFile(RESULT_FILE)).getProperty("json");
        } catch (IOException e) { return null; }
    }

    public static String eventId() {
        try { return PlayerProfiles.read(PlayerProfiles.dataFile(RESULT_FILE)).getProperty("eventId", "legacy"); }
        catch (IOException e) { return "legacy"; }
    }

    /** Freeze the first chapter's contribution when Harvest starts. */
    public static String chapterOneGrade() {
        try {
            String grade = PlayerProfiles.read(PlayerProfiles.dataFile(RESULT_FILE)).getProperty("chapter1Grade");
            if (grade != null) return grade.isEmpty() ? null : grade;
        } catch (IOException ignored) { }
        return com.override.shared.model.GameState.get().getCampaignChapterOneGrade();
    }

    public static void clear(Path transport) throws IOException {
        if (PlayerProfiles.active() == null) { Files.deleteIfExists(transport); return; }
        capture(transport);
        Files.deleteIfExists(PlayerProfiles.dataFile(RESULT_FILE));
    }

    private static void capture(Path transport) throws IOException {
        if (!Files.isRegularFile(transport)) return;
        Properties owner = PlayerProfiles.read(ownerFile());
        if (!owner.containsKey("playerId") || !owner.containsKey("eventId")) return; // Never give another player's unowned old result to a new login.
        UUID player;
        try { player = UUID.fromString(owner.getProperty("playerId")); }
        catch (IllegalArgumentException e) { return; }
        String json = Files.readString(transport).strip();
        // Polling must not consume the file halfway through Godot's write.
        var score = SCORE.matcher(json);
        if (!json.startsWith("{") || !json.endsWith("}") || !score.find() || !WIN.matcher(json).find()
                || !Double.isFinite(Double.parseDouble(score.group(1)))) return;
        Properties cached = new Properties();
        cached.setProperty("json", json);
        cached.setProperty("eventId", owner.getProperty("eventId"));
        if (owner.containsKey("chapter1Grade")) cached.setProperty("chapter1Grade", owner.getProperty("chapter1Grade"));
        PlayerProfiles.write(PlayerProfiles.directory(player).resolve(RESULT_FILE), cached);
        Files.delete(transport);
    }
}
