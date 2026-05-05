package gg.cnmc.battlemanager.battle.ui;

import gg.cnmc.battlemanager.utils.time.GameTimer;
import gg.cnmc.battlemanager.utils.time.TimerManager;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TimerBar {

    public enum Color {
        RED(BossBar.Color.RED),
        YELLOW(BossBar.Color.YELLOW),
        GREEN(BossBar.Color.GREEN),
        BLUE(BossBar.Color.BLUE),
        PURPLE(BossBar.Color.PURPLE);

        final BossBar.Color barColor;
        Color(BossBar.Color barColor) { this.barColor = barColor; }
    }

    private static ServerBossBar bossBar;
    private static GameTimer gameTimer;

    private static int totalTimeSeconds;
    private static int currentTimeSeconds;
    private static int tickCounter = 0;
    private static boolean isRunning = false;
    private static String barTitle = "Time Remaining";
    private static Color currentColor = Color.RED;

    // ------------------------------------------------------------------
    // Setup — call this to start any timer with a boss bar
    // ------------------------------------------------------------------
    public static void setup(MinecraftServer server, int totalSeconds, String title, Color color, Runnable onFinish) {
        stop(server);

        totalTimeSeconds = totalSeconds;
        currentTimeSeconds = totalSeconds;
        barTitle = title;
        currentColor = color;
        tickCounter = 0;
        isRunning = true;

        bossBar = new ServerBossBar(
                Text.literal(formatTitle(currentTimeSeconds)),
                color.barColor,
                BossBar.Style.PROGRESS
        );
        bossBar.setVisible(true);
        bossBar.setPercent(1.0f);
        bossBar.setDarkenSky(false);
        bossBar.setDragonMusic(false);
        bossBar.setThickenFog(false);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            bossBar.addPlayer(player);
        }

        gameTimer = new GameTimer(
                TimerManager.secondsToTicks(totalSeconds),
                () -> {
                    stop(server);
                    if (onFinish != null) onFinish.run();
                }
        );

        TimerManager.addTimer(gameTimer);
    }

    // ------------------------------------------------------------------
    // Tick — call once per server tick from BattleManager
    // ------------------------------------------------------------------
    public static void tick(MinecraftServer server) {
        if (!isRunning) return;

        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            updateDisplay();
        }
    }

    // ------------------------------------------------------------------
    // Player management (for join/leave events)
    // ------------------------------------------------------------------
    public static void addPlayer(ServerPlayerEntity player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

    public static void removePlayer(ServerPlayerEntity player) {
        if (bossBar != null) bossBar.removePlayer(player);
    }

    // ------------------------------------------------------------------
    // Stop / cleanup
    // ------------------------------------------------------------------
    public static void stop(MinecraftServer server) {
        isRunning = false;
        tickCounter = 0;

        if (bossBar != null) {
            bossBar.clearPlayers();
            bossBar.setVisible(false);
            bossBar = null;
        }

        if (gameTimer != null) {
            TimerManager.removeTimer(gameTimer);
            gameTimer = null;
        }
    }

    // Alias kept so existing callers don't break during migration
    public static void cleanup(MinecraftServer server) {
        stop(server);
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------
    public static boolean isRunning() { return isRunning; }
    public static int getCurrentTimeSeconds() { return currentTimeSeconds; }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------
    private static void updateDisplay() {
        if (!isRunning || bossBar == null) return;

        if (currentTimeSeconds > 0) currentTimeSeconds--;

        float percent = totalTimeSeconds > 0
                ? (float) currentTimeSeconds / (float) totalTimeSeconds
                : 0.0f;

        bossBar.setPercent(Math.max(0.0f, Math.min(1.0f, percent)));
        bossBar.setName(Text.literal(formatTitle(currentTimeSeconds)));
    }

    private static String formatTitle(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return "§e" + barTitle + ": §f" + String.format("%02d:%02d", minutes, secs);
    }
}