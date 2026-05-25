package com.override.backend.shared.service;

import com.override.backend.entity.HighScore;
import com.override.backend.repository.HighScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Loads and updates the per-game best score / combo / wave.
 *
 * <p>{@link #submit} only ever raises each "best" field, so submitting a weaker
 * run is a harmless no-op. The "assisted" flag follows whichever run holds the
 * top score.
 */
@Service
public class HighScoreService {

    private final HighScoreRepository repo;

    public HighScoreService(HighScoreRepository repo) {
        this.repo = repo;
    }

    /** The stored best for a game, or a fresh zeroed record if none exists yet. */
    public HighScore getBest(String gameType) {
        return repo.findByGameType(gameType).orElseGet(() -> new HighScore(gameType));
    }

    @Transactional
    public HighScore submit(String gameType, int score, int combo, int wave, boolean assisted) {
        HighScore h = repo.findByGameType(gameType).orElseGet(() -> new HighScore(gameType));

        boolean changed = h.getId() == null;
        if (score > h.getBestScore()) {
            h.setBestScore(score);
            h.setBestRunWasAssisted(assisted);
            h.setAchievedAt(LocalDateTime.now());
            changed = true;
        }
        if (combo > h.getBestCombo()) { h.setBestCombo(combo); changed = true; }
        if (wave > h.getBestWave())   { h.setBestWave(wave);   changed = true; }

        return changed ? repo.save(h) : h;
    }
}
