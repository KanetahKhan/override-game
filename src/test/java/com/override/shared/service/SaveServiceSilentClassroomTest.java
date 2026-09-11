package com.override.shared.service;

import com.override.shared.model.GameState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveServiceSilentClassroomTest {

    @TempDir
    Path temporaryHome;

    private String originalUserHome;

    @BeforeEach
    void redirectSaveDirectory() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", temporaryHome.toString());
        GameState.reset();
    }

    @AfterEach
    void restoreSaveDirectory() {
        System.setProperty("user.home", originalUserHome);
        GameState.reset();
    }

    @Test
    void anOldSaveWithoutSilentClassroomScoreLoadsWithZeroBest() throws Exception {
        Path save = temporaryHome.resolve(".override").resolve("save.properties");
        Files.createDirectories(save.getParent());
        Properties oldSave = new Properties();
        oldSave.setProperty("coins", "100");
        oldSave.setProperty("dependency", "0");
        oldSave.setProperty("chapterCompleted", "0");
        oldSave.setProperty("independentXp", "0");
        try (OutputStream output = Files.newOutputStream(save)) {
            oldSave.store(output, "pre-Silent-Classroom save");
        }

        assertTrue(SaveService.load());
        assertEquals(0, GameState.get().getSilentClassroomBestScore());
        assertEquals(0, GameState.get().getSilentClassroomInsightCharges());
    }

    @Test
    void bestScoreAndInsightTierRoundTripThroughPersistence() {
        GameState.get().recordSilentClassroomScore(2_450);
        SaveService.save();

        GameState.reset();
        assertTrue(SaveService.load());
        assertEquals(2_450, GameState.get().getSilentClassroomBestScore());
        assertEquals(2, GameState.get().getSilentClassroomInsightCharges());
    }
}
