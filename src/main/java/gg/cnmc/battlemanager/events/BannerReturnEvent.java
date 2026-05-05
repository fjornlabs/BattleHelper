package gg.cnmc.battlemanager.events;

import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.Assault;
import gg.cnmc.battlemanager.battle.BannerTracker;
import gg.cnmc.battlemanager.battle.BattleState;
import gg.cnmc.battlemanager.utils.TeamStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class BannerReturnEvent {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BannerReturnEvent::tick);
    }

    private static void tick(MinecraftServer server) {
        if (BattleManager.state != BattleState.ROUND_ACTIVE) return;
        if (!BattleManager.battleMode.equals("assault")) return;
        if (!BannerTracker.isBannerPickedUp()) return;
        if (!BannerTracker.hasReturnPos()) return;

        String carrier = BannerTracker.getBannerCarrier();
        if (carrier == null) return;

        BlockPos returnPos = BannerTracker.getAttackerReturnPos();
        TeamStorage storage = TeamStorage.getInstance(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String name = player.getName().getString();

            // Must be the carrier
            if (!carrier.equals(name)) continue;

            // Must still be an attacker
            if (!storage.getAttackers().contains(name)) continue;

            // Must have the banner somewhere in their inventory (not just in hand)
            boolean hasBanner = player.getInventory().containsAny(
                    stack -> stack.getItem() == Items.BLUE_BANNER
            );
            if (!hasBanner) continue;

            // Must be standing on or within 2 blocks of the return position
            if (!player.getBlockPos().isWithinDistance(returnPos, 2.0)) continue;

            // Flip state immediately so this tick can't fire again
            BattleManager.state = BattleState.ROUND_ENDED;

            // Remove the banner from their inventory
            player.getInventory().remove(
                    stack -> stack.getItem() == Items.BLUE_BANNER,
                    1,
                    player.getInventory()
            );

            // Hand off to Assault — it broadcasts the win title + message and cleans up
            Assault.attackerWin(server);
            return;
        }
    }
}