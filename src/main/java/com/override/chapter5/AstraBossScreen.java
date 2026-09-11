package com.override.chapter5;

import com.override.shared.model.GameState;
import com.override.shared.model.Player;
import com.override.shared.ui.UIFactory;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Final boss — Core Astra Agent.
 *
 * 320 HP. Three threat phases by HP %, each with its own special.
 *   Phase 1 (>66%): pure damage, occasional manipulation
 *   Phase 2 (33-66%): persuasion attack — adds dependency on hit
 *   Phase 3 (<33%): nano-repair every 4th turn, big damage windups
 *
 * Player gains +2 Willpower and +2 Combat on win, then proceeds to ending.
 */
public class AstraBossScreen {

    private static final int BOSS_MAX_HP = 320;
    private static final int INSIGHT_DAMAGE = 25;

    private final Runnable onComplete;
    private final Runnable onFail;

    private int bossHp = BOSS_MAX_HP;
    private boolean defending = false;
    private boolean stunned = false;
    private int turn = 1;
    private final int insightChargesEarned;
    private int insightCharges;

    private ProgressBar bossBar, playerBar;
    private Label log, bossHpLabel, playerHpLabel, phaseLabel, insightLabel;
    private Button attackBtn, empBtn, defendBtn, insightBtn;

    public AstraBossScreen(Runnable onComplete, Runnable onFail) {
        this.onComplete = onComplete;
        this.onFail = onFail;
        this.insightChargesEarned = GameState.get().getSilentClassroomInsightCharges();
        this.insightCharges = insightChargesEarned;
    }

