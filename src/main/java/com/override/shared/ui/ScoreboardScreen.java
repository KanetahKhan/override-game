package com.override.shared.ui;

import com.override.Main;
import com.override.shared.service.PlayerProfiles;
import com.override.shared.service.ScoreArchive;
import com.override.shared.service.ScoreArchive.Mode;
import com.override.shared.service.ScoreArchive.Run;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Five distinct players on the left; paged, searchable history of every run on the right. */
public final class ScoreboardScreen {
    private static final int PAGE_SIZE = 6;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yy · HH:mm");
    private final Runnable back;
    private final VBox leaders = new VBox(7);
    private final TableView<Run> table = new TableView<>();
    private final TextField search = new TextField();
    private final Label count = GameControls.label("", 11, "#86a5b3");
    private final Label pageLabel = GameControls.label("", 12, "#b9d5dd");
    private final Label error = GameControls.label("", 12, "#ffa5a5");
    private final ComboBox<Mode> mode = new ComboBox<>();
    private List<Run> history = List.of();
    private int page;
    private Button previous, next;

    public ScoreboardScreen() { this(() -> Main.switchScene(new MainMenuScreen().build())); }
    public ScoreboardScreen(Runnable back) { this.back = back; }

    public Parent build() {
        Label title = GameControls.label("PLAYER RECORDS", 36, "#e6f5ee");
        title.setStyle(title.getStyle() + " -fx-font-weight: bold;");
        VBox heading = new VBox(7, GameControls.label("OVERRIDE / SCOREBOARD", 11, "#6de6cc"), title,
            GameControls.label("Every run leaves a trace.", 13, "#91aebc"));
        Label active = GameControls.label(PlayerProfiles.active() == null ? "LOCAL ARCHIVE" : "PLAYER / " + PlayerProfiles.name(), 12, "#9cbec7");
        HBox header = new HBox(20, heading, spacer(), active); header.setAlignment(Pos.CENTER_LEFT);

        mode.getItems().setAll(Mode.values()); mode.setValue(Mode.CAMPAIGN);
        mode.setId("leaderboard-mode"); mode.setMaxWidth(Double.MAX_VALUE);
        mode.setOnAction(e -> updateLeaders());
        VBox top = new VBox(10, GameControls.label("TOP 5", 24, "#c6f3e6"), mode, leaders,
            GameControls.label("One best run per player.\nClassroom ranks cleared runs only.", 10, "#7f9daa"));
        top.setPadding(new Insets(22)); top.setMinWidth(320); top.setMaxWidth(320); top.setPrefWidth(320);
        top.getStyleClass().add("player-panel"); leaders.setId("top-five");

        search.setId("history-search"); search.setPromptText("Search player name"); search.setPrefHeight(34);
        search.textProperty().addListener((o, old, value) -> { page = 0; updateHistory(); });
        HBox historyHeading = new HBox(10, GameControls.label("RUN HISTORY", 20, "#d9e9e8"), spacer(), count);
        historyHeading.setAlignment(Pos.CENTER_LEFT);
        table.setId("run-history"); table.setFixedCellSize(32);
        table.setPrefHeight(254); table.setMinHeight(254); table.setMaxHeight(254);
        table.setPlaceholder(GameControls.label("No recorded runs yet. Finish a chapter to leave your mark.", 12, "#839fac"));
        table.getColumns().add(column("PLAYER", 148, Run::playerName));
        table.getColumns().add(column("GAME", 190, r -> r.mode().label));
        table.getColumns().add(column("SCORE", 86, r -> r.mode().format(r.score())));
        table.getColumns().add(column("RESULT", 124, r -> r.outcome() + (r.assisted() ? " / AI" : "")));
        table.getColumns().add(column("PLAYED", 166, r -> DATE.format(Instant.ofEpochMilli(r.when()).atZone(ZoneId.systemDefault()))));
        previous = GameControls.button("← PREV", 112, 34, false); previous.setId("history-prev");
        next = GameControls.button("NEXT →", 112, 34, false); next.setId("history-next");
        previous.setOnAction(e -> { page--; updateHistory(); }); next.setOnAction(e -> { page++; updateHistory(); });
        HBox pagination = new HBox(12, previous, pageLabel, next); pagination.setAlignment(Pos.CENTER_RIGHT);
        VBox archive = new VBox(12, historyHeading, search, table, pagination);
        archive.setPadding(new Insets(22)); archive.getStyleClass().add("player-panel"); HBox.setHgrow(archive, Priority.ALWAYS);
        HBox body = new HBox(24, top, archive); body.setPrefHeight(446); body.setMaxHeight(446);

        Button returnButton = GameControls.button("← BACK", 180, 42, false);
        returnButton.setId("scoreboard-back"); returnButton.setOnAction(e -> back.run());
        Button refresh = GameControls.button("REFRESH", 154, 42, false);
        refresh.setId("scoreboard-refresh"); refresh.setOnAction(e -> reload());
        HBox bottom = new HBox(12, returnButton, refresh, spacer(), GameControls.label("SAVED ON THIS COMPUTER", 11, "#7398a5"));
        bottom.setAlignment(Pos.CENTER_LEFT);
        error.setId("scoreboard-error"); error.setMinHeight(18);
        Button replay = GameControls.button("REPLAY CAMPAIGN", 204, 42, true);
        replay.setId("scoreboard-replay"); replay.setDisable(PlayerProfiles.active() == null);
        replay.setOnAction(e -> {
            com.override.game.minigames.GodotGameLauncher.clearResult();
            com.override.shared.model.GameState.reset();
            Main.switchScene(new IntroStoryScreen(() -> Main.switchScene(new com.override.chapter1.CurfewProtocolScreen().build())).build());
        });
        bottom.getChildren().add(2, replay);
        AnchorPane layout = new AnchorPane();
        place(layout, header, 28); place(layout, body, 142);
        place(layout, bottom, 616); place(layout, error, 672);
        Parent root = GameControls.page(layout, 0.83); root.setId("scoreboard");
        reload();
        return root;
    }

