package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.kingdom.GateSide;
import com.rihanx.kingdom.Kingdom;
import com.rihanx.kingdom.KingdomRole;
import com.rihanx.kingdom.KingdomService;
import com.rihanx.kingdom.WardLayer;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

/**
 * Handles {@code /kingdom} · {@code /realm} · {@code /citadel} · {@code /rx kingdom}.
 */
public final class KingdomModule {

    private final @NotNull RihanX plugin;

    public KingdomModule(@NotNull RihanX plugin) {
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
        if (!CommandSupport.checkPerm(player, PermissionNodes.KINGDOM, messages)) {
            return true;
        }
        KingdomService kingdoms = plugin.getKingdomService();
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            messages.send(player, "kingdom-usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "undo" -> {
                kingdoms.builder().undo(player);
                yield true;
            }
            case "create", "found" -> {
                if (!CommandSupport.checkOpPerm(player, PermissionNodes.KINGDOM_CREATE, messages)) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/kingdom create <name> [radius]");
                    yield true;
                }
                Integer radius = null;
                if (args.length >= 3) {
                    radius = NumberUtil.parseInt(args[2]);
                    if (radius == null) {
                        CommandSupport.invalidNumber(player, messages, args[2]);
                        yield true;
                    }
                }
                kingdoms.create(player, args[1], radius);
                yield true;
            }
            case "expand" -> {
                Kingdom kingdom = requireManaged(player, args, 2, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/kingdom expand <blocks>");
                    yield true;
                }
                Integer blocks = NumberUtil.parseInt(args[1]);
                if (blocks == null) {
                    CommandSupport.invalidNumber(player, messages, args[1]);
                    yield true;
                }
                kingdoms.expand(player, kingdom, blocks);
                yield true;
            }
            case "seal" -> {
                Kingdom kingdom = requireManaged(player, args, 1, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.seal(player, kingdom);
                yield true;
            }
            case "unseal" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/kingdom unseal <layer>");
                    yield true;
                }
                WardLayer layer = WardLayer.fromKey(args[1]);
                if (layer == null) {
                    messages.send(player, "kingdom-layer-unknown", MessageManager.placeholders("layer", args[1]));
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 2, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.unseal(player, kingdom, layer);
                yield true;
            }
            case "towers", "tower" -> handleTowers(player, args, messages, kingdoms);
            case "beacon" -> {
                if (args.length < 3 || !args[1].equalsIgnoreCase("color")) {
                    CommandSupport.usage(messages, player, "/kingdom beacon color <#hex|rainbow>");
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 3, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.setBeaconColor(player, kingdom, args[2]);
                yield true;
            }
            case "info" -> {
                Kingdom kingdom = requireAny(player, args, 1, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.sendInfo(player, kingdom);
                yield true;
            }
            case "list" -> {
                kingdoms.sendList(player);
                yield true;
            }
            case "dissolve", "disband" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                    kingdoms.confirmDissolve(player);
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 1, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.requestDissolve(player, kingdom);
                yield true;
            }
            case "invite" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/kingdom invite <player>");
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 2, messages, kingdoms);
                Player target = CommandSupport.resolveNamedPlayer(player, args[1], messages);
                if (kingdom == null || target == null) {
                    yield true;
                }
                kingdoms.invite(player, kingdom, target);
                yield true;
            }
            case "accept", "join" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/kingdom accept <name>");
                    yield true;
                }
                Kingdom kingdom = kingdoms.byId(args[1]);
                if (kingdom == null) {
                    messages.send(player, "kingdom-missing", MessageManager.placeholders("name", args[1]));
                    yield true;
                }
                kingdoms.acceptInvite(player, kingdom);
                yield true;
            }
            case "kick" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/kingdom kick <player>");
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 2, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                UUID id = target.getUniqueId();
                String name = target.getName() == null ? args[1] : target.getName();
                kingdoms.kick(player, kingdom, id, name);
                yield true;
            }
            case "role" -> {
                if (args.length < 3) {
                    CommandSupport.usage(messages, player, "/kingdom role <player> <sovereign|warden|citizen|guest>");
                    yield true;
                }
                KingdomRole role = KingdomRole.fromKey(args[2]);
                if (role == null) {
                    messages.send(player, "kingdom-role-unknown", MessageManager.placeholders("role", args[2]));
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 3, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String name = target.getName() == null ? args[1] : target.getName();
                kingdoms.setMemberRole(player, kingdom, target.getUniqueId(), name, role);
                yield true;
            }
            case "chat" -> {
                if (args.length >= 2) {
                    Kingdom kingdom = kingdoms.at(player.getLocation());
                    if (kingdom == null || !kingdom.isMember(player.getUniqueId())) {
                        messages.send(player, "kingdom-not-inside");
                        yield true;
                    }
                    kingdoms.sendChat(player, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
                    yield true;
                }
                kingdoms.toggleChat(player);
                yield true;
            }
            case "rules", "rule" -> {
                if (args.length < 2) {
                    CommandSupport.usage(messages, player, "/kingdom rules <pvp|mobs|fire|explosions>");
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 2, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.toggleRule(player, kingdom, args[1]);
                yield true;
            }
            case "gate" -> handleGate(player, args, messages, kingdoms);
            case "ward" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("show")) {
                    kingdoms.toggleWardShow(player);
                    yield true;
                }
                messages.send(player, "kingdom-usage");
                yield true;
            }
            case "drain" -> {
                Kingdom kingdom = requireManaged(player, args, 1, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.drain(player, kingdom);
                yield true;
            }
            case "restore" -> {
                Kingdom kingdom = requireManaged(player, args, 1, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.restoreFluids(player, kingdom);
                yield true;
            }
            case "banner" -> {
                Kingdom kingdom = requireManaged(player, args, 1, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.setBanner(player, kingdom);
                yield true;
            }
            case "compass" -> {
                kingdoms.giveCompass(player);
                yield true;
            }
            case "members" -> {
                Kingdom kingdom = requireAny(player, args, 1, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                messages.send(player, "kingdom-members-header", MessageManager.placeholders("name", kingdom.displayName()));
                for (var entry : kingdom.members().entrySet()) {
                    OfflinePlayer member = Bukkit.getOfflinePlayer(entry.getKey());
                    String name = member.getName() == null ? entry.getKey().toString().substring(0, 8) : member.getName();
                    messages.send(player, "kingdom-members-line", MessageManager.placeholders(
                            "player", name,
                            "role", entry.getValue().display()
                    ));
                }
                yield true;
            }
            default -> {
                messages.send(player, "kingdom-usage");
                yield true;
            }
        };
    }

    private boolean handleTowers(
            @NotNull Player player,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull KingdomService kingdoms
    ) {
        if (args.length < 2) {
            CommandSupport.usage(messages, player, "/kingdom towers <build|height|light|undo>");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("undo")) {
            kingdoms.builder().undo(player);
            return true;
        }
        Kingdom kingdom = requireManaged(player, args, action.equals("height") || action.equals("light") ? 3 : 2, messages, kingdoms);
        if (kingdom == null) {
            return true;
        }
        return switch (action) {
            case "build", "raise" -> {
                kingdoms.builder().buildTowers(player, kingdom);
                yield true;
            }
            case "undo" -> {
                kingdoms.builder().undo(player);
                yield true;
            }
            case "height" -> {
                if (args.length < 3) {
                    CommandSupport.usage(messages, player, "/kingdom towers height <blocks>");
                    yield true;
                }
                Integer height = NumberUtil.parseInt(args[2]);
                if (height == null) {
                    CommandSupport.invalidNumber(player, messages, args[2]);
                    yield true;
                }
                kingdoms.setTowerHeight(player, kingdom, height);
                yield true;
            }
            case "light", "lit" -> {
                if (args.length < 3) {
                    CommandSupport.usage(messages, player, "/kingdom towers light <tier|all>");
                    yield true;
                }
                kingdoms.lightTowers(player, kingdom, args[2]);
                yield true;
            }
            default -> {
                messages.send(player, "kingdom-usage");
                yield true;
            }
        };
    }

    private boolean handleGate(
            @NotNull Player player,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull KingdomService kingdoms
    ) {
        if (args.length < 2) {
            CommandSupport.usage(messages, player, "/kingdom gate <create|open|close|list>");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "create" -> {
                if (args.length < 4) {
                    CommandSupport.usage(messages, player, "/kingdom gate create <name> <side>");
                    yield true;
                }
                GateSide side = GateSide.fromKey(args[3]);
                if (side == null) {
                    messages.send(player, "kingdom-side-unknown", MessageManager.placeholders("side", args[3]));
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 4, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.createGate(player, kingdom, args[2], side);
                yield true;
            }
            case "open" -> {
                if (args.length < 3) {
                    CommandSupport.usage(messages, player, "/kingdom gate open <name> [duration]");
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 3, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                String duration = args.length >= 4 ? args[3] : null;
                kingdoms.openGate(player, kingdom, args[2], duration);
                yield true;
            }
            case "close" -> {
                if (args.length < 3) {
                    CommandSupport.usage(messages, player, "/kingdom gate close <name>");
                    yield true;
                }
                Kingdom kingdom = requireManaged(player, args, 3, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.closeGate(player, kingdom, args[2]);
                yield true;
            }
            case "list" -> {
                Kingdom kingdom = requireAny(player, args, 2, messages, kingdoms);
                if (kingdom == null) {
                    yield true;
                }
                kingdoms.listGates(player, kingdom);
                yield true;
            }
            default -> {
                messages.send(player, "kingdom-usage");
                yield true;
            }
        };
    }

    private @org.jetbrains.annotations.Nullable Kingdom requireAny(
            @NotNull Player player,
            @NotNull String[] args,
            int nameIndex,
            @NotNull MessageManager messages,
            @NotNull KingdomService kingdoms
    ) {
        String name = args.length > nameIndex ? args[nameIndex] : null;
        Kingdom kingdom = kingdoms.resolve(player, name);
        if (kingdom == null) {
            messages.send(player, "kingdom-not-inside");
        }
        return kingdom;
    }

    private @org.jetbrains.annotations.Nullable Kingdom requireManaged(
            @NotNull Player player,
            @NotNull String[] args,
            int nameIndex,
            @NotNull MessageManager messages,
            @NotNull KingdomService kingdoms
    ) {
        return requireAny(player, args, nameIndex, messages, kingdoms);
    }
}
