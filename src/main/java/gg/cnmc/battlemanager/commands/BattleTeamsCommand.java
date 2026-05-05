package gg.cnmc.battlemanager.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gg.cnmc.battlemanager.BattleManager;
import gg.cnmc.battlemanager.battle.BattleState;
import gg.cnmc.battlemanager.utils.LuckPermsUtils;
import gg.cnmc.battlemanager.utils.TeamStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import static gg.cnmc.battlemanager.utils.LuckPermsUtils.hasPermission;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BattleTeamsCommand {

    /**
     * Returns true if the source can modify teams right now.
     * - In PREP: anyone with battlemanager.teams can manage
     * - During battle (any non-IDLE, non-PREP state): only battlemanager.manage
     * - In IDLE: nobody can manage
     */
    private static boolean canManageTeams(ServerCommandSource source) {
        BattleState state = BattleManager.state;

        if (state == BattleState.IDLE) return false;

        if (state == BattleState.PREP) {
            return true;
        }

        // Active battle — only admins
        return hasPermission(source, "battlemanager.teams");
    }

    private static boolean sendLockedMessage(ServerCommandSource source) {
        BattleState state = BattleManager.state;

        if (state == BattleState.IDLE) {
            source.sendFeedback(() -> Text.literal("[BattleHelper] No battle is in prep or active!").formatted(Formatting.RED), false);
        } else {
            source.sendFeedback(() -> Text.literal("[BattleHelper] You dont have permission to do team changes during a battle!").formatted(Formatting.RED), false);
        }
        return false;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("battleteams")
                    .then(literal("attacker")
                            .executes(BattleTeamsCommand::attacker)
                            .then(argument("target", EntityArgumentType.player())
                                    .requires(source -> hasPermission(source, "battlemanager.teams"))
                                    .executes(BattleTeamsCommand::setUserAttacker))
                    )
                    .then(literal("defender")
                            .executes(BattleTeamsCommand::defender)
                            .then(argument("target", EntityArgumentType.player())
                                    .requires(source -> hasPermission(source, "battlemanager.teams"))
                                    .executes(BattleTeamsCommand::setUserDefender))
                    )
                    .then(literal("press")
                            .executes(BattleTeamsCommand::press)
                            .then(argument("target", EntityArgumentType.player())
                                    .requires(source -> hasPermission(source, "battlemanager.teams"))
                                    .executes(BattleTeamsCommand::setUserPress))
                    )
                    .then(literal("clear")
                            .then(literal("all")
                                    .requires(source -> hasPermission(source, "battlemanager.teams"))
                                    .executes(BattleTeamsCommand::clearAll)
                            )
                            .executes(BattleTeamsCommand::clear)
                    )
            );
        });
    }

    private static int attacker(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        if (!canManageTeams(source)) return sendLockedMessage(source) ? 1 : 0;

        if (!(source.getEntity() instanceof PlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        TeamStorage storage = TeamStorage.getInstance(source.getServer());
        storage.addAttacker(player.getName().getString());

        LuckPermsUtils.removeGroup(source, "defender");
        LuckPermsUtils.removeGroup(source, "press");
        LuckPermsUtils.addGroup(source, "attacker");

        source.sendFeedback(() ->
                Text.literal("[BattleManager] You are now an Attacker").formatted(Formatting.RED), false);
        return 1;
    }

    private static int setUserAttacker(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!canManageTeams(source)) return sendLockedMessage(source) ? 1 : 0;

        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
        TeamStorage storage = TeamStorage.getInstance(source.getServer());
        storage.addAttacker(target.getName().getString());

        LuckPermsUtils.removeGroup(target, "defender");
        LuckPermsUtils.removeGroup(target, "press");
        LuckPermsUtils.addGroup(target, "attacker");

        target.sendMessage(Text.literal("[BattleManager] You have been set as an Attacker").formatted(Formatting.RED));
        source.sendFeedback(() ->
                Text.literal("[BattleManager] Set " + target.getName().getString() + " as an Attacker").formatted(Formatting.RED), false);
        return 1;
    }

    private static int defender(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        if (!canManageTeams(source)) return sendLockedMessage(source) ? 1 : 0;

        if (!(source.getEntity() instanceof PlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        TeamStorage storage = TeamStorage.getInstance(source.getServer());
        storage.addDefender(player.getName().getString());

        LuckPermsUtils.removeGroup(source, "attacker");
        LuckPermsUtils.removeGroup(source, "press");
        LuckPermsUtils.addGroup(source, "defender");

        source.sendFeedback(() ->
                Text.literal("[BattleManager] You are now a Defender").formatted(Formatting.BLUE), false);
        return 1;
    }

    private static int setUserDefender(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!canManageTeams(source)) return sendLockedMessage(source) ? 1 : 0;

        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
        TeamStorage storage = TeamStorage.getInstance(source.getServer());
        storage.addDefender(target.getName().getString());

        LuckPermsUtils.removeGroup(target, "attacker");
        LuckPermsUtils.removeGroup(target, "press");
        LuckPermsUtils.addGroup(target, "defender");

        target.sendMessage(Text.literal("[BattleManager] You have been set as a Defender").formatted(Formatting.BLUE));
        source.sendFeedback(() ->
                Text.literal("[BattleManager] Set " + target.getName().getString() + " as a Defender").formatted(Formatting.BLUE), false);
        return 1;
    }

    private static int press(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        if (!canManageTeams(source)) return sendLockedMessage(source) ? 1 : 0;

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        TeamStorage storage = TeamStorage.getInstance(source.getServer());
        storage.addPress(player.getName().getString());

        LuckPermsUtils.removeGroup(player, "attacker");
        LuckPermsUtils.removeGroup(player, "defender");
        LuckPermsUtils.addGroup(player, "press");

        player.changeGameMode(GameMode.SPECTATOR);

        source.sendFeedback(() ->
                Text.literal("[BattleManager] You are now Press (spectator)").formatted(Formatting.YELLOW), false);
        return 1;
    }

    private static int setUserPress(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!canManageTeams(source)) return sendLockedMessage(source) ? 1 : 0;

        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
        TeamStorage storage = TeamStorage.getInstance(source.getServer());
        storage.addPress(target.getName().getString());

        LuckPermsUtils.removeGroup(target, "attacker");
        LuckPermsUtils.removeGroup(target, "defender");
        LuckPermsUtils.addGroup(target, "press");

        target.changeGameMode(GameMode.SPECTATOR);

        target.sendMessage(Text.literal("[BattleManager] You have been set as Press").formatted(Formatting.YELLOW));
        source.sendFeedback(() ->
                Text.literal("[BattleManager] Set " + target.getName().getString() + " as Press").formatted(Formatting.YELLOW), false);
        return 1;
    }

    public static int clearAll(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();
        TeamStorage storage = TeamStorage.getInstance(server);

        for (String playerName : storage.getAttackers()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null) LuckPermsUtils.removeGroup(player, "attacker");
        }

        for (String playerName : storage.getDefenders()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null) LuckPermsUtils.removeGroup(player, "defender");
        }

        for (String playerName : storage.getPress()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null) {
                player.changeGameMode(GameMode.SURVIVAL);
                LuckPermsUtils.removeGroup(player, "press");
            }
        }

        storage.clearAll();

        source.sendFeedback(() ->
                Text.literal("[BattleManager] All teams cleared!").formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        if (!canManageTeams(source)) return sendLockedMessage(source) ? 1 : 0;

        if (!(source.getEntity() instanceof PlayerEntity player)) {
            source.sendFeedback(() -> Text.literal("Only players can use this!"), false);
            return 0;
        }

        TeamStorage storage = TeamStorage.getInstance(source.getServer());
        String playerName = player.getName().getString();

        if (storage.getAttackers().contains(playerName)) {
            LuckPermsUtils.removeGroup(source, "attacker");
            storage.removeAttacker(playerName);
        } else if (storage.getDefenders().contains(playerName)) {
            LuckPermsUtils.removeGroup(source, "defender");
            storage.removeDefender(playerName);
        } else if (storage.getPress().contains(playerName)) {
            LuckPermsUtils.removeGroup(source, "press");
            storage.removePress(playerName);
        }

        source.sendFeedback(() ->
                Text.literal("[BattleManager] You have been removed from your team").formatted(Formatting.GREEN), false);
        return 1;
    }
}