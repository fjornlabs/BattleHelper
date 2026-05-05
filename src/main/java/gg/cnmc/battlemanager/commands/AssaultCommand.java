package gg.cnmc.battlemanager.commands;

import com.mojang.brigadier.context.CommandContext;
import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.BannerTracker;
import gg.cnmc.battlemanager.battle.BattleState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static gg.cnmc.battlemanager.utils.LuckPermsUtils.hasPermission;
import static net.minecraft.server.command.CommandManager.literal;

public class AssaultCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("assault")
                            .requires(source -> hasPermission(source, "battlemanager.manage"))

                            .then(literal("giveitems")
                                    .executes(AssaultCommand::giveItems)
                            )

                            .then(literal("resetmarkers")
                                    .executes(AssaultCommand::resetMarkers)
                            )
            );
        });
    }

    public static int giveItems(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (BattleManager.state != BattleState.PREP && !BattleManager.battleMode.equals("assault")){
            source.sendFeedback(() -> Text.literal("[BattleManager] No Battle of type Assault is in prep"), false);
            return 0;
        }

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        ItemStack banner = new ItemStack(Items.BLUE_BANNER);
        banner.setCustomName(Text.literal("§9§lBanner Objective").styled(s -> s.withItalic(false)));

        ItemStack blueConcrete = new ItemStack(Items.BLUE_CONCRETE, 1);
        blueConcrete.setCustomName(Text.literal("§9§lDefender Banner Block").styled(s -> s.withItalic(false)));

        ItemStack redConcrete = new ItemStack(Items.RED_CONCRETE, 1);
        redConcrete.setCustomName(Text.literal("§c§lAttacker Return Block").styled(s -> s.withItalic(false)));

        player.getInventory().offerOrDrop(banner);
        player.getInventory().offerOrDrop(blueConcrete);
        player.getInventory().offerOrDrop(redConcrete);

        source.sendFeedback(() -> Text.literal("[BattleManager] Given assault objective items.").formatted(Formatting.GREEN), false);
        source.sendFeedback(() -> Text.literal("§7Place the §9blue concrete §7where the banner will be defended."), false);
        source.sendFeedback(() -> Text.literal("§7Place the §cred concrete §7at attacker spawn where they return the banner."), false);
        source.sendFeedback(() -> Text.literal("§7Place the §9blue banner §7on top of the blue concrete."), false);

        return 1;
    }

    private static int resetMarkers(CommandContext<ServerCommandSource> ctx) {
        BannerTracker.reset();
        ctx.getSource().sendFeedback(
                () -> Text.literal("[BattleManager] Assault markers reset.").formatted(Formatting.YELLOW),
                false
        );
        return 1;
    }
}