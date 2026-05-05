package gg.cnmc.battlemanager.events;

import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.BattleData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

public class JoinHandler {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            if (!BattleManager.isActive()) {
                if (BattleData.isEliminated(player)) {
                    player.changeGameMode(GameMode.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                    BattleData.removeEliminated(player);
                }
            }
        });
    }
}