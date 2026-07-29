package com.rihanx.utils;

import com.rihanx.api.PermissionNodes;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Permission check helpers.
 */
public final class PermissionUtil {

    private PermissionUtil() {
    }

    public static boolean has(@NotNull CommandSender sender, @NotNull String permission) {
        return sender.hasPermission(permission) || sender.hasPermission(PermissionNodes.ALL);
    }

    /**
     * Ops-only gate for powerful admin commands (give, god, fly, gamemode, etc.).
     * Console always counts as op.
     */
    public static boolean hasOpOnly(@NotNull CommandSender sender, @NotNull String permission) {
        return sender.isOp() && has(sender, permission);
    }

    public static boolean hasOrAdmin(@NotNull CommandSender sender, @NotNull String permission) {
        return has(sender, permission) || sender.hasPermission(PermissionNodes.ADMIN);
    }

    public static boolean bypassCooldown(@NotNull CommandSender sender) {
        return has(sender, PermissionNodes.BYPASS_COOLDOWN) || sender.hasPermission(PermissionNodes.ADMIN);
    }

    public static boolean bypassTeleportDelay(@NotNull Player player) {
        return has(player, PermissionNodes.BYPASS_TELEPORT_DELAY) || player.hasPermission(PermissionNodes.ADMIN);
    }

    public static boolean canSeeVanished(@NotNull CommandSender sender) {
        return has(sender, PermissionNodes.SEE_VANISHED);
    }

    public static boolean isPlayer(@NotNull CommandSender sender) {
        return sender instanceof Player;
    }

    public static @NotNull Player asPlayer(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("Sender is not a player");
        }
        return player;
    }
}
