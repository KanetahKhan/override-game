package com.override.backend.repository;

import com.override.backend.entity.HighScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HighScoreRepository extends JpaRepository<HighScore, Long> {
    Optional<HighScore> findByGameType(String gameType);
}
