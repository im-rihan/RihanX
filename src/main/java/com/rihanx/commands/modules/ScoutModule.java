package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.kingdom.Kingdom;
import com.rihanx.kingdom.KingdomService;
import com.rihanx.kingdom.KingdomTower;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.NumberUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Themed scout teleport: players, coordinates, kingdoms, and the tower network.
 */
public final class ScoutModule {

    private final @NotNull RihanX plugin;

    public ScoutModule(@NotNull RihanX plugin) {
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
        if (!CommandSupport.checkPerm(player, PermissionNodes.SCOUT, messages)) {
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            messages.send(player, "scout-usage");
            return true;
        }

        KingdomService kingdoms = plugin.getKingdomService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "tower" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.SCOUT_TOWER, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/scout tower <name>");
                    yield true;
                }
                KingdomTower tower = kingdoms.findTower(args[1]);
                Kingdom kingdom = tower == null ? null : kingdoms.kingdomForTower(tower);
                if (tower == null || kingdom == null) {
                    messages.send(player, "scout-tower-missing", MessageManager.placeholders("tower", args[1]));
                    yield true;
                }
                kingdoms.teleportToTower(player, kingdom, tower);
                yield true;
            }
            case "kingdom", "realm", "citadel" -> {
                if (!CommandSupport.checkPerm(player, PermissionNodes.SCOUT_KINGDOM, messages)) {
                    yield true;
                }
                Kingdom kingdom = kingdoms.resolve(player, args.length >= 2 ? args[1] : null);
                if (kingdom == null) {
                    messages.send(player, "kingdom-missing", MessageManager.placeholders("name", args.length >= 2 ? args[1] : "here"));
                    yield true;
                }
                kingdoms.teleportToKingdom(player, kingdom);
                yield true;
            }
            case "player" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/scout player <name>");
                    yield true;
                }
                Player target = CommandSupport.resolveNamedPlayer(player, args[1], messages);
                if (target == null) {
                    yield true;
                }
                plugin.getTeleportService().teleportToPlayer(player, target);
                Kingdom dest = kingdoms.at(target.getLocation());
                if (dest != null) {
                    kingdoms.playArrival(player, dest);
                }
                yield true;
            }
            case "pos", "location", "loc" -> {
                if (args.length < 4) {
                    CommandSupport.usage(messages, player, "/scout loc <x> <y> <z>");
                    yield true;
                }
                Double x = NumberUtil.parseDouble(args[1]);
                Double y = NumberUtil.parseDouble(args[2]);
                Double z = NumberUtil.parseDouble(args[3]);
                if (x == null || y == null || z == null) {
                    CommandSupport.invalidNumber(player, messages, args[1]);
                    yield true;
                }
                Location dest = new Location(player.getWorld(), x, y, z);
                plugin.getTeleportService().teleport(player, dest, "scout");
                yield true;
            }
            default -> {
                Player target = plugin.getServer().getPlayerExact(args[0]);
                if (target != null) {
                    plugin.getTeleportService().teleportToPlayer(player, target);
                    yield true;
                }
                Kingdom kingdom = kingdoms.byId(args[0]);
                if (kingdom != null) {
                    kingdoms.teleportToKingdom(player, kingdom);
                    yield true;
                }
                KingdomTower tower = kingdoms.findTower(args[0]);
                Kingdom owner = tower == null ? null : kingdoms.kingdomForTower(tower);
                if (tower != null && owner != null) {
                    kingdoms.teleportToTower(player, owner, tower);
                    yield true;
                }
                messages.send(player, "scout-usage");
                yield true;
            }
        };
    }
}
