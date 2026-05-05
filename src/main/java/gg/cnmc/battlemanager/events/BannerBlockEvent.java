package gg.cnmc.battlemanager.events;

import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.BannerTracker;
import gg.cnmc.battlemanager.battle.BattleState;
import gg.cnmc.battlemanager.utils.TeamStorage;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class BannerBlockEvent {

    public static void register() {

        // UseBlockCallback: placing blue/red concrete sets the marker positions.
        // Placing the blue banner on blue concrete announces the objective.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            ItemStack held = player.getStackInHand(hand);
            // The block will be placed on the face of the hit block
            BlockPos placedPos = hitResult.getBlockPos().offset(hitResult.getSide());

            if (held.getItem() == Items.BLUE_CONCRETE) {
                BannerTracker.setBannerPos(placedPos);
                serverPlayer.sendMessage(
                        Text.literal("§9Banner marker set at " + fmt(placedPos) + ". Now place the blue banner on top of this block."),
                        false
                );
                return ActionResult.PASS;
            }

            if (held.getItem() == Items.RED_CONCRETE) {
                BannerTracker.setAttackerReturnPos(placedPos);
                serverPlayer.sendMessage(
                        Text.literal("§cAttacker return position set at " + fmt(placedPos)),
                        false
                );
                return ActionResult.PASS;
            }

            // Detect blue banner being placed on top of the tracked blue concrete block.
            // The banner will land at placedPos; the blue concrete should be directly below it.
            if (held.getItem() == Items.BLUE_BANNER) {
                BlockPos below = placedPos.down();
                if (BannerTracker.hasBannerPos()
                        && below.equals(BannerTracker.getBannerPos())
                        && world.getBlockState(below).getBlock() == Blocks.BLUE_CONCRETE) {

                    // Update the tracked position to where the banner itself will be
                    BannerTracker.setBannerPos(placedPos);

                    if (!BannerTracker.isBannerPlacedAndAnnounced()) {
                        BannerTracker.setBannerAnnounced(true);
                        BannerTracker.setOriginalBannerPos(placedPos); // ← add this
                        serverPlayer.getServer().getPlayerManager().broadcast(
                                Text.literal("§9The banner objective has been placed!"),
                                false
                        );
                    }
                }
            }

            return ActionResult.PASS;
        });

        // PlayerBlockBreakEvents: attacker breaking the objective banner seizes it.
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return true;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;

            // Only intercept during an active assault round
            if (BattleManager.state != BattleState.ROUND_ACTIVE) return true;
            if (!BattleManager.battleMode.equals("assault")) return true;

            // Only care about banners
            if (!(state.getBlock() instanceof BannerBlock)) return true;

            // Only care about the tracked objective banner
            if (!BannerTracker.hasBannerPos()) return true;
            if (!pos.equals(BannerTracker.getBannerPos())) return true;

            // Already picked up — shouldn't happen, but guard anyway
            if (BannerTracker.isBannerPickedUp()) return false;

            TeamStorage storage = TeamStorage.getInstance(serverPlayer.getServer());
            String name = serverPlayer.getName().getString();

            // Defenders cannot seize their own banner
            if (!storage.getAttackers().contains(name)) {
                serverPlayer.sendMessage(Text.literal("§cOnly attackers can seize the banner!"), false);
                return false;
            }

            // Mark as picked up before allowing the break so the tick loop can't race us
            BannerTracker.setBannerPickedUp(true, name);

            serverPlayer.getServer().getPlayerManager().broadcast(
                    Text.literal("§e" + name + " §ahas seized the banner! Return it to attacker spawn!"),
                    false
            );

            // Allow the break so the banner disappears from the world
            return true;
        });
    }

    private static String fmt(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}