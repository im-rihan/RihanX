package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.AfkManager;
import com.rihanx.managers.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles /afk and /rx afk.
 */
public final class AfkModule {

    private final @NotNull RihanX plugin;

    public AfkModule(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public boolean handle(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        Player player = CommandSupport.requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!CommandSupport.checkPerm(player, PermissionNodes.AFK, messages)) {
            return true;
        }

        AfkManager afkManager = plugin.getAfkManager();
        boolean nowAfk = afkManager.toggle(player);
        messages.broadcast(nowAfk ? "afk-on" : "afk-off", MessageManager.placeholders("player", player.getName()));
        return true;
    }
}
