package com.override.game.minigames;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;

/**
 * The game's single looping music bed.
 *
 * <p>Plays one track continuously from the moment the app starts until the
 * window goes away. Volume is polled from {@link ChiptuneSfx#getMasterVolume()}
 * on a small timeline so the existing sound slider drives the music too, and
 * {@link #setDucked(boolean)} fades the track down to a faint background tone
 * while a chapter is actually being played, then back up on the way out.</p>
 *
 * <p>Best-effort like the rest of the audio: a missing track or an audio device
 * that refuses to open simply means silence, never a crash.</p>
 */
public final class ChiptuneMusic {

    private static final String TRACK = "/assets/music/black-veil.mp3";

    /** Level while a chapter is being played: a faint background tone. */
    private static final double DUCK_GAIN = 0.16;
    /** Seconds to cross the full 0..1 gain range when ducking or restoring. */
    private static final double FADE_SECONDS = 1.2;
    private static final double TICK_MS = 100;

    private static MediaPlayer player;
    private static Timeline monitor;
    private static double gain = 1.0;
    private static boolean ducked;

    private ChiptuneMusic() {}

    /** Starts the bed if it is not already running. Safe to call more than once. */
    public static synchronized void start() {
        if (player != null) return;
        URL url = ChiptuneMusic.class.getResource(TRACK);
        if (url == null) return;
        try {
            player = new MediaPlayer(new Media(url.toExternalForm()));
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(0);
            player.setOnError(() -> stop());
            player.setMute(false);
            player.play();
        } catch (RuntimeException e) {
            player = null;
            return;
        }
        monitor = new Timeline(new KeyFrame(Duration.millis(TICK_MS), e -> pulse()));
        monitor.setCycleCount(Timeline.INDEFINITE);
        monitor.play();
    }

    /** Stops the bed and releases the player; no-op if already stopped. */
    public static synchronized void stop() {
        if (monitor != null) {
            monitor.stop();
            monitor = null;
        }
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    /**
     * Fades between the full track (menus, films, briefings) and the faint
     * background tone used while a game is being played.
     */
    public static void setDucked(boolean value) {
        runOnFx(() -> ducked = value);
    }

    public static boolean isDucked() {
        return ducked;
    }

    /** One step of the fade, plus a live read of the master slider. */
    private static void pulse() {
        if (player == null) return;
        double target = ducked ? DUCK_GAIN : 1.0;
        // Linear ramp: a predictable fade rather than an exponential tail.
        double step = (TICK_MS / 1000.0) / FADE_SECONDS;
        double delta = target - gain;
        if (Math.abs(delta) <= step) gain = target;
        else gain += Math.signum(delta) * step;
        double level = ChiptuneSfx.getMasterVolume() * gain;
        player.setVolume(Math.max(0.0, Math.min(1.0, level)));
    }

    private static void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }
}
