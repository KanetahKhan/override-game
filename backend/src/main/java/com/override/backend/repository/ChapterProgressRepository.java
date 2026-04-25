package com.override.backend.repository;

import com.override.backend.entity.ChapterProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChapterProgressRepository extends JpaRepository<ChapterProgress, Long> {
    List<ChapterProgress> findByPlayerId(Long playerId);
    Optional<ChapterProgress> findByPlayerIdAndChapterNumber(Long playerId, int chapterNumber);
}
