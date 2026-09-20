package com.override.shared.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/** Name-only, local player profiles. Names are never used as filesystem paths. */
public final class PlayerProfiles {
    public record Profile(UUID id, String name, long lastPlayed) { }
    private static Profile active;
    private PlayerProfiles() { }

    public static Path root() {
        return Path.of(System.getProperty("override.dataDir",
            Path.of(System.getProperty("user.home"), ".override").toString()));
    }

    public static Profile active() { return active; }
    public static String name() { return active == null ? "PLAYER" : active.name(); }
    public static Path directory(UUID id) { return root().resolve("players").resolve(id.toString()); }
    public static Path dataFile(String filename) {
        return (active == null ? root() : directory(active.id())).resolve(filename);
    }

    public static String validateName(String value) {
        String name = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
            .strip().replaceAll("\\s+", " ");
        int length = name.codePointCount(0, name.length());
        if (length < 2 || length > 24)
            throw new IllegalArgumentException("Use a name between 2 and 24 characters.");
        if (!name.matches("[\\p{L}\\p{M}\\p{N} ._'’-]+")
                || name.codePoints().noneMatch(Character::isLetterOrDigit))
            throw new IllegalArgumentException("Use letters, numbers, spaces, dots, hyphens or apostrophes.");
        return name;
    }

    public static synchronized Profile login(String value) throws IOException {
        String name = validateName(value);
        List<Profile> profiles = list();
        Profile match = profiles.stream().filter(p -> key(p.name()).equals(key(name))).findFirst().orElse(null);
        Profile profile = new Profile(match == null ? UUID.randomUUID() : match.id(),
            match == null ? name : match.name(), System.currentTimeMillis());
        Properties props = new Properties();
        props.setProperty("name", profile.name());
        props.setProperty("lastPlayed", Long.toString(profile.lastPlayed()));
        write(directory(profile.id()).resolve("profile.properties"), props);
        active = profile;
        return profile;
    }

    public static void logout() { active = null; }

    public static List<Profile> list() throws IOException {
        importPreviousSave();
        Path folder = root().resolve("players");
        if (!Files.isDirectory(folder)) return List.of();
        List<Profile> profiles = new ArrayList<>();
        try (var dirs = Files.list(folder)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                try {
                    UUID id = UUID.fromString(dir.getFileName().toString());
                    Properties p = read(dir.resolve("profile.properties"));
                    profiles.add(new Profile(id, validateName(p.getProperty("name")),
                        Long.parseLong(p.getProperty("lastPlayed", "0"))));
                } catch (IllegalArgumentException | IOException damaged) { System.err.println("Could not read a player profile; its files have been preserved."); }
            }
        }
        return profiles.stream().sorted(Comparator.comparingLong(Profile::lastPlayed).reversed()
            .thenComparing(Profile::name)).toList();
    }

    /** Non-destructive, one-time recovery; unnamed old scores are not invented as named runs. */
    private static void importPreviousSave() throws IOException {
        Path source = root().resolve("save.properties");
        Path imported = root().resolve("profiles-imported.properties");
        if (!Files.isRegularFile(source) || Files.exists(imported)) return;
        UUID id = UUID.nameUUIDFromBytes("override-previous-player".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Path destination = directory(id);
        Files.createDirectories(destination);
        try (var files = Files.list(root())) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".properties") && !name.contains("settings") && !name.contains("preferences")
                        && !name.startsWith("chapter2-") && !Files.exists(destination.resolve(name)))
                    Files.copy(file, destination.resolve(name));
            }
        }
        Properties p = new Properties();
        p.setProperty("name", "Previous Player");
        p.setProperty("lastPlayed", "0");
        if (!Files.exists(destination.resolve("profile.properties"))) write(destination.resolve("profile.properties"), p);
        write(imported, p);
    }

    private static String key(String value) { return value.toLowerCase(Locale.ROOT); }

    public static Properties read(Path path) throws IOException {
        Properties p = new Properties();
        if (Files.isRegularFile(path)) try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid player data: " + path.getFileName(), e); }
        return p;
    }

    /** Replace a complete record atomically, so interrupted writes preserve the previous save. */
    public static void write(Path path, Properties properties) throws IOException {
        Files.createDirectories(path.getParent());
        Path pending = Files.createTempFile(path.getParent(), "pending-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(pending)) { properties.store(out, "Override player data"); }
            try { Files.move(pending, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(pending, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(pending); }
    }
}
