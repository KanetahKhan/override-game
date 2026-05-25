package com.override.backend.shared.controller;

import com.override.backend.dto.LeaderboardResponse;
import com.override.backend.shared.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardResponse>> getLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getTop20());
    }
}
