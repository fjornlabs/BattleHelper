package gg.cncmc.battlehelper.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gg.cncmc.battlehelper.utility.teamStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static gg.cncmc.battlehelper.utility.LuckPermsHook.hasPermission;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class battleTeamsCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("battleteams")
                    .then(literal("attacker")
                            .executes(battleTeamsCommand::attacker)
                    )
                    .then(literal("defender")
                            .executes(battleTeamsCommand::defender)
                    )
                    .then(literal("press")
                            .executes(battleTeamsCommand::press)
                            .then(literal("accept")
                                    .requires(source -> hasPermission(source, "battlehelper.manage"))
                                    .then(argument("target", EntityArgumentType.player())
                                            .executes(battleTeamsCommand::pressAccept)
                                    )
                            )
                    )
                    .then(literal("clear")
                            .requires(source -> hasPermission(source, "battlehelper.manage"))
                            .executes(battleTeamsCommand::clear)
                    )
            );
        });
    }

    private static int attacker(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (!(source.getEntity() instanceof PlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        String playerName = player.getName().getString();
        teamStorage storage = teamStorage.getInstance(source.getServer());

        storage.addAttacker(playerName);

        source.getServer().getCommandManager().executeWithPrefix(
                source.getServer().getCommandSource(),
                "lp user " + playerName + " parent add attacker"
        );

        source.sendFeedback(() ->
                Text.literal("[BattleHelper] You are now an Attacker").formatted(Formatting.RED), false);

        return 1;
    }

    private static int defender(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (!(source.getEntity() instanceof PlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        String playerName = player.getName().getString();
        teamStorage storage = teamStorage.getInstance(source.getServer());

        storage.addDefender(playerName);

        source.getServer().getCommandManager().executeWithPrefix(
                source.getServer().getCommandSource(),
                "lp user " + playerName + " parent add defender"
        );

        source.sendFeedback(() ->
                Text.literal("[BattleHelper] You are now a Defender").formatted(Formatting.BLUE), false);

        return 1;
    }

    private static int press(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        if (!(source.getEntity() instanceof PlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        String playerName = player.getName().getString();

        source.sendFeedback(() ->
                Text.literal("[BattleHelper] You must be accepted by staff!").formatted(Formatting.YELLOW), false);

        broadcastToPermission(
                source.getServer(),
                "battlehelper.manage",
                Text.literal("[BattleHelper] " + playerName + " is requesting to become press! /battleteams press accept " + playerName).formatted(Formatting.YELLOW)
        );

        return 1;
    }

    private static int pressAccept(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
        String playerName = target.getName().getString();
        teamStorage storage = teamStorage.getInstance(source.getServer());

        storage.addPress(playerName);

        source.getServer().getCommandManager().executeWithPrefix(
                source.getServer().getCommandSource(),
                "lp user " + playerName + " parent add press"
        );

        source.getServer().getCommandManager().executeWithPrefix(
                source.getServer().getCommandSource(),
                "gamemode spectator " + playerName
        );

        target.sendMessage(Text.literal("[BattleHelper] You have been accepted as press!").formatted(Formatting.GREEN));

        source.sendFeedback(() ->
                Text.literal("[BattleHelper] Accepted " + playerName + " as press!").formatted(Formatting.GREEN), false);

        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();
        teamStorage storage = teamStorage.getInstance(server);

        // Remove attacker roles
        for (String playerName : storage.getAttackers()) {
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "lp user " + playerName + " parent remove attacker"
            );
        }

        // Remove defender roles
        for (String playerName : storage.getDefenders()) {
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "lp user " + playerName + " parent remove defender"
            );
        }

        // Set press back to survival
        for (String playerName : storage.getPress()) {
            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "gamemode survival " + playerName
            );

            server.getCommandManager().executeWithPrefix(
                    server.getCommandSource(),
                    "lp user " + playerName + " parent remove press"
            );
        }

        storage.clearAll();

        source.sendFeedback(() ->
                Text.literal("[BattleHelper] All teams cleared!").formatted(Formatting.GREEN), false);

        return 1;
    }

    private static void broadcastToPermission(MinecraftServer server, String permission, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (hasPermission(player.getCommandSource(), permission)) {
                player.sendMessage(message);
            }
        }
    }
}