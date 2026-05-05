package gg.cnmc.battlemanager.battle;

import gg.cnmc.battlemanager.battle.ui.DeathmatchScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.*;

public class BattleData {

    private static final Set<UUID> eliminatedPlayers = new HashSet<>();

    public static void addEliminated(ServerPlayerEntity player) {
        eliminatedPlayers.add(player.getUuid());
    }

    public static void removeEliminated(ServerPlayerEntity player) {
        eliminatedPlayers.remove(player.getUuid());
    }

    public static boolean isEliminated(ServerPlayerEntity player) {
        return eliminatedPlayers.contains(player.getUuid());
    }

    public static void resetPlayers(MinecraftServer server) {
        for (UUID uuid : eliminatedPlayers) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                player.changeGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
            }
        }
        eliminatedPlayers.clear();
    }

    public static void reset(MinecraftServer server) {
        resetPlayers(server);
        DeathmatchScoreboard.cleanup(server);
    }
}