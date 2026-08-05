package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.warp.WarpService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /warp, /setwarp, /delwarp, /warps and /rx warp …
 */
public final class WarpModule {

    private final @NotNull RihanX plugin;

    public WarpModule(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public boolean handle(
            @NotNull CommandSender sender,
            @NotNull String command,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        Player player = CommandSupport.requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }

        WarpService warps = plugin.getWarpService();
        String cmd = command.toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "setwarp" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.WARP_SET, messages)) {
                    yield true;
                }
                if (args.length < 1) {
                    CommandSupport.usage(messages, sender, "/setwarp <name>");
                    yield true;
                }
                warps.setWarp(player, args[0]);
                yield true;
            }
            case "delwarp" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.WARP_DELETE, messages)) {
                    yield true;
                }
                if (args.length < 1) {
                    CommandSupport.usage(messages, sender, "/delwarp <name>");
                    yield true;
                }
                warps.delWarp(player, args[0]);
                yield true;
            }
            case "warps" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.WARP_LIST, messages)) {
                    yield true;
                }
                warps.sendWarpList(player);
                yield true;
            }
            case "warp" -> handleWarp(player, args, messages, warps);
            default -> {
                CommandSupport.usage(messages, sender, "/warp <name> | /setwarp <name> | /delwarp <name> | /warps");
                yield true;
            }
        };
    }

    private boolean handleWarp(
            @NotNull Player player,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull WarpService warps
    ) {
        if (args.length == 0) {
            CommandSupport.usage(messages, player, "/warp <name> | /warps");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set", "setwarp" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.WARP_SET, messages)) {
                    return true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/rx warp set <name>");
                    return true;
                }
                warps.setWarp(player, args[1]);
                return true;
            }
            case "del", "delete", "remove", "delwarp" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.WARP_DELETE, messages)) {
                    return true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/rx warp del <name>");
                    return true;
                }
                warps.delWarp(player, args[1]);
                return true;
            }
            case "list", "warps" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.WARP_LIST, messages)) {
                    return true;
                }
                warps.sendWarpList(player);
                return true;
            }
            default -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.WARP, messages)) {
                    return true;
                }
                warps.teleportWarp(player, args[0]);
                return true;
            }
        }
    }
}
