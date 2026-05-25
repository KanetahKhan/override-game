package com.override.game.minigames;

/**
 * Callback invoked once when a {@link MiniGame} finishes, carrying the
 * {@link MiniGameResult}. The launching chapter uses it to award XP and update
 * the global Dependency Meter.
 */
@FunctionalInterface
public interface ResultListener {
    void onResult(MiniGameResult result);
}
