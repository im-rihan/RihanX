package com.rihanx.commands;

import com.rihanx.managers.MessageManager;
import com.rihanx.utils.PermissionUtil;
import com.rihanx.utils.PlayerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared command helpers for {@link RihanXCommand} and module handlers.
 */
public final class CommandSupport {

    private CommandSupport() {
    }

    public static @Nullable Player requirePlayer(
            @NotNull CommandSender sender,
            @NotNull MessageManager messages
    ) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return null;
        }
        return player;
    }

    public static void usage(@NotNull MessageManager messages, @NotNull CommandSender sender, @NotNull String usage) {
        messages.send(sender, "invalid-usage", MessageManager.placeholders("usage", usage));
    }

    public static boolean checkPerm(
            @NotNull CommandSender sender,
            @NotNull String permission,
            @NotNull MessageManager messages
    ) {
        if (!PermissionUtil.has(sender, permission)) {
            messages.send(sender, "no-permission");
            return false;
        }
        return true;
    }

    public static boolean checkOpPerm(
            @NotNull CommandSender sender,
            @NotNull String permission,
            @NotNull MessageManager messages
    ) {
        if (!PermissionUtil.hasOpOnly(sender, permission)) {
            messages.send(sender, "no-permission");
            return false;
        }
        return true;
    }

    /**
     * No / blank name → sender only. Explicit name → that one player only.
     */
    public static @Nullable Player resolveSelfOrNamed(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            int nameIndex,
            @NotNull MessageManager messages
    ) {
        if (args.length > nameIndex) {
            String raw = args[nameIndex] == null ? "" : args[nameIndex].trim();
            if (!raw.isEmpty()) {
                return resolveNamedPlayer(sender, raw, messages);
            }
        }
        return requirePlayer(sender, messages);
    }

    public static @Nullable Player resolveNamedPlayer(
            @NotNull CommandSender sender,
            @NotNull String name,
            @NotNull MessageManager messages
    ) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            messages.send(sender, "player-not-found", MessageManager.placeholders("player", name));
            return null;
        }
        return PlayerUtil.findPlayerOrHint(sender, trimmed, messages);
    }

    public static void invalidNumber(
            @NotNull CommandSender sender,
            @NotNull MessageManager messages,
            @NotNull String input
    ) {
        messages.send(sender, "invalid-number", MessageManager.placeholders("input", input));
    }
}
