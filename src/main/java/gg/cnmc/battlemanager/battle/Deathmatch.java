package gg.cnmc.battlemanager.battle;

import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.ui.DeathmatchScoreboard;
import gg.cnmc.battlemanager.battle.ui.TimerBar;
import gg.cnmc.battlemanager.utils.TeamStorage;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;

public class Deathmatch {

    public static final int MAX_ROUNDS = 3;
    private static int currentRound = 0;

    public static void start(MinecraftServer server) {
        currentRound = 0;
        DeathmatchScoreboard.setup(server);
        startNextRound(server);
    }

    public static void startNextRound(MinecraftServer server) {
        if (currentRound >= MAX_ROUNDS) {
            endBattle(server);
            return;
        }

        currentRound++;
        BattleManager.state = BattleState.STARTING;

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("§e§lROUND " + currentRound))
        );
        server.getPlayerManager().sendToAll(
                new SubtitleS2CPacket(Text.literal("§7Starting in 10 seconds..."))
        );

        TimerBar.setup(
                server,
                10,
                "Round " + currentRound + " Starts In",
                TimerBar.Color.GREEN,
                () -> startRound(server)
        );
    }

    private static void startRound(MinecraftServer server) {
        BattleManager.state = BattleState.ROUND_ACTIVE;
        BattleData.resetPlayers(server);

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("§c§lROUND " + currentRound + " — FIGHT!"))
        );

        TimerBar.setup(
                server,
                15 * 60,
                "Round " + currentRound,
                TimerBar.Color.YELLOW,
                () -> endRound(server)
        );
    }

    public static void endRound(MinecraftServer server) {
        BattleManager.state = BattleState.ROUND_ENDED;

        int attackerDeaths = DeathmatchScoreboard.getDeaths(server, "attackers");
        int defenderDeaths = DeathmatchScoreboard.getDeaths(server, "defenders");

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("§l§cROUND " + currentRound + " ENDED"))
        );
        server.getPlayerManager().broadcast(
                Text.literal("§7Attackers: §c" + attackerDeaths + " deaths  §7Defenders: §c" + defenderDeaths + " deaths"),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Round {} ended — Attackers: {} deaths, Defenders: {} deaths",
                currentRound, attackerDeaths, defenderDeaths);

        BattleData.resetPlayers(server);
        teleportTeamsToSpawn(server);

        TimerBar.setup(
                server,
                30,
                "Next Round In",
                TimerBar.Color.GREEN,
                () -> startNextRound(server)
        );
    }

    public static void endBattle(MinecraftServer server) {
        BattleManager.state = BattleState.BATTLE_ENDED;

        TimerBar.cleanup(server);

        int attackerDeaths = DeathmatchScoreboard.getDeaths(server, "attackers");
        int defenderDeaths = DeathmatchScoreboard.getDeaths(server, "defenders");

        String winner;
        if (attackerDeaths < defenderDeaths) {
            winner = "§c§lATTACKERS WIN!";
        } else if (defenderDeaths < attackerDeaths) {
            winner = "§9§lDEFENDERS WIN!";
        } else {
            winner = "§e§lDRAW!";
        }

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal(winner))
        );
        server.getPlayerManager().broadcast(
                Text.literal("§7Final — Attackers: §c" + attackerDeaths + " deaths  §7Defenders: §c" + defenderDeaths + " deaths"),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Battle ended — Attackers: {} deaths, Defenders: {} deaths",
                attackerDeaths, defenderDeaths);

        BattleData.reset(server);
        DeathmatchScoreboard.cleanup(server);
        BattleManager.state = BattleState.IDLE;
    }

    private static void teleportTeamsToSpawn(MinecraftServer server) {
        TeamStorage storage = TeamStorage.getInstance(server);
        Set<String> attackers = storage.getAttackers();
        Set<String> defenders = storage.getDefenders();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String name = player.getName().getString();
            if (!attackers.contains(name) && !defenders.contains(name)) continue;

            BlockPos spawn = player.getSpawnPointPosition();
            ServerWorld spawnWorld = server.getWorld(player.getSpawnPointDimension());

            if (spawn != null && spawnWorld != null) {
                player.teleport(spawnWorld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0f, 0f);
            } else {
                BlockPos worldSpawn = server.getOverworld().getSpawnPos();
                player.teleport(server.getOverworld(), worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0f, 0f);
            }
        }
    }

    public static void checkRoundEndCondition(MinecraftServer server) {
        if (BattleManager.state != BattleState.ROUND_ACTIVE) return;

        TeamStorage storage = TeamStorage.getInstance(server);
        Set<String> attackers = storage.getAttackers();
        Set<String> defenders = storage.getDefenders();

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();

        boolean attackersAlive = players.stream()
                .filter(p -> attackers.contains(p.getName().getString()))
                .anyMatch(p -> !p.isDead());

        boolean defendersAlive = players.stream()
                .filter(p -> defenders.contains(p.getName().getString()))
                .anyMatch(p -> !p.isDead());

        if (!attackersAlive || !defendersAlive) {
            TimerBar.cleanup(server);
            endRound(server);
        }
    }

    public static int getCurrentRound() { return currentRound; }

    public static void forceStop(MinecraftServer server) {
        currentRound = 0;
        TimerBar.cleanup(server);
        DeathmatchScoreboard.cleanup(server);
        BattleData.reset(server);
        BattleManager.state = BattleState.IDLE;
    }
}