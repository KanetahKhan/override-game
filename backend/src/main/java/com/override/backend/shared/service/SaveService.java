package com.override.backend.shared.service;

import com.override.backend.dto.SaveRequest;
import com.override.backend.entity.GameSave;
import com.override.backend.entity.PlayerProfile;
import com.override.backend.repository.GameSaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaveService {

    private final GameSaveRepository saveRepo;
    private final PlayerService playerService;

    public SaveService(GameSaveRepository saveRepo, PlayerService playerService) {
        this.saveRepo = saveRepo;
        this.playerService = playerService;
    }

    @Transactional
    public GameSave save(String username, SaveRequest req) {
        PlayerProfile profile = playerService.getProfile(username);

        GameSave gs = new GameSave();
        gs.setPlayer(profile);
        gs.setChapterNumber(req.getChapterNumber());
        gs.setCheckpoint(req.getCheckpoint());
        gs.setPlayerHp(req.getPlayerHp());
        gs.setDependencyMeter(req.getDependencyMeter());
        gs.setCoins(req.getCoins());
        gs.setChoicesJson(req.getChoicesJson());
        gs.setUnlockedSkillsJson(req.getUnlockedSkillsJson());
        gs.setTimestamp(LocalDateTime.now());

        return saveRepo.save(gs);
    }

    public List<GameSave> loadSaves(String username) {
        PlayerProfile profile = playerService.getProfile(username);
        return saveRepo.findByPlayerIdOrderByTimestampDesc(profile.getId());
    }
}
