package gg.cnmc.battlemanager.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class LuckPermsUtils {

    // --- Permission Check ---

    public static boolean hasPermission(ServerCommandSource source, String permission) {
        try {
            if (source.getEntity() == null) return source.hasPermissionLevel(2);

            User user = LuckPermsProvider.get()
                    .getUserManager()
                    .getUser(source.getPlayer().getUuid());

            if (user == null) return source.hasPermissionLevel(2);

            return user.getCachedData()
                    .getPermissionData()
                    .checkPermission(permission)
                    .asBoolean();
        } catch (Exception e) {
            return source.hasPermissionLevel(2);
        }
    }

    // --- Add Group ---

    public static void addGroup(ServerPlayerEntity player, String group) {
        addGroup(player.getUuid(), group);
    }

    public static void addGroup(ServerCommandSource source, String group) {
        try { addGroup(source.getPlayer().getUuid(), group); } catch (Exception ignored) {}
    }

    public static void addGroup(UUID uuid, String group) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;
            user.data().add(InheritanceNode.builder(group).build());
            luckPerms.getUserManager().saveUser(user);
        });
    }

    // --- Remove Group ---

    public static void removeGroup(ServerPlayerEntity player, String group) {
        removeGroup(player.getUuid(), group);
    }

    public static void removeGroup(ServerCommandSource source, String group) {
        try { removeGroup(source.getPlayer().getUuid(), group); } catch (Exception ignored) {}
    }

    public static void removeGroup(UUID uuid, String group) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;
            Set<InheritanceNode> toRemove = user.data().toCollection().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> (InheritanceNode) node)
                    .filter(node -> node.getGroupName().equals(group))
                    .collect(Collectors.toSet());
            toRemove.forEach(node -> user.data().remove(node));
            luckPerms.getUserManager().saveUser(user);
        });
    }

    // --- Remove Groups By Prefix ---

    public static void removeGroupsByPrefix(ServerPlayerEntity player, String prefix) {
        removeGroupsByPrefix(player.getUuid(), prefix);
    }

    public static void removeGroupsByPrefix(ServerCommandSource source, String prefix) {
        try { removeGroupsByPrefix(source.getPlayer().getUuid(), prefix); } catch (Exception ignored) {}
    }

    public static void removeGroupsByPrefix(UUID uuid, String prefix) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) return;
            Set<InheritanceNode> toRemove = user.data().toCollection().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> (InheritanceNode) node)
                    .filter(node -> node.getGroupName().startsWith(prefix))
                    .collect(Collectors.toSet());
            toRemove.forEach(node -> user.data().remove(node));
            luckPerms.getUserManager().saveUser(user);
        });
    }

}