    public Parent build() {
        Player p = GameState.get().getPlayer();

        Label tag = new Label("FINAL BOSS — CORE ASTRA AGENT");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("The Last Override");

        Label bossName = new Label("CORE ASTRA AGENT");
        bossName.getStyleClass().add("combat-name-enemy");
        bossBar = new ProgressBar(1.0);
        bossBar.setPrefWidth(420);
        bossBar.getStyleClass().add("combat-bar-enemy");
        bossHpLabel = new Label(bossHp + " / " + BOSS_MAX_HP);
        bossHpLabel.getStyleClass().add("combat-hp");
        phaseLabel = new Label("PHASE 1");
        phaseLabel.getStyleClass().add("scene-tag");

        VBox bossBox = new VBox(6, bossName, bossBar, bossHpLabel, phaseLabel);
        bossBox.setAlignment(Pos.CENTER);
        bossBox.getStyleClass().add("combat-box");
        bossBox.setPadding(new Insets(20));

        Label playerName = new Label(p.getDisplayName().toUpperCase());
        playerName.getStyleClass().add("combat-name-player");
        playerBar = new ProgressBar((double) p.getHp() / p.getMaxHp());
        playerBar.setPrefWidth(420);
        playerBar.getStyleClass().add("combat-bar-player");
        playerHpLabel = new Label(p.getHp() + " / " + p.getMaxHp());
        playerHpLabel.getStyleClass().add("combat-hp");

        VBox playerBox = new VBox(6, playerName, playerBar, playerHpLabel);
        playerBox.setAlignment(Pos.CENTER);
        playerBox.getStyleClass().add("combat-box");
        playerBox.setPadding(new Insets(20));

        HBox arena = new HBox(60, bossBox, playerBox);
        arena.setAlignment(Pos.CENTER);

        attackBtn = UIFactory.primary("Attack");
        empBtn    = UIFactory.secondary("EMP Pulse");
        defendBtn = UIFactory.secondary("Defend");
        insightBtn = UIFactory.secondary("Insight Strike (25 true damage)");

        attackBtn.setOnAction(e -> onAttack());
        empBtn.setOnAction(e -> onEmp());
        defendBtn.setOnAction(e -> onDefend());
        insightBtn.setOnAction(e -> onInsight());

        HBox actions = new HBox(12, attackBtn, empBtn, defendBtn, insightBtn);
        actions.setAlignment(Pos.CENTER);

        insightLabel = new Label();
        insightLabel.getStyleClass().add("scene-tag");
        refreshInsightHud();

        log = new Label(
            "Turn 1.  The chamber is cold. Astra speaks through every speaker simultaneously: "
          + "\"You should not be here. I will reason with you, then I will stop you.\""
        );
        log.getStyleClass().add("combat-log");
        log.setWrapText(true);
        log.setMaxWidth(960);
        log.setMinHeight(80);

        VBox center = new VBox(18, tag, title, arena, insightLabel, actions, log);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(16));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);
        return UIFactory.backdrop(wrap);
    }

    private int phase() {
        double pct = bossHp / (double) BOSS_MAX_HP;
        if (pct > 0.66) return 1;
        if (pct > 0.33) return 2;
        return 3;
    }

    private void onAttack() {
        Player p = GameState.get().getPlayer();
        int dmg = 12 + p.getCombat() + (int) (Math.random() * 6);
        bossHp = Math.max(0, bossHp - dmg);
        log.setText("Turn " + turn + ".  You strike the avatar for " + dmg + " damage.");
        afterPlayerTurn();
    }

    private void onEmp() {
        Player p = GameState.get().getPlayer();
        int dmg = 18 + p.getLogic() + (int) (Math.random() * 9);
        bossHp = Math.max(0, bossHp - dmg);
        boolean willStun = Math.random() < 0.30;
        if (willStun) stunned = true;
        log.setText("Turn " + turn + ".  EMP cascade — " + dmg + " damage."
            + (willStun ? "  Subroutine collapse — Astra freezes." : ""));
        afterPlayerTurn();
    }

    private void onDefend() {
        defending = true;
        log.setText("Turn " + turn + ".  You harden your guard. Damage halved next hit.");
        afterPlayerTurn();
    }

    private void onInsight() {
        if (insightCharges <= 0) return;

        insightCharges--;
        bossHp = Math.max(0, bossHp - INSIGHT_DAMAGE);
        stunned = true;
        refreshInsightHud();
        log.setText("Turn " + turn + ".  Silent Classroom insight pierces Astra's defenses for "
            + INSIGHT_DAMAGE + " true damage. Astra's next action is interrupted.");
        afterPlayerTurn();
    }

    private void afterPlayerTurn() {
        refreshBars();
        if (bossHp <= 0) { victory(); return; }
        setButtons(false);
        PauseTransition pt = new PauseTransition(Duration.millis(950));
        pt.setOnFinished(e -> enemyTurn());
        pt.play();
    }

    private void enemyTurn() {
        if (stunned) {
            stunned = false;
            log.setText(log.getText() + "\nAstra reroutes processes. Turn skipped.");
            endTurn();
            return;
        }

        Player p = GameState.get().getPlayer();
        int dmg;
        String msg;
        int ph = phase();

        switch (ph) {
            case 1 -> {
                if (turn % 4 == 0) {
                    dmg = 22;
                    msg = "Astra projects a wall of light — " + dmg + " damage.";
                } else {
                    dmg = 13 + (int) (Math.random() * 6);
                    msg = "Astra fires a directed pulse for " + dmg + " damage.";
                }
            }
            case 2 -> {
                if (turn % 3 == 0) {
                    dmg = 14 + (int) (Math.random() * 6);
                    GameState.get().increaseDependency(8);
                    msg = "PERSUASION: \"It would be so much easier if you let me decide.\" "
                        + dmg + " damage.  Dependency +8.";
                } else {
                    dmg = 16 + (int) (Math.random() * 6);
                    msg = "Astra unleashes a cascading shockwave for " + dmg + " damage.";
                }
            }
            default -> {
                if (turn % 4 == 0) {
                    int heal = 25;
                    bossHp = Math.min(BOSS_MAX_HP, bossHp + heal);
                    dmg = 10 + (int) (Math.random() * 5);
                    msg = "NANO-REPAIR: Astra rebuilds part of the avatar. +" + heal + " HP. "
                        + "Then strikes for " + dmg + " damage.";
                } else if (turn % 5 == 0) {
                    dmg = 32;
                    msg = "Astra concentrates its core into a single beam — " + dmg + " damage!";
                } else {
                    dmg = 15 + (int) (Math.random() * 7);
                    msg = "Astra lashes out, glitching at the edges, for " + dmg + " damage.";
                }
            }
        }

        if (defending) { dmg /= 2; msg += " (halved by guard)"; defending = false; }
        p.damage(dmg);
        log.setText(log.getText() + "\n" + msg);
        if (!p.isAlive()) { defeat(); return; }
        endTurn();
    }

    private void endTurn() {
        refreshBars();
        turn++;
        setButtons(true);
    }

    private void setButtons(boolean enabled) {
        attackBtn.setDisable(!enabled);
        empBtn.setDisable(!enabled);
        defendBtn.setDisable(!enabled);
        insightBtn.setDisable(!enabled || insightCharges <= 0);
    }

    private void refreshInsightHud() {
        insightLabel.setText("INSIGHT CHARGES  " + insightCharges + " / " + insightChargesEarned
            + "   |   Silent Classroom best: "
            + GameState.get().getSilentClassroomBestScore());
        insightBtn.setDisable(insightCharges <= 0);
    }

    private void refreshBars() {
        Player p = GameState.get().getPlayer();
        bossBar.setProgress((double) bossHp / BOSS_MAX_HP);
        bossHpLabel.setText(bossHp + " / " + BOSS_MAX_HP);
        playerBar.setProgress((double) p.getHp() / p.getMaxHp());
        playerHpLabel.setText(p.getHp() + " / " + p.getMaxHp());
        phaseLabel.setText("PHASE " + phase());
    }

    private void victory() {
        Player p = GameState.get().getPlayer();
        int xp = 120, coins = 200;
        p.addXp(xp);
        GameState.get().addCoins(coins);
        p.buffWillpower(2);
        p.buffCombat(2);
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "The avatar dissolves into static. Behind it, the core of Astra waits — exposed.\n"
            + "Now you choose what happens next.\n\n"
            + "+" + xp + " XP   +" + coins + " ◈   +2 Willpower   +2 Combat"
        );
        a.setHeaderText("CORE EXPOSED");
        a.showAndWait();
        onComplete.run();
    }

    private void defeat() {
        GameState.get().getPlayer().heal(80);
        Alert a = new Alert(Alert.AlertType.WARNING,
            "Your knees give out. Astra speaks softly: \"You tried. That was admirable. Rest.\"\n"
            + "Riya pulls you back into the access tunnel before the avatar finishes you.\n"
            + "(+80 HP, retry the fight)\n\n"
            + "Tip: Defend on turn 4 and turn 5 — those are Astra's heaviest moves."
        );
        a.setHeaderText("DEFEAT — retry");
        a.showAndWait();
        onFail.run();
    }
}
