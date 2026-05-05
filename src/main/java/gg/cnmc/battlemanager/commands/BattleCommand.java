package gg.cnmc.battlemanager.commands;

import com.mojang.brigadier.context.CommandContext;
import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.Assault;
import gg.cnmc.battlemanager.battle.BannerTracker;
import gg.cnmc.battlemanager.battle.BattleState;
import gg.cnmc.battlemanager.battle.Deathmatch;
import gg.cnmc.battlemanager.utils.time.TimerManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

import static gg.cnmc.battlemanager.utils.LuckPermsUtils.hasPermission;
import static net.minecraft.server.command.CommandManager.literal;

public class BattleCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("battle")
                            .requires(source -> hasPermission(source, "battlemanager.manage"))

                            .then(literal("prep")
                                    .executes(BattleCommand::prep)
                            )

                            .then(literal("start")
                                    .executes(BattleCommand::start)
                            )

                            .then(literal("stop")
                                    .executes(BattleCommand::stop)
                            )

                            .then(literal("mode")
                                    .then(literal("deathmatch")
                                            .executes(BattleCommand::setDeathmatch)
                                    )
                                    .then(literal("assault")
                                            .executes(BattleCommand::setAssault)
                                    )
                            )
            );
        });
    }

    private static int prep(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (BattleManager.state != BattleState.IDLE) {
            source.sendFeedback(
                    () -> Text.literal("[BattleManager] Can only enter prep from IDLE state!").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        if (BattleManager.battleMode.equals("none")) {
            source.sendFeedback(
                    () -> Text.literal("[BattleManager] Set a battle mode first! (/battle mode <mode>)").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        BattleManager.state = BattleState.PREP;

        source.getServer().getPlayerManager().sendToAll(
                new TitleS2CPacket(Text.literal("Battle is now in prep"))
        );

        source.getServer().getPlayerManager().sendToAll(
                new SubtitleS2CPacket(Text.literal(BattleManager.battleMode.toUpperCase(Locale.ROOT)))
        );

        if (BattleManager.battleMode.equals("deathmatch")) {
            source.getServer().getPlayerManager().broadcast(
                    Text.literal("[BattleManager] Use /battleteams to select a team").formatted(Formatting.YELLOW),
                    false
            );
            source.getServer().getPlayerManager().broadcast(
                    Text.literal(
                            "Fighters must defeat all enemy combatants. Team members who hide or otherwise prolong the battle unnecessarily may receive a penalty.\n" +
                                    "The battle is divided into 3 rounds.\n" +
                                    "Teams will respawn and replenish between rounds.\n" +
                                    "Each round takes 15 minutes.\n" +
                                    "Fighters have 1 life per round.\n" +
                                    "The fight may take place anywhere within the contested District.\n" +
                                    "Teams will be given a general idea of where the other team is starting.\n" +
                                    "Kills that happen outside the contested District will NOT be counted.\n" +
                                    "Vehicles are allowed to be used by both teams.\n" +
                                    "Neither side may enter their spawn area or the enemies spawn area during the round.\n"
                    ).formatted(Formatting.RED, Formatting.BOLD),
                    false
            );
        }

        if (BattleManager.battleMode.equals("assault")) {
            source.getServer().getPlayerManager().broadcast(
                    Text.literal("[BattleManager] Use /battleteams to select a team").formatted(Formatting.YELLOW),
                    false
            );
            source.getServer().getPlayerManager().broadcast(
                    Text.literal(
                            "Offense will assault 1 objective per round. There is 1 round.\n" +
                                    "Each round takes 45 minutes.\n" +
                                    "Defense must defend a unique banner objective a player on their team holds; the offense must seize that banner.\n" +
                                    "To seize the banner, the Offense must break the banner and return it to their spawn.\n" +
                                    "The defense may surround the banner with a max of 1 layer of Obsidian or blocks with an equivalent breaking time.\n" +
                                    "Teams will be given a general idea of where the other team is starting.\n" +
                                    "The player holding the objective may not do things that make them intentionally unlocatable.\n" +
                                    "Vehicles are allowed to be used by both teams.\n" +
                                    "Each side may enter their own spawn area during the battle, but not the enemies spawn area.\n" +
                                    "Each side has infinite respawns.\n"
                    ).formatted(Formatting.RED, Formatting.BOLD),
                    false
            );
            AssaultCommand.giveItems(ctx);
        }

        source.sendFeedback(
                () -> Text.literal("[BattleManager] Battle is now in prep").formatted(Formatting.YELLOW),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Battle prep started by {}", source.getName());
        return 1;
    }

    private static int start(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (BattleManager.state != BattleState.PREP) {
            source.sendFeedback(
                    () -> Text.literal("[BattleManager] You must run /battle prep first!").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        if (BattleManager.battleMode.equals("deathmatch")) {
            Deathmatch.start(source.getServer());
        } else if (BattleManager.battleMode.equals("assault")) {
            if (BannerTracker.getBannerPos() == null || BannerTracker.getAttackerReturnPos() == null){
                source.sendFeedback(
                        () -> Text.literal("[BattleManager] Set Banner Positions first!").formatted(Formatting.RED), false
                );
                return 0;
            }

            Assault.start(source.getServer());
        }

        source.sendFeedback(
                () -> Text.literal("[BattleManager] Battle started in mode: " + BattleManager.battleMode).formatted(Formatting.GREEN),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Battle started by {}", source.getName());
        return 1;
    }

    private static int stop(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (BattleManager.state == BattleState.IDLE) {
            source.sendFeedback(
                    () -> Text.literal("[BattleManager] There is no active battle!").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        if (BattleManager.state == BattleState.PREP) {
            BattleManager.state = BattleState.IDLE;
            BattleTeamsCommand.clearAll(ctx);
            BattleManager.battleMode = "none";
            source.sendFeedback(
                    () -> Text.literal("[BattleManager] Prep cancelled.").formatted(Formatting.YELLOW),
                    false
            );
            return 1;
        }

        TimerManager.clearAll();

        if (BattleManager.battleMode.equals("deathmatch")) {
            Deathmatch.forceStop(source.getServer());
        } else if (BattleManager.battleMode.equals("assault")) {
            Assault.forceStop(source.getServer());
        }

        BattleTeamsCommand.clearAll(ctx);
        BattleManager.battleMode = "none";

        source.sendFeedback(
                () -> Text.literal("[BattleManager] Battle stopped!").formatted(Formatting.GREEN),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Battle stopped by {}", source.getName());
        return 1;
    }

    private static int setDeathmatch(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (BattleManager.state != BattleState.IDLE) {
            source.sendFeedback(
                    () -> Text.literal("[BattleManager] Cannot change mode during an active battle!").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        BattleManager.battleMode = "deathmatch";

        source.sendFeedback(
                () -> Text.literal("[BattleManager] Battle mode set to deathmatch.").formatted(Formatting.GREEN),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Battle mode set to deathmatch by {}", source.getName());
        return 1;
    }

    private static int setAssault(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (BattleManager.state != BattleState.IDLE) {
            source.sendFeedback(
                    () -> Text.literal("[BattleManager] Cannot change mode during an active battle!").formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        BattleManager.battleMode = "assault";

        source.sendFeedback(
                () -> Text.literal("[BattleManager] Battle mode set to assault.").formatted(Formatting.GREEN),
                false
        );

        BattleManager.LOGGER.info("[BattleManager] Battle mode set to assault by {}", source.getName());
        return 1;
    }
}