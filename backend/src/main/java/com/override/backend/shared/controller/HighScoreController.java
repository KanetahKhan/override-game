package com.override.backend.shared.controller;

import com.override.backend.dto.HighScoreRequest;
import com.override.backend.entity.HighScore;
import com.override.backend.shared.service.HighScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public mini-game high-score endpoints. Unauthenticated so the desktop game can
 * read/write the global best without forcing a login.
 */
@RestController
@RequestMapping("/api/highscore")
public class HighScoreController {

    private final HighScoreService service;

    public HighScoreController(HighScoreService service) {
        this.service = service;
    }

    @GetMapping("/{gameType}")
    public ResponseEntity<HighScore> getBest(@PathVariable String gameType) {
        return ResponseEntity.ok(service.getBest(gameType));
    }

    @PostMapping
    public ResponseEntity<HighScore> submit(@RequestBody HighScoreRequest req) {
        HighScore best = service.submit(
                req.getGameType(), req.getScore(), req.getCombo(), req.getWave(), req.isAssisted());
        return ResponseEntity.ok(best);
    }
}
