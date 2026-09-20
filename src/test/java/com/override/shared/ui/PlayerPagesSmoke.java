package com.override.shared.ui;

import com.override.shared.service.PlayerProfiles;
import com.override.shared.service.ScoreArchive;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelFormat;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.imageio.ImageIO;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class PlayerPagesSmoke {
    public static void main(String[] args) throws Exception {
        System.setProperty("override.dataDir", Files.createTempDirectory("override-ui-test-").toString());
        System.setProperty("override.chapter2Dir", Files.createTempDirectory("override-harvest-test-").toString());
        Path output = Path.of("target/player-previews"); Files.createDirectories(output);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                Stage stage = new Stage(); int[] calls = {0};
                Parent login = new LoginScreen(() -> calls[0]++, () -> {}, () -> {}).build();
                show(stage, login); snapshot(login, output.resolve("login.png"));
                button(login, "login-submit").fire(); check(calls[0] == 0, "Invalid name blocked");
                ((TextField) login.lookup("#player-name")).setText("Ayesha");
                button(login, "login-submit").fire(); check(calls[0] == 1 && PlayerProfiles.name().equals("Ayesha"), "Login enters profile");
                Parent empty = new ScoreboardScreen(() -> {}).build(); show(stage, empty);
                check(((TableView<?>) empty.lookup("#run-history")).getItems().isEmpty(), "Empty history");
                snapshot(empty, output.resolve("scoreboard-empty.png"));
                String[] names = {"Ayesha", "Kanetah", "Samira", "Rafi", "Noor", "Zayan", "Maya"};
                for (int i = 0; i < names.length; i++) {
                    PlayerProfiles.login(names[i]);
                    ScoreArchive.record("fixture", ScoreArchive.Mode.CAMPAIGN, 96 - i * 5, "RESISTANCE", false);
                }
                PlayerProfiles.login("Ayesha");
                Parent board = new ScoreboardScreen(() -> {}).build(); show(stage, board);
                TableView<?> table = (TableView<?>) board.lookup("#run-history");
                check(table.getItems().size() == 6, "First history page");
                button(board, "history-next").fire(); check(table.getItems().size() == 1, "Next history page");
                ((TextField) board.lookup("#history-search")).setText("noor");
                check(table.getItems().size() == 1, "Search all pages");
                ((TextField) board.lookup("#history-search")).clear();
                snapshot(board, output.resolve("scoreboard.png"));
                Parent returning = new LoginScreen(() -> {}, () -> {}, () -> {}).build(); show(stage, returning);
                snapshot(returning, output.resolve("returning-player.png"));
                int[] actions = new int[2];
                Parent menu = new OpeningMenuView(true, new OpeningMenuView.Actions(() -> {}, () -> {}, () -> {}, () -> {}, () -> actions[0]++, () -> actions[1]++), false);
                show(stage, menu); button(menu, "scoreboard-menu").fire(); button(menu, "switch-player").fire();
                check(actions[0] == 1 && actions[1] == 1, "Scoreboard and switch-player routes");
                check(button(menu, "quit").localToScene(button(menu, "quit").getBoundsInLocal()).getMaxY() < 675, "Menu fits above footer");
                snapshot(menu, output.resolve("main-menu.png"));
                com.override.shared.model.GameState.reset();
                com.override.shared.model.GameState.get().setCampaignChapterOneGrade("B");
                Path transport = Path.of(System.getProperty("override.chapter2Dir"), "build", "chapter2_result.json");
                Files.createDirectories(transport.getParent());
                com.override.shared.service.ChapterResultStore.begin(transport);
                Files.writeString(transport, "{\"win\":true,\"final_score_percent\":80,\"bad_killed\":10,\"bad_spawned\":10,\"good_killed\":0,\"game_time_left\":20,\"progress_percent\":100}");
                int before = ScoreArchive.history().size();
                Parent result = new com.override.chapter2.ChapterTwoResultScreen().build(); show(stage, result);
                check(ScoreArchive.history().size() == before + 2, "Harvest and campaign recorded");
                check(ScoreArchive.history().stream().anyMatch(r -> r.mode() == ScoreArchive.Mode.CAMPAIGN && r.score() == 70), "Campaign keeps 50/50 chapter weighting");
                snapshot(result, output.resolve("campaign-result.png"));
                com.override.shared.model.GameState.get().setCampaignChapterOneGrade("S");
                check("B".equals(com.override.shared.service.ChapterResultStore.chapterOneGrade()), "Pending result retains its chapter grade");
                show(stage, new com.override.chapter2.ChapterTwoResultScreen().build());
                check(ScoreArchive.history().size() == before + 2, "Reopening result does not duplicate history");
                Files.writeString(PlayerProfiles.dataFile("save.properties"), "bad=\\uZZZZ");
                Parent broken = new LoginScreen(() -> calls[0]++, () -> {}, () -> {}).build(); show(stage, broken);
                ((TextField) broken.lookup("#player-name")).setText("Ayesha");
                button(broken, "login-submit").fire();
                check(PlayerProfiles.active() == null && calls[0] == 1, "Unreadable save does not leave an active reset profile");
                stage.close(); System.out.println("PASS: login, validation, history pagination/search, routes, campaign scoring, deduplication and six real UI screenshots");
            } catch (Throwable e) { failure.set(e); }
            finally { done.countDown(); }
        });
        done.await(); Platform.exit(); if (failure.get() != null) throw new AssertionError("Player pages failed", failure.get());
    }
    private static Button button(Parent root, String id) { return (Button) root.lookup("#" + id); }
    private static void show(Stage stage, Parent root) {
        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(PlayerPagesSmoke.class.getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene); stage.show(); root.applyCss(); root.layout();
    }
    private static void snapshot(Parent root, Path path) throws Exception {
        root.applyCss(); root.layout();
        // Viewport and tolerance as in OpeningVisualSmoke: pixel snapping on a
        // HiDPI display can push an edge a fraction of a pixel past the design
        // space, which is rounding, not overflow. See that class for the detail.
        var viewport = new javafx.scene.SnapshotParameters();
        viewport.setViewport(new javafx.geometry.Rectangle2D(0, 0, 1280, 720));
        var image = root.snapshot(viewport, null);
        var bounds = root.getBoundsInLocal();
        check(bounds.getMinX() > -1 && bounds.getMinY() > -1
                && bounds.getMaxX() < 1281 && bounds.getMaxY() < 721,
            "Page fits design space");
        BufferedImage png = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] data = ((DataBufferInt) png.getRaster().getDataBuffer()).getData();
        image.getPixelReader().getPixels(0, 0, 1280, 720, PixelFormat.getIntArgbPreInstance(), data, 0, 1280);
        try (var bytes = new java.io.ByteArrayOutputStream()) {
            ImageIO.write(png, "png", bytes); Files.write(path, bytes.toByteArray());
        }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
