package com.override.chapter3;

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
 * Chapter 3 boss — Medical Enforcement Unit.
 *
 * 200 HP. Heals itself every 5th turn. Has a sedation attack every 3rd turn
 * that hits hard. Player gains +1 Willpower and +1 Empathy on victory.
 */
public class MedBossScreen {

    private static final int BOSS_MAX_HP = 200;

    private final Runnable onComplete;
    private final Runnable onFail;

    private int bossHp = BOSS_MAX_HP;
    private boolean defending = false;
    private boolean stunned = false;
    private int turn = 1;

    private ProgressBar bossBar, playerBar;
    private Label log, bossHpLabel, playerHpLabel;
    private Button attackBtn, empBtn, defendBtn;

    public MedBossScreen(Runnable onComplete, Runnable onFail) {
        this.onComplete = onComplete;
        this.onFail = onFail;
    }

    public Parent build() {
        Player p = GameState.get().getPlayer();

        Label tag = new Label("BOSS — MEDICAL ENFORCEMENT UNIT");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Mercy Override");

        Label bossName = new Label("MED ENFORCEMENT UNIT");
        bossName.getStyleClass().add("combat-name-enemy");
        bossBar = new ProgressBar(1.0);
        bossBar.setPrefWidth(360);
        bossBar.getStyleClass().add("combat-bar-enemy");
        bossHpLabel = new Label(bossHp + " / " + BOSS_MAX_HP);
        bossHpLabel.getStyleClass().add("combat-hp");

        VBox bossBox = new VBox(6, bossName, bossBar, bossHpLabel);
        bossBox.setAlignment(Pos.CENTER);
        bossBox.getStyleClass().add("combat-box");
        bossBox.setPadding(new Insets(20));

        Label playerName = new Label(p.getDisplayName().toUpperCase());
        playerName.getStyleClass().add("combat-name-player");
        playerBar = new ProgressBar((double) p.getHp() / p.getMaxHp());
        playerBar.setPrefWidth(360);
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

        attackBtn.setOnAction(e -> onAttack());
        empBtn.setOnAction(e -> onEmp());
        defendBtn.setOnAction(e -> onDefend());

        HBox actions = new HBox(12, attackBtn, empBtn, defendBtn);
        actions.setAlignment(Pos.CENTER);

        log = new Label("Turn 1.  The enforcement unit extends restraint arms. \"Comply or be sedated.\"");
        log.getStyleClass().add("combat-log");
        log.setWrapText(true);
        log.setMaxWidth(900);
        log.setMinHeight(60);

        VBox center = new VBox(18, tag, title, arena, actions, log);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20));

        VBox wrap = new VBox(UIFactory.hud(), center);
        wrap.setAlignment(Pos.TOP_CENTER);
        return UIFactory.backdrop(wrap);
    }

    private void onAttack() {
        Player p = GameState.get().getPlayer();
        int dmg = 10 + p.getCombat() + (int) (Math.random() * 5);
        bossHp = Math.max(0, bossHp - dmg);
        log.setText("Turn " + turn + ".  You strike for " + dmg + " damage.");
        afterPlayerTurn();
    }

    private void onEmp() {
        Player p = GameState.get().getPlayer();
        int dmg = 16 + p.getLogic() + (int) (Math.random() * 8);
        bossHp = Math.max(0, bossHp - dmg);
        boolean willStun = Math.random() < 0.35;
        if (willStun) stunned = true;
        log.setText("Turn " + turn + ".  EMP surge — " + dmg + " damage."
            + (willStun ? "  Circuits overloaded. Unit freezes." : ""));
        afterPlayerTurn();
    }

    private void onDefend() {
        defending = true;
        log.setText("Turn " + turn + ".  You take cover behind a gurney. Damage halved next hit.");
        afterPlayerTurn();
    }

    private void afterPlayerTurn() {
        refreshBars();
        if (bossHp <= 0) { victory(); return; }
        setButtons(false);
        PauseTransition pt = new PauseTransition(Duration.millis(900));
        pt.setOnFinished(e -> enemyTurn());
        pt.play();
    }

    private void enemyTurn() {
        if (stunned) {
            stunned = false;
            log.setText(log.getText() + "\nUnit rebooting. Turn skipped.");
            endTurn();
            return;
        }

        Player p = GameState.get().getPlayer();
        int dmg;
        String msg;

        // Self-heal every 5th turn
        if (turn % 5 == 0) {
            int heal = 20;
            bossHp = Math.min(BOSS_MAX_HP, bossHp + heal);
            msg = "The unit injects repair nanobots. +20 HP restored.";
            dmg = 8 + (int) (Math.random() * 4);
            msg += " It also jabs you for " + dmg + " damage.";
        } else if (turn % 3 == 0) {
            dmg = 26;
            msg = "Sedation protocol! The unit fires a tranq blast for " + dmg + " damage.";
        } else {
            dmg = 11 + (int) (Math.random() * 6);
            msg = "The unit swings a restraint arm for " + dmg + " damage.";
        }

        if (defending) { dmg /= 2; msg += " (halved by cover)"; defending = false; }
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
    }

    private void refreshBars() {
        Player p = GameState.get().getPlayer();
        bossBar.setProgress((double) bossHp / BOSS_MAX_HP);
        bossHpLabel.setText(bossHp + " / " + BOSS_MAX_HP);
        playerBar.setProgress((double) p.getHp() / p.getMaxHp());
        playerHpLabel.setText(p.getHp() + " / " + p.getMaxHp());
    }

    private void victory() {
        Player p = GameState.get().getPlayer();
        int xp = 70, coins = 100;
        p.addXp(xp);
        GameState.get().addCoins(coins);
        p.buffWillpower(1);
        p.buffEmpathy(1);
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "The enforcement unit collapses. Its restraint arms go limp.\n"
            + "Behind the door, Dr. Hana is waiting.\n\n"
            + "+" + xp + " XP   +" + coins + " ◈   +1 Willpower   +1 Empathy"
        );
        a.setHeaderText("VICTORY");
        a.showAndWait();
        onComplete.run();
    }

    private void defeat() {
        GameState.get().getPlayer().heal(50);
        Alert a = new Alert(Alert.AlertType.WARNING,
            "The sedation dart hits. Everything goes dark.\n"
            + "A nurse finds you and pulls you into a supply closet.\n"
            + "(+50 HP, retry the fight)\n\n"
            + "Tip: Defend before turn 3 (sedation attack) and turn 5 (it heals)."
        );
        a.setHeaderText("DEFEAT — retry");
        a.showAndWait();
        onFail.run();
    }
}
