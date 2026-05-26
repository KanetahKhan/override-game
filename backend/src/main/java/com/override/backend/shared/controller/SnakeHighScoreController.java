package com.override.backend.shared.controller;

import com.override.backend.entity.SnakeHighScore;
import com.override.backend.shared.service.SnakeHighScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoints for the "Syntax Snake" mini-game's best score. Unauthenticated
 * so the desktop client can read/write the global best without a login (mirrors
 * the existing {@code /api/highscore} contract used by Kernel Panic).
 */
@RestController
@RequestMapping("/api/snake-highscore")
public class SnakeHighScoreController {

    private final SnakeHighScoreService service;

    public SnakeHighScoreController(SnakeHighScoreService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<SnakeHighScore> getBest() {
        return ResponseEntity.ok(service.getBest());
    }

    @PostMapping
    public ResponseEntity<SnakeHighScore> submit(@RequestBody Map<String, Object> body) {
        int score = 0;
        Object raw = body.get("score");
        if (raw instanceof Number n) score = n.intValue();
        else if (raw instanceof String s) {
            try { score = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return ResponseEntity.ok(service.submit(score));
    }
}
