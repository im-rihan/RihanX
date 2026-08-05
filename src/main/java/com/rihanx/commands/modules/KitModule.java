package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.kits.KitService;
import com.rihanx.managers.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /kit and /kits.
 */
public final class KitModule {

    private final @NotNull RihanX plugin;

    public KitModule(@NotNull RihanX plugin) {
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

        KitService kits = plugin.getKitService();
        String cmd = command.toLowerCase(Locale.ROOT);

        return switch (cmd) {
            case "kits" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.KIT_LIST, messages)) {
                    yield true;
                }
                kits.sendKitList(player);
                yield true;
            }
            case "kit" -> {
                if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                    if (!CommandSupport.checkPerm(player, PermissionNodes.KIT_LIST, messages)) {
                        yield true;
                    }
                    kits.sendKitList(player);
                    yield true;
                }
                if (!CommandSupport.checkPerm(player, PermissionNodes.KIT, messages)) {
                    yield true;
                }
                kits.giveKit(player, args[0]);
                yield true;
            }
            default -> {
                CommandSupport.usage(messages, sender, "/kit <name> | /kits");
                yield true;
            }
        };
    }
}
