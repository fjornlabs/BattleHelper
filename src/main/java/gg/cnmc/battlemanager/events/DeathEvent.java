package gg.cnmc.battlemanager.events;

import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.Assault;
import gg.cnmc.battlemanager.battle.BattleData;
import gg.cnmc.battlemanager.battle.Deathmatch;
import gg.cnmc.battlemanager.battle.ui.DeathmatchScoreboard;
import gg.cnmc.battlemanager.utils.TeamStorage;
import gg.cnmc.battlemanager.utils.time.TimerManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import gg.cnmc.battlemanager.battle.BannerTracker;

public class DeathEvent {

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register(DeathEvent::onPlayerDeath);
    }

    private static boolean onPlayerDeath(LivingEntity entity, DamageSource source, float damageAmount) {
        if (!BattleManager.isActive()) return true;
        if (!(entity instanceof ServerPlayerEntity player)) return true;

        MinecraftServer server = player.getServer();

        // Assault mode — infinite respawns, tp back to spawn
        if (BattleManager.battleMode.equals("assault")) {
            if (server != null) {
                player.setHealth(player.getMaxHealth());
                Assault.teleportToSpawn(player, server);

                String playerName = player.getName().getString();
                if (BannerTracker.isBannerPickedUp() && playerName.equals(BannerTracker.getBannerCarrier())) {
                    BannerTracker.returnBanner(server, player); // ← pass player directly
                    server.getPlayerManager().broadcast(
                            Text.literal("§9The banner has been returned to the defenders!"),
                            false
                    );
                }
            }
            return false;
        }

        // Deathmatch elimination
        if (BattleData.isEliminated(player)) return false;

        if (server != null) {
            String playerName = player.getName().getString();
            TeamStorage storage = TeamStorage.getInstance(server);

            if (storage.getAttackers().contains(playerName)) {
                DeathmatchScoreboard.addDeath(server, "attackers");
            } else if (storage.getDefenders().contains(playerName)) {
                DeathmatchScoreboard.addDeath(server, "defenders");
            }

            server.getPlayerManager().broadcast(
                    Text.literal("§e" + playerName + " §7has been eliminated from the battle!"),
                    false
            );

            TimerManager.runLater(() -> Deathmatch.checkRoundEndCondition(server));
        }

        player.changeGameMode(GameMode.SPECTATOR);
        BattleData.addEliminated(player);
        player.setHealth(player.getMaxHealth());

        player.sendMessage(
                Text.literal("§cYou have been eliminated! You are now a spectator."),
                false
        );

        return false;
    }
}