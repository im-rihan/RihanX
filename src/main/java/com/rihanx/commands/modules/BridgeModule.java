package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.bridge.BridgeService;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.NumberUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /bridge and /rx bridge …
 */
public final class BridgeModule {

    private final @NotNull RihanX plugin;

    public BridgeModule(@NotNull RihanX plugin) {
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
        if (!CommandSupport.checkPerm(player, PermissionNodes.BRIDGE, messages)) {
            return true;
        }

        BridgeService bridges = plugin.getBridgeService();
        if (args.length == 0) {
            if (!CommandSupport.checkPerm(player, PermissionNodes.BRIDGE_BUILD, messages)) {
                return true;
            }
            bridges.build(player, null, null, null);
            return true;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (first.equals("help") || first.equals("?")) {
            messages.send(player, "bridge-usage");
            return true;
        }
        if (first.equals("undo")) {
            if (!CommandSupport.checkPerm(player, PermissionNodes.BRIDGE_UNDO, messages)) {
                return true;
            }
            bridges.undo(player);
            return true;
        }

        if (!CommandSupport.checkPerm(player, PermissionNodes.BRIDGE_BUILD, messages)) {
            return true;
        }

        Integer length = NumberUtil.parseInt(args[0]);
        if (length == null) {
            // /bridge <material>
            bridges.build(player, null, null, args[0]);
            return true;
        }

        Integer width = null;
        String material = null;
        if (args.length >= 2) {
            Integer parsedWidth = NumberUtil.parseInt(args[1]);
            if (parsedWidth != null) {
                width = parsedWidth;
                if (args.length >= 3) {
                    material = args[2];
                }
            } else {
                material = args[1];
            }
        }
        bridges.build(player, length, width, material);
        return true;
    }
}
