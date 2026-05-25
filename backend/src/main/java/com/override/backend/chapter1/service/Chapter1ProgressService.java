package com.override.backend.chapter1.service;

import com.override.backend.dto.ProgressRequest;
import com.override.backend.entity.ChapterProgress;
import com.override.backend.entity.PlayerProfile;
import com.override.backend.repository.ChapterProgressRepository;
import com.override.backend.shared.service.PlayerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class Chapter1ProgressService {

    private static final int CHAPTER_NUMBER = 1;

    private final ChapterProgressRepository progressRepo;
    private final PlayerService playerService;

    public Chapter1ProgressService(ChapterProgressRepository progressRepo, PlayerService playerService) {
        this.progressRepo = progressRepo;
        this.playerService = playerService;
    }

    public List<ChapterProgress> getProgress(String username) {
        PlayerProfile profile = playerService.getProfile(username);
        return progressRepo.findByPlayerIdAndChapterNumber(profile.getId(), CHAPTER_NUMBER)
                .stream().toList();
    }

    @Transactional
    public ChapterProgress updateProgress(String username, ProgressRequest req) {
        PlayerProfile profile = playerService.getProfile(username);

        ChapterProgress cp = progressRepo
                .findByPlayerIdAndChapterNumber(profile.getId(), CHAPTER_NUMBER)
                .orElseGet(() -> new ChapterProgress(profile, CHAPTER_NUMBER));

        cp.setCompleted(req.isCompleted());
        cp.setScore(req.getScore());
        cp.setTimeSpentSeconds(req.getTimeSpentSeconds());
        cp.setAiHelpUsed(req.getAiHelpUsed());

        return progressRepo.save(cp);
    }
}
