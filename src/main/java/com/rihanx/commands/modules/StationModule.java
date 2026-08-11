package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.base.StationService;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

/**
 * Handles /station and /rx station …
 */
public final class StationModule {

    private static final Set<String> TEMPLATE_IDS = Set.of(
            "station", "depot", "crossing", "rail", "terminal", "mine",
            "kingdom", "western", "adacia", "yard"
    );

    private final @NotNull RihanX plugin;

    public StationModule(@NotNull RihanX plugin) {
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
        if (!CommandSupport.checkPerm(player, PermissionNodes.STATION, messages)) {
            return true;
        }

        StationService stations = plugin.getStationService();
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            messages.send(player, "station-usage");
            stations.sendList(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "list", "templates" -> {
                stations.sendList(player);
                messages.send(player, "station-usage");
                yield true;
            }
            case "stops", "destinations" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.STATION_LIST, messages)) {
                    yield true;
                }
                stations.sendStops(player);
                yield true;
            }
            case "set", "register", "add" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.STATION_LINK, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/station set <stopName>");
                    yield true;
                }
                stations.registerStop(player, args[1]);
                yield true;
            }
            case "link" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.STATION_LINK, messages)) {
                    yield true;
                }
                if (args.length < 3) {
                    CommandSupport.usage(messages, player, "/station link <stopA> <stopB>");
                    yield true;
                }
                stations.linkStops(player, args[1], args[2]);
                yield true;
            }
            case "unlink", "delete", "del", "remove" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.STATION_LINK, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/station delete <stopName|all>");
                    yield true;
                }
                stations.deleteStop(player, args[1]);
                yield true;
            }
            case "undo" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.STATION_UNDO, messages)) {
                    yield true;
                }
                plugin.getBaseService().undo(player);
                yield true;
            }
            case "build", "paste" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.STATION_BUILD, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/station build <template> [stopName]");
                    yield true;
                }
                String template = args[1].toLowerCase(Locale.ROOT);
                String stopName = args.length >= 3 ? args[2] : null;
                stations.paste(player, template, stopName);
                yield true;
            }
            default -> {
                // /station <template> [stopName]
                if (TEMPLATE_IDS.contains(sub) || stations.get(sub) != null) {
                    if (!CommandSupport.checkPerm(player, PermissionNodes.STATION_BUILD, messages)) {
                        yield true;
                    }
                    String stopName = args.length >= 2 ? args[1] : null;
                    stations.paste(player, sub, stopName);
                    yield true;
                }
                messages.send(player, "station-usage");
                stations.sendList(player);
                yield true;
            }
        };
    }
}
