package com.override.backend.shared.controller;

import com.override.backend.dto.SaveRequest;
import com.override.backend.entity.GameSave;
import com.override.backend.shared.service.SaveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/save")
public class SaveController {

    private final SaveService saveService;

    public SaveController(SaveService saveService) {
        this.saveService = saveService;
    }

    @PostMapping
    public ResponseEntity<GameSave> save(@AuthenticationPrincipal UserDetails user,
                                         @RequestBody SaveRequest req) {
        return ResponseEntity.ok(saveService.save(user.getUsername(), req));
    }

    @GetMapping
    public ResponseEntity<List<GameSave>> loadSaves(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(saveService.loadSaves(user.getUsername()));
    }
}
