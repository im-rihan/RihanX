package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.portal.PortalService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Handles /portal and /rx portal …
 */
public final class PortalModule {

    private final @NotNull RihanX plugin;

    public PortalModule(@NotNull RihanX plugin) {
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
        if (!CommandSupport.checkPerm(player, PermissionNodes.PORTAL, messages)) {
            return true;
        }

        PortalService portals = plugin.getPortalService();
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            messages.send(player, "portal-usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create", "set", "make" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.PORTAL_CREATE, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/portal create <name>");
                    yield true;
                }
                portals.create(player, args[1]);
                yield true;
            }
            case "link" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.PORTAL_LINK, messages)) {
                    yield true;
                }
                if (args.length < 3) {
                    CommandSupport.usage(messages, player, "/portal link <nameA> <nameB>");
                    yield true;
                }
                portals.link(player, args[1], args[2]);
                yield true;
            }
            case "delete", "del", "remove" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.PORTAL_DELETE, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/portal delete <name>");
                    yield true;
                }
                portals.delete(player, args[1]);
                yield true;
            }
            case "list" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.PORTAL_LIST, messages)) {
                    yield true;
                }
                portals.sendList(player);
                yield true;
            }
            case "tp", "teleport", "use" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.PORTAL_TP, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/portal tp <name>");
                    yield true;
                }
                portals.teleportTo(player, args[1]);
                yield true;
            }
            default -> {
                messages.send(player, "portal-usage");
                yield true;
            }
        };
    }
}
