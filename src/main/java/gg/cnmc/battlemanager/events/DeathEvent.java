package gg.cnmc.battlemanager.events;

import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.*;
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

public class DeathEvent {

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register(DeathEvent::onPlayerDeath);
    }

    private static boolean onPlayerDeath(LivingEntity entity, DamageSource source, float damageAmount) {
        if (!BattleManager.isActive()) return true;
        if (!(entity instanceof ServerPlayerEntity player)) return true;

        MinecraftServer server = player.getServer();
        String playerName = player.getName().getString();
        TeamStorage storage = TeamStorage.getInstance(server);

        if (!storage.getAttackers().contains(playerName) && !storage.getDefenders().contains(playerName)) {
            return true;
        }

        if (BattleManager.battleMode.equals("assault")) {
            if (server != null) {

                // Handle banner return before dropping items
                if (BannerTracker.isBannerPickedUp() && playerName.equals(BannerTracker.getBannerCarrier())) {
                    // Remove the banner from inventory first so it doesn't get dropped
                    net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
                    for (int i = 0; i < inv.size(); i++) {
                        net.minecraft.item.ItemStack stack = inv.getStack(i);
                        if (stack.getItem() == net.minecraft.item.Items.BLUE_BANNER) {
                            inv.removeStack(i);
                            break;
                        }
                    }

                    BannerTracker.returnBanner(server, player);
                    server.getPlayerManager().broadcast(
                            Text.literal("§9The banner has been returned to the defenders!"),
                            false
                    );
                }

                // Manually drop all remaining inventory items at death position
                net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    net.minecraft.item.ItemStack stack = inv.removeStack(i);
                    if (!stack.isEmpty()) {
                        player.dropItem(stack, true, false);
                    }
                }

                player.setHealth(player.getMaxHealth());
                Assault.teleportToSpawn(player, server);
            }
            return false;
        }


        // Deathmatch elimination
        if (BattleData.isEliminated(player)) return false;

        if (server != null) {

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

        net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            net.minecraft.item.ItemStack stack = inv.removeStack(i);
            if (!stack.isEmpty()) {
                player.dropItem(stack, true, false);
            }
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