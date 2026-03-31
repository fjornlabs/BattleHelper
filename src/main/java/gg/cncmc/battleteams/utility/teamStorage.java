package gg.cncmc.battleteams.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static com.mojang.text2speech.Narrator.LOGGER;

public class teamStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "battleteams.json";

    private Set<String> attackers = new HashSet<>();
    private Set<String> defenders = new HashSet<>();
    private Set<String> press = new HashSet<>();

    private final File filePath;

    private static teamStorage instance;

    private teamStorage(File filePath) {
        this.filePath = filePath;
        load();
    }

    public static teamStorage getInstance(MinecraftServer server) {
        if (instance == null) {
            File file = new File(server.getRunDirectory(), FILE_NAME);
            instance = new teamStorage(file);
        }
        return instance;
    }

    // --- Attackers ---
    public void addAttacker(String playerName) { attackers.add(playerName); save(); }
    public void removeAttacker(String playerName) { attackers.remove(playerName); save(); }
    public Set<String> getAttackers() { return new HashSet<>(attackers); }

    // --- Defenders ---
    public void addDefender(String playerName) { defenders.add(playerName); save(); }
    public void removeDefender(String playerName) { defenders.remove(playerName); save(); }
    public Set<String> getDefenders() { return new HashSet<>(defenders); }

    // --- Press ---
    public void addPress(String playerName) { press.add(playerName); save(); }
    public void removePress(String playerName) { press.remove(playerName); save(); }
    public Set<String> getPress() { return new HashSet<>(press); }

    // --- Clear all ---
    public void clearAll() {
        attackers.clear();
        defenders.clear();
        press.clear();
        save();
    }

    private void save() {
        try (Writer writer = new FileWriter(filePath)) {
            GSON.toJson(new StorageData(attackers, defenders, press), writer);
        } catch (IOException e) {
            LOGGER.error("[BattleTeams] Failed to save teams: " + e.getMessage());
        }
    }

    private void load() {
        if (!filePath.exists()) return;

        try (Reader reader = new FileReader(filePath)) {
            Type type = new TypeToken<StorageData>() {}.getType();
            StorageData data = GSON.fromJson(reader, type);
            if (data != null) {
                if (data.attackers != null) attackers = data.attackers;
                if (data.defenders != null) defenders = data.defenders;
                if (data.press != null) press = data.press;
            }
        } catch (IOException e) {
            LOGGER.error("[BattleTeams] Failed to load teams: " + e.getMessage());
        }
    }

    private static class StorageData {
        Set<String> attackers;
        Set<String> defenders;
        Set<String> press;

        StorageData(Set<String> attackers, Set<String> defenders, Set<String> press) {
            this.attackers = attackers;
            this.defenders = defenders;
            this.press = press;
        }
    }
}