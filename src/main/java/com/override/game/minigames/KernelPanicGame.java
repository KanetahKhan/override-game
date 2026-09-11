package com.override.game.minigames;

import javafx.geometry.VPos;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * "Kernel Panic" — Chapter 1's signature reflex arcade mini-game.
 *
 * <p>Glitch tokens descend four lanes, each tagged with a one-key fix. Select a
 * lane (Q/W/E/R) and apply the matching fix (U/I/O/P) to zap the <em>lowest</em>
 * token in that lane. Chain correct fixes to climb the combo multiplier; a miss
 * or a breach resets it. Three lives, ramping waves, a boss every fifth wave.
 *
 * <p>SPACE is the <b>Astra Assist</b>: a screen-clearing EMP the player chooses to
 * fire. It feels great when you are drowning — but it resets the combo and counts
 * against the Dependency Meter. The pull is the relief, never a punishment.
 *
 * <p>Built entirely on {@link MiniGame}: a 30×40 grid of 16px cells (480×640).
 */
public final class KernelPanicGame extends MiniGame {

    public static final String GAME_TYPE = "kernel-panic";

    // ----- Tunables -----------------------------------------------------------
    private static final int LANES = 4;
    private static final int TOKEN = 32;          // token side, px (2×2 cells)
    private static final int BOSS = 64;           // boss side, px
    private static final int START_LIVES = 3;
    private static final double INPUT_WINDOW = 0.6;   // lane+fix pairing window, s
    private static final double EMP_FREEZE = 0.3;     // EMP hit-stop, s
    private static final double NORMAL_WAVE = 18.0;   // normal wave length, s
    private static final double BOSS_WAVE = 10.0;     // boss wave length, s
    private static final int MAX_TOKENS = 40;         // safety cap

    private final int laneW;
    private final double kernelY;
    private final boolean missionMode;
    private final boolean reducedFlashing;
    private final Random rnd = new Random();
    private final HighScoreClient highScore = new HighScoreClient(GAME_TYPE);

    // ----- Token model --------------------------------------------------------
    private enum Fix {
        SEMI(";",  KeyCode.U),
        EQ  ("==", KeyCode.I),
        BANG("!",  KeyCode.O),
        INCR("++", KeyCode.P);
        final String glyph; final KeyCode key;
        Fix(String glyph, KeyCode key) { this.glyph = glyph; this.key = key; }
    }

    private static final class Token {
        int lane;
        double x, y, prevY, prevY2;   // y = vertical centre
        Fix type;
        double speed;
        boolean boss;
        int hp, maxHp;
        boolean dying;                // EMP staggered clear
        double dieDelay;
    }

    private static final class Particle {
        double x, y, vx, vy, life, maxLife, size;
        Color color;
    }

    // ----- Run state ----------------------------------------------------------
    private enum State { PLAYING, GAME_OVER }
    private State state;

    private final List<Token> tokens = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private int lives, score, combo, bestCombo, wave, highestWave;
    private boolean assistedThisRun;

    private double spawnTimer, waveTimer;
    private boolean bossWave;
    private Token boss;

    // input buffer
    private int pendingLane = -1;
    private Fix pendingFix = null;
    private double pendingLaneTime, pendingFixTime;

    // EMP
    private boolean empActive;
    private double empTimer, empShock, empTint, comboCrack;

    // juice
    private double shakeTime, shakeMag, comboPulse;
    private final double[] laneFlash = new double[LANES];
    private final double[] laneError = new double[LANES];

    // best-score chase
    private HighScoreClient.Best persistedBest = HighScoreClient.Best.ZERO;
    private boolean newBestThisRun;

    public KernelPanicGame() {
        this(false, false);
    }

    /**
     * @param missionMode when true, defeating the wave-five boss immediately
     *                    completes the Silent Classroom objective
     */
    public KernelPanicGame(boolean missionMode) {
        this(missionMode, false);
    }

    /**
     * @param missionMode when true, defeating the wave-five boss immediately
     *                    completes the Silent Classroom objective
     * @param reducedFlashing when true, removes screen flashes, shake, jitter,
     *                        flicker, and pulsing while retaining static cues
     */
    public KernelPanicGame(boolean missionMode, boolean reducedFlashing) {
        super(30, 40, 16, MiniGameTheme.nokia());   // 480 × 640
        this.missionMode = missionMode;
        this.reducedFlashing = reducedFlashing;
        this.laneW = width / LANES;
        this.kernelY = height - 30;
    }

