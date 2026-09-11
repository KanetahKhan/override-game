package com.override.game.minigames;

import javafx.geometry.VPos;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * "Syntax Snake" — Chapter 1's second signature mini-game (after Kernel Panic).
 *
 * <p>A retheme of classic Snake as a classroom terminal: the snake is a blinking
 * <b>cursor</b>, "food" is scattered <b>knowledge bits</b>. Eating bits ramps the
 * speed smoothly toward a cap so the run stays playable while pushing pressure.
 *
 * <p>The chapter beat is <b>cleared</b> the first time the score hits
 * {@link #STORY_GATE_TARGET}, but the run continues so the player keeps chasing
 * a high score. Game over is instant-retry on ENTER, exit on ESC.
 *
 * <p>SPACE toggles the <b>Astra Assist</b> autopilot — the cursor greedily,
 * smoothly auto-steers toward the nearest knowledge bit. It feels relaxing to
 * switch on (no jerk, no takeover sensation: it reuses the same one-direction-
 * per-step input pipeline the player uses). The cost is the
 * <em>Independence Bonus</em>: any use during a run sets it to zero and
 * increments {@code dependencyUsed} once.
 *
 * <p>Built entirely on {@link MiniGame}; the playfield is a 32×24 grid of 20px
 * cells (640×480). The first thing {@link #render()} does each frame is an
 * <b>opaque</b> full-canvas fill, so the play area never reads as transparent.
 */
public final class SnakeGame extends MiniGame {

    public static final String GAME_TYPE = "syntax-snake";

    // ----- Tunables -----------------------------------------------------------
    private static final int START_LEN = 4;
    private static final double START_STEP = 0.16;     // seconds per cell at start
    private static final double MIN_STEP = 0.055;      // cap at hard mode
    private static final double STEP_DECAY = 0.0035;   // per bit eaten
    private static final int SCORE_PER_BIT = 10;
    private static final int STORY_GATE_TARGET = 300;  // ~30 bits to clear the beat
    private static final int INDEPENDENCE_SCORE_BONUS = 500;
    private static final int INDEPENDENCE_XP_BONUS = 50;
    private static final int NUM_BITS = 4;             // bits on the board at once
    private static final double MISSION_FAST_SECONDS = 90;
    private static final double MISSION_SLOW_SECONDS = 180;

    // ----- Models -------------------------------------------------------------
    private enum Dir {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);
        final int dx, dy;
        Dir(int dx, int dy) { this.dx = dx; this.dy = dy; }
        boolean opposes(Dir o) { return dx == -o.dx && dy == -o.dy; }
    }

    private static final class Bit {
        int x, y;
        double phase;        // for the subtle pulse animation
        Bit(int x, int y, double phase) { this.x = x; this.y = y; this.phase = phase; }
    }

    private static final class FloatText {
        double x, y, life, maxLife;
        String text;
        Color color;
    }

    private enum State { PLAYING, GAME_OVER }

    // ----- Run state ----------------------------------------------------------
    private State state;
    private final Deque<int[]> body = new ArrayDeque<>();   // head = first
    private final Set<Long> bodySet = new HashSet<>();
    private final List<Bit> bits = new ArrayList<>();
    private final List<FloatText> floats = new ArrayList<>();

    private Dir dir, queuedDir;
    private double step, stepAccum;
    private int score, bitsEaten;

    private boolean assistOn;
    private boolean assistedThisRun;       // never cleared until reset
    private boolean storyGateReachedRun;   // true if THIS run hit the gate
    private boolean storyGateReachedEver;  // sticky across retries this session
    private final boolean missionMode;
    private final boolean reducedFlashing;

    // juice
    private double shakeTime, shakeMag, flash, headBlink;
    private final Random rnd = new Random();

    // persistence
    private final SnakeHighScoreClient highScore = new SnakeHighScoreClient();
    private SnakeHighScoreClient.Best persistedBest = SnakeHighScoreClient.Best.ZERO;
    private boolean newBestThisRun;

    public SnakeGame() {
        this(false, false);
    }

    /**
     * @param missionMode when true, reaching 300 base score immediately returns
     *                    a successful Silent Classroom result
     */
    public SnakeGame(boolean missionMode) {
        this(missionMode, false);
    }

    /**
     * @param missionMode when true, reaching 300 base score immediately returns
     *                    a successful Silent Classroom result
     * @param reducedFlashing when true, removes flashes, shake, blinking, and
     *                        pulsing while keeping all gameplay cues visible
     */
    public SnakeGame(boolean missionMode, boolean reducedFlashing) {
        super(32, 24, 20, MiniGameTheme.nokia());   // 640 × 480
        this.missionMode = missionMode;
        this.reducedFlashing = reducedFlashing;
    }

    public static SnakeGame mission() {
        return new SnakeGame(true);
    }

    // ----- Lifecycle ----------------------------------------------------------

    @Override
    protected void init() {
        SnakeAssets.preload();
        persistedBest = highScore.loadBest();
        highScore.refreshFromBackendAsync();
        resetRun();
    }

    private void resetRun() {
        body.clear();
        bodySet.clear();
        bits.clear();
        floats.clear();

        // Lay the snake out so the head is on the right and the body trails to
        // the left, matching the initial RIGHT direction. Adding to the back
        // (head=false) leaves the head fixed at (cx, cy) and grows the tail
        // leftwards — so the first step walks into empty space, not the body.
        int cx = cols / 2, cy = rows / 2;
        addSegment(cx, cy, true);                                        // head
        for (int i = 1; i < START_LEN; i++) addSegment(cx - i, cy, false); // body to the left
        dir = queuedDir = Dir.RIGHT;
        step = START_STEP;
        stepAccum = 0;
        score = 0;
        bitsEaten = 0;

        assistOn = false;
        assistedThisRun = false;
        storyGateReachedRun = false;
        newBestThisRun = false;

        shakeTime = shakeMag = flash = 0;
        headBlink = 0;

        for (int i = 0; i < NUM_BITS; i++) spawnBit();
        state = State.PLAYING;
    }

    // ----- Update -------------------------------------------------------------

    @Override
    protected void update(double dt) {
        decayJuice(dt);
        updateFloats(dt);
        headBlink = (headBlink + dt) % 1.0;

        if (state == State.GAME_OVER) return;

        // Astra Assist re-aims the cursor *once per step*, exactly where the
        // player's input would go — so it feels smooth, never a takeover.
        if (assistOn) assistChooseDir();

        stepAccum += dt;
        // Use while-loop so a large dt (e.g. window drag) doesn't desync the sim;
        // MiniGame already clamps dt to 0.05, which keeps this cheap.
        while (stepAccum >= step && state == State.PLAYING) {
            stepAccum -= step;
            stepOnce();
        }
    }

    private void stepOnce() {
        // Commit the queued turn at the moment of stepping — guarantees no
        // double-180 from rapid input and gives the most responsive feel.
        if (queuedDir != null && !queuedDir.opposes(dir)) dir = queuedDir;

        int[] head = body.peekFirst();
        int nx = head[0] + dir.dx;
        int ny = head[1] + dir.dy;

        // Wall collision = death. (No wrap-around — keeps it tense.)
        if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) { die(); return; }

        // Self collision = death — but the tail tip is about to move out of the
        // way unless we just ate, so allow stepping onto it in that case.
        boolean willGrow = bitAt(nx, ny) != null;
        int[] tail = body.peekLast();
        long tailKey = key(tail[0], tail[1]);
        long nextKey = key(nx, ny);
        if (bodySet.contains(nextKey) && !(nextKey == tailKey && !willGrow)) {
            die(); return;
        }

        // Move: add new head; if we ate, leave the tail (grows by 1), else pop.
        addSegment(nx, ny, true);
        Bit eaten = bitAt(nx, ny);
        if (eaten != null) {
            consume(eaten);
        } else {
            int[] popped = body.pollLast();
            bodySet.remove(key(popped[0], popped[1]));
        }
    }

    private void consume(Bit eaten) {
        bits.remove(eaten);
        bitsEaten++;
        int gain = SCORE_PER_BIT;
        // Tiny multiplier for back-to-back eats? Keep it simple: flat +10. Pace
        // pressure comes from speed-up, not score math.
        score += gain;

        // smooth speed ramp, capped
        step = Math.max(MIN_STEP, step - STEP_DECAY);

        // juice
        flash = 0.10;
        shake(2.4, 0.10);
        spawnFloat(eaten.x, eaten.y, "+" + gain, theme.glow());
        onSfxEat();

        spawnBit();

        if (!storyGateReachedRun && score >= STORY_GATE_TARGET) {
            storyGateReachedRun = true;
            storyGateReachedEver = true;
            spawnFloat(cols / 2.0, rows / 2.0 - 1, "// BEAT CLEARED", theme.accent());
            if (missionMode) finishMissionSuccess();
        }
    }

    private void die() {
        if (state == State.GAME_OVER) return;
        state = State.GAME_OVER;
        shake(7, 0.32);
        flash = 0.18;
        newBestThisRun = score > persistedBest.score();
        persistedBest = highScore.submit(score);
        onSfxDie();
        if (missionMode) finish(false, score, 0, 0);
    }

    private void decayJuice(double dt) {
        shakeTime = Math.max(0, shakeTime - dt);
        flash = Math.max(0, flash - dt);
    }

    private void updateFloats(double dt) {
        for (Iterator<FloatText> it = floats.iterator(); it.hasNext(); ) {
            FloatText f = it.next();
            f.life -= dt;
            f.y -= dt * 1.6;   // float upward in cell units
            if (f.life <= 0) it.remove();
        }
    }

    // ----- Astra Assist (autopilot) ------------------------------------------

    /**
     * Greedy, single-step direction picker. Picks the axis with the bigger gap
     * to the nearest bit, falls back to the other axis if that move would die,
     * and finally picks any survivable direction. The chosen direction is
     * piped through the same {@link #queuedDir} the player uses, so the next
     * step commits it exactly like a human turn — no jerk, no teleporting.
     */
    private void assistChooseDir() {
        int[] head = body.peekFirst();
        Bit target = nearestBit(head[0], head[1]);
        if (target == null) return;

        int dx = Integer.compare(target.x, head[0]);
        int dy = Integer.compare(target.y, head[1]);
        Dir preferA = dx != 0 ? (dx > 0 ? Dir.RIGHT : Dir.LEFT)
                              : (dy > 0 ? Dir.DOWN  : Dir.UP);
        Dir preferB = dy != 0 && Math.abs(target.y - head[1]) >= Math.abs(target.x - head[0])
                              ? (dy > 0 ? Dir.DOWN : Dir.UP)
                              : (dx > 0 ? Dir.RIGHT : Dir.LEFT);

        Dir[] order = { preferA, preferB, Dir.UP, Dir.RIGHT, Dir.DOWN, Dir.LEFT };
        for (Dir d : order) {
            if (d == null || d.opposes(dir)) continue;
            if (survivable(head[0] + d.dx, head[1] + d.dy)) { queuedDir = d; return; }
        }
        // No safe move — keep current direction; the bump will end the run, which
        // is fine: assist is help, not invincibility.
    }

    private boolean survivable(int nx, int ny) {
        if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) return false;
        int[] tail = body.peekLast();
        long k = key(nx, ny);
        boolean willGrow = bitAt(nx, ny) != null;
        if (!bodySet.contains(k)) return true;
        return !willGrow && k == key(tail[0], tail[1]);
    }

    private Bit nearestBit(int x, int y) {
        Bit best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Bit b : bits) {
            int d = Math.abs(b.x - x) + Math.abs(b.y - y);
            if (d < bestDist) { bestDist = d; best = b; }
        }
        return best;
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
            case UP, W    -> turn(Dir.UP);
            case DOWN, S  -> turn(Dir.DOWN);
            case LEFT, A  -> turn(Dir.LEFT);
            case RIGHT, D -> turn(Dir.RIGHT);
            case SPACE    -> toggleAssist();
            case ESCAPE   -> finishRun();
            default       -> { }
        }
    }

    private void turn(Dir d) {
        // queue the turn; it commits on the next step. Ignore 180-deg reversals
        // (they would just self-collide and read as a control bug, not a death).
        if (!d.opposes(dir)) queuedDir = d;
    }

    private void toggleAssist() {
        assistOn = !assistOn;
        if (assistOn) {
            assistedThisRun = true;
            dependencyUsed++;          // every activation is one Astra Assist
            onSfxAssist();
        }
    }

    // ----- Finish -------------------------------------------------------------

    private void finishRun() {
        if (missionMode) {
            finish(false, score, 0, 0);
            return;
        }
        boolean won = storyGateReachedEver;
        int independenceScoreBonus = !assistedThisRun ? INDEPENDENCE_SCORE_BONUS : 0;
        int independenceXpBonus    = !assistedThisRun ? INDEPENDENCE_XP_BONUS    : 0;
        int totalScore = score + independenceScoreBonus;
        int xp = Math.max(0, totalScore / 4 + independenceXpBonus - dependencyUsed * 5);
        finish(won, totalScore, xp);
    }

    private void finishMissionSuccess() {
        newBestThisRun = score > persistedBest.score();
        persistedBest = highScore.submit(score);

        double performance = Math.min(1.0, score / (double) STORY_GATE_TARGET);
        double speed = (MISSION_SLOW_SECONDS - time)
                / (MISSION_SLOW_SECONDS - MISSION_FAST_SECONDS);
        int chapterPoints = MiniGameResult.calculateChapterPoints(
                true, performance, speed, dependencyUsed);
        int independenceXp = dependencyUsed == 0 ? INDEPENDENCE_XP_BONUS : 0;
        int xp = Math.max(0, score / 4 + independenceXp - dependencyUsed * 5);
        finish(true, score, xp, chapterPoints);
    }

    // ----- Bits ---------------------------------------------------------------

    private void spawnBit() {
        if (bits.size() >= NUM_BITS) return;
        // try a bounded number of times — the board has plenty of room at all
        // realistic lengths, but cap the attempts to be safe.
        for (int tries = 0; tries < 200; tries++) {
            int x = rnd.nextInt(cols);
            int y = rnd.nextInt(rows);
            if (bodySet.contains(key(x, y))) continue;
            if (bitAt(x, y) != null) continue;
            bits.add(new Bit(x, y, rnd.nextDouble() * Math.PI * 2));
            return;
        }
    }

    private Bit bitAt(int x, int y) {
        for (Bit b : bits) if (b.x == x && b.y == y) return b;
        return null;
    }

    private void addSegment(int x, int y, boolean head) {
        int[] s = { x, y };
        if (head) body.addFirst(s); else body.addLast(s);
        bodySet.add(key(x, y));
    }

    private static long key(int x, int y) { return (((long) x) << 32) ^ (y & 0xffffffffL); }

    // ----- Float popups -------------------------------------------------------

    private void spawnFloat(double cx, double cy, String text, Color color) {
        FloatText f = new FloatText();
        f.x = cx + 0.5;
        f.y = cy;
        f.text = text;
        f.color = color;
        f.maxLife = f.life = 0.7;
        floats.add(f);
    }

    private void shake(double mag, double t) {
        if (reducedFlashing) return;
        shakeMag = Math.max(shakeMag, mag);
        shakeTime = Math.max(shakeTime, t);
    }

    // ----- SFX hooks ----------------------------------------------------------
    // These are deliberately tiny so future audio passes can swap them out
    // without touching gameplay code. They never block: ChiptuneSfx itself
    // silently disables on the first failure (headless box / no audio device).

    private void onSfxEat()    { ChiptuneSfx.hit(1 + bitsEaten / 5); }
    private void onSfxDie()    { ChiptuneSfx.gameOver(); }
    private void onSfxAssist() { ChiptuneSfx.emp(); }

    // ----- Render -------------------------------------------------------------

    @Override
    protected void render() {
        SnakeAssets.drawBackground(g, width, height);

        g.save();
        if (!reducedFlashing && shakeTime > 0) {
            double m = shakeMag * (shakeTime > 0 ? 1 : 0);
            g.translate((rnd.nextDouble() - 0.5) * 2 * m, (rnd.nextDouble() - 0.5) * 2 * m);
        }
        drawBits();
        drawSnake();
        drawFloats();
        g.restore();

        if (flash > 0 && !reducedFlashing) {
            g.setGlobalAlpha(Math.min(0.55, flash * 4));
            g.setFill(theme.glow());
            g.fillRect(0, 0, width, height);
            g.setGlobalAlpha(1);
        }

        drawScanlines();
        drawHud();
        if (state == State.GAME_OVER) drawGameOver();
    }

    private void drawBits() {
        for (Bit b : bits) {
            double alpha = reducedFlashing ? 0.85 : 0.6 + 0.4 * Math.sin(time * 4 + b.phase);
            int variant = (int) (b.phase * 3) % 3;
            SnakeAssets.drawFood(g, b.x, b.y, cell, variant, alpha);
        }
    }

    private void drawSnake() {
        int i = 0;
        int n = body.size();
        for (int[] seg : body) {
            if (i == 0) {
                boolean blinkOn = reducedFlashing || headBlink < 0.55;
                SnakeAssets.drawHead(g, seg[0], seg[1], cell, blinkOn);
            } else {
                double t = (double) i / Math.max(1, n);
                double alpha = 1.0 - 0.55 * Math.min(1, 0.15 + 0.4 * t);
                SnakeAssets.drawBody(g, seg[0], seg[1], cell, alpha);
            }
            i++;
        }
    }

    private void drawFloats() {
        for (FloatText f : floats) {
            double a = Math.max(0, f.life / f.maxLife);
            g.setGlobalAlpha(a);
            g.setFill(f.color);
            g.setFont(mono(12, true));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(f.text, f.x * cell, f.y * cell);
            g.setGlobalAlpha(1);
        }
    }

    private void drawScanlines() {
        g.setFill(Color.rgb(0, 0, 0, 0.10));
        for (int y = 0; y < height; y += 4) g.fillRect(0, y, width, 1);
    }

    private void drawHud() {
        // HUD ribbon along the top (drawn in the play area; bg already opaque)
        g.setFill(theme.background().deriveColor(0, 1, 1, 0.7));
        g.fillRect(0, 0, width, 28);
        g.setStroke(theme.dim());
        g.strokeLine(0, 28, width, 28);

        g.setFont(mono(13, true));
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(theme.foreground());
        g.fillText("SCORE " + score, 8, 14);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(storyGateReachedRun || storyGateReachedEver ? theme.accent() : theme.dim());
        g.fillText("BEAT " + Math.min(score, STORY_GATE_TARGET) + "/" + STORY_GATE_TARGET,
                width / 2.0, 14);

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFill(theme.dim());
        g.fillText("BEST " + persistedBest.score(), width - 8, 14);

        // Bottom strip: assist indicator + controls hint
        g.setFill(theme.background().deriveColor(0, 1, 1, 0.7));
        g.fillRect(0, height - 22, width, 22);
        g.setStroke(theme.dim());
        g.strokeLine(0, height - 22, width, height - 22);

        g.setFont(mono(11, true));
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.LEFT);
        if (assistOn) {
            // pulse so it reads as ACTIVE without flashing the eye
            double pulse = reducedFlashing ? 1.0 : 0.6 + 0.4 * Math.sin(time * 6);
            g.setGlobalAlpha(pulse);
            g.setFill(theme.warning());
            g.fillText("[ ASTRA ASSIST ]", 8, height - 11);
            g.setGlobalAlpha(1);
        } else {
            g.setFill(assistedThisRun ? theme.warning() : theme.dim());
            g.fillText("SPACE: ASTRA ASSIST", 8, height - 11);
        }

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFill(theme.dim());
        g.fillText("ARROWS / WASD   ESC: EXIT", width - 8, height - 11);
    }

    private void drawGameOver() {
        SnakeAssets.drawGameOver(g, width, height);

        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);

        g.setFill(theme.danger());
        g.setFont(mono(28, true));
        g.fillText("SESSION TERMINATED", width / 2.0, 110);

        g.setFont(mono(14, false));
        g.setFill(theme.foreground());
        double y = 165;
        g.fillText("SCORE        " + score, width / 2.0, y); y += 22;
        g.fillText("BIT EATEN    " + bitsEaten, width / 2.0, y); y += 22;
        g.fillText("BEST         " + persistedBest.score(), width / 2.0, y); y += 30;

        g.setFont(mono(16, true));
        g.setFill(assistedThisRun ? theme.warning() : theme.accent());
        g.fillText(assistedThisRun ? "[ ASSISTED ]" : "[ INDEPENDENT ]", width / 2.0, y);
        y += 26;

        if (!assistedThisRun) {
            g.setFont(mono(12, false));
            g.setFill(theme.accent());
            g.fillText("+ INDEPENDENCE BONUS  +" + INDEPENDENCE_SCORE_BONUS
                    + " score   +" + INDEPENDENCE_XP_BONUS + " xp", width / 2.0, y);
        } else {
            g.setFont(mono(12, false));
            g.setFill(theme.dim());
            g.fillText("Independence Bonus forfeited (Assist used)", width / 2.0, y);
        }
        y += 30;

        if (newBestThisRun) {
            g.setFont(mono(13, true));
            g.setFill(theme.glow());
            g.fillText("*** NEW BEST ***", width / 2.0, y);
            y += 22;
        }

        g.setFont(mono(12, false));
        g.setFill(storyGateReachedEver ? theme.accent() : theme.dim());
        g.fillText(storyGateReachedEver ? "Chapter beat cleared" : "Chapter beat not yet cleared",
                width / 2.0, y);

        g.setFont(mono(13, true));
        g.setFill(theme.foreground());
        g.fillText("PRESS ENTER TO RETRY", width / 2.0, height - 60);
        g.setFill(theme.dim());
        g.setFont(mono(11, false));
        g.fillText("PRESS ESC TO EXIT", width / 2.0, height - 38);
    }

    // ----- helpers ------------------------------------------------------------

    private Font mono(int size, boolean bold) {
        return Font.font("Monospaced", bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
    }
}
