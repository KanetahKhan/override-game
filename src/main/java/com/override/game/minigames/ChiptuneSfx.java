package com.override.game.minigames;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Tiny procedural chiptune sound effects, synthesized at runtime with
 * {@code javax.sound.sampled} — no audio files and no extra dependencies.
 *
 * <p>Everything is best-effort: if there is no audio device (headless box, CI),
 * the first failure disables sound for the session and the game plays on silently.
 * Playback happens on a small bounded thread pool so rapid hits can overlap
 * without ever blocking the JavaFX thread; excess simultaneous sounds are dropped
 * rather than queued.
 */
public final class ChiptuneSfx {

    private static final float SAMPLE_RATE = 44_100f;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false); // 16-bit mono LE signed

    private static volatile boolean enabled = true;
    private static volatile double masterVolume = 1.0;

    /** Bounded pool: up to a few concurrent SFX, drop the rest (no queue lag). */
    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(
            2, 6, 5, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8),
            r -> { Thread t = new Thread(r, "sfx"); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.DiscardPolicy());

    private ChiptuneSfx() {}

    private enum Wave { SQUARE, SINE, TRIANGLE, NOISE }

    // ----- Public cues --------------------------------------------------------

    /**
     * Sets the volume used by subsequently queued cues. Values outside the
     * {@code 0..1} range are clamped; non-finite values are treated as full
     * volume. Muting does not disable the audio engine.
     */
    public static void setMasterVolume(double volume) {
        masterVolume = Double.isFinite(volume)
                ? Math.max(0.0, Math.min(1.0, volume))
                : 1.0;
    }

    /** Returns the current master volume in the {@code 0..1} range. */
    public static double getMasterVolume() {
        return masterVolume;
    }

    /** Rising blip; pitch climbs with the combo for that "chain" feel. */
    public static void hit(int combo) {
        int c = Math.min(combo, 20);
        double f = 620 + c * 38;
        play(tone(Wave.SQUARE, f, 0.07, 0.16));
    }

    /** Harsh low buzz for a wrong fix. */
    public static void wrongFix() {
        play(tone(Wave.SQUARE, 150, 0.13, 0.22));
    }

    /** Descending thud when a token breaches the kernel line. */
    public static void breach() {
        play(sweep(Wave.TRIANGLE, 320, 110, 0.18, 0.28));
    }

    /** Deep bass sweep — the EMP / KK Assist relief cue. */
    public static void emp() {
        byte[] bass = sweep(Wave.SINE, 130, 38, 0.36, 0.42);
        byte[] shimmer = tone(Wave.SQUARE, 880, 0.06, 0.10);
        play(mix(bass, shimmer));
    }

    /** Two-note rising fanfare on a new wave. */
    public static void wave() {
        play(concat(tone(Wave.SQUARE, 523, 0.08, 0.16),
                    tone(Wave.SQUARE, 784, 0.12, 0.18)));
    }

    /** Boss appears — ominous low pulse. */
    public static void boss() {
        play(concat(tone(Wave.SQUARE, 196, 0.12, 0.2),
                    tone(Wave.SQUARE, 175, 0.16, 0.2)));
    }

    /** Three-note descending game-over sting. */
    public static void gameOver() {
        play(concat(
                tone(Wave.SQUARE, 440, 0.14, 0.18),
                tone(Wave.SQUARE, 349, 0.14, 0.18),
                tone(Wave.SQUARE, 262, 0.26, 0.2)));
    }

    /** Short latch-and-hinge click for opening or closing a classroom door. */
    public static void door() {
        play(concat(
                tone(Wave.NOISE, 1, 0.025, 0.13),
                sweep(Wave.TRIANGLE, 190, 105, 0.095, 0.18)));
    }

    /** Compact metallic step for the hallway sentinel's servo movement. */
    public static void servoStep() {
        servoStep(1.0);
    }

    /** {@link #servoStep()} at a fraction of full volume, so distant steps fade out. */
    public static void servoStep(double volume) {
        play(mix(
                sweep(Wave.SQUARE, 270, 115, 0.075, 0.12),
                tone(Wave.NOISE, 1, 0.025, 0.07)), volume);
    }

    /** Urgent two-pulse cue used while the sentinel is actively chasing. */
    public static void chaseBeat() {
        play(concat(
                tone(Wave.SQUARE, 165, 0.075, 0.18),
                tone(Wave.SQUARE, 220, 0.105, 0.19)));
    }

    /*
     * The cues below sit deliberately under the alert cues above. The sentinel's
     * servoStep() is the one sound the stealth actually depends on hearing, so
     * footfalls, heartbeat and cover noise are mixed low enough never to mask it.
     */

    /** Gain ceiling for the continuous cues, so they stay under the alerts. */
    private static final double UNDERLAY_GAIN = 0.5;

    /** The player's own footfall. Crouching is quieter and duller than a sprint. */
    public static void footstep(boolean sprinting, boolean crouching) {
        double amp = crouching ? 0.05 : sprinting ? 0.11 : 0.08;
        double f0  = crouching ? 118 : sprinting ? 188 : 152;
        play(mix(
                sweep(Wave.TRIANGLE, f0, f0 * 0.55, 0.055, amp),
                tone(Wave.NOISE, 1, crouching ? 0.012 : 0.02, amp * 0.35)),
            crouching ? UNDERLAY_GAIN * 0.7 : UNDERLAY_GAIN);
    }

    /** Rising sting for the moment the floor first takes an interest in you. */
    public static void noticed() {
        play(concat(
                tone(Wave.SQUARE, 330, 0.05, 0.13),
                sweep(Wave.SQUARE, 440, 620, 0.12, 0.15)), 0.55);
    }

    /**
     * One lub-dub. {@code intensity} in {@code 0..1} lifts the pitch and level a
     * little, so a closing pursuit reads as a harder beat and not just a faster one.
     */
    public static void heartbeat(double intensity) {
        double k = Double.isFinite(intensity) ? Math.max(0.0, Math.min(1.0, intensity)) : 0.0;
        double f = 62 + 16 * k;
        play(concat(
                sweep(Wave.SINE, f, f * 0.70, 0.085, 0.22 + 0.12 * k),
                silence(0.055),
                sweep(Wave.SINE, f * 0.92, f * 0.62, 0.100, 0.16 + 0.10 * k)),
            UNDERLAY_GAIN);
    }

    /** Cloth-and-breath whoosh on slipping into cover. */
    public static void conceal() {
        play(mix(
                tone(Wave.NOISE, 1, 0.16, 0.05),
                sweep(Wave.SINE, 300, 140, 0.18, 0.07)), UNDERLAY_GAIN);
    }

    /** The reverse of {@link #conceal()}: stepping back out into the open. */
    public static void reveal() {
        play(mix(
                tone(Wave.NOISE, 1, 0.12, 0.045),
                sweep(Wave.SINE, 150, 320, 0.14, 0.07)), UNDERLAY_GAIN);
    }

    // ----- Synthesis ----------------------------------------------------------

    private static byte[] tone(Wave wave, double freq, double seconds, double amp) {
        return sweep(wave, freq, freq, seconds, amp);
    }

    /** A single note that glides from {@code f0} to {@code f1} with an envelope. */
    private static byte[] sweep(Wave wave, double f0, double f1, double seconds, double amp) {
        int n = (int) (seconds * SAMPLE_RATE);
        byte[] out = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double freq = f0 + (f1 - f0) * t;
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            double s = switch (wave) {
                case SQUARE   -> Math.sin(phase) >= 0 ? 1 : -1;
                case SINE     -> Math.sin(phase);
                case TRIANGLE -> 2 / Math.PI * Math.asin(Math.sin(phase));
                case NOISE    -> Math.random() * 2 - 1;
            };
            // Short attack, smooth decay to avoid clicks.
            double env = Math.min(1.0, i / (SAMPLE_RATE * 0.005)) * (1.0 - t);
            short v = (short) (s * env * amp * Short.MAX_VALUE);
            out[i * 2]     = (byte) (v & 0xff);
            out[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }

    /** A gap, so a two-part cue like {@link #heartbeat(double)} can have a pause in it. */
    private static byte[] silence(double seconds) {
        return new byte[(int) (seconds * SAMPLE_RATE) * 2];
    }

    private static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) len += p.length;
        byte[] out = new byte[len];
        int o = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, out, o, p.length); o += p.length; }
        return out;
    }

    /** Sum two buffers sample-by-sample (length = max), clamped. */
    private static byte[] mix(byte[] a, byte[] b) {
        int n = Math.max(a.length, b.length);
        byte[] out = new byte[n - (n % 2)];
        for (int i = 0; i + 1 < out.length; i += 2) {
            int va = i + 1 < a.length ? (short) ((a[i] & 0xff) | (a[i + 1] << 8)) : 0;
            int vb = i + 1 < b.length ? (short) ((b[i] & 0xff) | (b[i + 1] << 8)) : 0;
            int v = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, va + vb));
            out[i]     = (byte) (v & 0xff);
            out[i + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }

    private static void play(byte[] pcm) {
        play(pcm, 1.0);
    }

    private static void play(byte[] pcm, double gain) {
        if (!enabled) return;
        double volume = masterVolume * Math.max(0.0, Math.min(1.0, gain));
        if (volume <= 0.0) return;

        POOL.execute(() -> {
            byte[] playbackPcm = scaleVolume(pcm, volume);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
                line.open(FORMAT, Math.max(playbackPcm.length, 4096));
                line.start();
                line.write(playbackPcm, 0, playbackPcm.length);
                line.drain();
                line.stop();
            } catch (Throwable t) {
                // No mixer / line unavailable / security — give up on audio quietly.
                enabled = false;
            }
        });
    }

    /** Returns a scaled copy so concurrent cues never mutate shared PCM data. */
    private static byte[] scaleVolume(byte[] pcm, double volume) {
        if (volume >= 1.0) return pcm;

        byte[] out = new byte[pcm.length];
        int evenLength = pcm.length - (pcm.length % 2);
        for (int i = 0; i < evenLength; i += 2) {
            short sample = (short) ((pcm[i] & 0xff) | (pcm[i + 1] << 8));
            short scaled = (short) Math.round(sample * volume);
            out[i] = (byte) (scaled & 0xff);
            out[i + 1] = (byte) ((scaled >> 8) & 0xff);
        }
        if (evenLength < pcm.length) out[evenLength] = pcm[evenLength];
        return out;
    }
}
