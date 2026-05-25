package com.override.chapter4;

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
 * Chapter 4 boss — Lab Defense Prototype.
 *
 * 240 HP. Has two distinct attacks:
 *   - Code Injection (turn % 3 == 0): inflicts a stat-debuff "logic worm"
 *     that reduces player damage for 2 turns.
 *   - Recompile (turn % 6 == 0): heals itself for 30 HP.
 * Player gains +2 Logic and +1 Combat on win.
 */
public class CodeBossScreen {

    private static final int BOSS_MAX_HP = 240;

    private final Runnable onComplete;
    private final Runnable onFail;

    private int bossHp = BOSS_MAX_HP;
    private boolean defending = false;
    private boolean stunned = false;
    private int turn = 1;
    private int wormTurns = 0; // turns of player damage debuff remaining

    private ProgressBar bossBar, playerBar;
    private Label log, bossHpLabel, playerHpLabel;
    private Button attackBtn, empBtn, defendBtn;

    public CodeBossScreen(Runnable onComplete, Runnable onFail) {
        this.onComplete = onComplete;
        this.onFail = onFail;
    }

    public Parent build() {
        Player p = GameState.get().getPlayer();

        Label tag = new Label("BOSS — LAB DEFENSE PROTOTYPE");
        tag.getStyleClass().add("scene-tag");

        Label title = UIFactory.title("Compile-Time Error");

        Label bossName = new Label("LAB DEFENSE PROTOTYPE");
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

        log = new Label("Turn 1.  The prototype boots. Its display flashes: \"INPUT VALIDATED. RESPONSE: COMPILE\".");
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

    private int wormPenalty(int dmg) {
        // While the worm is active, player damage is cut by ~30%.
        return wormTurns > 0 ? (int) Math.round(dmg * 0.7) : dmg;
    }

    private void onAttack() {
        Player p = GameState.get().getPlayer();
        int raw = 11 + p.getCombat() + (int) (Math.random() * 5);
        int dmg = wormPenalty(raw);
        bossHp = Math.max(0, bossHp - dmg);
        log.setText("Turn " + turn + ".  You strike for " + dmg + " damage."
            + (wormTurns > 0 ? "  (logic worm reduced output)" : ""));
        afterPlayerTurn();
    }

    private void onEmp() {
        Player p = GameState.get().getPlayer();
        int raw = 17 + p.getLogic() + (int) (Math.random() * 8);
        int dmg = wormPenalty(raw);
        bossHp = Math.max(0, bossHp - dmg);
        boolean willStun = Math.random() < 0.30;
        if (willStun) stunned = true;
        log.setText("Turn " + turn + ".  EMP overload — " + dmg + " damage."
            + (willStun ? "  Cooling fans stall. Prototype freezes." : "")
            + (wormTurns > 0 ? "  (worm penalty applied)" : ""));
        afterPlayerTurn();
    }

    private void onDefend() {
        defending = true;
        log.setText("Turn " + turn + ".  You crouch behind a server cabinet. Damage halved next turn.");
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
            log.setText(log.getText() + "\nThe prototype recompiles. Turn skipped.");
            endTurn();
            return;
        }

        Player p = GameState.get().getPlayer();
        int dmg;
        String msg;

        if (turn % 6 == 0) {
            int heal = 30;
            bossHp = Math.min(BOSS_MAX_HP, bossHp + heal);
            msg = "RECOMPILE: the prototype rebuilds itself. +" + heal + " HP restored.";
            dmg = 9 + (int) (Math.random() * 5);
            msg += " Then it lashes out for " + dmg + " damage.";
        } else if (turn % 3 == 0) {
            dmg = 16 + (int) (Math.random() * 6);
            wormTurns = 2;
            msg = "CODE INJECTION! A logic worm burrows in for " + dmg + " damage. "
                + "Your output is reduced for 2 turns.";
        } else {
            dmg = 12 + (int) (Math.random() * 7);
            msg = "The prototype slams a manipulator arm for " + dmg + " damage.";
        }

        if (defending) { dmg /= 2; msg += " (halved by cover)"; defending = false; }
        p.damage(dmg);
        log.setText(log.getText() + "\n" + msg);
        if (!p.isAlive()) { defeat(); return; }
        endTurn();
    }

    private void endTurn() {
        if (wormTurns > 0) wormTurns--;
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
        int xp = 80, coins = 120;
        p.addXp(xp);
        GameState.get().addCoins(coins);
        p.buffLogic(2);
        p.buffCombat(1);
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "The prototype crashes mid-recompile. Smoke pours from its exposed core.\n"
            + "On the legacy rack behind it, the fourth access fragment is waiting.\n\n"
            + "+" + xp + " XP   +" + coins + " ◈   +2 Logic   +1 Combat"
        );
        a.setHeaderText("VICTORY");
        a.showAndWait();
        onComplete.run();
    }

    private void defeat() {
        GameState.get().getPlayer().heal(60);
        Alert a = new Alert(Alert.AlertType.WARNING,
            "The logic worm overloads your visor. Everything goes white.\n"
            + "An old developer named Riya pulls you out and patches your suit.\n"
            + "(+60 HP, retry the fight)\n\n"
            + "Tip: Defend before turn 3 (code injection) and turn 6 (it heals)."
        );
        a.setHeaderText("DEFEAT — retry");
        a.showAndWait();
        onFail.run();
    }
}
