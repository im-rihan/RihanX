package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.home.HomeService;
import com.rihanx.managers.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /home, /sethome, /delhome, /homes and /rx home …
 */
public final class HomeModule {

    private final @NotNull RihanX plugin;

    public HomeModule(@NotNull RihanX plugin) {
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

        HomeService homes = plugin.getHomeService();
        String cmd = command.toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "sethome" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.HOME_SET, messages)) {
                    yield true;
                }
                homes.setHome(player, args.length >= 1 ? args[0] : null);
                yield true;
            }
            case "delhome" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.HOME_DELETE, messages)) {
                    yield true;
                }
                homes.delHome(player, args.length >= 1 ? args[0] : null);
                yield true;
            }
            case "homes" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.HOME_LIST, messages)) {
                    yield true;
                }
                homes.sendHomeList(player);
                yield true;
            }
            case "home" -> handleHome(player, args, messages, homes);
            default -> {
                CommandSupport.usage(messages, sender, "/home [name] | /sethome [name] | /delhome [name] | /homes");
                yield true;
            }
        };
    }

    private boolean handleHome(
            @NotNull Player player,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull HomeService homes
    ) {
        if (args.length >= 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "set", "sethome" -> {
                    if (!CommandSupport.checkPerm(player, PermissionNodes.HOME_SET, messages)) {
                        return true;
                    }
                    homes.setHome(player, args.length >= 2 ? args[1] : null);
                    return true;
                }
                case "del", "delete", "remove", "delhome" -> {
                    if (!CommandSupport.checkPerm(player, PermissionNodes.HOME_DELETE, messages)) {
                        return true;
                    }
                    homes.delHome(player, args.length >= 2 ? args[1] : null);
                    return true;
                }
                case "list", "homes" -> {
                    if (!CommandSupport.checkPerm(player, PermissionNodes.HOME_LIST, messages)) {
                        return true;
                    }
                    homes.sendHomeList(player);
                    return true;
                }
                default -> { /* home name */ }
            }
        }
        if (!CommandSupport.checkPerm(player, PermissionNodes.HOME, messages)) {
            return true;
        }
        homes.teleportHome(player, args.length >= 1 ? args[0] : null);
        return true;
    }
}
