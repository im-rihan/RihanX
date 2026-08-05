package com.rihanx.tabcomplete;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.RihanXCommand;
import com.rihanx.utils.BiomeUtil;
import com.rihanx.utils.PermissionUtil;
import com.rihanx.utils.PlayerUtil;
import com.rihanx.utils.StructureUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tab completion for /rx and standalone module commands.
 * Always returns a mutable list (Paper/clients handle this more reliably).
 */
public final class RihanXTabCompleter implements TabCompleter {

    private static final List<String> MODULES = List.of(
            "help", "slime", "world", "find", "chunk", "tp", "back", "player", "inventory", "item",
            "search", "server", "performance", "protect", "edit", "home", "warp", "tpa", "kit", "admin"
    );

    private static final Set<String> TP_PLAYER_SUBS = Set.of("player", "here", "home");
    private static final Set<String> PLAYER_NAME_ACTIONS = Set.of(
            "info", "heal", "feed", "fly", "god", "freeze", "unfreeze", "vanish", "cleareffects", "clearpotions", "ping"
    );
    private static final List<String> EDIT_DIRECTIONS = List.of(
            "up", "down", "north", "south", "east", "west", "vert", "all"
    );
    private static final Set<String> INV_PLAYER_SUBS = Set.of("see", "ender", "clear", "repair");

    private final @NotNull RihanX plugin;

    public RihanXTabCompleter(@NotNull RihanX plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!canTab(sender, command)) {
            return new ArrayList<>();
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        boolean root = RihanXCommand.ROOT_NAMES.contains(commandName);

        final String module;
        final String[] prefixed;

        if (root) {
            if (args.length <= 1) {
                return filter(MODULES, args.length == 0 ? "" : args[0]);
            }
            module = RihanXCommand.normalizeModule(args[0]);
            prefixed = args;
        } else {
            module = RihanXCommand.normalizeModule(commandName);
            if (module.equals("back")) {
                return new ArrayList<>();
            }
            if (RihanXCommand.SELF_ACTION_COMMANDS.contains(module)) {
                return completeSelfAction(module, args);
            }
            prefixed = new String[args.length + 1];
            prefixed[0] = module;
            System.arraycopy(args, 0, prefixed, 1, args.length);
        }

        return switch (module) {
            case "slime" -> completeSlime(prefixed);
            case "world" -> completeWorld(prefixed);
            case "find" -> completeFind(prefixed);
            case "chunk" -> completeChunk(prefixed);
            case "tp" -> completeTp(prefixed);
            case "player" -> completePlayer(prefixed);
            case "inventory", "inv" -> completeInventory(prefixed);
            case "item" -> completeItem(prefixed);
            case "search" -> completeSearch(prefixed);
            case "server" -> completeServer(prefixed);
            case "performance", "perf" -> completePerformance(prefixed);
            case "protect", "guard" -> completeProtect(prefixed);
            case "edit", "we" -> completeEdit(prefixed);
            case "home", "sethome", "delhome", "homes" -> completeHome(sender, module, prefixed);
            case "warp", "setwarp", "delwarp", "warps" -> completeWarp(module, prefixed);
            case "tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel" -> completeTpa(module, prefixed);
            case "kit", "kits" -> completeKit(module, prefixed);
            case "admin" -> completeAdmin(prefixed);
            default -> new ArrayList<>();
        };
    }

    private boolean canTab(@NotNull CommandSender sender, @NotNull Command command) {
        if (sender.isOp()) {
            return true;
        }
        if (PermissionUtil.has(sender, PermissionNodes.USE)) {
            return true;
        }
        String perm = command.getPermission();
        return perm != null && !perm.isEmpty() && sender.hasPermission(perm);
    }

