package com.override.chapter1;

import com.override.Main;
import com.override.shared.model.GameState;
import com.override.shared.ui.ChapterMapScreen;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.List;

/**
 * Field briefing for Chapter 1, in the same frame as the Harvest Protocol one:
 * controls on the left, the floor on the right, BACK and READY underneath.
 *
 * <p>READY drops straight onto the floor — the briefing has already said
 * everything the in-chapter title card would have.
 */
public class ChapterOneBriefingController {

    @FXML private Label playerHeader;
    @FXML private Label creditLabel;
    @FXML private Label subTitle;
    @FXML private Label curfewLabel;
    @FXML private HBox lastRunBox;
    @FXML private Label lastRunTag;
    @FXML private Label lastRunDetail;

    /** Loads the briefing; falls back to the chapter itself if the FXML is missing. */
    public static Parent build() {
        try {
            FXMLLoader loader = new FXMLLoader(
                ChapterOneBriefingController.class.getResource("/menu/ch1_briefing.fxml"));
            return loader.load();
        } catch (IOException e) {
            System.err.println("[chapter1] briefing unavailable (" + e.getMessage() + ")");
            return new CurfewProtocolScreen().build();
        }
    }

    @FXML
    private void initialize() {
        GameState state = GameState.get();
        playerHeader.setText(state.getPlayer().getDisplayName() + " // SYS.WATCHING");
        creditLabel.setText("CREDITS // " + state.getCoins());

        CurfewDifficulty difficulty = CurfewSettings.load().difficulty;
        subTitle.setText("FIELD BRIEFING  //  CHAPTER 1  //  " + difficulty.label);
        curfewLabel.setText("CURFEW — " + (difficulty.seconds / 60) + " minutes.");
        fillLastRun(difficulty);
    }

    /** Shows the best run on this difficulty, when there is one to beat. */
    private void fillLastRun(CurfewDifficulty difficulty) {
        List<CurfewRecords.Run> runs = CurfewRecords.load().topRuns(difficulty);
        if (runs.isEmpty()) return;
        CurfewRecords.Run best = runs.get(0);
        lastRunTag.setText("BEST ON " + difficulty.label);
        lastRunDetail.setText("GRADE " + best.grade() + "   ·   " + best.score() + " PTS   ·   out in "
            + (best.escapeSecs() / 60) + ":" + String.format("%02d", best.escapeSecs() % 60)
            + (best.astra() > 0 ? "   ·   Astra ×" + best.astra() : "   ·   no Astra"));
        lastRunBox.setVisible(true);
        lastRunBox.setManaged(true);
    }

    @FXML
    private void handleStartGame() {
        Main.switchScene(new CurfewProtocolScreen(true).build());
    }

    @FXML
    private void handleBack() {
        Main.switchScene(new ChapterMapScreen().build());
    }
}
