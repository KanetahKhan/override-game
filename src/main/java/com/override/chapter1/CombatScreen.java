package com.override.chapter1;

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
 * Turn-based boss combat against the Campus Sentinel.
 *
 * Player actions:
 *   - Attack:  reliable damage; uses Combat stat
 *   - EMP:     bigger damage with chance to stun (uses Logic stat)
 *   - Defend:  half-damage on the next enemy hit
 *
 * Enemy AI: simple alternating attacks with occasional power move.
 *
 * Designed for the prototype scope — no animations beyond text feedback,
 * keeps the screen readable for the demo.
 */
public class CombatScreen {

    private static final int BOSS_MAX_HP = 120;

    private final Runnable onComplete;
    private final Runnable onFail;

    private int bossHp = BOSS_MAX_HP;
    private boolean defending = false;
    private boolean stunned = false;
    private int turn = 1;

    private ProgressBar bossBar;
    private ProgressBar playerBar;
    private Label log;
    private Label bossHpLabel;
    private Label playerHpLabel;
    private Button attackBtn, empBtn, defendBtn;

    public CombatScreen(Runnable onComplete, Runnable onFail) {
        this.onComplete = onComplete;
        this.onFail = onFail;
    }

    public Parent build() {
        Player p = GameState.get().getPlayer();

        Label tag = new Label("BOSS — CAMPUS SENTINEL BOT");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Confrontation");

        // Boss panel
        Label bossName = new Label("CAMPUS SENTINEL");
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

        // Action buttons
        attackBtn = UIFactory.primary("Attack");
        empBtn    = UIFactory.secondary("EMP Pulse");
        defendBtn = UIFactory.secondary("Defend");

        attackBtn.setOnAction(e -> onAttack());
        empBtn.setOnAction(e -> onEmp());
        defendBtn.setOnAction(e -> onDefend());

        HBox actions = new HBox(12, attackBtn, empBtn, defendBtn);
        actions.setAlignment(Pos.CENTER);

        log = new Label("Turn 1.  The sentinel scans you and locks on.");
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
        int dmg = 8 + p.getCombat() + (int) (Math.random() * 4);
        bossHp = Math.max(0, bossHp - dmg);
        log.setText("Turn " + turn + ".  You strike the sentinel for " + dmg + " damage.");
        afterPlayerTurn();
    }

    private void onEmp() {
        Player p = GameState.get().getPlayer();
        int dmg = 12 + p.getLogic() + (int) (Math.random() * 6);
        bossHp = Math.max(0, bossHp - dmg);
        boolean willStun = Math.random() < 0.45;
        if (willStun) stunned = true;
        log.setText("Turn " + turn + ".  EMP burst — " + dmg + " damage."
            + (willStun ? "  The sentinel staggers, systems offline." : ""));
        afterPlayerTurn();
    }

    private void onDefend() {
        defending = true;
        log.setText("Turn " + turn + ".  You brace. Damage taken next turn is halved.");
        afterPlayerTurn();
    }

    private void afterPlayerTurn() {
        refreshBars();
        if (bossHp <= 0) {
            victory();
            return;
        }
        // Disable buttons during enemy turn
        setButtons(false);
        PauseTransition pt = new PauseTransition(Duration.millis(900));
        pt.setOnFinished(e -> enemyTurn());
        pt.play();
    }

    private void enemyTurn() {
        if (stunned) {
            stunned = false;
            log.setText(log.getText() + "\nThe sentinel reboots. It loses its turn.");
            endTurn();
            return;
        }
        Player p = GameState.get().getPlayer();
        // alternating: turn 2, 4, 6 ... use power move every 3 turns
        int dmg;
        String msg;
        if (turn % 3 == 0) {
            dmg = 22;
            msg = "The sentinel charges a piercing rail-shot for " + dmg + " damage.";
        } else {
            dmg = 10 + (int) (Math.random() * 6);
            msg = "The sentinel fires for " + dmg + " damage.";
        }
        if (defending) { dmg /= 2; msg += " (halved by your guard)"; defending = false; }
        p.damage(dmg);
        log.setText(log.getText() + "\n" + msg);
        if (!p.isAlive()) {
            defeat();
            return;
        }
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
        int xp = 50, coins = 80;
        p.addXp(xp);
        GameState.get().addCoins(coins);
        p.buffCombat(1);
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "The sentinel collapses. The path to the data archive is open.\n\n"
            + "+" + xp + " XP   +" + coins + " ◈   +1 Combat"
        );
        a.setHeaderText("VICTORY");
        a.showAndWait();
        onComplete.run();
    }

    private void defeat() {
        // Soft fail: heal a little and let player retry
        GameState.get().getPlayer().heal(40);
        Alert a = new Alert(Alert.AlertType.WARNING,
            "Your systems blacked out. A passing student dragged you to safety.\n"
            + "(+40 HP, retry the fight)\n\n"
            + "Tip: Defend before a turn 3 — the sentinel uses a heavy attack then."
        );
        a.setHeaderText("DEFEAT — retry");
        a.showAndWait();
        onFail.run();
    }
}
