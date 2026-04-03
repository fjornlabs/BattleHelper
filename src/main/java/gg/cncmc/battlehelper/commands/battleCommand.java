package gg.cncmc.battlehelper.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import gg.cncmc.battlehelper.BattleHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import static gg.cncmc.battlehelper.utility.LuckPermsHook.hasPermission;
import static net.minecraft.server.command.CommandManager.literal;

public class battleCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("battle")
                            .requires(source -> hasPermission(source, "battlehelper.manage"))// OP level 2 required

                            // /battle start
                            .then(literal("start")
                                    .executes(battleCommand::start)
                            )

                            .then(literal("stop")
                                    .executes(battleCommand::stop)
                            )

            );
        });
    }

    private static int start(CommandContext<ServerCommandSource> ctx){
        ServerCommandSource source = ctx.getSource();

        if (BattleHelper.battleActive) {
            source.sendFeedback(
                    () -> Text.literal("[BattleHelper] A battle is already active!").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        BattleHelper.battleActive = true;

        source.getServer().getPlayerManager().sendToAll(new TitleS2CPacket(Text.literal("§l§cBATTLE started")));

        BattleHelper.LOGGER.info("[BattleHelper] Battle started by {}", source.getName());
        return 1;
    }

    private static int stop(CommandContext<ServerCommandSource> ctx){
        ServerCommandSource source = ctx.getSource();

        if (!BattleHelper.battleActive) {
            source.sendFeedback(
                    () -> Text.literal("[BattleHelper] There is no active Battle!").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        BattleHelper.battleActive = false;



        BattleHelper.LOGGER.info("[BattleHelper] Battle stopped by {}", source.getName());
        return 1;
    }

}