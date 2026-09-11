package com.override.chapter1;

import com.override.game.minigames.MiniGameResult;
import com.override.shared.model.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentClassroomRewardBankTest {

    @BeforeEach
    void resetCampaign() {
        GameState.reset();
    }

    @Test
    void partialRunsChangeNothingAndACompletedBankCommitsOnlyOnce() {
        GameState state = GameState.get();
        int startingCoins = state.getCoins();
        int startingXp = state.getPlayer().getXp();
        SilentClassroomRewardBank bank = new SilentClassroomRewardBank(true);
        bank.bank(new MiniGameResult(true, 300, 10, 0, 850), true);
        bank.bank(new MiniGameResult(true, 300, 99, 0, 900), false);

        // This is the state left by quitting before Room 301.
        assertEquals(startingXp, state.getPlayer().getXp());
        assertEquals(startingCoins, state.getCoins());
        assertEquals(0, state.getIndependentXp());

        assertTrue(bank.commit(state));
        assertEquals(startingXp + 10, state.getPlayer().getXp());
        assertEquals(startingCoins + 8, state.getCoins());
        assertEquals(15, state.getIndependentXp());
        assertFalse(bank.commit(state));
        assertEquals(startingCoins + 8, state.getCoins());
    }

    @Test
    void aReplayBankCannotAwardCampaignResources() {
        GameState state = GameState.get();
        int startingCoins = state.getCoins();
        SilentClassroomRewardBank replayBank = new SilentClassroomRewardBank(false);
        replayBank.bank(new MiniGameResult(true, 999, 100, 0, 1_000), true);

        assertFalse(replayBank.commit(state));
        assertEquals(startingCoins, state.getCoins());
        assertEquals(0, state.getPlayer().getXp());
        assertEquals(0, state.getIndependentXp());
    }
}
