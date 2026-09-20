package com.override.shared.service;

import com.override.shared.model.GameCharacter;
import com.override.shared.model.GameState;
import com.override.shared.model.Player;

import java.io.*;
import java.util.Properties;

/**
 * Save / load using java.util.Properties — no external JSON lib needed.
 *
 * File location: ~/.override/players/<profile-id>/save.properties
 *
 * For the final project you can swap this to a Spring Boot REST call
 * (see README.md "Backend hook-up" section) without touching the screens.
 */
public class SaveService {

    private static File saveFile() {
        return PlayerProfiles.dataFile("save.properties").toFile();
    }

    public static boolean saveExists() {
        return saveFile().exists();
    }

    public static boolean save() {
        GameState s = GameState.get();
        Player p = s.getPlayer();

        Properties props = new Properties();
        props.setProperty("coins",          String.valueOf(s.getCoins()));
        props.setProperty("dependency",     String.valueOf(s.getDependency()));
        props.setProperty("chapterUnlocked",String.valueOf(s.getChapterUnlocked()));
        props.setProperty("chapterCompleted",String.valueOf(s.getChapterCompleted()));
        props.setProperty("independentXp",  String.valueOf(s.getIndependentXp()));
        props.setProperty("chapter1BestScore", String.valueOf(s.getSilentClassroomBestScore()));
        props.setProperty("campaign.chapter1Grade", s.getCampaignChapterOneGrade() == null ? "" : s.getCampaignChapterOneGrade());
        props.setProperty("unlocked",       String.join(",", s.getUnlockedCharacters()));

        if (s.getSelectedCharacter() != null)
            props.setProperty("character", s.getSelectedCharacter().getId());

        props.setProperty("p.name",      p.getDisplayName());
        props.setProperty("p.level",     String.valueOf(p.getLevel()));
        props.setProperty("p.xp",        String.valueOf(p.getXp()));
        props.setProperty("p.hp",        String.valueOf(p.getHp()));
        props.setProperty("p.maxHp",     String.valueOf(p.getMaxHp()));
        props.setProperty("p.logic",     String.valueOf(p.getLogic()));
        props.setProperty("p.awareness", String.valueOf(p.getAwareness()));
        props.setProperty("p.willpower", String.valueOf(p.getWillpower()));
        props.setProperty("p.combat",    String.valueOf(p.getCombat()));
        props.setProperty("p.empathy",   String.valueOf(p.getEmpathy()));

        try {
            PlayerProfiles.write(saveFile().toPath(), props);
            return true;
        } catch (IOException e) {
            System.err.println("Could not save: " + e.getMessage());
            return false;
        }
    }

    public static boolean load() {
        if (!saveExists()) return false;

        Properties props = new Properties();
        try (InputStream is = new FileInputStream(saveFile())) {
            props.load(is);
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }

        GameState.reset();
        GameState s = GameState.get();

        s.addCoins(parseInt(props, "coins", 0) - s.getCoins());
        s.increaseDependency(parseInt(props, "dependency", 0));
        int completed = parseInt(props, "chapterCompleted", 0);
        for (int i = 0; i < completed; i++)
            s.completeChapter(i + 1);
        // Restore the unlocked ceiling, but never above what the player has earned.
        // Saves written while the dev default was 5 all carry chapterUnlocked=5;
        // taking them at face value would hand a fresh player the whole map.
        s.restoreChapterUnlocked(
            Math.min(parseInt(props, "chapterUnlocked", 1), completed + 1));
        s.addIndependentXp(parseInt(props, "independentXp", 0));
        s.recordSilentClassroomScore(parseInt(props, "chapter1BestScore", 0));
        s.setCampaignChapterOneGrade(props.getProperty("campaign.chapter1Grade"));
        if (!props.containsKey("campaign.chapter1Grade") && s.getChapterCompleted() >= 1) {
            var previous = com.override.chapter1.CurfewRecords.latestRun();
            if (previous != null) s.setCampaignChapterOneGrade(previous.grade());
        }

        // Restore unlocked characters
        String unlocked = props.getProperty("unlocked", "ren");
        for (String id : unlocked.split(",")) {
            if (id.isBlank()) continue;
            for (GameCharacter c : GameCharacter.roster()) {
                if (c.getId().equals(id.trim())) {
                    s.getUnlockedCharacters().add(c.getId());
                }
            }
        }

        // Restore selected character (re-creates the player base)
        String charId = props.getProperty("character", "ren");
        for (GameCharacter c : GameCharacter.roster()) {
            if (c.getId().equals(charId)) {
                s.setSelectedCharacter(c);
                break;
            }
        }

        // Then overlay saved player numbers
        Player p = s.getPlayer();
        p.setDisplayName(PlayerProfiles.active() == null ? props.getProperty("p.name", "REN") : PlayerProfiles.name());
        // simple way to re-apply numbers: cumulative buffs from base 5
        p.buffLogic     (parseInt(props, "p.logic",     5) - p.getLogic());
        p.buffAwareness (parseInt(props, "p.awareness", 5) - p.getAwareness());
        p.buffWillpower (parseInt(props, "p.willpower", 5) - p.getWillpower());
        p.buffCombat    (parseInt(props, "p.combat",    5) - p.getCombat());
        p.buffEmpathy   (parseInt(props, "p.empathy",   5) - p.getEmpathy());
        // Level, XP and HP are restored as stored. Replaying addXp() here would
        // reset the player to level 1, because the saved xp is only the leftover
        // above the last level-up threshold.
        p.restoreProgress(
            parseInt(props, "p.level", 1),
            parseInt(props, "p.xp",    0),
            parseInt(props, "p.hp",    100),
            parseInt(props, "p.maxHp", 100));

        return true;
    }

    public static void deleteSave() {
        File f = saveFile();
        if (f.exists()) f.delete();
    }

    private static int parseInt(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }
}
