package gg.cnmc.battlemanager.battle;

import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.ui.TimerBar;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class Assault {

    public static final int ROUND_SECONDS = 45 * 60;
    private static boolean active = false;

    // ------------------------------------------------------------------
    // Start — 10-second countdown then the real round begins
    // ------------------------------------------------------------------
    public static void start(MinecraftServer server) {
        active = true;
        BattleManager.state = BattleState.STARTING;
        BattleData.resetPlayers(server);

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("§e§lASSAULT"))
        );
        server.getPlayerManager().sendToAll(
                new SubtitleS2CPacket(Text.literal("§7Starting in 10 seconds..."))
        );
        server.getPlayerManager().broadcast(
                Text.literal("§7Attackers must break the banner and return it to their spawn. Defenders must protect it!"),
                false
        );

        TimerBar.setup(
                server,
                10,
                "Round Starts In",
                TimerBar.Color.GREEN,
                () -> startRound(server)
        );
    }

    // ------------------------------------------------------------------
    // Called after the 10-second countdown finishes
    // ------------------------------------------------------------------
    private static void startRound(MinecraftServer server) {
        if (!active) return;

        BattleManager.state = BattleState.ROUND_ACTIVE;

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("§c§lFIGHT — SEIZE THE BANNER!"))
        );

        TimerBar.setup(
                server,
                ROUND_SECONDS,
                "Time Remaining",
                TimerBar.Color.YELLOW,
                () -> defenderWin(server)
        );
    }

    // ------------------------------------------------------------------
    // Attacker win — called from BannerReturnEvent
    // ------------------------------------------------------------------
    public static void attackerWin(MinecraftServer server) {
        if (!active) return;
        end(server);

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("§c§lATTACKERS WIN!"))
        );
        server.getPlayerManager().broadcast(
                Text.literal("§cThe attackers seized the banner and returned it to their spawn!"),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Assault ended — Attackers win");
        cleanup(server);
    }

    // ------------------------------------------------------------------
    // Defender win — called when the timer runs out
    // ------------------------------------------------------------------
    public static void defenderWin(MinecraftServer server) {
        if (!active) return;
        end(server);

        server.getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("§9§lDEFENDERS WIN!"))
        );
        server.getPlayerManager().broadcast(
                Text.literal("§9The defenders held the banner for the full 45 minutes!"),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Assault ended — Defenders win");
        cleanup(server);
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------
    private static void end(MinecraftServer server) {
        active = false;
        TimerBar.cleanup(server);
    }

    private static void cleanup(MinecraftServer server) {
        BattleData.reset(server);
        BannerTracker.reset();
        BattleManager.state = BattleState.IDLE;
    }

    public static void handleDeath(ServerPlayerEntity player, MinecraftServer server) {
        teleportToSpawn(player, server);
    }

    public static void teleportToSpawn(ServerPlayerEntity player, MinecraftServer server) {
        BlockPos spawn = player.getSpawnPointPosition();
        ServerWorld spawnWorld = server.getWorld(player.getSpawnPointDimension());

        if (spawn != null && spawnWorld != null) {
            player.teleport(spawnWorld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0f, 0f);
        } else {
            BlockPos worldSpawn = server.getOverworld().getSpawnPos();
            player.teleport(server.getOverworld(), worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0f, 0f);
        }
    }

    public static boolean isActive() { return active; }

    public static void forceStop(MinecraftServer server) {
        active = false;
        TimerBar.cleanup(server);
        BannerTracker.reset();
        BattleData.reset(server);
        BattleManager.state = BattleState.IDLE;
    }
}