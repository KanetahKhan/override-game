package com.override.backend.shared.service;

import com.override.backend.entity.PlayerProfile;
import com.override.backend.entity.User;
import com.override.backend.repository.PlayerProfileRepository;
import com.override.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private final UserRepository userRepo;
    private final PlayerProfileRepository profileRepo;

    public PlayerService(UserRepository userRepo, PlayerProfileRepository profileRepo) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
    }

    public PlayerProfile getProfile(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return profileRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    public PlayerProfile updateProfile(String username, PlayerProfile update) {
        PlayerProfile profile = getProfile(username);

        profile.setDisplayName(update.getDisplayName());
        profile.setLevel(update.getLevel());
        profile.setXp(update.getXp());
        profile.setLogicStat(update.getLogicStat());
        profile.setAwarenessStat(update.getAwarenessStat());
        profile.setWillpowerStat(update.getWillpowerStat());
        profile.setCombatStat(update.getCombatStat());
        profile.setEmpathyStat(update.getEmpathyStat());
        profile.setDependencyMeter(update.getDependencyMeter());
        profile.setCurrentChapter(update.getCurrentChapter());
        profile.setCoins(update.getCoins());
        profile.setIndependentXp(update.getIndependentXp());

        return profileRepo.save(profile);
    }
}
