package com.override.backend.service;

import com.override.backend.dto.ProgressRequest;
import com.override.backend.entity.ChapterProgress;
import com.override.backend.entity.PlayerProfile;
import com.override.backend.repository.ChapterProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProgressService {

    private final ChapterProgressRepository progressRepo;
    private final PlayerService playerService;

    public ProgressService(ChapterProgressRepository progressRepo, PlayerService playerService) {
        this.progressRepo = progressRepo;
        this.playerService = playerService;
    }

    public List<ChapterProgress> getProgress(String username) {
        PlayerProfile profile = playerService.getProfile(username);
        return progressRepo.findByPlayerId(profile.getId());
    }

    @Transactional
    public ChapterProgress updateProgress(String username, ProgressRequest req) {
        PlayerProfile profile = playerService.getProfile(username);

        ChapterProgress cp = progressRepo
                .findByPlayerIdAndChapterNumber(profile.getId(), req.getChapterNumber())
                .orElseGet(() -> new ChapterProgress(profile, req.getChapterNumber()));

        cp.setCompleted(req.isCompleted());
        cp.setScore(req.getScore());
        cp.setTimeSpentSeconds(req.getTimeSpentSeconds());
        cp.setAiHelpUsed(req.getAiHelpUsed());

        return progressRepo.save(cp);
    }
}
