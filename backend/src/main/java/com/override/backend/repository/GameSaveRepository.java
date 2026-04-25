package com.override.backend.repository;

import com.override.backend.entity.GameSave;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameSaveRepository extends JpaRepository<GameSave, Long> {
    List<GameSave> findByPlayerIdOrderByTimestampDesc(Long playerId);
}
