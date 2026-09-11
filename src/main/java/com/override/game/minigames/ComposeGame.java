package com.override.game.minigames;

import javafx.geometry.VPos;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Compose is a timed argument-ordering exercise for Silent Classroom.
 * Players arrange educational reasoning blocks into a defensible sequence,
 * learning the role of claims, evidence, reasoning, counterclaims, and source
 * checks while they play.
 */
public final class ComposeGame extends MiniGame {

    public static final String GAME_TYPE = "compose";
    public static final double TIME_LIMIT_SECONDS = 75;

    private static final double BLOCK_TOP = 112;
    private static final double BLOCK_HEIGHT = 92;
    private static final double BLOCK_GAP = 8;
    private static final double BLOCK_X = 36;
    private static final double BLOCK_WIDTH = 688;

    private record Block(String role, String text) { }
    private record Puzzle(String prompt, List<Block> answer) { }

    private static final List<Puzzle> PUZZLES = List.of(
            new Puzzle(
                    "Build an argument for independent thinking before AI assistance.",
                    List.of(
                            new Block("CLAIM", "Students should attempt a problem independently before asking AI."),
                            new Block("EVIDENCE", "Active recall strengthens memory more than simply rereading an answer."),
                            new Block("REASONING", "Making an attempt reveals knowledge gaps and makes feedback meaningful."),
                            new Block("CONCLUSION", "Use AI after the first attempt, as feedback rather than a substitute."))),
            new Puzzle(
                    "Answer a counterargument about AI-written assignments.",
                    List.of(
                            new Block("COUNTERCLAIM", "AI drafting saves time, so students should let it write every first draft."),
                            new Block("CONCESSION", "AI can quickly suggest structure and help a student get unstuck."),
                            new Block("REBUTTAL", "But replacing the draft removes the practice that develops the student's voice."),
                            new Block("CONCLUSION", "Ask AI for critique after writing, not for a finished submission."))),
            new Puzzle(
                    "Decide whether an AI-generated factual claim is trustworthy.",
                    List.of(
                            new Block("QUESTION", "Identify the exact claim and what evidence would verify it."),
                            new Block("SOURCE", "Find a relevant primary or authoritative source independent of the AI answer."),
                            new Block("COMPARE", "Check whether the source supports the claim in its original context."),
                            new Block("DECIDE", "Accept, revise, or reject the claim and cite the evidence used.")))
    );

    private final Random random = new Random();
    private final List<Block> order = new ArrayList<>();

    private int puzzleIndex;
    private int selected;
    private int completedPuzzles;
    private int incorrectSubmissions;
    private boolean grabbed;
    private double remaining;
    private double errorFlash;
    private double successFlash;
    private double assistFlash;

    public ComposeGame() {
        super(38, 30, 20, MiniGameTheme.nokia()); // 760 x 600

        canvas.setOnMouseClicked(e -> {
            int index = (int) ((e.getY() - BLOCK_TOP) / (BLOCK_HEIGHT + BLOCK_GAP));
            if (index >= 0 && index < order.size()) {
                selected = index;
                grabbed = false;
                canvas.requestFocus();
            }
        });
    }

    @Override
    protected void init() {
        puzzleIndex = 0;
        selected = 0;
        completedPuzzles = 0;
        incorrectSubmissions = 0;
        grabbed = false;
        remaining = TIME_LIMIT_SECONDS;
        errorFlash = successFlash = assistFlash = 0;
        loadPuzzle();
    }

    @Override
    protected void update(double dt) {
        remaining = Math.max(0, remaining - dt);
        errorFlash = Math.max(0, errorFlash - dt);
        successFlash = Math.max(0, successFlash - dt);
        assistFlash = Math.max(0, assistFlash - dt);

        if (remaining <= 0) finish(false, rawScore(), 0, 0);
    }