    private @NotNull List<String> completeSelfAction(@NotNull String action, @NotNull String[] args) {
        if (action.equals("gm")) {
            if (args.length <= 1) {
                return filter(List.of("survival", "creative", "adventure", "spectator"),
                        args.length == 0 ? "" : args[0]);
            }
            if (args.length == 2) {
                return players(args[1]);
            }
            return new ArrayList<>();
        }
        if (action.equals("vanish")) {
            return new ArrayList<>();
        }
        // /fly [player] — show names even when args is empty or [""]
        if (args.length <= 1) {
            return players(args.length == 0 ? "" : args[0]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeSlime(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("nearest", "search", "density", "map", "tp"), args[1]);
        }
        if (args.length == 3 && List.of("nearest", "search", "density", "map").contains(args[1].toLowerCase(Locale.ROOT))) {
            return filter(List.of("8", "16", "32", "64"), args[2]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeWorld(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("info", "seed", "weather", "difficulty", "time", "border", "spawn", "setspawn"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("weather")) {
            return filter(List.of("clear", "rain", "thunder"), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("difficulty")) {
            return filter(List.of("peaceful", "easy", "normal", "hard"), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("time")) {
            return filter(List.of("day", "noon", "night", "midnight", "0", "6000", "13000", "18000"), args[2]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeFind(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("biome", "structure"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("biome")) {
            return filter(BiomeUtil.suggestions(args[2]), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("structure")) {
            return filter(StructureUtil.suggestions(args[2]), args[2]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeChunk(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("info", "load", "unload", "regenerate", "border", "entities", "tileentities"), args[1]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeTp(@NotNull String[] args) {
        if (args.length == 2) {
            String partial = args[1];
            List<String> subs = filter(List.of(
                    "pos", "player", "here", "home", "world", "biome", "structure", "chunk", "random", "safe", "back"
            ), partial);
            // Client often sends ["tp","player"] when asking for the next token — show names
            if (TP_PLAYER_SUBS.contains(partial.toLowerCase(Locale.ROOT))) {
                return players("");
            }
            return subs;
        }
        if (args.length >= 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (TP_PLAYER_SUBS.contains(sub)) {
                // args[2] may be missing empty string; use last arg as prefix
                String prefix = args.length >= 3 ? args[args.length - 1] : "";
                if (args.length == 3) {
                    return players(args[2]);
                }
                if (args.length == 4 && sub.equals("player")) {
                    return players(args[3]);
                }
                return players(prefix);
            }
            if (args.length == 3 && sub.equals("world")) {
                return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[2]);
            }
            if (args.length == 3 && sub.equals("biome")) {
                return filter(BiomeUtil.suggestions(args[2]), args[2]);
            }
            if (args.length == 3 && sub.equals("structure")) {
                return filter(StructureUtil.suggestions(args[2]), args[2]);
            }
            if (args.length == 5 && sub.equals("pos")) {
                return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[4]);
            }
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completePlayer(@NotNull String[] args) {
        if (args.length == 2) {
            String partial = args[1];
            List<String> actions = filter(List.of(
                    "info", "heal", "feed", "fly", "god", "gamemode", "speed", "freeze", "unfreeze",
                    "vanish", "cleareffects", "ping"
            ), partial);
            if (PLAYER_NAME_ACTIONS.contains(partial.toLowerCase(Locale.ROOT))) {
                return players("");
            }
            return actions;
        }
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("speed")) {
                return filter(List.of("0.1", "0.2", "0.5", "1.0"), args[2]);
            }
            if (args[1].equalsIgnoreCase("gamemode") || args[1].equalsIgnoreCase("gm")) {
                return filter(List.of("survival", "creative", "adventure", "spectator"), args[2]);
            }
            return players(args[2]);
        }
        if (args.length == 4 && (args[1].equalsIgnoreCase("speed")
                || args[1].equalsIgnoreCase("gamemode")
                || args[1].equalsIgnoreCase("gm"))) {
            return players(args[3]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeInventory(@NotNull String[] args) {
        if (args.length == 2) {
            String partial = args[1];
            List<String> subs = filter(List.of("see", "ender", "clear", "repair"), partial);
            if (INV_PLAYER_SUBS.contains(partial.toLowerCase(Locale.ROOT))) {
                return players("");
            }
            return subs;
        }
        if (args.length == 3) {
            return players(args[2]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeItem(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("info", "give", "rename", "lore", "enchant", "repair"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("give")) {
            return filter(com.rihanx.utils.MaterialUtil.suggestions(args[2]), args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            List<String> options = new ArrayList<>(List.of("1", "16", "32", "64"));
            options.addAll(PlayerUtil.onlineNames(args[3]));
            return filter(options, args[3]);
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("give")) {
            return players(args[4]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeSearch(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("slime", "cave", "lava", "water", "spawner", "village"), args[1]);
        }
        if (args.length == 3) {
            return filter(List.of("16", "32", "64", "128"), args[2]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeServer(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("info", "tps", "mspt", "memory", "uptime"), args[1]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completePerformance(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("chunks", "entities"), args[1]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeProtect(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of(
                    "flag", "flags", "wand", "pos1", "pos2", "define", "redefine", "delete",
                    "info", "list", "setflag", "addmember", "removemember", "addowner", "removeowner",
                    "priority", "bypass"
            ), args[1]);
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("flag") || sub.equals("setflag")) {
                return filter(Arrays.asList(com.rihanx.protection.ProtectionFlag.keys()), args[2]);
            }
            if (List.of("delete", "info", "redefine", "setflag", "addmember", "removemember",
                    "addowner", "removeowner", "priority").contains(sub)) {
                return filter(regionNames(), args[2]);
            }
            if (sub.equals("flags") || sub.equals("list")) {
                return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[2]);
            }
        }
        if (args.length == 4) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("flag") || sub.equals("setflag")) {
                return filter(List.of("allow", "deny", "unset", "true", "false"), args[3]);
            }
            if (List.of("addmember", "removemember", "addowner", "removeowner").contains(sub)) {
                return players(args[3]);
            }
            if (sub.equals("priority")) {
                return filter(List.of("0", "1", "5", "10", "100"), args[3]);
            }
            if (sub.equals("delete")) {
                return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[3]);
            }
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("flag")) {
            return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[4]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> regionNames() {
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            plugin.getProtectionService().getRegions().getRegions(world.getName())
                    .forEach(region -> names.add(region.getName()));
        }
        return names;
    }

    private @NotNull List<String> completeEdit(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of(
                    "wand", "pos1", "pos2", "size", "count", "set", "replace", "walls", "outline",
                    "hollow", "clear", "copy", "paste", "rotate", "expand", "contract", "undo", "redo"
            ), args[1]);
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (List.of("set", "walls", "outline", "hollow", "count").contains(sub)) {
                return filter(com.rihanx.utils.MaterialUtil.suggestions(args[2]), args[2]);
            }
            if (sub.equals("replace")) {
                return filter(com.rihanx.utils.MaterialUtil.suggestions(args[2]), args[2]);
            }
            if (sub.equals("rotate")) {
                return filter(List.of("90", "180", "270"), args[2]);
            }
            if (sub.equals("expand") || sub.equals("contract")) {
                return filter(List.of("1", "5", "10", "16", "32"), args[2]);
            }
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("replace")) {
            return filter(com.rihanx.utils.MaterialUtil.suggestions(args[3]), args[3]);
        }
        if (args.length == 4 && (args[1].equalsIgnoreCase("expand") || args[1].equalsIgnoreCase("contract"))) {
            return filter(EDIT_DIRECTIONS, args[3]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeHome(
            @NotNull CommandSender sender,
            @NotNull String module,
            @NotNull String[] args
    ) {
        if (module.equals("homes")) {
            return new ArrayList<>();
        }
        List<String> homes = homeNamesFor(sender instanceof org.bukkit.entity.Player player ? player : null);
        if (args.length == 2) {
            if (module.equals("home")) {
                List<String> options = new ArrayList<>(List.of("set", "del", "list"));
                options.addAll(homes);
                return filter(options, args[1]);
            }
            return filter(homes, args[1]);
        }
        if (args.length == 3 && module.equals("home")
                && List.of("set", "del", "delete", "remove", "sethome", "delhome").contains(args[1].toLowerCase(Locale.ROOT))) {
            return filter(homes, args[2]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeWarp(@NotNull String module, @NotNull String[] args) {
        if (module.equals("warps")) {
            return new ArrayList<>();
        }
        if (args.length == 2) {
            if (module.equals("warp")) {
                List<String> options = new ArrayList<>(List.of("set", "del", "list"));
                options.addAll(warpNames());
                return filter(options, args[1]);
            }
            if (module.equals("setwarp")) {
                return new ArrayList<>();
            }
            return filter(warpNames(), args[1]);
        }
        if (args.length == 3 && module.equals("warp")
                && List.of("set", "del", "delete", "remove").contains(args[1].toLowerCase(Locale.ROOT))) {
            return filter(warpNames(), args[2]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeTpa(@NotNull String module, @NotNull String[] args) {
        if (module.equals("tpa") || module.equals("tpahere")) {
            if (args.length == 2) {
                return players(args[1]);
            }
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> completeKit(@NotNull String module, @NotNull String[] args) {
        if (module.equals("kits")) {
            return new ArrayList<>();
        }
        if (args.length == 2) {
            List<String> options = new ArrayList<>(List.of("list"));
            options.addAll(plugin.getKitService().listKits());
            return filter(options, args[1]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> homeNamesFor(@Nullable org.bukkit.entity.Player player) {
        if (player == null) {
            return List.of();
        }
        return plugin.getHomeService().listHomes(player);
    }

    private @NotNull List<String> warpNames() {
        return plugin.getWarpService().listWarps();
    }

    private @NotNull List<String> completeAdmin(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("reload", "debug", "cache", "config", "cancel"), args[1]);
        }
        return new ArrayList<>();
    }

    private @NotNull List<String> players(@Nullable String prefix) {
        return new ArrayList<>(PlayerUtil.onlineNames(prefix == null ? "" : prefix));
    }

    private @NotNull List<String> filter(@NotNull List<String> options, @NotNull String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (lower.isEmpty() || option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
