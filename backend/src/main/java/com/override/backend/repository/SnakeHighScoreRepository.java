package com.override.backend.repository;

import com.override.backend.entity.SnakeHighScore;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SnakeHighScore}. The table holds a single row in
 * practice; {@link #findTopBest()} returns the highest-scoring record so the
 * service can update or extend it.
 */
public interface SnakeHighScoreRepository extends JpaRepository<SnakeHighScore, Long> {

    @Query("SELECT s FROM SnakeHighScore s ORDER BY s.bestScore DESC")
    List<SnakeHighScore> findTopOrderedByScore(PageRequest page);

    default Optional<SnakeHighScore> findTopBest() {
        List<SnakeHighScore> rows = findTopOrderedByScore(PageRequest.of(0, 1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
