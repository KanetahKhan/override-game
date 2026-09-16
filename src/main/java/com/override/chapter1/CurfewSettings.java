package com.override.chapter1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Player settings for Curfew Protocol — look sensitivity, invert Y, field of
 * view, volume, the red chase pulse and the chosen difficulty — kept in
 * {@code ~/.override/curfew-settings.properties} so they survive restarts.
 */
final class CurfewSettings {

    double sensitivity = 1.0;
    boolean invertY;
    double fov = 74;
    double volume = 1.0;
    boolean chaseFlash = true;
    CurfewDifficulty difficulty = CurfewDifficulty.NORMAL;

    static CurfewSettings load() {
        CurfewSettings s = new CurfewSettings();
        Properties p = new Properties();
        File f = file();
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (IOException ignored) {
                // unreadable file: keep the defaults
            }
        }
        s.sensitivity = num(p, "sensitivity", s.sensitivity, 0.3, 3.0);
        s.invertY = Boolean.parseBoolean(p.getProperty("invertY", "false"));
        s.fov = num(p, "fov", s.fov, 55, 100);
        s.volume = num(p, "volume", s.volume, 0, 1);
        s.chaseFlash = Boolean.parseBoolean(p.getProperty("chaseFlash", "true"));
        try {
            s.difficulty = CurfewDifficulty.valueOf(p.getProperty("difficulty", "NORMAL"));
        } catch (IllegalArgumentException ignored) {
            // unknown value: stay on Normal
        }
        return s;
    }

    void save() {
        Properties p = new Properties();
        p.setProperty("sensitivity", String.valueOf(sensitivity));
        p.setProperty("invertY", String.valueOf(invertY));
        p.setProperty("fov", String.valueOf(fov));
        p.setProperty("volume", String.valueOf(volume));
        p.setProperty("chaseFlash", String.valueOf(chaseFlash));
        p.setProperty("difficulty", difficulty.name());
        File f = file();
        f.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            p.store(out, "Curfew Protocol settings");
        } catch (IOException e) {
            System.err.println("Could not save Curfew Protocol settings: " + e.getMessage());
        }
    }

    private static double num(Properties p, String key, double fallback, double min, double max) {
        try {
            double v = Double.parseDouble(p.getProperty(key, String.valueOf(fallback)));
            return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static File file() {
        return new File(new File(System.getProperty("user.home"), ".override"), "curfew-settings.properties");
    }
}
