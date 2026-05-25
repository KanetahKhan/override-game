package com.override.backend.shared.controller;

import com.override.backend.entity.PlayerProfile;
import com.override.backend.shared.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerProfile> getProfile(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(playerService.getProfile(user.getUsername()));
    }

    @PutMapping("/update")
    public ResponseEntity<PlayerProfile> updateProfile(@AuthenticationPrincipal UserDetails user,
                                                       @RequestBody PlayerProfile update) {
        return ResponseEntity.ok(playerService.updateProfile(user.getUsername(), update));
    }
}
