package com.override.game.minigames;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MiniGameCancellationTest {

    @Test
    void cancellationReportsOneFailedRewardlessResultAndPreservesAssistCount() {
        StubMiniGame game = new StubMiniGame();
        List<MiniGameResult> results = new ArrayList<>();
        game.setResultListener(results::add);
        game.recordAssist();

        game.cancel();
        game.cancel();

        assertEquals(1, results.size());
        MiniGameResult result = results.getFirst();
        assertFalse(result.won());
        assertEquals(0, result.score());
        assertEquals(0, result.xpEarned());
        assertEquals(0, result.chapterPoints());
        assertEquals(1, result.dependencyUsed());
    }

    private static final class StubMiniGame extends MiniGame {
        StubMiniGame() {
            super(2, 2, 4, MiniGameTheme.nokia());
        }

        void recordAssist() {
            dependencyUsed++;
        }

        @Override
        protected void init() {
        }

        @Override
        protected void update(double dt) {
        }

        @Override
        protected void render() {
        }

        @Override
        protected void onKey(KeyCode code) {
        }
    }
}
