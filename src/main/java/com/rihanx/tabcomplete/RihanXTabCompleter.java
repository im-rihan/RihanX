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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Tab completion for /rx and standalone module commands.
 */
public final class RihanXTabCompleter implements TabCompleter {

    private static final List<String> MODULES = List.of(
            "help", "slime", "world", "find", "chunk", "tp", "back", "player", "inventory", "item",
            "search", "server", "performance", "protect", "edit", "admin"
    );

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
        if (!PermissionUtil.has(sender, PermissionNodes.USE)) {
            return List.of();
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        boolean root = RihanXCommand.ROOT_NAMES.contains(commandName);

        final String module;
        final String[] prefixed;

        if (root) {
            if (args.length == 1) {
                return filter(MODULES, args[0]);
            }
            module = RihanXCommand.normalizeModule(args[0]);
            prefixed = args;
        } else {
            module = RihanXCommand.normalizeModule(commandName);
            if (module.equals("back")) {
                return List.of();
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
            case "admin" -> completeAdmin(prefixed);
            default -> List.of();
        };
    }

    private @NotNull List<String> completeSlime(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("nearest", "search", "density", "map", "tp"), args[1]);
        }
        if (args.length == 3 && List.of("nearest", "search", "density", "map").contains(args[1].toLowerCase(Locale.ROOT))) {
            return filter(List.of("8", "16", "32", "64"), args[2]);
        }
        return List.of();
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
        return List.of();
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
        return List.of();
    }

    private @NotNull List<String> completeChunk(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("info", "load", "unload", "regenerate", "border", "entities", "tileentities"), args[1]);
        }
        return List.of();
    }

    private @NotNull List<String> completeTp(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("pos", "player", "world", "biome", "structure", "chunk", "random", "safe", "back"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("player")) {
            return filter(PlayerUtil.onlineNames(args[2]), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("world")) {
            return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("biome")) {
            return filter(BiomeUtil.suggestions(args[2]), args[2]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("structure")) {
            return filter(StructureUtil.suggestions(args[2]), args[2]);
        }
        return List.of();
    }

    private @NotNull List<String> completePlayer(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("info", "heal", "feed", "fly", "god", "gamemode", "speed", "freeze", "unfreeze", "vanish", "cleareffects", "ping"), args[1]);
        }
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("speed")) {
                return filter(List.of("0.1", "0.2", "0.5", "1.0"), args[2]);
            }
            if (args[1].equalsIgnoreCase("gamemode") || args[1].equalsIgnoreCase("gm")) {
                return filter(List.of("survival", "creative", "adventure", "spectator"), args[2]);
            }
            return filter(PlayerUtil.onlineNames(args[2]), args[2]);
        }
        if (args.length == 4 && (args[1].equalsIgnoreCase("speed") || args[1].equalsIgnoreCase("gamemode") || args[1].equalsIgnoreCase("gm"))) {
            return filter(PlayerUtil.onlineNames(args[3]), args[3]);
        }
        return List.of();
    }

    private @NotNull List<String> completeInventory(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("see", "ender", "clear", "repair"), args[1]);
        }
        if (args.length == 3) {
            return filter(PlayerUtil.onlineNames(args[2]), args[2]);
        }
        return List.of();
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
            return filter(PlayerUtil.onlineNames(args[4]), args[4]);
        }
        return List.of();
    }

    private @NotNull List<String> completeSearch(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("slime", "cave", "lava", "water", "spawner", "village"), args[1]);
        }
        if (args.length == 3) {
            return filter(List.of("16", "32", "64", "128"), args[2]);
        }
        return List.of();
    }

    private @NotNull List<String> completeServer(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("info", "tps", "mspt", "memory", "uptime"), args[1]);
        }
        return List.of();
    }

    private @NotNull List<String> completePerformance(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("chunks", "entities"), args[1]);
        }
        return List.of();
    }

    private @NotNull List<String> completeProtect(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of(
                    "flag", "flags", "wand", "pos1", "pos2", "define", "redefine", "delete",
                    "info", "list", "setflag", "addmember", "removemember", "bypass"
            ), args[1]);
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("flag") || sub.equals("setflag")) {
                return filter(Arrays.asList(com.rihanx.protection.ProtectionFlag.keys()), args[2]);
            }
            if (List.of("delete", "info", "redefine", "setflag", "addmember", "removemember").contains(sub)) {
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
            if (sub.equals("addmember") || sub.equals("removemember")) {
                return filter(PlayerUtil.onlineNames(args[3]), args[3]);
            }
            if (sub.equals("delete")) {
                return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[3]);
            }
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("flag")) {
            return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[4]);
        }
        return List.of();
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
                    "hollow", "clear", "copy", "paste", "rotate", "undo", "redo"
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
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("replace")) {
            return filter(com.rihanx.utils.MaterialUtil.suggestions(args[3]), args[3]);
        }
        return List.of();
    }

    private @NotNull List<String> completeAdmin(@NotNull String[] args) {
        if (args.length == 2) {
            return filter(List.of("reload", "debug", "cache", "config", "cancel"), args[1]);
        }
        return List.of();
    }

    private @NotNull List<String> filter(@NotNull List<String> options, @NotNull String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