    private void reload() {
        try { history = ScoreArchive.history(); error.setText(ScoreArchive.lastError() == null ? "" : ScoreArchive.lastError()); }
        catch (IOException e) { error.setText("The score archive could not be read. Your existing files have been kept."); }
        updateLeaders(); updateHistory();
    }

    private void updateLeaders() {
        leaders.getChildren().clear();
        List<Run> best = ScoreArchive.topFive(history, mode.getValue());
        for (int i = 0; i < 5; i++) {
            Label rank = GameControls.label(String.format("%02d", i + 1), 19, i == 0 ? "#e7c782" : "#7fa8b2");
            VBox player;
            if (i < best.size()) {
                Run r = best.get(i);
                Label name = GameControls.label(r.playerName(), 12, "#e0eded"); name.setMaxWidth(184);
                player = new VBox(3, name, GameControls.label(r.mode().format(r.score()) + "  /  " + r.outcome(), 11, "#77d6c2"));
            } else player = new VBox(3, GameControls.label("—", 13, "#577381"), GameControls.label("NO RUN YET", 10, "#577381"));
            HBox row = new HBox(15, rank, player); row.setAlignment(Pos.CENTER_LEFT); row.setMinHeight(45);
            row.getStyleClass().add("rank-row"); leaders.getChildren().add(row);
        }
    }

    private void updateHistory() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        List<Run> visible = history.stream().filter(r -> r.playerName().toLowerCase(Locale.ROOT).contains(query)).toList();
        int pages = Math.max(1, (visible.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        table.getItems().setAll(visible.subList(page * PAGE_SIZE, Math.min(visible.size(), (page + 1) * PAGE_SIZE)));
        pageLabel.setText((page + 1) + " / " + pages);
        count.setText(visible.size() + " RUNS / " + visible.stream().map(Run::playerId).distinct().count() + " PLAYERS");
        if (previous != null) previous.setDisable(page == 0);
        if (next != null) next.setDisable(page + 1 >= pages);
    }

    private static TableColumn<Run, String> column(String name, double width, Function<Run, String> text) {
        TableColumn<Run, String> c = new TableColumn<>(name);
        c.setPrefWidth(width); c.setSortable(false); c.setReorderable(false);
        c.setCellValueFactory(v -> new ReadOnlyStringWrapper(text.apply(v.getValue())));
        return c;
    }
    private static void place(AnchorPane pane, javafx.scene.Node node, double top) {
        AnchorPane.setLeftAnchor(node, 56.0); AnchorPane.setRightAnchor(node, 56.0);
        AnchorPane.setTopAnchor(node, top); pane.getChildren().add(node);
    }
    private static Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
}
