package com.override.chapter1;

import com.override.game.minigames.ChiptuneSfx;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * The three hacking-node mini-games of Curfew Protocol (Kernel Panic,
 * Circuit Breaker, Silent Code), ported from the original page. Each one is
 * a self-contained panel that reports a single {@link Outcome}.
 */
final class CurfewNodeGames {

    /** ASSISTED: Astra finished the node for the player. */
    enum Outcome { WON, FAILED, QUIT, ASSISTED }

    /** What letting Astra finish a node costs on the Dependency Meter. */
    static final int ASTRA_NODE_DEPENDENCY = 10;

    interface NodeGame {
        Parent view();
        void onKey(KeyCode key);
        void stop();
        /** Freeze the node's own clock while the chapter is paused. */
        default void setPaused(boolean paused) { }
    }

    static final String MONO = "'Consolas', 'Monaco', monospace";
    static final String BODY = "'Segoe UI', 'Inter', sans-serif";

    private static final Random RNG = new Random();

    private CurfewNodeGames() { }

    /* ---------------------------------------------------------- shared UI */

    static Label text(String s, String family, double size, String color) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: " + family + "; -fx-font-size: " + size + "px; -fx-text-fill: " + color + ";");
        return l;
    }

    private static Parent frame(String title, String accent, String bg, Label status, String help,
                                Node body, double width, Runnable quit, Runnable assist) {
        String[] heading = title.split(" // ", 2);
        Label eyebrow = text("OVERRIDE   /   MANUAL RECOVERY   /   " + heading[0], MONO, 11, accent);
        Label name = text(heading.length > 1 ? heading[1] : title, BODY, 28, "#eaf5f4");
        name.setStyle(name.getStyle() + " -fx-font-weight: bold;");
        status.setPadding(new Insets(7, 10, 7, 10));
        status.setStyle(status.getStyle() + " -fx-background-color: " + alpha(accent, 0.1)
            + "; -fx-background-radius: 5;");
        HBox titleRow = new HBox(16, name, spacer(), status);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox header = new VBox(10, eyebrow, titleRow);
        header.setPadding(new Insets(22, 24, 18, 24));
        header.setStyle("-fx-border-color: transparent transparent " + alpha(accent, 0.2) + " transparent;");

        Label desc = text(help, BODY, 14, "#aebfc7");
        desc.setWrapText(true);
        desc.setPadding(new Insets(14, 24, 12, 24));

        Button disconnect = footButton("DISCONNECT  [ESC]", "rgba(255,90,74,0.55)", "#ffb0a4");
        disconnect.setOnAction(e -> quit.run());
        // Astra never refuses: the easy way out is always on screen, with its price.
        Button astra = footButton("ASK ASTRA  [H]", "rgba(179,136,255,0.6)", "#d9c4ff");
        astra.setOnAction(e -> assist.run());
        HBox buttons = new HBox(10, astra, disconnect);
        HBox.setHgrow(astra, Priority.ALWAYS);
        HBox.setHgrow(disconnect, Priority.ALWAYS);
        Label price = text("Astra finishes the node for you: half the credits, +" + ASTRA_NODE_DEPENDENCY + " dependency.",
            MONO, 11, "rgba(217,196,255,0.7)");
        price.setWrapText(true);
        VBox foot = new VBox(8, buttons, price);
        foot.setPadding(new Insets(14, 20, 18, 20));

        VBox panel = new VBox(header, desc, body, foot);
        panel.setMaxWidth(width);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setStyle("-fx-background-color: linear-gradient(to bottom right, #172631, #0b121b);"
            + " -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: " + alpha(accent, 0.4) + ";"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 40, 0.15, 0, 18);");

        StackPane shade = new StackPane(panel);
        shade.setStyle("-fx-background-color: rgba(2,6,8,0.84);");
        return shade;
    }

    private static Button footButton(String label, String border, String ink) {
        Button b = new Button(label);
        b.setFocusTraversable(false);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: transparent; -fx-border-color: " + border + ";"
            + " -fx-text-fill: " + ink + "; -fx-font-family: " + MONO + "; -fx-font-size: 12px; -fx-padding: 11 0 11 0;"
            + " -fx-cursor: hand; -fx-background-radius: 0; -fx-border-radius: 0;");
        return b;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** "#rrggbb" plus alpha as a CSS rgba(). */
    static String alpha(String hex, double a) {
        int v = Integer.parseInt(hex.substring(1), 16);
        return "rgba(" + ((v >> 16) & 0xff) + "," + ((v >> 8) & 0xff) + "," + (v & 0xff) + "," + a + ")";
    }

    /* ======================================================= KERNEL PANIC */

    /** Glitch tokens fall through four lanes; patch 12 before 5 slip past. */
    static NodeGame kernelPanic(double difficulty, Consumer<Outcome> done) {
        return new NodeGame() {
            static final double W = 560, H = 300;
            final Pane field = new Pane();
            final Label status = text("", MONO, 13, "#ffb347");
            final HBox progress = new HBox(5);
            final List<double[]> tokens = new ArrayList<>();   // {lane, top}
            final List<Label> tokenViews = new ArrayList<>();
            int fixed, missed;
            double acc;
            boolean over;
            final Timeline tick = new Timeline(new KeyFrame(Duration.millis(60), e -> step()));
            final Parent view;

            {
                field.setPrefSize(W, H);
                field.setMinSize(W, H);
                field.setMaxSize(W, H);
                field.setStyle("-fx-background-color: #080f19; -fx-border-color: #324a5e; -fx-background-radius: 6;");
                Rectangle clip = new Rectangle(W, H);
                clip.setArcWidth(12); clip.setArcHeight(12);
                field.setClip(clip);
                Canvas tracks = new Canvas(W, H);
                GraphicsContext g = tracks.getGraphicsContext2D();
                g.setStroke(Color.web("#1c3042"));
                for (int y = 18; y < H; y += 24) g.strokeLine(0, y, W, y);
                for (int i = 0; i < 4; i++) {
                    g.setStroke(Color.web("#254555"));
                    g.strokeLine(i * W / 4 + W / 8, 0, i * W / 4 + W / 8, H);
                    g.setFill(Color.web("#6d899d"));
                    g.setFont(Font.font("Monospaced", 10));
                    g.fillText("BUS / 0" + (i + 1), i * W / 4 + 14, 20);
                }
                tracks.setMouseTransparent(true);
                field.getChildren().add(tracks);
                Region band = new Region();
                band.setLayoutY(H * 0.60);
                band.setPrefSize(W, H * 0.34);
                band.setStyle("-fx-background-color: rgba(53,224,216,0.09); -fx-border-color: rgba(53,224,216,0.45) transparent;");
                band.setMouseTransparent(true);
                field.getChildren().add(band);
                String[] keys = {"Q", "W", "E", "R"};
                for (int i = 0; i < 4; i++) {
                    final int lane = i;
                    StackPane l = new StackPane(text(keys[i], MONO, 15, "#35e0d8"));
                    l.getChildren().get(0).setStyle(l.getChildren().get(0).getStyle()
                        + " -fx-border-color: rgba(53,224,216,0.5); -fx-padding: 2 9 2 9;");
                    StackPane.setAlignment(l.getChildren().get(0), Pos.BOTTOM_CENTER);
                    l.setPadding(new Insets(0, 0, 6, 0));
                    l.setLayoutX(i * W / 4);
                    l.setPrefSize(W / 4, H);
                    l.setStyle("-fx-border-color: transparent " + (i < 3 ? "rgba(53,224,216,0.14)" : "transparent")
                        + " transparent transparent; -fx-border-style: dashed; -fx-cursor: hand;");
                    l.setOnMouseClicked(e -> hit(lane));
                    field.getChildren().add(l);
                }
                progress.setAlignment(Pos.CENTER);
                VBox body = new VBox(10, field, progress);
                body.setAlignment(Pos.CENTER);
                body.setPadding(new Insets(8, 20, 0, 20));
                view = frame("NODE 01 // KERNEL PANIC", "#35e0d8", "linear-gradient(to bottom, rgba(6,22,24,0.98), rgba(3,10,12,0.98))",
                    status, "Glitch tokens are falling through four kernel lanes. Hit the lane key (Q W E R or 1-4) — or click the lane — while a token is inside the patch band.",
                    body, 640, () -> finish(Outcome.QUIT), () -> finish(Outcome.ASSISTED));
                refresh();
                tick.setCycleCount(Animation.INDEFINITE);
                tick.play();
            }

            void step() {
                for (int i = tokens.size() - 1; i >= 0; i--) {
                    double[] t = tokens.get(i);
                    t[1] += 1.35 * difficulty;
                    if (t[1] > 100) { missed++; remove(i); ChiptuneSfx.breach(); }
                }
                acc += 60;
                if (acc > 620) {
                    acc = 0;
                    Label v = text(String.valueOf("0123456789ABCDEF".charAt(RNG.nextInt(16))), MONO, 15, "#ff3d5a");
                    v.setMouseTransparent(true);
                    v.setAlignment(Pos.CENTER);
                    v.setPrefSize(38, 38);
                    tokens.add(new double[] {RNG.nextInt(4), -4});
                    tokenViews.add(v);
                    field.getChildren().add(v);
                }
                refresh();
                if (missed >= 5) finish(Outcome.FAILED);
            }

            void hit(int lane) {
                if (over) return;
                int best = -1;
                for (int i = 0; i < tokens.size(); i++) {
                    double[] t = tokens.get(i);
                    if (t[0] == lane && t[1] > 60 && t[1] < 94 && (best < 0 || t[1] > tokens.get(best)[1])) best = i;
                }
                if (best < 0) return;
                remove(best);
                fixed++;
                ChiptuneSfx.hit(fixed);
                refresh();
                if (fixed >= 12) finish(Outcome.WON);
            }

            void remove(int i) {
                tokens.remove(i);
                field.getChildren().remove(tokenViews.remove(i));
            }

            void refresh() {
                status.setText(fixed + "/12 PATCHED · " + missed + "/5 LOST");
                progress.getChildren().clear();
                for (int n = 0; n < 12; n++) {
                    Rectangle segment = new Rectangle(40, 5, Color.web(n < fixed ? "#62e6bd" : "#26394b"));
                    segment.setArcWidth(4); segment.setArcHeight(4);
                    progress.getChildren().add(segment);
                }
                for (int i = 0; i < tokens.size(); i++) {
                    double[] t = tokens.get(i);
                    Label v = tokenViews.get(i);
                    boolean in = t[1] > 60 && t[1] < 94;
                    String col = in ? "#4dff9e" : "#ff3d5a";
                    v.setLayoutX(t[0] * W / 4 + W / 8 - 19);
                    v.setLayoutY(t[1] / 100 * H - 19);
                    v.setStyle("-fx-font-family: " + MONO + "; -fx-font-size: 15px; -fx-text-fill: " + col + ";"
                        + " -fx-border-color: " + col + "; -fx-background-color: #162536; -fx-background-radius: 6; -fx-border-radius: 6; -fx-font-weight: bold;"
                        + " -fx-effect: dropshadow(gaussian, " + col + ", 12, 0, 0, 0);");
                }
            }

            void finish(Outcome o) {
                if (over) return;
                over = true;
                tick.stop();
                done.accept(o);
            }

            @Override public Parent view() { return view; }

            @Override public void onKey(KeyCode k) {
                int lane = switch (k) {
                    case DIGIT1, NUMPAD1, Q -> 0;
                    case DIGIT2, NUMPAD2, W -> 1;
                    case DIGIT3, NUMPAD3, E -> 2;
                    case DIGIT4, NUMPAD4, R -> 3;
                    default -> -1;
                };
                if (lane >= 0) hit(lane);
                else if (k == KeyCode.H) finish(Outcome.ASSISTED);
            }

            @Override public void stop() { over = true; tick.stop(); }

            @Override public void setPaused(boolean p) {
                if (over) return;
                if (p) tick.pause(); else tick.play();
            }
        };
    }

    /* ==================================================== CIRCUIT BREAKER */

    /** Rotate tiles until the signal runs from the left tap to the right sink. */
    static NodeGame circuitBreaker(double timeScale, Consumer<Outcome> done) {
        return new NodeGame() {
            static final int N = 4;
            static final double TILE = 82;
            final boolean[] straight = new boolean[N * N];
            final int[] rot = new int[N * N];
            final StackPane[] tiles = new StackPane[N * N];
            final int time = (int) Math.round(60 * timeScale);
            final Label status = text("OPEN CIRCUIT", MONO, 13, "#ff6f9c");
            final Circle sink = new Circle(5);
            boolean solved, over;
            int cur, left = time;   // keyboard cursor; seconds before the node resets
            final PauseTransition win = new PauseTransition(Duration.millis(600));
            final Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickDown()));
            final Parent view;

            {
                generate();
                GridPane grid = new GridPane();
                grid.setHgap(6);
                grid.setVgap(6);
                for (int i = 0; i < N * N; i++) {
                    final int idx = i;
                    StackPane t = new StackPane();
                    t.setPrefSize(TILE, TILE);
                    t.setOnMouseClicked(e -> { cur = idx; rotate(idx); });
                    tiles[i] = t;
                    grid.add(t, i % N, i / N);
                }
                Circle tap = new Circle(5);
                tap.setStyle("-fx-fill: #ff3d7f; -fx-effect: dropshadow(gaussian, #ff3d7f, 12, 0, 0, 0);");
                // The path always enters the top-left tile and leaves the bottom-right one.
                VBox tapCol = new VBox(tap);
                tapCol.setPadding(new Insets(TILE / 2 - 5, 0, 0, 0));
                VBox sinkCol = new VBox(sink);
                sinkCol.setAlignment(Pos.BOTTOM_LEFT);
                sinkCol.setPadding(new Insets(0, 0, TILE / 2 - 5, 0));
                HBox body = new HBox(8, tapCol, grid, sinkCol);
                body.setAlignment(Pos.CENTER);
                body.setPadding(new Insets(14, 20, 6, 20));
                view = frame("NODE 02 // CIRCUIT BREAKER", "#ff3d7f", "linear-gradient(to bottom, rgba(24,6,16,0.98), rgba(6,3,8,0.98))",
status, "Click a tile (or move with the arrow keys and press SPACE) to rotate it. Route the signal "
                        + "from the left tap to the right sink before the node resets.",
                    body, 440, () -> finish(Outcome.QUIT), () -> finish(Outcome.ASSISTED));
                redraw();
                win.setOnFinished(e -> finish(Outcome.WON));
                countdown.setCycleCount(time);
                countdown.play();
            }

            void tickDown() {
                if (solved || over) return;
                left--;
                redraw();
                if (left <= 0) finish(Outcome.FAILED);
            }

            /** Same construction as the original: carve a monotone path, then scramble every tile. */
            void generate() {
                int[][] grid = new int[N * N][];
                int r = 0, c = 0, inSide = 3;
                while (true) {
                    boolean goRight = c == N - 1 ? false : (r == N - 1 || RNG.nextBoolean());
                    int out = (r == N - 1 && c == N - 1) ? 1 : (goRight ? 1 : 2);
                    grid[r * N + c] = new int[] {inSide, out};
                    if (r == N - 1 && c == N - 1) break;
                    if (out == 1) { c++; inSide = 3; } else { r++; inSide = 0; }
                }
                for (int i = 0; i < N * N; i++) {
                    int[] s = grid[i] != null ? grid[i] : (RNG.nextBoolean() ? new int[] {0, 2} : new int[] {0, 1});
                    boolean st = (s[0] + 2) % 4 == s[1];
                    int[] base = st ? new int[] {0, 2} : new int[] {0, 1};
                    int want = 0;
                    for (int k = 0; k < 4; k++) {
                        if (sameSet(new int[] {(base[0] + k) % 4, (base[1] + k) % 4}, s)) { want = k; break; }
                    }
                    straight[i] = st;
                    rot[i] = (want + 1 + RNG.nextInt(3)) % 4;
                }
            }

            boolean sameSet(int[] a, int[] b) {
                int[] x = a.clone(), y = b.clone();
                Arrays.sort(x);
                Arrays.sort(y);
                return Arrays.equals(x, y);
            }

            int[] conn(int i) {
                int[] base = straight[i] ? new int[] {0, 2} : new int[] {0, 1};
                return new int[] {(base[0] + rot[i]) % 4, (base[1] + rot[i]) % 4};
            }

            boolean has(int[] c, int d) { return c[0] == d || c[1] == d; }

            boolean[] powered() {
                boolean[] seen = new boolean[N * N];
                if (!has(conn(0), 3)) return seen;
                seen[0] = true;
                List<Integer> stack = new ArrayList<>(List.of(0));
                while (!stack.isEmpty()) {
                    int i = stack.remove(stack.size() - 1);
                    int r = i / N, c = i % N;
                    for (int d : conn(i)) {
                        int nr = r + (d == 2 ? 1 : d == 0 ? -1 : 0);
                        int nc = c + (d == 1 ? 1 : d == 3 ? -1 : 0);
                        if (nr < 0 || nc < 0 || nr >= N || nc >= N) continue;
                        int j = nr * N + nc;
                        if (seen[j] || !has(conn(j), (d + 2) % 4)) continue;
                        seen[j] = true;
                        stack.add(j);
                    }
                }
                return seen;
            }

            boolean isSolved() {
                return powered()[N * N - 1] && has(conn(N * N - 1), 1);
            }

            void rotate(int i) {
                if (solved || over) return;
                rot[i] = (rot[i] + 1) % 4;
                solved = isSolved();
                redraw();
                if (solved) win.play();
            }

            void redraw() {
                String ink = solved ? "#4dff9e" : "#ffcf83";
                boolean[] live = powered();
                for (int i = 0; i < N * N; i++) {
                    StackPane t = tiles[i];
boolean at = i == cur && !solved;
                    String liveCol = live[i] ? "#4dff9e" : "#345360";
                    String border = at ? "#ffd0e0" : liveCol;
                    t.setStyle("-fx-background-color: #152832; -fx-border-color: " + border
                        + "; -fx-background-radius: 6; -fx-border-radius: 6; -fx-cursor: hand;");
                    Canvas pcb = new Canvas(TILE, TILE);
                    pcb.setMouseTransparent(true);
                    GraphicsContext g = pcb.getGraphicsContext2D();
                    // Copper pads, substrate traces and four mounting screws.
                    g.setStroke(Color.web("#29434c"));
                    g.setLineWidth(1);
                    for (int n = 12; n < TILE; n += 14) {
                        g.strokeLine(n, 8, n, TILE - 8);
                        g.strokeLine(8, n, TILE - 8, n);
                    }
                    g.setFill(Color.web("#70818a"));
                    for (double x : new double[] {7, TILE - 7}) for (double y : new double[] {7, TILE - 7})
                        g.fillOval(x - 2, y - 2, 4, 4);
                    double mid = TILE / 2;
                    g.setLineCap(StrokeLineCap.ROUND);
                    for (int d : conn(i)) {
                        double x = d == 1 ? TILE : d == 3 ? 0 : mid;
                        double y = d == 2 ? TILE : d == 0 ? 0 : mid;
                        g.setStroke(Color.web("#070f18")); g.setLineWidth(13);
                        g.strokeLine(mid, mid, x, y);
                        g.setStroke(Color.web(live[i] ? "#62edb9" : "#b58c55")); g.setLineWidth(7);
                        g.strokeLine(mid, mid, x, y);
                        g.setStroke(Color.web(live[i] ? "#c5ffe4" : "#edc88c")); g.setLineWidth(2);
                        g.strokeLine(mid, mid, x, y);
                    }
                    g.setFill(Color.web("#09151e")); g.fillOval(mid - 9, mid - 9, 18, 18);
                    g.setStroke(Color.web(live[i] ? "#62edb9" : "#edc88c")); g.setLineWidth(2);
                    g.strokeOval(mid - 6, mid - 6, 12, 12);
                    t.getChildren().setAll(pcb);
                }
                status.setText(solved ? "SIGNAL LOCKED" : "OPEN CIRCUIT · " + clock(left));
                status.setStyle(status.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + ink + ";"));
                String sinkCol = solved ? "#4dff9e" : "rgba(255,61,127,0.35)";
                sink.setStyle("-fx-fill: " + sinkCol + "; -fx-effect: dropshadow(gaussian, " + sinkCol + ", 12, 0, 0, 0);");
            }

            void finish(Outcome o) {
                if (over) return;
                over = true;
                win.stop();
                countdown.stop();
                done.accept(o);
            }

            @Override public Parent view() { return view; }

            @Override public void onKey(KeyCode k) {
                switch (k) {
                    case LEFT, A -> cur = cur % N == 0 ? cur : cur - 1;
                    case RIGHT, D -> cur = cur % N == N - 1 ? cur : cur + 1;
                    case UP, W -> cur = cur < N ? cur : cur - N;
                    case DOWN, S -> cur = cur >= N * (N - 1) ? cur : cur + N;
                    case SPACE, ENTER -> { rotate(cur); return; }
                    case H -> { finish(Outcome.ASSISTED); return; }
                    default -> { return; }
                }
                redraw();
            }

            @Override public void stop() { over = true; win.stop(); countdown.stop(); }

            @Override public void setPaused(boolean p) {
                if (over) return;
                if (p) countdown.pause(); else countdown.play();
            }
        };
    }

    /* ======================================================== SILENT CODE */

    private static final String[] CODE_LINES = {
        "init curfew_daemon()",
        "auth.bypass(student_id)",
        "floor2.doors.unlock()",
        "sentinel.vision.dim(0.4)",
        "log.wipe(last_60s)",
        "return EXIT_GRANTED"
    };

    /** The lockdown routine was scrambled; swap lines until it runs in order. */
    static NodeGame silentCode(double timeScale, Consumer<Outcome> done) {
        return new NodeGame() {
            final List<Integer> order = new ArrayList<>();
            final VBox lines = new VBox(5);
            final int time = (int) Math.round(50 * timeScale);
            final Label status = text("0 SWAPS", MONO, 13, "#ffb347");
            int sel = -1, moves, cur, left = time;
            boolean solved, over;
            final PauseTransition win = new PauseTransition(Duration.millis(650));
            final Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickDown()));
            final Parent view;

            {
                for (int i = 0; i < CODE_LINES.length; i++) order.add(i);
                do Collections.shuffle(order, RNG); while (isSorted());
                lines.setPadding(new Insets(14, 20, 4, 20));
                view = frame("NODE 03 // SILENT CODE", "#4dff9e", "linear-gradient(to bottom, rgba(5,22,16,0.98), rgba(3,10,8,0.98))",
status, "The lockdown routine was scrambled. Click two lines (or use the arrow keys and SPACE) to swap "
                        + "them until the sequence runs clean, before the node resets.",
                    lines, 600, () -> finish(Outcome.QUIT), () -> finish(Outcome.ASSISTED));
                redraw();
                win.setOnFinished(e -> finish(Outcome.WON));
                countdown.setCycleCount(time);
                countdown.play();
            }

            void tickDown() {
                if (solved || over) return;
                left--;
                redraw();
                if (left <= 0) finish(Outcome.FAILED);
            }

            boolean isSorted() {
                for (int i = 0; i < order.size(); i++) if (order.get(i) != i) return false;
                return true;
            }

            void pick(int i) {
                if (solved || over) return;
                if (sel < 0) sel = i;
                else if (sel == i) sel = -1;
                else {
                    Collections.swap(order, i, sel);
                    sel = -1;
                    moves++;
                    solved = isSorted();
                    if (solved) win.play();
                }
                redraw();
            }

            void redraw() {
status.setText(solved ? "SEQUENCE VERIFIED" : moves + " SWAPS · " + clock(left));
                lines.getChildren().clear();
                for (int i = 0; i < order.size(); i++) {
                    final int idx = i;
                    Label num = text(String.format("%02d", i + 1), MONO, 13, "#718a9d");
                    num.setMinWidth(30);
                    String source = CODE_LINES[order.get(i)];
                    int cut = source.indexOf('(');
                    if (cut < 0) cut = source.indexOf(' ');
                    HBox code = new HBox(0,
                        text(source.substring(0, cut), MONO, 15, "#7ec9f5"),
                        text(source.substring(cut), MONO, 15, "#dfcca6"));
                    Label marker = text(solved ? "OK" : sel == i ? "SELECTED" : "SWAP", MONO, 10,
                        solved ? "#62e6bd" : "#849cae");
                    HBox row = new HBox(12, num, code, spacer(), marker);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(10, 14, 10, 14));
boolean on = sel == i, at = cur == i && !solved;
                    String bg = on ? "rgba(77,255,158,0.16)" : "#101d2a";
                    String edge = on ? "rgba(77,255,158,0.7)" : at ? "rgba(223,250,236,0.55)" : "#2b4051";
                    row.setStyle("-fx-background-color: " + bg + ";"
                        + " -fx-border-color: " + edge + "; -fx-background-radius: 5; -fx-border-radius: 5; -fx-cursor: hand;");
                    row.setOnMouseClicked(e -> { cur = idx; pick(idx); });
                    lines.getChildren().add(row);
                }
            }

            void finish(Outcome o) {
                if (over) return;
                over = true;
                win.stop();
                countdown.stop();
                done.accept(o);
            }

            @Override public Parent view() { return view; }

            @Override public void onKey(KeyCode k) {
                switch (k) {
                    case UP, W -> cur = Math.max(0, cur - 1);
                    case DOWN, S -> cur = Math.min(order.size() - 1, cur + 1);
                    case SPACE, ENTER -> { pick(cur); return; }
                    case H -> { finish(Outcome.ASSISTED); return; }
                    default -> { return; }
                }
                redraw();
            }

            @Override public void stop() { over = true; win.stop(); countdown.stop(); }

            @Override public void setPaused(boolean p) {
                if (over) return;
                if (p) countdown.pause(); else countdown.play();
            }
        };
    }

    static String clock(int s) { return s / 60 + ":" + String.format("%02d", s % 60); }
}
