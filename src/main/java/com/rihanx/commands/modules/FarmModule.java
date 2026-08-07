package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.base.FarmService;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /farm and /rx farm …
 */
public final class FarmModule {

    private final @NotNull RihanX plugin;

    public FarmModule(@NotNull RihanX plugin) {
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
        if (!CommandSupport.checkPerm(player, PermissionNodes.FARM, messages)) {
            return true;
        }

        FarmService farms = plugin.getFarmService();
        if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")
                || args[0].equalsIgnoreCase("help")) {
            farms.openMenu(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            farms.sendList(player);
            messages.send(player, "farm-usage");
            return true;
        }

        if (!CommandSupport.checkPerm(player, PermissionNodes.FARM_BUILD, messages)) {
            return true;
        }
        farms.paste(player, args[0].toLowerCase(Locale.ROOT));
        return true;
    }
}
