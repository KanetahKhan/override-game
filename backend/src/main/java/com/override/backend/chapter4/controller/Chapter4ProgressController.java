package com.override.backend.chapter4.controller;

import com.override.backend.chapter4.service.Chapter4ProgressService;
import com.override.backend.dto.ProgressRequest;
import com.override.backend.entity.ChapterProgress;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapter4/progress")
public class Chapter4ProgressController {

    private final Chapter4ProgressService progressService;

    public Chapter4ProgressController(Chapter4ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public ResponseEntity<List<ChapterProgress>> getProgress(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(progressService.getProgress(user.getUsername()));
    }

    @PostMapping("/update")
    public ResponseEntity<ChapterProgress> updateProgress(@AuthenticationPrincipal UserDetails user,
                                                          @RequestBody ProgressRequest req) {
        return ResponseEntity.ok(progressService.updateProgress(user.getUsername(), req));
    }
}
