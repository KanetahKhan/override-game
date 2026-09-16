package com.override.chapter1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Curfew Protocol records that outlive a single run (and a New Game): the top
 * five escapes per difficulty, the fastest escape, unlocked achievements and
 * which story notes have been read. Kept locally in
 * {@code ~/.override/curfew-records.properties}; the all-time best per
 * difficulty is also synced to the backend through HighScoreClient.
 */
final class CurfewRecords {

    record Achievement(String id, String title, String description) {}

    static final List<Achievement> ACHIEVEMENTS = List.of(
        new Achievement("GHOST", "Ghost", "Escape without being detected once."),
        new Achievement("LAST_REAL_MIND", "The Last Real Mind", "Escape without any help from Astra."),
        new Achievement("BEFORE_THE_BELL", "Before the Bell", "Escape in under 4 minutes."),
        new Achievement("UNTOUCHED", "Untouched", "Escape with full integrity."),
        new Achievement("BOOKWORM", "Bookworm", "Find both hollow ledgers in one run."),
        new Achievement("NIGHT_SHIFT", "Night Shift", "Escape on Hard."),
        new Achievement("ARCHIVIST", "Archivist", "Read all six notes on the floor."));

    /** One escape: its score, how long it took, the grade and how often Astra helped. */
    record Run(int score, int escapeSecs, String grade, int astra, long when) {
        String encode() {
            return score + "|" + escapeSecs + "|" + grade + "|" + astra + "|" + when;
        }

        static Run decode(String s) {
            try {
                String[] f = s.split("\\|");
                return new Run(Integer.parseInt(f[0]), Integer.parseInt(f[1]), f[2],
                    Integer.parseInt(f[3]), Long.parseLong(f[4]));
            } catch (RuntimeException e) {
                return null;   // missing or damaged entry
            }
        }
    }

    private static final int TOP = 5;

    private final Properties props = new Properties();

    static CurfewRecords load() {
        CurfewRecords r = new CurfewRecords();
        File f = file();
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                r.props.load(in);
            } catch (IOException ignored) {
                // unreadable file: start fresh
            }
        }
        return r;
    }

    /* ------------------------------------------------------ achievements */

    static Achievement achievement(String id) {
        for (Achievement a : ACHIEVEMENTS) if (a.id().equals(id)) return a;
        throw new IllegalArgumentException("Unknown achievement: " + id);
    }

    boolean has(String id) { return props.containsKey("ach." + id); }

    /** True the first time an achievement is unlocked. */
    boolean unlock(String id) {
        if (has(id)) return false;
        props.setProperty("ach." + id, String.valueOf(System.currentTimeMillis()));
        save();
        return true;
    }

    int achievementsUnlocked() {
        int n = 0;
        for (Achievement a : ACHIEVEMENTS) if (has(a.id())) n++;
        return n;
    }

    /* ------------------------------------------------------------- notes */

    /** True the first time this note is read. */
    boolean markNoteRead(String id) {
        if (props.containsKey("note." + id)) return false;
        props.setProperty("note." + id, String.valueOf(System.currentTimeMillis()));
        save();
        return true;
    }

    int notesRead() {
        int n = 0;
        for (CurfewLore.Note note : CurfewLore.NOTES) if (props.containsKey("note." + note.id())) n++;
        return n;
    }

    /* -------------------------------------------------------------- runs */

    List<Run> topRuns(CurfewDifficulty d) {
        List<Run> runs = new ArrayList<>();
        for (int i = 0; i < TOP; i++) {
            Run r = Run.decode(props.getProperty(runKey(d, i), ""));
            if (r != null) runs.add(r);
        }
        return runs;
    }

    /** Adds an escape to the difficulty's top five; returns its rank (1-based), or 0 if it didn't place. */
    int addRun(CurfewDifficulty d, Run run) {
        List<Run> runs = topRuns(d);
        runs.add(run);
        runs.sort(Comparator.comparingInt(Run::score).reversed().thenComparingInt(Run::escapeSecs));
        int rank = runs.indexOf(run) + 1;
        for (int i = 0; i < Math.min(TOP, runs.size()); i++) props.setProperty(runKey(d, i), runs.get(i).encode());
        int fastest = fastest(d);
        if (fastest < 0 || run.escapeSecs() < fastest) props.setProperty("fastest." + d.name(), String.valueOf(run.escapeSecs()));
        save();
        return rank <= TOP ? rank : 0;
    }

    /** Fastest escape on this difficulty in seconds, or -1 if there is none yet. */
    int fastest(CurfewDifficulty d) {
        try {
            return Integer.parseInt(props.getProperty("fastest." + d.name(), "-1"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /* ---------------------------------------------------------- storage */

    private static String runKey(CurfewDifficulty d, int i) { return "run." + d.name() + "." + i; }

    private void save() {
        File f = file();
        f.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            props.store(out, "Curfew Protocol records");
        } catch (IOException e) {
            System.err.println("Could not save Curfew Protocol records: " + e.getMessage());
        }
    }

    private static File file() {
        return new File(new File(System.getProperty("user.home"), ".override"), "curfew-records.properties");
    }
}
