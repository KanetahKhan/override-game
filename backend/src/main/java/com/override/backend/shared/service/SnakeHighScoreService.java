package com.override.backend.shared.service;

import com.override.backend.entity.SnakeHighScore;
import com.override.backend.repository.SnakeHighScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Loads and updates the global best score for the "Syntax Snake" mini-game.
 *
 * <p>{@link #submit(int)} only ever raises the stored best, so a weaker run is
 * a harmless no-op. The companion desktop {@code SnakeHighScoreClient} caches
 * the best locally so the game UI never blocks on the network.
 */
@Service
public class SnakeHighScoreService {

    private final SnakeHighScoreRepository repo;

    public SnakeHighScoreService(SnakeHighScoreRepository repo) {
        this.repo = repo;
    }

    /** The current global best, or a zeroed record if none exists yet. */
    public SnakeHighScore getBest() {
        return repo.findTopBest().orElseGet(SnakeHighScore::new);
    }

    /**
     * Submit a finished run. If {@code score} beats the stored best (or no row
     * exists yet) the record is updated/created and its updated copy returned;
     * otherwise the stored best is returned unchanged.
     */
    @Transactional
    public SnakeHighScore submit(int score) {
        SnakeHighScore best = repo.findTopBest().orElse(null);
        if (best == null) {
            best = new SnakeHighScore(Math.max(0, score), LocalDateTime.now());
            return repo.save(best);
        }
        if (score > best.getBestScore()) {
            best.setBestScore(score);
            best.setBestRunDate(LocalDateTime.now());
            return repo.save(best);
        }
        return best;
    }
}
