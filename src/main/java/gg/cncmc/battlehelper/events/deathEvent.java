package gg.cncmc.battlehelper.events;

import gg.cncmc.battlehelper.BattleHelper;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.text.Text;

public class deathEvent {

    public static void register() {
        // Fires just before a living entity actually dies
        ServerLivingEntityEvents.ALLOW_DEATH.register(deathEvent::onPlayerDeath);
    }

    /**
     * Called when any LivingEntity is about to die.
     * If a battle is active and the dying entity is a player,
     * switch them to Spectator and cancel the death so they don't drop items / respawn.
     *
     * Return true  → allow the death to proceed normally.
     * Return false → cancel the death (entity stays alive at 0 HP briefly, then we heal).
     */
    private static boolean onPlayerDeath(LivingEntity entity, DamageSource source, float damageAmount) {
        if (!BattleHelper.battleActive) {
            // Battle not active — let death happen normally
            return true;
        }

        if (!(entity instanceof ServerPlayerEntity player)) {
            // Not a player — let it die normally
            return true;
        }

        // Switch to Spectator mode
        player.changeGameMode(GameMode.SPECTATOR);

        // Heal the player so they don't stay at 0 HP when death is cancelled
        player.setHealth(player.getMaxHealth());

        // Notify the player
        player.sendMessage(
                Text.literal("§cYou have been eliminated! You are now a spectator."),
                false
        );

        // Broadcast to the server
        if (player.getServer() != null) {
            player.getServer().getPlayerManager().broadcast(
                    Text.literal("§e" + player.getName().getString() + " §7has been eliminated from the battle!"),
                    false
            );
        }

        // Cancel the death event
        return false;
    }
}