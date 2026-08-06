package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.spawn.SpawnService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /spawn, /setspawn and /rx spawn / /rx setspawn.
 */
public final class SpawnModule {

    private final @NotNull RihanX plugin;

    public SpawnModule(@NotNull RihanX plugin) {
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

        SpawnService spawn = plugin.getSpawnService();
        String cmd = command.toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "spawn" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.SPAWN, messages)) {
                    yield true;
                }
                spawn.teleportToSpawn(player);
                yield true;
            }
            case "setspawn" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.SETSPAWN, messages)) {
                    yield true;
                }
                spawn.setSpawn(player);
                yield true;
            }
            default -> {
                CommandSupport.usage(messages, sender, "/spawn | /setspawn");
                yield true;
            }
        };
    }
}
