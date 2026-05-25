package com.override.backend.shared.service;

import com.override.backend.dto.LeaderboardResponse;
import com.override.backend.entity.LeaderboardEntry;
import com.override.backend.entity.PlayerProfile;
import com.override.backend.repository.LeaderboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepo;
    private final PlayerService playerService;

    public LeaderboardService(LeaderboardRepository leaderboardRepo, PlayerService playerService) {
        this.leaderboardRepo = leaderboardRepo;
        this.playerService = playerService;
    }

    public List<LeaderboardResponse> getTop20() {
        return leaderboardRepo.findTop20ByOrderByScoreDesc().stream()
                .map(e -> new LeaderboardResponse(
                        e.getPlayer().getDisplayName(),
                        e.getScore(),
                        e.getEndingType(),
                        e.getCompletionTimeSeconds()
                ))
                .toList();
    }

    @Transactional
    public LeaderboardEntry submit(String username, int score, String endingType, long completionTimeSeconds) {
        PlayerProfile profile = playerService.getProfile(username);

        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setPlayer(profile);
        entry.setScore(score);
        entry.setEndingType(endingType);
        entry.setCompletionTimeSeconds(completionTimeSeconds);

        return leaderboardRepo.save(entry);
    }
}
