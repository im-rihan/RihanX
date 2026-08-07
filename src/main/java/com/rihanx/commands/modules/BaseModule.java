package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.base.BaseService;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /base and /rx base …
 */
public final class BaseModule {

    private final @NotNull RihanX plugin;

    public BaseModule(@NotNull RihanX plugin) {
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
        if (!CommandSupport.checkPerm(player, PermissionNodes.BASE, messages)) {
            return true;
        }

        BaseService bases = plugin.getBaseService();
        if (args.length == 0 || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("help")
                || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                bases.sendList(player);
                messages.send(player, "base-usage");
            } else {
                bases.openMenu(player);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("undo")) {
            if (!CommandSupport.checkPerm(player, PermissionNodes.BASE_UNDO, messages)) {
                return true;
            }
            bases.undo(player);
            return true;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        if (!CommandSupport.checkPerm(player, PermissionNodes.BASE_BUILD, messages)) {
            return true;
        }
        bases.paste(player, name);
        return true;
    }
}