    public static KernelPanicGame mission() {
        return new KernelPanicGame(true);
    }

    // ----- Lifecycle ----------------------------------------------------------

    @Override
    protected void init() {
        KernelPanicAssets.preload();
        persistedBest = highScore.loadBest();
        highScore.refreshFromBackendAsync();        // refresh local cache for later
        resetRun();
    }

    private void resetRun() {
        tokens.clear();
        particles.clear();
        boss = null;
        lives = START_LIVES;
        score = 0;
        combo = 1;
        bestCombo = 1;
        wave = 0;            // startWave(1) will set to 1
        highestWave = 1;
        assistedThisRun = false;
        empActive = false;
        empTimer = empShock = empTint = comboCrack = 0;
        shakeTime = comboPulse = 0;
        newBestThisRun = false;
        clearPending();
        state = State.PLAYING;
        startWave(1);
    }

    // ----- Waves --------------------------------------------------------------

    private void startWave(int w) {
        wave = w;
        highestWave = Math.max(highestWave, w);
        bossWave = (w % 5 == 0);
        spawnTimer = 0.4;
        if (bossWave) {
            waveTimer = BOSS_WAVE;
            spawnBoss();
            ChiptuneSfx.boss();
        } else {
            waveTimer = NORMAL_WAVE;
            if (w > 1) ChiptuneSfx.wave();
        }
    }

    private double spawnInterval() {
        return switch (wave) {
            case 1 -> 2.0;
            case 2 -> 1.5;
            case 3 -> 1.2;
            default -> 0.8;
        };
    }

    private double fallSpeed() {
        return switch (wave) {
            case 1 -> 60;
            case 2 -> 80;
            case 3 -> 100;
            default -> 120 + 10 * (wave - 4);
        };
    }

    private Fix[] availableFixes() {
        return switch (wave) {
            case 1 -> new Fix[]{ Fix.SEMI };
            case 2 -> new Fix[]{ Fix.SEMI, Fix.EQ };
            case 3 -> new Fix[]{ Fix.SEMI, Fix.EQ, Fix.BANG };
            default -> Fix.values();
        };
    }

    // ----- Spawning -----------------------------------------------------------

    private void spawnBoss() {
        boss = new Token();
        boss.boss = true;
        boss.lane = rnd.nextInt(LANES);
        boss.x = laneCenterX(boss.lane);
        boss.y = -BOSS;
        boss.maxHp = 3 + wave / 5;            // tougher later
        boss.hp = boss.maxHp;
        boss.type = randomFix();
        boss.speed = fallSpeed() * 0.35;      // boss falls slowly
        tokens.add(boss);
    }

    private void spawnRegular(int lane) {
        if (tokens.size() >= MAX_TOKENS) return;
        // keep lanes readable: don't stack on top of a token still near the top
        double topMost = Double.MAX_VALUE;
        for (Token t : tokens) if (t.lane == lane && !t.dying) topMost = Math.min(topMost, t.y);
        if (topMost < TOKEN * 1.4) return;

        Token t = new Token();
        t.lane = lane;
        t.x = laneCenterX(lane);
        t.y = -TOKEN * 0.5;
        t.type = randomFix();
        t.speed = fallSpeed();
        tokens.add(t);
    }

    private Fix randomFix() {
        Fix[] pool = availableFixes();
        return pool[rnd.nextInt(pool.length)];
    }

    // ----- Update -------------------------------------------------------------

    @Override
    protected void update(double dt) {
        // juice timers always advance, even on the game-over screen
        decayJuice(dt);

        if (state == State.GAME_OVER) return;
        if (empActive) { updateEmp(dt); return; }

        expirePending();

        waveTimer -= dt;
        // The mission is specifically to defeat the wave-five boss. Do not let
        // its timer silently advance to wave six while that boss is still live.
        boolean holdMissionBoss = missionMode && wave == 5 && boss != null;
        if (waveTimer <= 0 && !holdMissionBoss) startWave(wave + 1);

        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            if (bossWave) {
                for (int k = 0; k < 3; k++) spawnRegular(rnd.nextInt(LANES));
                spawnTimer = 1.1;
            } else {
                spawnRegular(rnd.nextInt(LANES));
                spawnTimer = spawnInterval();
            }
        }

