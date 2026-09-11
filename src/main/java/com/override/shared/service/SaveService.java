package com.override.shared.service;

import com.override.shared.model.GameCharacter;
import com.override.shared.model.GameState;
import com.override.shared.model.Player;

import java.io.*;
import java.util.Properties;

/**
 * Save / load using java.util.Properties — no external JSON lib needed.
 *
 * File location: ~/.override/save.properties
 *
 * For the final project you can swap this to a Spring Boot REST call
 * (see README.md "Backend hook-up" section) without touching the screens.
 */
public class SaveService {

    private static File saveFile() {
        File dir = new File(System.getProperty("user.home"), ".override");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "save.properties");
    }

    public static boolean saveExists() {
        return saveFile().exists();
    }

    public static void save() {
        GameState s = GameState.get();
        Player p = s.getPlayer();

        Properties props = new Properties();
        props.setProperty("coins",          String.valueOf(s.getCoins()));
        props.setProperty("dependency",     String.valueOf(s.getDependency()));
        props.setProperty("chapterUnlocked",String.valueOf(s.getChapterUnlocked()));
        props.setProperty("chapterCompleted",String.valueOf(s.getChapterCompleted()));
        props.setProperty("independentXp",  String.valueOf(s.getIndependentXp()));
        props.setProperty("silentClassroomBestScore",
            String.valueOf(s.getSilentClassroomBestScore()));
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

        try (OutputStream os = new FileOutputStream(saveFile())) {
            props.store(os, "Override save data");
        } catch (IOException e) {
            System.err.println("Could not save: " + e.getMessage());
        }
    }

    public static boolean load() {
        if (!saveExists()) return false;

        Properties props = new Properties();
        try (InputStream is = new FileInputStream(saveFile())) {
            props.load(is);
        } catch (IOException e) {
            return false;
        }

        GameState.reset();
        GameState s = GameState.get();

        s.addCoins(parseInt(props, "coins", 0) - s.getCoins());
        s.increaseDependency(parseInt(props, "dependency", 0));
        for (int i = 0; i < parseInt(props, "chapterCompleted", 0); i++)
            s.completeChapter(i + 1);
        s.addIndependentXp(parseInt(props, "independentXp", 0));
        // Missing in saves created before Silent Classroom scoring; defaults to 0.
        s.recordSilentClassroomScore(parseInt(props, "silentClassroomBestScore", 0));

        // Restore unlocked characters
        String unlocked = props.getProperty("unlocked", "ayan");
        for (String id : unlocked.split(",")) {
            if (id.isBlank()) continue;
            for (GameCharacter c : GameCharacter.roster()) {
                if (c.getId().equals(id.trim())) {
                    s.getUnlockedCharacters().add(c.getId());
                }
            }
        }

        // Restore selected character (re-creates the player base)
        String charId = props.getProperty("character", "ayan");
        for (GameCharacter c : GameCharacter.roster()) {
            if (c.getId().equals(charId)) {
                s.setSelectedCharacter(c);
                break;
            }
        }

        // Then overlay saved player numbers
        Player p = s.getPlayer();
        p.setDisplayName(props.getProperty("p.name", "Ayan"));
        // simple way to re-apply numbers: cumulative buffs from base 5
        p.buffLogic     (parseInt(props, "p.logic",     5) - p.getLogic());
        p.buffAwareness (parseInt(props, "p.awareness", 5) - p.getAwareness());
        p.buffWillpower (parseInt(props, "p.willpower", 5) - p.getWillpower());
        p.buffCombat    (parseInt(props, "p.combat",    5) - p.getCombat());
        p.buffEmpathy   (parseInt(props, "p.empathy",   5) - p.getEmpathy());
        // XP restoration is best-effort
        p.addXp(parseInt(props, "p.xp", 0));

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