    @Override
    protected void onKey(KeyCode code) {
        switch (code) {
            case UP, W -> moveCursor(-1);
            case DOWN, S -> moveCursor(1);
            case SPACE -> grabbed = !grabbed;
            case ENTER -> submitOrder();
            case A -> useAstraAssist();
            case ESCAPE -> finish(false, rawScore(), 0, 0);
            default -> { }
        }
    }

    private void moveCursor(int delta) {
        if (order.isEmpty()) return;
        int next = Math.floorMod(selected + delta, order.size());
        if (grabbed) Collections.swap(order, selected, next);
        selected = next;
    }

    private void submitOrder() {
        grabbed = false;
        if (!order.equals(currentPuzzle().answer())) {
            incorrectSubmissions++;
            errorFlash = 0.45;
            ChiptuneSfx.wrongFix();
            return;
        }

        completedPuzzles++;
        successFlash = 0.35;
        ChiptuneSfx.wave();
        if (completedPuzzles >= PUZZLES.size()) {
            finishSuccess();
            return;
        }

        puzzleIndex++;
        loadPuzzle();
    }

    private void useAstraAssist() {
        List<Block> answer = currentPuzzle().answer();
        for (int i = 0; i < answer.size(); i++) {
            if (order.get(i).equals(answer.get(i))) continue;

            Block desired = answer.get(i);
            int from = order.indexOf(desired);
            order.remove(from);
            order.add(i, desired);
            selected = i;
            grabbed = false;
            dependencyUsed++;
            assistFlash = 0.8;
            ChiptuneSfx.emp();
            return;
        }
    }

    private void loadPuzzle() {
        order.clear();
        order.addAll(currentPuzzle().answer());
        do {
            Collections.shuffle(order, random);
        } while (order.equals(currentPuzzle().answer()));
        selected = 0;
        grabbed = false;
    }

    private Puzzle currentPuzzle() {
        return PUZZLES.get(puzzleIndex);
    }

    private void finishSuccess() {
        double performance = 1.0 - incorrectSubmissions / 6.0;
        double speed = remaining / TIME_LIMIT_SECONDS;
        int chapterPoints = MiniGameResult.calculateChapterPoints(
                true, performance, speed, dependencyUsed);
        int score = rawScore();
        int xp = Math.max(0, chapterPoints / 10 - dependencyUsed * 5);
        finish(true, score, xp, chapterPoints);
    }

    private int rawScore() {
        int progress = completedPuzzles * 300;
        int timeBonus = completedPuzzles == PUZZLES.size()
                ? (int) Math.ceil(remaining) * 10
                : 0;
        return Math.max(0, progress + timeBonus - incorrectSubmissions * 25);
    }

    @Override
    protected void render() {
        drawBackground();
        drawHeader();
        drawBlocks();
        drawFooter();
        drawFeedback();
    }

    private void drawBackground() {
        clearScreen();
        g.setStroke(theme.dim().deriveColor(0, 1, 1, 0.25));
        g.setLineWidth(1);
        for (int x = 0; x < width; x += 20) g.strokeLine(x, 0, x, height);
        for (int y = 0; y < height; y += 20) g.strokeLine(0, y, width, y);
    }

    private void drawHeader() {
        g.setFill(theme.background().deriveColor(0, 1, 1.15, 0.96));
        g.fillRect(0, 0, width, 104);
        g.setStroke(theme.dim());
        g.strokeLine(0, 104, width, 104);

        g.setTextBaseline(VPos.CENTER);
        g.setFont(mono(22, true));
        g.setTextAlign(TextAlignment.LEFT);
        g.setFill(theme.glow());
        g.fillText("COMPOSE // ARGUMENT BUILDER", 20, 25);

        g.setFont(mono(14, true));
        g.setFill(theme.foreground());
        g.fillText("ROUND " + (puzzleIndex + 1) + "/" + PUZZLES.size(), 20, 56);

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFill(remaining <= 15 ? theme.danger() : theme.warning());
        g.fillText(String.format("TIME %02d", (int) Math.ceil(remaining)), width - 20, 56);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(theme.foreground());
        g.setFont(mono(12, false));
        g.fillText(currentPuzzle().prompt(), width / 2.0, 84, width - 40);
    }

