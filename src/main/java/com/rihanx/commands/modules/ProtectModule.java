package com.rihanx.commands.modules;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.CommandSupport;
import com.rihanx.managers.MessageManager;
import com.rihanx.protection.FlagValue;
import com.rihanx.protection.ProtectionFlag;
import com.rihanx.utils.MessageUtil;
import com.rihanx.utils.PermissionUtil;
import com.rihanx.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

/**
 * World/region protection command handler.
 */
public final class ProtectModule {

    private final @NotNull RihanX plugin;

    public ProtectModule(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    public boolean handle(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        if (!PermissionUtil.hasOpOnly(sender, PermissionNodes.PROTECT)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            CommandSupport.usage(messages, sender,
                    "/rx protect <flag|flags|wand|pos1|pos2|define|redefine|delete|info|list|setflag|addmember|removemember|addowner|removeowner|priority|bypass>");
            return true;
        }
        var protection = plugin.getProtectionService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "flag" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_FLAG, messages)) return true;
                if (args.length < 3) {
                    CommandSupport.usage(messages, sender, "/rx protect flag <flag> <allow|deny|unset> [world]");
                    return true;
                }
                ProtectionFlag flag = ProtectionFlag.fromKey(args[1]);
                FlagValue value = FlagValue.parse(args[2]);
                if (flag == null || value == null) {
                    messages.send(sender, "invalid-argument", MessageManager.placeholders("input", args[1] + "/" + args[2]));
                    return true;
                }
                World world = resolveWorld(sender, args, 3, messages);
                if (world == null) return true;
                protection.setWorldFlag(sender, world, flag, value);
            }
            case "flags" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_FLAG, messages)) return true;
                World world = resolveWorld(sender, args, 1, messages);
                if (world == null) return true;
                protection.listWorldFlags(sender, world);
            }
            case "wand" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_WAND, messages)) return true;
                Player player = CommandSupport.requirePlayer(sender, messages);
                if (player == null) return true;
                protection.giveWand(player);
            }
            case "pos1", "pos2" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_WAND, messages)) return true;
                Player player = CommandSupport.requirePlayer(sender, messages);
                if (player == null) return true;
                protection.setPos(player, sub.equals("pos1") ? 1 : 2, player.getLocation());
            }
            case "define" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                Player player = CommandSupport.requirePlayer(sender, messages);
                if (player == null) return true;
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx protect define <name>");
                    return true;
                }
                protection.define(player, args[1]);
            }
            case "redefine" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                Player player = CommandSupport.requirePlayer(sender, messages);
                if (player == null) return true;
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx protect redefine <name>");
                    return true;
                }
                protection.redefine(player, args[1]);
            }
            case "delete" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                if (args.length < 2) {
                    CommandSupport.usage(messages, sender, "/rx protect delete <name> [world]");
                    return true;
                }
                World world = resolveWorld(sender, args, 2, messages);
                if (world == null) return true;
                String regionName = args[1];
                if (sender instanceof Player player) {
                    plugin.getGuiManager().confirm(
                            MessageUtil.parse("<red>Delete region " + regionName + "?</red>"),
                            confirmed -> protection.delete(confirmed, world, regionName),
                            cancelled -> messages.send(cancelled, "confirm-cancelled")
                    ).open(player);
                } else {
                    protection.delete(sender, world, regionName);
                }
            }
            case "info" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                Player player = CommandSupport.requirePlayer(sender, messages);
                if (player == null) return true;
                protection.sendInfo(sender, player, args.length >= 2 ? args[1] : null);
            }
            case "list" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                World world = resolveWorld(sender, args, 1, messages);
                if (world == null) return true;
                protection.listRegions(sender, world);
            }
            case "setflag" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                if (args.length < 4) {
                    CommandSupport.usage(messages, sender, "/rx protect setflag <name> <flag> <allow|deny|unset>");
                    return true;
                }
                Player player = CommandSupport.requirePlayer(sender, messages);
                if (player == null) return true;
                ProtectionFlag flag = ProtectionFlag.fromKey(args[2]);
                FlagValue value = FlagValue.parse(args[3]);
                if (flag == null || value == null) {
                    messages.send(sender, "invalid-argument", MessageManager.placeholders("input", args[2] + "/" + args[3]));
                    return true;
                }
                protection.setRegionFlag(sender, player.getWorld(), args[1], flag, value);
            }
            case "addmember", "removemember", "addowner", "removeowner" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                if (args.length < 3) {
                    CommandSupport.usage(messages, sender, "/rx protect " + sub + " <name> <player>");
                    return true;
                }
                Player actor = CommandSupport.requirePlayer(sender, messages);
                if (actor == null) return true;
                ResolvedTarget target = resolveOfflineTarget(sender, args[2], messages);
                if (target == null) return true;
                switch (sub) {
                    case "addmember" -> protection.addMember(sender, actor.getWorld(), args[1], target.id(), target.name());
                    case "removemember" -> protection.removeMember(sender, actor.getWorld(), args[1], target.id(), target.name());
                    case "addowner" -> protection.addOwner(sender, actor.getWorld(), args[1], target.id(), target.name());
                    case "removeowner" -> protection.removeOwner(sender, actor.getWorld(), args[1], target.id(), target.name());
                    default -> { }
                }
            }
            case "priority" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_REGION, messages)) return true;
                if (args.length < 3) {
                    CommandSupport.usage(messages, sender, "/rx protect priority <name> <priority>");
                    return true;
                }
                Player actor = CommandSupport.requirePlayer(sender, messages);
                if (actor == null) return true;
                Integer priority = com.rihanx.utils.NumberUtil.parseInt(args[2]);
                if (priority == null) {
                    CommandSupport.invalidNumber(sender, messages, args[2]);
                    return true;
                }
                protection.setPriority(sender, actor.getWorld(), args[1], priority);
            }
            case "bypass" -> {
                if (!CommandSupport.checkOpPerm(sender, PermissionNodes.PROTECT_BYPASS, messages)) return true;
                Player player = CommandSupport.requirePlayer(sender, messages);
                if (player == null) return true;
                boolean enabled = protection.toggleBypass(player);
                messages.send(player, enabled ? "protect-bypass-on" : "protect-bypass-off");
            }
            default -> CommandSupport.usage(messages, sender,
                    "/rx protect <flag|flags|wand|pos1|pos2|define|redefine|delete|info|list|setflag|addmember|removemember|addowner|removeowner|priority|bypass>");
        }
        return true;
    }

    private @org.jetbrains.annotations.Nullable World resolveWorld(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            int worldIndex,
            @NotNull MessageManager messages
    ) {
        if (args.length > worldIndex) {
            World world = Bukkit.getWorld(args[worldIndex]);
            if (world == null) {
                messages.send(sender, "world-not-found", MessageManager.placeholders("world", args[worldIndex]));
                return null;
            }
            return world;
        }
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        messages.send(sender, "player-only");
        return null;
    }

    private @org.jetbrains.annotations.Nullable ResolvedTarget resolveOfflineTarget(
            @NotNull CommandSender sender,
            @NotNull String name,
            @NotNull MessageManager messages
    ) {
        Player online = PlayerUtil.findPlayer(name);
        if (online != null) {
            return new ResolvedTarget(online.getUniqueId(), online.getName());
        }
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.getUniqueId() == null) {
            messages.send(sender, "player-not-found", MessageManager.placeholders("player", name));
            return null;
        }
        String targetName = offline.getName() == null ? name : offline.getName();
        return new ResolvedTarget(offline.getUniqueId(), targetName);
    }

    private record ResolvedTarget(@NotNull UUID id, @NotNull String name) {
    }
}
