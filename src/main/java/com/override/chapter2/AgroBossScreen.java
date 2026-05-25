package com.override.chapter2;

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
 * Chapter 2 boss — Agro Drone Controller.
 *
 * Tougher than the Campus Sentinel. Has 160 HP and a harvest blade
 * attack every 4th turn. Player gains +1 Combat and +1 Awareness on win.
 */
public class AgroBossScreen {

    private static final int BOSS_MAX_HP = 160;

    private final Runnable onComplete;
    private final Runnable onFail;

    private int bossHp = BOSS_MAX_HP;
    private boolean defending = false;
    private boolean stunned = false;
    private int turn = 1;

    private ProgressBar bossBar, playerBar;
    private Label log, bossHpLabel, playerHpLabel;
    private Button attackBtn, empBtn, defendBtn;

    public AgroBossScreen(Runnable onComplete, Runnable onFail) {
        this.onComplete = onComplete;
        this.onFail = onFail;
    }

    public Parent build() {
        Player p = GameState.get().getPlayer();

        Label tag = new Label("BOSS — AGRO DRONE CONTROLLER");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Harvest Protocol");

        // Boss panel
        Label bossName = new Label("AGRO DRONE CONTROLLER");
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

        // Player panel
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

        log = new Label("Turn 1.  The drone controller unfolds its harvest blades.");
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
        int dmg = 14 + p.getLogic() + (int) (Math.random() * 7);
        bossHp = Math.max(0, bossHp - dmg);
        boolean willStun = Math.random() < 0.40;
        if (willStun) stunned = true;
        log.setText("Turn " + turn + ".  EMP blast — " + dmg + " damage."
            + (willStun ? "  Motors seize. Systems offline." : ""));
        afterPlayerTurn();
    }

    private void onDefend() {
        defending = true;
        log.setText("Turn " + turn + ".  You brace behind cover. Incoming damage halved.");
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
            log.setText(log.getText() + "\nThe controller reboots. Turn lost.");
            endTurn();
            return;
        }
        Player p = GameState.get().getPlayer();
        int dmg;
        String msg;
        if (turn % 4 == 0) {
            dmg = 28;
            msg = "The controller swings its harvest blade for " + dmg + " damage!";
        } else {
            dmg = 12 + (int) (Math.random() * 7);
            msg = "The controller fires pesticide darts for " + dmg + " damage.";
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
        int xp = 60, coins = 90;
        p.addXp(xp);
        GameState.get().addCoins(coins);
        p.buffCombat(1);
        p.buffAwareness(1);
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "The Agro Drone Controller crashes into the field. Sparks scatter across the dirt.\n\n"
            + "+" + xp + " XP   +" + coins + " ◈   +1 Combat   +1 Awareness"
        );
        a.setHeaderText("VICTORY");
        a.showAndWait();
        onComplete.run();
    }

    private void defeat() {
        GameState.get().getPlayer().heal(50);
        Alert a = new Alert(Alert.AlertType.WARNING,
            "The drone's blade sends you tumbling. A farmer drags you into a barn.\n"
            + "(+50 HP, retry the fight)\n\n"
            + "Tip: Defend before turn 4 — the controller uses its harvest blade."
        );
        a.setHeaderText("DEFEAT — retry");
        a.showAndWait();
        onFail.run();
    }
}