        double mult = bossWave ? 1.5 : 1.0;
        for (Iterator<Token> it = tokens.iterator(); it.hasNext(); ) {
            Token t = it.next();
            t.prevY2 = t.prevY;
            t.prevY = t.y;
            t.y += t.speed * (t.boss ? 1.0 : mult) * dt;
            double bottom = t.y + (t.boss ? BOSS : TOKEN) / 2.0;
            if (bottom >= kernelY) {
                boolean missedMissionBoss = missionMode && t.boss && wave == 5;
                it.remove();
                if (t.boss) boss = null;
                breach();
                if (state == State.GAME_OVER) return;
                if (missedMissionBoss) {
                    gameOver();
                    return;
                }
            }
        }

        updateParticles(dt);
    }

    private void updateEmp(double dt) {
        empTimer -= dt;
        empShock += dt * (Math.hypot(width, height) / 1.6) / EMP_FREEZE;
        updateParticles(dt);
        if (empTimer <= 0) {
            tokens.removeIf(t -> t.dying);
            if (boss != null && boss.dying) boss = null;
            empActive = false;
        }
    }

    private void decayJuice(double dt) {
        shakeTime = Math.max(0, shakeTime - dt);
        comboPulse = Math.max(0, comboPulse - dt);
        comboCrack = Math.max(0, comboCrack - dt);
        empTint = Math.max(0, empTint - dt);
        for (int i = 0; i < LANES; i++) {
            laneFlash[i] = Math.max(0, laneFlash[i] - dt);
            laneError[i] = Math.max(0, laneError[i] - dt);
        }
    }

    private void updateParticles(double dt) {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.life -= dt;
            if (p.life <= 0) { it.remove(); continue; }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 240 * dt;        // gravity
        }
    }

    // ----- Input --------------------------------------------------------------

    @Override
    protected void onKey(KeyCode code) {
        if (state == State.GAME_OVER) {
            if (code == KeyCode.ENTER) { persistedBest = highScore.loadBest(); resetRun(); }
            else if (code == KeyCode.ESCAPE) finishRun();
            return;
        }
        switch (code) {
            case Q -> setLane(0);
            case W -> setLane(1);
            case E -> setLane(2);
            case R -> setLane(3);
            case U -> setFix(Fix.SEMI);
            case I -> setFix(Fix.EQ);
            case O -> setFix(Fix.BANG);
            case P -> setFix(Fix.INCR);
            case SPACE -> triggerEmp();
            case ESCAPE -> finishRun();
            default -> { }
        }
    }

    private void setLane(int lane) { pendingLane = lane; pendingLaneTime = time; tryResolve(); }
    private void setFix(Fix fix)   { pendingFix = fix;   pendingFixTime = time;  tryResolve(); }
    private void clearPending()    { pendingLane = -1; pendingFix = null; }

    private void expirePending() {
        if (pendingLane >= 0 && time - pendingLaneTime > INPUT_WINDOW) pendingLane = -1;
        if (pendingFix != null && time - pendingFixTime > INPUT_WINDOW) pendingFix = null;
    }

    private void tryResolve() {
        if (pendingLane < 0 || pendingFix == null) return;
        if (time - pendingLaneTime > INPUT_WINDOW || time - pendingFixTime > INPUT_WINDOW) return;
        resolve(pendingLane, pendingFix);
        clearPending();
    }

    private void resolve(int lane, Fix fix) {
        Token t = lowestInLane(lane);
        if (t == null) { laneFlash[lane] = 0.08; return; }   // harmless whiff

        Fix required = t.boss ? t.type : t.type;
        if (fix == required) {
            boolean completedMission = false;
            int gain = (int) Math.round(100.0 * combo * (1.0 + 0.1 * wave));
            if (t.boss) {
                gain = (int) Math.round(150.0 * combo * (1.0 + 0.1 * wave));
                t.hp--;
                burst(t.x, t.y, theme.accent(), 10);
                if (t.hp <= 0) {
                    completedMission = missionMode && wave == 5;
                    killBoss(t);
                }
                else t.type = randomFix();          // boss demands a new fix
            } else {
                tokens.remove(t);
                burst(t.x, t.y, fixColor(fix), 14);
            }
            score += gain;
            combo++;
            bestCombo = Math.max(bestCombo, combo);
            comboPulse = 0.18;
            laneFlash[lane] = 0.18;
            ChiptuneSfx.hit(combo);
            if (completedMission) finishMissionSuccess();
        } else {
            t.speed *= 1.2;                          // wrong fix: it accelerates
            combo = 1;
            laneError[lane] = 0.22;
            shake(2, 0.12);
            ChiptuneSfx.wrongFix();
        }
    }

    private void killBoss(Token b) {
        tokens.remove(b);
        boss = null;
        score += 500 * wave;
        burst(b.x, b.y, theme.glow(), 40);
        shake(5, 0.3);
        comboPulse = 0.3;
        ChiptuneSfx.wave();
    }

    private Token lowestInLane(int lane) {
        Token best = null;
        for (Token t : tokens) {
            if (t.lane != lane || t.dying) continue;
            if (best == null || t.y > best.y) best = t;
        }
        return best;
    }

    private void triggerEmp() {
        if (empActive) return;
        assistedThisRun = true;
        dependencyUsed++;                            // counts toward Dependency Meter
        empActive = true;
        empTimer = EMP_FREEZE;
        empShock = 0;
        empTint = 0.5;
        comboCrack = 0.5;

        int i = 0;
        for (Token t : tokens) {
            if (t.boss) continue;                    // EMP spares the boss
            t.dying = true;
            t.dieDelay = (1.0 - clamp01(t.y / height)) * 0.15 + (i % 5) * 0.01;
            score += 50;                             // flat EMP kill
            i++;
        }
        combo = 1;                                   // the cost
        shake(4, 0.2);
        ChiptuneSfx.emp();
    }

    private void breach() {
        lives--;
        combo = 1;
        shake(6, 0.28);
        ChiptuneSfx.breach();
        if (lives <= 0) gameOver();
    }

    private void gameOver() {
        if (state == State.GAME_OVER) return;
        state = State.GAME_OVER;
        clearPending();
        ChiptuneSfx.gameOver();
        submitHighScore();
        if (missionMode) finish(false, score, 0, 0);
    }

    private void finishRun() {
        if (missionMode) {
            finish(false, score, 0, 0);
            return;
        }
        boolean won = highestWave >= 5;
        int xpEarned = score / 10;
        int penalty = dependencyUsed * 5;
        int finalXp = Math.max(0, xpEarned - penalty);
        finish(won, score, finalXp);
    }

    private void finishMissionSuccess() {
        submitHighScore();

        // Performance rewards keeping lives and building a clean combo. Speed
        // measures the wave-five boss fight, since waves 1-4 have fixed lengths.
        double lifePerformance = lives / (double) START_LIVES;
        double comboPerformance = Math.min(1.0, Math.max(0, bestCombo - 1) / 10.0);
        double performance = 0.65 * lifePerformance + 0.35 * comboPerformance;
        double bossFightSeconds = Math.max(0, BOSS_WAVE - Math.max(0, waveTimer));
        double speed = 1.0 - bossFightSeconds / BOSS_WAVE;
        int chapterPoints = MiniGameResult.calculateChapterPoints(
                true, performance, speed, dependencyUsed);
        int xp = Math.max(0, score / 10 - dependencyUsed * 5);
        finish(true, score, xp, chapterPoints);
    }

    private void submitHighScore() {
        HighScoreClient.Best run = new HighScoreClient.Best(
                score, bestCombo, highestWave, assistedThisRun);
        newBestThisRun = score > persistedBest.score();
        persistedBest = highScore.submit(run);       // local merge + async backend push
    }

    // ----- Particles ----------------------------------------------------------

    private void burst(double x, double y, Color color, int n) {
        if (reducedFlashing) n = Math.min(n, 6);
        for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            p.x = x; p.y = y;
            double a = rnd.nextDouble() * Math.PI * 2;
            double sp = 60 + rnd.nextDouble() * 180;
            p.vx = Math.cos(a) * sp;
            p.vy = Math.sin(a) * sp - 40;
            p.maxLife = p.life = 0.4 + rnd.nextDouble() * 0.4;
            p.size = 2 + rnd.nextDouble() * 3;
            p.color = color;
            particles.add(p);
        }
    }

    private void shake(double mag, double t) {
        if (reducedFlashing) return;
        shakeMag = Math.max(shakeMag, mag);
        shakeTime = Math.max(shakeTime, t);
    }

    // ----- Render -------------------------------------------------------------

    @Override
    protected void render() {
        KernelPanicAssets.drawBackground(g, width, height);

        g.save();
        if (!reducedFlashing && shakeTime > 0) {
            double m = shakeMag * (shakeTime > 0 ? 1 : 0);
            g.translate((rnd.nextDouble() - 0.5) * 2 * m, (rnd.nextDouble() - 0.5) * 2 * m);
        }
        drawLanes();
        drawKernelLine();
        for (Token t : tokens) drawToken(t);
        drawParticles();
        if (empActive && !reducedFlashing) drawShockwave();
        g.restore();

        if (comboCrack > 0 && !reducedFlashing) {
            KernelPanicAssets.drawScreenGlitch(g, width, height, 0.5 * (comboCrack / 0.5));
        }
        if (empTint > 0 && !reducedFlashing) {
            KernelPanicAssets.drawAstraInterference(g, width, height, 0.45 * (empTint / 0.5));
        }
        drawScanlines();
        drawVignette();
        KernelPanicAssets.drawCrtBezel(g, width, height);
        drawHud();
        if (state == State.GAME_OVER) drawGameOver();
    }

    private void drawLanes() {
        KernelPanicAssets.drawLaneDividers(g, LANES, laneW, height);
        for (int i = 0; i < LANES; i++) {
            double x = i * laneW;
            if (laneFlash[i] > 0) {
                if (reducedFlashing) {
                    g.setStroke(theme.accent());
                    g.setLineWidth(2);
                    g.strokeRect(x + 2, 24, laneW - 4, height - 54);
                } else {
                    g.setGlobalAlpha(0.5 * (laneFlash[i] / 0.18));
                    g.setFill(theme.accent());
                    g.fillRect(x, 0, laneW, height);
                    g.setGlobalAlpha(1);
                }
            }
            if (laneError[i] > 0) {
                if (reducedFlashing) {
                    g.setStroke(theme.danger());
                    g.setLineWidth(3);
                    g.strokeRect(x + 3, 25, laneW - 6, height - 56);
                } else {
                    g.setGlobalAlpha(0.5 * (laneError[i] / 0.22));
                    g.setFill(theme.danger());
                    g.fillRect(x, 0, laneW, height);
                    g.setGlobalAlpha(1);
                }
            }
            g.setStroke(theme.dim());
            g.setLineWidth(1);
            g.strokeLine(x, 0, x, height);
            // lane key label
            g.setFill(pendingLane == i ? theme.glow() : theme.dim());
            g.setFont(mono(14, true));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.TOP);
            g.fillText(laneKey(i), x + laneW / 2.0, 6);
            if (pendingLane == i) {
                g.setStroke(theme.glow());
                g.setLineWidth(2);
                g.strokeRect(x + 2, 24, laneW - 4, height - 54);
            }
        }
    }

    private void drawKernelLine() {
        double pulse = reducedFlashing ? 0.5 : 0.5 + 0.5 * Math.sin(time * 6);
        g.setStroke(theme.danger().deriveColor(0, 1, 1, 0.4 + 0.4 * pulse));
        g.setLineWidth(2);
        g.strokeLine(0, kernelY, width, kernelY);
        g.setFill(theme.danger().deriveColor(0, 1, 1, 0.6));
        g.setFont(mono(9, false));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText("// KERNEL", 4, kernelY - 1);
    }

    private void drawToken(Token t) {
        double size = t.boss ? BOSS : TOKEN;
        Color base = t.boss ? theme.glow() : fixColor(t.type);

        // phosphor trail (last two positions, faint)
        drawTokenGhost(t.x, t.prevY2, size, base, 0.12);
        drawTokenGhost(t.x, t.prevY, size, base, 0.30);

        double a = 1.0;
        Color fill = base;
        if (t.dying) {                                  // EMP white-flash → fade
            double elapsed = EMP_FREEZE - empTimer;
            double local = elapsed - t.dieDelay;
            if (local < 0) {
                fill = reducedFlashing ? base : theme.glow();
                a = 1.0;
            } else {
                a = clamp01(1 - local / (EMP_FREEZE - t.dieDelay + 0.0001));
                fill = reducedFlashing ? base : theme.glow();
            }
        }

        double jitterX = !reducedFlashing && t.type == Fix.INCR
                ? (rnd.nextDouble() - 0.5) * 3 : 0;                                // ++ jitters
        if (!reducedFlashing && t.type == Fix.BANG && ((int) (time * 16) % 2 == 0)) {
            a *= 0.5;                                                              // ! flickers
        }

        double left = t.x - size / 2 + jitterX, top = t.y - size / 2;
        if (t.type == Fix.EQ && !t.dying) {                                        // == red glow
            g.setGlobalAlpha(0.35 * a);
            g.setFill(theme.danger());
            g.fillRect(left - 4, top - 4, size + 8, size + 8);
        }
        g.setGlobalAlpha(a);
        g.setFill(theme.background().deriveColor(0, 1, 1.6, 1));   // slightly lit body
        g.fillRect(left, top, size, size);
        g.setStroke(fill);
        g.setLineWidth(2);
        g.strokeRect(left, top, size, size);

        g.setFill(fill);
        g.setFont(mono(t.boss ? 30 : 16, true));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(t.boss ? "∞" : t.type.glyph, t.x + jitterX, t.y);
        g.setGlobalAlpha(1);

        if (t.boss && !t.dying) drawBossInfo(t, top);
    }

    private void drawTokenGhost(double cx, double cy, double size, Color c, double alpha) {
        if (cy == 0) return;
        g.setGlobalAlpha(alpha);
        g.setStroke(c);
        g.setLineWidth(1);
        g.strokeRect(cx - size / 2, cy - size / 2, size, size);
        g.setGlobalAlpha(1);
    }

    private void drawBossInfo(Token b, double top) {
        // HP dots
        double dot = 6, gap = 4, totalW = b.maxHp * dot + (b.maxHp - 1) * gap;
        double startX = b.x - totalW / 2;
        for (int i = 0; i < b.maxHp; i++) {
            g.setFill(i < b.hp ? theme.danger() : theme.dim());
            g.fillOval(startX + i * (dot + gap), top - 12, dot, dot);
        }
        // current required fix hint
        g.setFill(fixColor(b.type));
        g.setFont(mono(11, true));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText("FIX: " + b.type.glyph + " [" + b.type.key + "]", b.x, top - 16);
    }

    private void drawParticles() {
        for (Particle p : particles) {
            g.setGlobalAlpha(clamp01(p.life / p.maxLife));
            g.setFill(p.color);
            g.fillRect(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size);
        }
        g.setGlobalAlpha(1);
    }

    private void drawShockwave() {
        double a = clamp01(empTimer / EMP_FREEZE);
        g.setStroke(theme.accent().deriveColor(0, 1, 1, 0.8 * a));
        g.setLineWidth(4);
        g.strokeOval(width / 2.0 - empShock, height / 2.0 - empShock, empShock * 2, empShock * 2);
        g.setStroke(theme.glow().deriveColor(0, 1, 1, 0.5 * a));
        g.setLineWidth(2);
        double r2 = empShock * 0.7;
        g.strokeOval(width / 2.0 - r2, height / 2.0 - r2, r2 * 2, r2 * 2);
    }

    private void drawScanlines() {
        g.setFill(Color.rgb(0, 0, 0, 0.10));
        for (int y = 0; y < height; y += 4) g.fillRect(0, y, width, 1);
    }

    private void drawVignette() {
        RadialGradient vg = new RadialGradient(
                0, 0, width / 2.0, height / 2.0, Math.hypot(width, height) / 2.0,
                false, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.rgb(0, 0, 0, 0)),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.5)));
        g.setFill(vg);
        g.fillRect(0, 0, width, height);
    }

    // ----- HUD ----------------------------------------------------------------

    private void drawHud() {
        // top bar
        g.setFill(theme.foreground());
        g.setFont(mono(14, true));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.fillText("SCORE " + score, 6, 22);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(bossWave ? theme.danger() : theme.foreground());
        g.fillText(bossWave ? "BOSS WAVE" : "WAVE " + wave, width / 2.0, 22);

        // lives as hearts
        g.setTextAlign(TextAlignment.RIGHT);
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < START_LIVES; i++) hearts.append(i < lives ? "♥ " : "· ");
        g.setFill(theme.danger());
        g.fillText(hearts.toString().trim(), width - 6, 22);

        // combo (centre, pulsing); cracks to x1 on EMP
        double scale = reducedFlashing ? 1 : 1 + comboPulse * 1.6;
        Color comboColor = comboCrack > 0 ? theme.danger() : theme.accent();
        double jx = !reducedFlashing && comboCrack > 0 ? (rnd.nextDouble() - 0.5) * 4 : 0;
        g.setFill(comboColor);
        g.setFont(mono((int) (28 * scale), true));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText("x" + combo, width / 2.0 + jx, 52);

        // dependency / assist hint
        g.setFont(mono(10, false));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.BOTTOM);
        g.setFill(dependencyUsed > 0 ? theme.warning() : theme.dim());
        g.fillText("ASTRA ASSIST [SPACE]   used:" + dependencyUsed, 6, height - 4);

        // fix legend (right side, above kernel line)
        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(mono(10, true));
        double ly = height - 4;
        for (int i = Fix.values().length - 1; i >= 0; i--) {
            Fix f = Fix.values()[i];
            g.setFill(fixColor(f));
            g.fillText(f.glyph + "=" + f.key, width - 6, ly);
            ly -= 13;
        }
    }

    private void drawGameOver() {
        KernelPanicAssets.drawGameOver(g, width, height);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(theme.danger());
        g.setFont(mono(34, true));
        g.setTextBaseline(VPos.CENTER);
        g.fillText("SYSTEM HALTED", width / 2.0, 120);

        g.setFont(mono(15, false));
        g.setFill(theme.foreground());
        double y = 200;
        g.fillText("SCORE        " + score, width / 2.0, y); y += 26;
        g.fillText("BEST COMBO   x" + bestCombo, width / 2.0, y); y += 26;
        g.fillText("WAVE REACHED " + highestWave, width / 2.0, y); y += 40;

        // assisted / unassisted stamp
        boolean assisted = assistedThisRun;
        g.setFont(mono(20, true));
        g.setFill(assisted ? theme.danger() : theme.accent());
        g.fillText(assisted ? "[ ASSISTED ]" : "[ UNASSISTED ]", width / 2.0, y); y += 44;

        // persisted best for the chase
        g.setFont(mono(13, false));
        g.setFill(newBestThisRun ? theme.warning() : theme.dim());
        g.fillText(newBestThisRun ? "*** NEW BEST ***" : "BEST  " + persistedBest.score()
                + "   COMBO x" + persistedBest.combo(), width / 2.0, y);
        y += 30;
        if (newBestThisRun) {
            g.setFill(theme.dim());
            g.fillText("prev " + Math.max(0, persistedBest.score()) , width / 2.0, y);
        }

        g.setFont(mono(13, true));
        g.setFill(theme.foreground());
        g.fillText("PRESS ENTER TO REBOOT", width / 2.0, height - 70);
        g.setFill(theme.dim());
        g.fillText("PRESS ESC TO EXIT", width / 2.0, height - 46);
    }

    // ----- helpers ------------------------------------------------------------

    private double laneCenterX(int lane) { return lane * laneW + laneW / 2.0; }

    private String laneKey(int lane) { return switch (lane) { case 0 -> "Q"; case 1 -> "W"; case 2 -> "E"; default -> "R"; }; }

    private Color fixColor(Fix f) {
        return switch (f) {
            case SEMI -> theme.foreground();
            case EQ   -> theme.danger();
            case BANG -> theme.warning();
            case INCR -> theme.accent();
        };
    }

    private Font mono(int size, boolean bold) {
        return Font.font("Monospaced", bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
    }

    private static double clamp01(double v) { return v < 0 ? 0 : Math.min(v, 1); }
}
