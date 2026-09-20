package com.override.game.minigames;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * The continuous room tone for a floor that is still powered and still watching.
 *
 * <p>A low hum with a slow swell under it, generated a block at a time on one
 * daemon thread. {@link ChiptuneSfx} cannot do this: its cues are one-shot
 * buffers played on a bounded pool, and holding a pool thread open for the whole
 * chapter would starve the alert cues that share it. So this keeps its own line.</p>
 *
 * <p>{@link #setIntensity(double)} leans the tone in as the floor closes on the
 * player — louder, and with a dissonant partial that is absent while patrolling.
 * Volume follows {@link ChiptuneSfx#getMasterVolume()} live, so the existing
 * settings slider drives this too.</p>
 *
 * <p>Best-effort like the rest of the audio: no mixer means silence, not a crash.</p>
 */
public final class ChiptuneAmbience {

    /** A low hum needs no bandwidth; half rate costs half the samples to generate. */
    private static final float SAMPLE_RATE = 22_050f;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false); // 16-bit mono LE signed

    /** Samples per write. ~46ms: small enough to follow the slider, big enough not to stutter. */
    private static final int BLOCK = 1024;

    /** Ceiling for the tone, well under the alert cues. */
    private static final double GAIN = 0.34;

    /** Seconds to cross the full level range, so start/stop and swells never click. */
    private static final double FADE_SECONDS = 1.5;

    private static volatile boolean running;
    private static volatile double intensity;
    private static Thread thread;

    private ChiptuneAmbience() {}

    /** Starts the tone if it is not already running. Safe to call more than once. */
    public static synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(ChiptuneAmbience::pump, "ambience");
        thread.setDaemon(true);
        thread.start();
    }

    /** Fades the tone out and releases the line. No-op if already stopped. */
    public static synchronized void stop() {
        running = false;
        thread = null; // the pump fades out, closes its own line and exits
    }

    /** How hard the floor is leaning on the player, {@code 0..1}. */
    public static void setIntensity(double value) {
        intensity = Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    private static void pump() {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
            line.open(FORMAT, BLOCK * 8);
            line.start();

            byte[] block = new byte[BLOCK * 2];
            double humA = 0, humB = 0, alertPhase = 0, swell = 0, level = 0, rumble = 0;
            double step = (BLOCK / SAMPLE_RATE) / FADE_SECONDS;

            // Keep going past stop() until the fade reaches zero, so the tone
            // never cuts off mid-sample.
            while (running || level > 0.0005) {
                double target = running
                        ? ChiptuneSfx.getMasterVolume() * GAIN * (0.55 + 0.45 * intensity)
                        : 0.0;
                double delta = target - level;
                level += Math.abs(delta) <= step ? delta : Math.signum(delta) * step;

                double alertMix = intensity;
                for (int i = 0; i < BLOCK; i++) {
                    humA += 2 * Math.PI * 54.0 / SAMPLE_RATE;
                    humB += 2 * Math.PI * 81.5 / SAMPLE_RATE;   // a fifth up, for body
                    alertPhase += 2 * Math.PI * 87.0 / SAMPLE_RATE; // beats against humB
                    swell += 2 * Math.PI * 0.07 / SAMPLE_RATE;  // distant machinery

                    // One-pole low pass on white noise: air-handling rumble, not hiss.
                    rumble += ((Math.random() * 2 - 1) - rumble) * 0.0045;

                    double s = Math.sin(humA) * 0.55
                             + Math.sin(humB) * 0.22
                             + Math.sin(alertPhase) * 0.18 * alertMix
                             + rumble * 3.2;
                    s *= 0.82 + 0.18 * Math.sin(swell);

                    short v = (short) (Math.max(-1, Math.min(1, s)) * level * Short.MAX_VALUE);
                    block[i * 2]     = (byte) (v & 0xff);
                    block[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
                }
                line.write(block, 0, block.length);
            }
            line.drain();
            line.stop();
        } catch (Throwable t) {
            // No mixer / line unavailable / security — give up on the tone quietly.
        } finally {
            running = false;
        }
    }
}
