package com.override.game.minigames;

/**
 * Immutable outcome of a single mini-game run, handed back to the chapter that
 * launched it via a {@link ResultListener}.
 *
 * @param won            whether the run met the game's success condition
 * @param score          final score
 * @param xpEarned       XP to award the player (already net of any penalty)
 * @param dependencyUsed how many times the player leaned on an "KK Assist";
 *                       the chapter adds this to the global Dependency Meter
 */
public record MiniGameResult(boolean won, int score, int xpEarned, int dependencyUsed) {
}
