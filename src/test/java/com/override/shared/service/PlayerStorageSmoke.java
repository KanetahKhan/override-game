package com.override.shared.service;

import com.override.shared.model.GameState;
import com.override.shared.model.GameCharacter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PlayerStorageSmoke {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("override-player-test-");
        System.setProperty("override.dataDir", root.toString());
        for (String bad : new String[]{"", " ", "a", "../escape", "a".repeat(25), "---"}) {
            try { PlayerProfiles.login(bad); throw new AssertionError("Accepted invalid name"); }
            catch (IllegalArgumentException expected) { }
        }
        var alice = PlayerProfiles.login("  Alice   Khan  ");
        check(PlayerProfiles.login("alice khan").id().equals(alice.id()), "Case insensitive returning login");
        GameState.reset(); GameState.get().addCoins(42);
        GameState.get().setCampaignChapterOneGrade("B");
        GameState.get().setSelectedCharacter(GameCharacter.roster().get(0));
        check(GameState.get().getPlayer().getDisplayName().equals("Alice Khan"), "Persona keeps login name");
        check(SaveService.save(), "Save Alice");
        ScoreArchive.record("run-1", ScoreArchive.Mode.CAMPAIGN, 88, "RESISTANCE", false);
        ScoreArchive.record("run-1", ScoreArchive.Mode.CAMPAIGN, 88, "RESISTANCE", false);
        check(ScoreArchive.history().size() == 1, "Same event only saved once");
        Path transport = root.resolve("result.json");
        ChapterResultStore.begin(transport);
        Files.writeString(transport, "{\"final_score_percent\":");
        check(!ChapterResultStore.has(transport), "Partial result not consumed");
        PlayerProfiles.login("Bob"); GameState.reset();
        check(!SaveService.saveExists(), "New player has no other player's save");
        Files.writeString(transport, "{\"final_score_percent\":72,\"win\":true}");
        check(!ChapterResultStore.has(transport), "Pending result stays with Alice");
        check(SaveService.save(), "Save Bob");
        PlayerProfiles.login("Alice Khan");
        check(SaveService.load() && GameState.get().getCoins() == 142, "Alice save restored");
        check("B".equals(GameState.get().getCampaignChapterOneGrade()), "Current campaign grade restored");
        check(ChapterResultStore.has(transport), "Alice can recover own result");
        String event = ChapterResultStore.eventId();
        check(ChapterResultStore.read(transport).contains("72") && ChapterResultStore.eventId().equals(event), "Stable result identity");
        ChapterResultStore.clear(transport);
        check(!ChapterResultStore.has(transport), "Clear only own pending result");
        ChapterResultStore.begin(transport);
        Files.writeString(transport, "{\"final_score_percent\":72,\"win\":true}");
        check(ChapterResultStore.has(transport) && !ChapterResultStore.eventId().equals(event), "Identical scores in a new run have a new identity");
        for (int i = 0; i < 7; i++) {
            PlayerProfiles.login("Player " + i);
            ScoreArchive.record("a", ScoreArchive.Mode.CAMPAIGN, 90 + i, "RESISTANCE", false);
            ScoreArchive.record("b", ScoreArchive.Mode.CAMPAIGN, 80 + i, "RESISTANCE", false);
        }
        var best = ScoreArchive.topFive(ScoreArchive.history(), ScoreArchive.Mode.CAMPAIGN);
        check(best.size() == 5 && best.get(0).score() == 96 && best.get(4).score() == 92, "Five distinct players ranked by best score");
        ScoreArchive.record("failed", ScoreArchive.Mode.CLASSROOM_NORMAL, 9000, "FAILED", false);
        ScoreArchive.record("cleared", ScoreArchive.Mode.CLASSROOM_NORMAL, 200, "CLEARED", true);
        check(ScoreArchive.topFive(ScoreArchive.history(), ScoreArchive.Mode.CLASSROOM_NORMAL).get(0).score() == 200, "Failed classroom attempts stay out of ranking");
        int count = ScoreArchive.history().size();
        GameState.reset(); PlayerProfiles.logout(); PlayerProfiles.login("Alice Khan");
        check(GameState.get().getCampaignChapterOneGrade() == null, "Replay cannot inherit a previous campaign grade");
        check(ScoreArchive.history().size() == count, "History survives reset and login");
        Files.writeString(root.resolve("score-history/broken.properties"), "bad=\\uZZZZ");
        check(ScoreArchive.history().size() == count, "One corrupt history row does not hide good runs");
        Properties old = new Properties(); old.setProperty("entry.0", "REN|64|RESISTANCE|1234");
        PlayerProfiles.write(root.resolve("campaign-scoreboard.properties"), old);
        check(ScoreArchive.history().stream().anyMatch(r -> r.playerName().equals("REN (legacy)")), "Preserve upstream old scoreboard");
        Files.writeString(root.resolve("save.properties"), "coins=123\n");
        check(PlayerProfiles.list().stream().anyMatch(p -> p.name().equals("Previous Player")), "Recover old anonymous save");
        check(Files.readString(root.resolve("save.properties")).equals("coins=123\n"), "Migration leaves original untouched");
        System.out.println("PASS: names, separate saves, persona identity, complete history, deduplication, top five, result ownership, legacy migration");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