    private void drawBlocks() {
        for (int i = 0; i < order.size(); i++) {
            Block block = order.get(i);
            double y = BLOCK_TOP + i * (BLOCK_HEIGHT + BLOCK_GAP);
            boolean active = i == selected;

            g.setFill(active
                    ? theme.dim().deriveColor(0, 1, 1.35, 0.92)
                    : theme.background().deriveColor(0, 1, 1.35, 0.90));
            g.fillRoundRect(BLOCK_X, y, BLOCK_WIDTH, BLOCK_HEIGHT, 9, 9);
            g.setStroke(active ? (grabbed ? theme.warning() : theme.accent()) : theme.dim());
            g.setLineWidth(active ? 3 : 1);
            g.strokeRoundRect(BLOCK_X, y, BLOCK_WIDTH, BLOCK_HEIGHT, 9, 9);

            g.setTextBaseline(VPos.TOP);
            g.setTextAlign(TextAlignment.LEFT);
            g.setFont(mono(13, true));
            g.setFill(active ? theme.glow() : theme.accent());
            g.fillText((i + 1) + "  " + block.role(), BLOCK_X + 16, y + 11);

            g.setFont(mono(12, false));
            g.setFill(theme.foreground());
            drawWrapped(block.text(), BLOCK_X + 16, y + 37, 88, 17);

            if (active) {
                g.setTextAlign(TextAlignment.RIGHT);
                g.setFont(mono(11, true));
                g.setFill(grabbed ? theme.warning() : theme.dim());
                g.fillText(grabbed ? "MOVING" : "SELECTED", BLOCK_X + BLOCK_WIDTH - 14, y + 13);
            }
        }
    }

    private void drawFooter() {
        g.setFill(theme.background().deriveColor(0, 1, 1.15, 0.97));
        g.fillRect(0, height - 62, width, 62);
        g.setStroke(theme.dim());
        g.strokeLine(0, height - 62, width, height - 62);

        g.setFont(mono(11, true));
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(theme.foreground());
        g.fillText("UP/DOWN: SELECT   SPACE: PICK/DROP   ENTER: SUBMIT",
                width / 2.0, height - 39);
        g.setFill(dependencyUsed > 0 ? theme.warning() : theme.dim());
        g.fillText("A: ASTRA ASSIST (PLACES ONE BLOCK)   ESC: EXIT",
                width / 2.0, height - 17);
    }

    private void drawFeedback() {
        if (errorFlash > 0) drawOverlay(theme.danger(), "ORDER DOES NOT FOLLOW THE ARGUMENT YET");
        else if (assistFlash > 0) drawOverlay(theme.warning(), "ASTRA PLACED ONE BLOCK // ASSIST RECORDED");
        else if (successFlash > 0) drawOverlay(theme.accent(), "ARGUMENT VERIFIED");
    }

    private void drawOverlay(Color color, String message) {
        g.setGlobalAlpha(0.88);
        g.setFill(theme.background());
        g.fillRoundRect(115, height / 2.0 - 31, width - 230, 62, 10, 10);
        g.setStroke(color);
        g.setLineWidth(3);
        g.strokeRoundRect(115, height / 2.0 - 31, width - 230, 62, 10, 10);
        g.setGlobalAlpha(1);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(mono(13, true));
        g.setFill(color);
        g.fillText(message, width / 2.0, height / 2.0);
    }

    private void drawWrapped(String text, double x, double y, int maxChars, double lineHeight) {
        StringBuilder line = new StringBuilder();
        int lineNo = 0;
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > maxChars) {
                g.fillText(line.toString(), x, y + lineNo++ * lineHeight, BLOCK_WIDTH - 32);
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) g.fillText(line.toString(), x, y + lineNo * lineHeight, BLOCK_WIDTH - 32);
    }

    private Font mono(int size, boolean bold) {
        return Font.font("Monospaced", bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
    }
}
