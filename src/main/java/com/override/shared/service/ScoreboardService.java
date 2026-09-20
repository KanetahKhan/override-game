package com.override.shared.service;

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
 * Campaign scoreboard — one entry per finished two-chapter run.
 *
 * <p>Local-first: entries live in
 * {@code ~/.override/campaign-scoreboard.properties} so the scoreboard renders
 * instantly and works fully offline. When the login system arrives, swap this
 * class's storage for a backend call keyed by the authenticated player (the
 * existing {@code HighScoreClient} shows that exact local-cache + best-effort
 * sync shape); the entry model stays the same.
 *
 * <p>Dedup safety: the player can reach the result screen more than once for
 * the same run (map -> result -> scoreboard -> map -> result...). Each
 * {@link #record(CampaignRun, String)} call carries the raw result JSON as a
 * signature; re-showing the identical result is ignored, so a run is counted
 * exactly once.
 */
public final class ScoreboardService {

    /** One completed campaign run on the scoreboard. */
    public record CampaignRun(String player, double scorePct, String verdict, long when) {
        String encode() {
            return player + "|" + scorePct + "|" + verdict + "|" + when;
        }

        static CampaignRun decode(String s) {
            try {
                String[] f = s.split("\\|");
                return new CampaignRun(f[0], Double.parseDouble(f[1]), f[2], Long.parseLong(f[3]));
            } catch (RuntimeException e) {
                return null;   // missing or damaged entry
            }
        }
    }

    private static final int TOP = 10;

    private final Properties props = new Properties();

    public static ScoreboardService load() {
        ScoreboardService s = new ScoreboardService();
        File f = file();
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                s.props.load(in);
            } catch (IOException ignored) {
                // unreadable file: start fresh
            }
        }
        return s;
    }

    /** The top runs, best campaign score first (newest breaks ties). */
    public List<CampaignRun> top() {
        List<CampaignRun> runs = new ArrayList<>();
        for (int i = 0; i < TOP; i++) {
            CampaignRun r = CampaignRun.decode(props.getProperty("entry." + i, ""));
            if (r != null) runs.add(r);
        }
        return runs;
    }

    /**
     * Add a finished campaign run unless the same result is already recorded
     * (same {@code signature} as the last recorded run). Returns true when the
     * board changed.
     */
    public boolean record(CampaignRun run, String signature) {
        String last = props.getProperty("lastSignature");
        if (signature != null && signature.equals(last)) return false;

        List<CampaignRun> runs = top();
        runs.add(run);
        runs.sort(Comparator
            .comparingDouble(CampaignRun::scorePct).reversed()
            .thenComparingLong(r -> -r.when()));
        for (int i = 0; i < Math.min(TOP, runs.size()); i++) {
            props.setProperty("entry." + i, runs.get(i).encode());
        }
        props.setProperty("lastSignature", signature == null ? "" : signature);
        save();
        return true;
    }

    /* ---------------------------------------------------------- storage */

    private void save() {
        File f = file();
        f.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            props.store(out, "Override campaign scoreboard");
        } catch (IOException e) {
            System.err.println("Could not save campaign scoreboard: " + e.getMessage());
        }
    }

    private static File file() {
        return new File(new File(System.getProperty("user.home"), ".override"), "campaign-scoreboard.properties");
    }
}