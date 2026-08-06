package com.rihanx.commands;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.commands.modules.AfkModule;
import com.rihanx.commands.modules.BaseModule;
import com.rihanx.commands.modules.ChatModule;
import com.rihanx.commands.modules.EditModule;
import com.rihanx.commands.modules.HomeModule;
import com.rihanx.commands.modules.KitModule;
import com.rihanx.commands.modules.ProtectModule;
import com.rihanx.commands.modules.SpawnModule;
import com.rihanx.commands.modules.TpaModule;
import com.rihanx.commands.modules.WarpModule;
import com.rihanx.managers.CooldownManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.models.ChunkCoord;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.search.BlockSearchService;
import com.rihanx.slime.SlimeService;
import com.rihanx.utils.NumberUtil;
import com.rihanx.utils.PermissionUtil;
import com.rihanx.utils.PlayerUtil;
import com.rihanx.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Root /rx command router and standalone module commands (/slime, /protect, …).
 */
public final class RihanXCommand implements CommandExecutor {

    public static final Set<String> ROOT_NAMES = Set.of("rihanx", "rx", "rihan");
    public static final Set<String> MODULE_COMMANDS = Set.of(
            "slime", "world", "find", "chunk", "rxtp", "player", "inventory", "item",
            "search", "server", "performance", "protect", "edit", "admin", "back",
            "fly", "god", "heal", "feed", "vanish", "gm",
            "home", "sethome", "delhome", "homes",
            "warp", "setwarp", "delwarp", "warps",
            "tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel",
            "kit", "kits",
            "msg", "reply", "afk", "spawn", "setspawn",
            "base"
    );

    /** Shortcuts that map to /player <action> and default to the sender only. */
    public static final Set<String> SELF_ACTION_COMMANDS = Set.of(
            "fly", "god", "heal", "feed", "vanish", "gm"
    );

    private final @NotNull RihanX plugin;
    private final @NotNull HomeModule homeModule;
    private final @NotNull WarpModule warpModule;
    private final @NotNull TpaModule tpaModule;
    private final @NotNull KitModule kitModule;
    private final @NotNull ProtectModule protectModule;
    private final @NotNull EditModule editModule;
    private final @NotNull ChatModule chatModule;
    private final @NotNull AfkModule afkModule;
    private final @NotNull SpawnModule spawnModule;
    private final @NotNull BaseModule baseModule;

    public RihanXCommand(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.homeModule = new HomeModule(plugin);
        this.warpModule = new WarpModule(plugin);
        this.tpaModule = new TpaModule(plugin);
        this.kitModule = new KitModule(plugin);
        this.protectModule = new ProtectModule(plugin);
        this.editModule = new EditModule(plugin);
        this.chatModule = new ChatModule(plugin);
        this.afkModule = new AfkModule(plugin);
        this.spawnModule = new SpawnModule(plugin);
        this.baseModule = new BaseModule(plugin);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        MessageManager messages = plugin.getMessageManager();
        CooldownManager cooldowns = plugin.getCooldownManager();

        String commandName = command.getName().toLowerCase(Locale.ROOT);
        boolean root = ROOT_NAMES.contains(commandName);

        final String module;
        final String[] subArgs;

        if (root) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sendHelp(sender, label);
                return true;
            }
            module = normalizeModule(args[0]);
            subArgs = shift(args);
        } else {
            module = normalizeModule(commandName);
            subArgs = args;
        }

        return dispatch(sender, module, subArgs, messages, cooldowns, label);
    }

    private boolean dispatch(
            @NotNull CommandSender sender,
            @NotNull String module,
            @NotNull String[] subArgs,
            @NotNull MessageManager messages,
            @NotNull CooldownManager cooldowns,
            @NotNull String label
    ) {
        return switch (module) {
            case "slime" -> handleSlime(sender, subArgs, messages, cooldowns);
            case "world" -> handleWorld(sender, subArgs, messages);
            case "find" -> handleFind(sender, subArgs, messages, cooldowns);
            case "chunk" -> handleChunk(sender, subArgs, messages);
            case "tp" -> handleTp(sender, subArgs, messages, cooldowns);
            case "back" -> handleTp(sender, new String[]{"back"}, messages, cooldowns);
            case "player" -> handlePlayer(sender, subArgs, messages);
            case "fly", "god", "heal", "feed", "vanish", "gm" -> handleSelfAction(sender, module, subArgs, messages);
            case "inventory", "inv" -> handleInventory(sender, subArgs, messages);
            case "item" -> handleItem(sender, subArgs, messages);
            case "search" -> handleSearch(sender, subArgs, messages, cooldowns);
            case "server" -> handleServer(sender, subArgs, messages);
            case "performance", "perf" -> handlePerformance(sender, subArgs, messages);
            case "protect", "guard" -> protectModule.handle(sender, subArgs, messages);
            case "edit", "we" -> editModule.handle(sender, subArgs, messages);
            case "home", "sethome", "delhome", "homes" -> homeModule.handle(sender, module, subArgs, messages);
            case "warp", "setwarp", "delwarp", "warps" -> warpModule.handle(sender, module, subArgs, messages);
            case "tpa", "tpahere", "tpaccept", "tpdeny", "tpcancel" -> tpaModule.handle(sender, module, subArgs, messages);
            case "kit", "kits" -> kitModule.handle(sender, module, subArgs, messages);
            case "msg", "reply" -> chatModule.handle(sender, module, subArgs, messages);
            case "afk" -> afkModule.handle(sender, subArgs, messages);
            case "spawn", "setspawn" -> spawnModule.handle(sender, module, subArgs, messages);
            case "base" -> baseModule.handle(sender, module, subArgs, messages);
            case "admin" -> handleAdmin(sender, subArgs, messages);
            default -> {
                usage(sender, "/" + label + " help");
                yield true;
            }
        };
    }

    /**
     * Standalone /fly /god /heal /feed /vanish /gm.
     * /vanish is always self-only; other shortcuts accept an optional player name.
     */
    private boolean handleSelfAction(
            @NotNull CommandSender sender,
            @NotNull String action,
            @NotNull String[] args,
            @NotNull MessageManager messages
    ) {
        if (action.equals("vanish")) {
            return handlePlayer(sender, new String[]{"vanish"}, messages);
        }
        if (action.equals("gm")) {
            // /gm <mode> [player]
            String[] playerArgs = new String[args.length + 1];
            playerArgs[0] = "gamemode";
            System.arraycopy(args, 0, playerArgs, 1, args.length);
            return handlePlayer(sender, playerArgs, messages);
        }
        String[] playerArgs;
        if (args.length == 0) {
            playerArgs = new String[]{action};
        } else {
            playerArgs = new String[args.length + 1];
            playerArgs[0] = action;
            System.arraycopy(args, 0, playerArgs, 1, args.length);
        }
        return handlePlayer(sender, playerArgs, messages);
    }

    public static @NotNull String normalizeModule(@NotNull String input) {
        String module = input.toLowerCase(Locale.ROOT);
        return switch (module) {
            case "guard" -> "protect";
            case "we" -> "edit";
            case "inv" -> "inventory";
            case "perf" -> "performance";
            case "rxtp" -> "tp";
            case "sethome", "delhome", "homes" -> module;
            case "setwarp", "delwarp", "warps" -> module;
            case "tpahere", "tpaccept", "tpdeny", "tpcancel" -> module;
            case "kits" -> module;
            case "tell", "w" -> "msg";
            case "r" -> "reply";
            default -> module;
        };
    }

    private void sendHelp(@NotNull CommandSender sender, @NotNull String label) {
        if (!PermissionUtil.has(sender, PermissionNodes.USE)) {
            plugin.getMessageManager().send(sender, "no-permission");
            return;
        }
        MessageManager messages = plugin.getMessageManager();
        messages.send(sender, "help-header");
        sendHelpLine(messages, sender, "slime", "Slime chunk tools");
        sendHelpLine(messages, sender, "world", "World info and control");
        sendHelpLine(messages, sender, "find", "Locate biomes and structures");
        sendHelpLine(messages, sender, "chunk", "Chunk tools");
        sendHelpLine(messages, sender, "tp", "Teleport utilities");
        sendHelpLine(messages, sender, "player", "Player utilities");
        sendHelpLine(messages, sender, "inventory", "Inventory tools");
        sendHelpLine(messages, sender, "item", "Item tools");
        sendHelpLine(messages, sender, "search", "Block and structure search");
        sendHelpLine(messages, sender, "server", "Server info");
        sendHelpLine(messages, sender, "performance", "Performance reports");
        sendHelpLine(messages, sender, "protect", "World/region protection");
        sendHelpLine(messages, sender, "edit", "WorldEdit-lite tools");
        sendHelpLine(messages, sender, "home", "Homes");
        sendHelpLine(messages, sender, "warp", "Warps");
        sendHelpLine(messages, sender, "tpa", "Teleport requests");
        sendHelpLine(messages, sender, "kit", "Kits");
        sendHelpLine(messages, sender, "base", "House / base templates");
        sendHelpLine(messages, sender, "msg", "Private messages (/r to reply)");
        sendHelpLine(messages, sender, "afk", "Toggle AFK status");
        sendHelpLine(messages, sender, "spawn", "Teleport to spawn");
        sendHelpLine(messages, sender, "admin", "Admin tools");
        messages.send(sender, "help-dual");
    }

    private void sendHelpLine(
            @NotNull MessageManager messages,
            @NotNull CommandSender sender,
            @NotNull String module,
            @NotNull String desc
    ) {
        messages.send(sender, "help-line", MessageManager.placeholders(
                "module", module,
                "desc", desc
        ));
    }

    private boolean handleSlime(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull CooldownManager cooldowns
    ) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.SLIME)) {
            messages.send(sender, "no-permission");
            return true;
        }
        SlimeService slime = plugin.getSlimeService();
        SchedulerUtil scheduler = plugin.getSchedulerUtil();

        if (args.length == 0) {
            slime.check(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "nearest" -> {
                if (!checkPerm(player, PermissionNodes.SLIME_NEAREST, messages)) yield true;
                if (checkCooldown(player, cooldowns, "slime", messages)) yield true;
                int radius;
                if (args.length > 1) {
                    Integer parsed = NumberUtil.parseInt(args[1]);
                    if (parsed == null) {
                        invalidNumber(sender, messages, args[1]);
                        yield true;
                    }
                    radius = parsed;
                } else {
                    radius = plugin.getConfigManager().getSlimeNearestRadius();
                }
                slime.findNearestAsync(player, radius).thenAccept(found -> scheduler.runSync(() -> {
                    if (found == null) {
                        messages.send(player, "slime-nearest-none", MessageManager.placeholders("radius", radius));
                    } else {
                        slime.cacheSlimeResult(player, found);
                        messages.send(player, "slime-nearest", MessageManager.placeholders(
                                "x", found.getX(), "z", found.getZ(),
                                "distance", String.format("%.0f", player.getLocation().distance(
                                        com.rihanx.utils.LocationUtil.centerOfChunk(player.getWorld(), found.getX(), found.getZ())
                                ))
                        ));
                    }
                }));
                cooldowns.setCooldown(player, "slime");
                yield true;
            }
            case "search" -> {
                if (!checkPerm(player, PermissionNodes.SLIME_SEARCH, messages)) yield true;
                if (checkCooldown(player, cooldowns, "search", messages)) yield true;
                int finalRadius;
                if (args.length > 1) {
                    Integer parsed = NumberUtil.parseInt(args[1]);
                    if (parsed == null) {
                        invalidNumber(sender, messages, args[1]);
                        yield true;
                    }
                    finalRadius = slime.clampRadius(parsed);
                } else {
                    finalRadius = slime.clampRadius(0);
                }
                slime.searchAsync(player, finalRadius).thenAccept(results -> scheduler.runSync(() -> {
                    if (results.isEmpty()) {
                        messages.send(player, "search-no-results", MessageManager.placeholders("radius", finalRadius));
                    } else {
                        messages.send(player, "search-complete", MessageManager.placeholders("count", results.size()));
                        for (ChunkCoord coord : results) {
                            slime.cacheSlimeResult(player, coord);
                            messages.send(player, "search-result", MessageManager.placeholders(
                                    "x", coord.centerBlockX(), "y", player.getLocation().getBlockY(), "z", coord.centerBlockZ(),
                                    "distance", String.format("%.0f", ChunkCoord.fromLocation(player.getLocation()).distance(coord))
                            ));
                        }
                    }
                }));
                cooldowns.setCooldown(player, "search");
                yield true;
            }
            case "density" -> {
                if (!checkPerm(player, PermissionNodes.SLIME_DENSITY, messages)) yield true;
                if (checkCooldown(player, cooldowns, "slime", messages)) yield true;
                int finalRadius;
                if (args.length > 1) {
                    Integer parsed = NumberUtil.parseInt(args[1]);
                    if (parsed == null) {
                        invalidNumber(sender, messages, args[1]);
                        yield true;
                    }
                    finalRadius = slime.clampRadius(parsed);
                } else {
                    finalRadius = slime.clampRadius(0);
                }
                slime.densityAsync(player, finalRadius).thenAccept(result -> scheduler.runSync(() -> {
                    String farm = result.bestFarmChunk() == null ? "N/A"
                            : result.bestFarmChunk().getX() + ", " + result.bestFarmChunk().getZ();
                    messages.send(player, "slime-density", MessageManager.placeholders(
                            "percent", slime.formatPercent(result.percent()),
                            "count", result.count(),
                            "total", result.total(),
                            "fx", result.bestFarmChunk() == null ? "?" : result.bestFarmChunk().getX(),
                            "fz", result.bestFarmChunk() == null ? "?" : result.bestFarmChunk().getZ(),
                            "farm", farm + " (" + result.bestFarmSlimeCount() + "/9)"
                    ));
                }));
                cooldowns.setCooldown(player, "slime");
                yield true;
            }
            case "map" -> {
                if (!checkPerm(player, PermissionNodes.SLIME_MAP, messages)) yield true;
                int finalRadius;
                if (args.length > 1) {
                    Integer parsed = NumberUtil.parseInt(args[1]);
                    if (parsed == null) {
                        invalidNumber(sender, messages, args[1]);
                        yield true;
                    }
                    finalRadius = slime.clampMapRadius(parsed);
                } else {
                    finalRadius = slime.clampMapRadius(0);
                }
                messages.send(player, "slime-map-header", MessageManager.placeholders("radius", finalRadius));
                sender.sendMessage(slime.buildMap(player, finalRadius));
                yield true;
            }
            case "tp" -> {
                if (!checkPerm(player, PermissionNodes.SLIME_TP, messages)) yield true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) yield true;
                int radius = plugin.getConfigManager().getSlimeNearestRadius();
                slime.findNearestAsync(player, radius).thenAccept(found -> scheduler.runSync(() -> {
                    if (found == null) {
                        messages.send(player, "slime-nearest-none", MessageManager.placeholders("radius", radius));
                    } else {
                        messages.send(player, "slime-tp", MessageManager.placeholders("x", found.getX(), "z", found.getZ()));
                        plugin.getTeleportService().teleportToSlime(player, found);
                    }
                }));
                cooldowns.setCooldown(player, "teleport");
                yield true;
            }
            default -> {
                slime.check(player);
                yield true;
            }
        };
    }

    private boolean handleWorld(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.WORLD)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx world <info|seed|weather|difficulty|time|border|spawn|setspawn>");
            return true;
        }
        World world = player.getWorld();
        var worldService = plugin.getWorldService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "info" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_INFO, messages)) return true;
                    worldService.sendInfo(sender, world);
                }
                case "seed" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_SEED, messages)) return true;
                    worldService.sendSeed(sender, world);
                }
                case "weather" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_WEATHER, messages)) return true;
                    if (args.length < 2) {
                        usage(sender, "/rx world weather <clear|rain|thunder>");
                        return true;
                    }
                    worldService.setWeather(world, args[1]);
                    messages.send(sender, "world-weather-set", MessageManager.placeholders("weather", args[1]));
                }
                case "difficulty" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_DIFFICULTY, messages)) return true;
                    if (args.length < 2) {
                        usage(sender, "/rx world difficulty <peaceful|easy|normal|hard>");
                        return true;
                    }
                    worldService.setDifficulty(world, args[1]);
                    messages.send(sender, "world-difficulty-set", MessageManager.placeholders("difficulty", args[1]));
                }
                case "time" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_TIME, messages)) return true;
                    if (args.length < 2) {
                        usage(sender, "/rx world time <day|night|noon|midnight|ticks>");
                        return true;
                    }
                    worldService.setTime(world, args[1]);
                    messages.send(sender, "world-time-set", MessageManager.placeholders("time", args[1]));
                }
                case "border" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_BORDER, messages)) return true;
                    worldService.sendBorder(sender, world);
                }
                case "spawn" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_SPAWN, messages)) return true;
                    worldService.sendSpawn(sender, world);
                }
                case "setspawn" -> {
                    if (!checkPerm(player, PermissionNodes.WORLD_SETSPAWN, messages)) return true;
                    worldService.setSpawn(player);
                }
                default -> usage(sender, "/rx world <info|seed|weather|difficulty|time|border|spawn|setspawn>");
            }
        } catch (IllegalArgumentException ex) {
            messages.send(sender, "invalid-argument", MessageManager.placeholders("input", ex.getMessage()));
        }
        return true;
    }

    private boolean handleFind(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull CooldownManager cooldowns
    ) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.FIND)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            usage(sender, "/rx find <biome|structure> <name>");
            return true;
        }
        if (checkCooldown(player, cooldowns, "find", messages)) {
            return true;
        }
        if (plugin.getAsyncTaskTracker().hasActive(player.getUniqueId())) {
            messages.send(player, "search-already-running");
            return true;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        String name = TextUtil.joinFrom(1, " ", args);
        var findService = plugin.getFindService();
        SchedulerUtil scheduler = plugin.getSchedulerUtil();

        if (type.equals("biome")) {
            if (!checkPerm(player, PermissionNodes.FIND_BIOME, messages)) return true;
            Biome biome = findService.resolveBiome(name);
            if (biome == null) {
                messages.send(player, "biome-invalid", MessageManager.placeholders("input", name));
                return true;
            }
            findService.locateBiomeAsync(player, biome).thenAccept(found ->
                    scheduler.runSync(() -> findService.handleBiomeResult(player, biome, found)));
        } else if (type.equals("structure")) {
            if (!checkPerm(player, PermissionNodes.FIND_STRUCTURE, messages)) return true;
            Structure structure = findService.resolveStructure(name);
            if (structure == null) {
                messages.send(player, "structure-invalid", MessageManager.placeholders("input", name));
                return true;
            }
            findService.locateStructureAsync(player, structure).thenAccept(found ->
                    scheduler.runSync(() -> findService.handleStructureResult(player, structure, found)));
        } else {
            usage(sender, "/rx find <biome|structure> <name>");
            return true;
        }
        cooldowns.setCooldown(player, "find");
        return true;
    }

    private boolean handleChunk(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.CHUNK)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx chunk <info|load|unload|regenerate|border|entities|tileentities>");
            return true;
        }
        var chunkService = plugin.getChunkService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info" -> {
                if (!checkPerm(player, PermissionNodes.CHUNK_INFO, messages)) return true;
                chunkService.sendInfo(player);
            }
            case "load" -> {
                if (!checkPerm(player, PermissionNodes.CHUNK_LOAD, messages)) return true;
                chunkService.load(player);
                messages.send(player, "chunk-loaded");
            }
            case "unload" -> {
                if (!checkPerm(player, PermissionNodes.CHUNK_UNLOAD, messages)) return true;
                chunkService.unload(player);
                messages.send(player, "chunk-unloaded");
            }
            case "regenerate", "regen" -> {
                if (!checkPerm(player, PermissionNodes.CHUNK_REGENERATE, messages)) return true;
                if (chunkService.regenerate(player)) {
                    messages.send(player, "chunk-regenerated");
                }
            }
            case "border" -> {
                if (!checkPerm(player, PermissionNodes.CHUNK_BORDER, messages)) return true;
                chunkService.sendBorder(player);
            }
            case "entities" -> {
                if (!checkPerm(player, PermissionNodes.CHUNK_ENTITIES, messages)) return true;
                chunkService.sendEntityCount(player);
            }
            case "tileentities", "tiles" -> {
                if (!checkPerm(player, PermissionNodes.CHUNK_TILEENTITIES, messages)) return true;
                chunkService.sendTileEntityCount(player);
            }
            default -> usage(sender, "/rx chunk <info|load|unload|regenerate|border|entities|tileentities>");
        }
        return true;
    }

    private boolean handleTp(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull CooldownManager cooldowns
    ) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.TP)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx tp <pos|player|here|home|world|biome|structure|chunk|random|safe|back> ...");
            return true;
        }
        var tp = plugin.getTeleportService();
        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "pos" -> {
                if (!checkPerm(player, PermissionNodes.TP_POS, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                if (args.length < 4) {
                    usage(sender, "/rx tp pos <x> <y> <z> [world]");
                    return true;
                }
                Double x = NumberUtil.parseDouble(args[1]);
                Double y = NumberUtil.parseDouble(args[2]);
                Double z = NumberUtil.parseDouble(args[3]);
                if (x == null || y == null || z == null) {
                    messages.send(sender, "invalid-number");
                    return true;
                }
                World world = args.length > 4 ? Bukkit.getWorld(args[4]) : player.getWorld();
                if (world == null) {
                    messages.send(sender, "world-not-found", MessageManager.placeholders("world", args[4]));
                    return true;
                }
                tp.teleportToPos(player, x, y, z, world);
                cooldowns.setCooldown(player, "teleport");
            }
            case "player" -> {
                if (!checkPerm(player, PermissionNodes.TP_PLAYER, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                if (args.length < 2) {
                    usage(sender, "/rxtp player <name> [destinationPlayer]");
                    return true;
                }
                Player first = PlayerUtil.findPlayer(args[1]);
                if (first == null) {
                    messages.send(sender, "player-not-found", MessageManager.placeholders("player", args[1]));
                    return true;
                }
                if (args.length >= 3) {
                    // Teleport first player TO second player
                    Player destination = PlayerUtil.findPlayer(args[2]);
                    if (destination == null) {
                        messages.send(sender, "player-not-found", MessageManager.placeholders("player", args[2]));
                        return true;
                    }
                    tp.teleportPlayerToPlayer(first, destination);
                    messages.send(sender, "teleport-player-other", MessageManager.placeholders(
                            "player", first.getName(),
                            "target", destination.getName()
                    ));
                } else {
                    // Teleport self TO named player
                    tp.teleportToPlayer(player, first);
                }
                cooldowns.setCooldown(player, "teleport");
            }
            case "here" -> {
                if (!checkPerm(player, PermissionNodes.TP_HERE, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                if (args.length < 2) {
                    usage(sender, "/rxtp here <player>");
                    return true;
                }
                Player victim = PlayerUtil.findPlayer(args[1]);
                if (victim == null) {
                    messages.send(sender, "player-not-found", MessageManager.placeholders("player", args[1]));
                    return true;
                }
                tp.teleportHere(player, victim);
                messages.send(sender, "teleport-here", MessageManager.placeholders("player", victim.getName()));
                cooldowns.setCooldown(player, "teleport");
            }
            case "home" -> {
                if (!checkPerm(player, PermissionNodes.TP_HOME, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                Player homeTarget = player;
                if (args.length >= 2) {
                    homeTarget = PlayerUtil.findPlayer(args[1]);
                    if (homeTarget == null) {
                        messages.send(sender, "player-not-found", MessageManager.placeholders("player", args[1]));
                        return true;
                    }
                }
                tp.teleportHome(homeTarget);
                if (!homeTarget.equals(player)) {
                    messages.send(sender, "teleport-home-other", MessageManager.placeholders("player", homeTarget.getName()));
                }
                cooldowns.setCooldown(player, "teleport");
            }
            case "world" -> {
                if (!checkPerm(player, PermissionNodes.TP_WORLD, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                if (args.length < 2) {
                    usage(sender, "/rx tp world <name>");
                    return true;
                }
                World world = Bukkit.getWorld(args[1]);
                if (world == null) {
                    messages.send(sender, "world-not-found", MessageManager.placeholders("world", args[1]));
                    return true;
                }
                tp.teleportToWorldSpawn(player, world);
                cooldowns.setCooldown(player, "teleport");
            }
            case "biome" -> {
                if (!checkPerm(player, PermissionNodes.TP_BIOME, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                if (args.length < 2) {
                    usage(sender, "/rx tp biome <name>");
                    return true;
                }
                tp.teleportToBiome(player, TextUtil.joinFrom(1, " ", args));
                cooldowns.setCooldown(player, "teleport");
            }
            case "structure" -> {
                if (!checkPerm(player, PermissionNodes.TP_STRUCTURE, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                if (args.length < 2) {
                    usage(sender, "/rx tp structure <name>");
                    return true;
                }
                tp.teleportToStructure(player, TextUtil.joinFrom(1, " ", args));
                cooldowns.setCooldown(player, "teleport");
            }
            case "chunk" -> {
                if (!checkPerm(player, PermissionNodes.TP_CHUNK, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                if (args.length < 3) {
                    usage(sender, "/rx tp chunk <x> <z>");
                    return true;
                }
                Integer cx = NumberUtil.parseInt(args[1]);
                Integer cz = NumberUtil.parseInt(args[2]);
                if (cx == null || cz == null) {
                    messages.send(sender, "invalid-number");
                    return true;
                }
                tp.teleportToChunk(player, cx, cz);
                cooldowns.setCooldown(player, "teleport");
            }
            case "random" -> {
                if (!checkPerm(player, PermissionNodes.TP_RANDOM, messages)) return true;
                if (checkCooldown(player, cooldowns, "random-tp", messages)) return true;
                tp.teleportRandom(player);
                cooldowns.setCooldown(player, "random-tp");
            }
            case "safe" -> {
                if (!checkPerm(player, PermissionNodes.TP_SAFE, messages)) return true;
                if (checkCooldown(player, cooldowns, "teleport", messages)) return true;
                tp.teleportSafe(player);
                cooldowns.setCooldown(player, "teleport");
            }
            case "back" -> {
                if (!checkPerm(player, PermissionNodes.TP_BACK, messages)) return true;
                tp.teleportBack(player);
            }
            default -> usage(sender, "/rx tp <pos|player|world|biome|structure|chunk|random|safe|back> ...");
        }
        return true;
    }

    private boolean handlePlayer(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        if (!PermissionUtil.has(sender, PermissionNodes.PLAYER) && !sender.isOp()) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/player <info|heal|feed|fly|god|gamemode|speed|freeze|unfreeze|vanish|cleareffects|ping> [player]");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        Player target;

        // Self-by-default actions: no name → YOU only. Explicit name → that one player only.
        if (List.of("heal", "feed", "fly", "god", "info", "ping", "cleareffects", "clearpotions", "vanish").contains(sub)) {
            target = resolveSelfOrNamed(sender, args, 1, messages);
            if (target == null) {
                return true;
            }
        } else if (sub.equals("speed")) {
            // /player speed <val> [player]
            if (args.length >= 3) {
                target = resolveNamedPlayer(sender, args[2], messages);
            } else {
                target = requirePlayer(sender, messages);
            }
            if (target == null) {
                return true;
            }
        } else if (sub.equals("gamemode") || sub.equals("gm")) {
            // /player gamemode <mode> [player]
            if (args.length >= 3) {
                target = resolveNamedPlayer(sender, args[2], messages);
            } else {
                target = requirePlayer(sender, messages);
            }
            if (target == null) {
                return true;
            }
        } else if (sub.equals("freeze") || sub.equals("unfreeze")) {
            // freeze always needs an explicit target (never "everyone")
            if (args.length < 2) {
                usage(sender, "/player " + sub + " <player>");
                return true;
            }
            target = resolveNamedPlayer(sender, args[1], messages);
            if (target == null) {
                return true;
            }
        } else {
            target = resolveSelfOrNamed(sender, args, 1, messages);
            if (target == null) {
                return true;
            }
        }

        var playerService = plugin.getPlayerService();
        switch (sub) {
            case "info" -> {
                if (!checkPerm(sender, PermissionNodes.PLAYER_INFO, messages)) return true;
                playerService.sendInfo(sender, target);
            }
            case "heal" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_HEAL, messages)) return true;
                playerService.heal(sender, target);
            }
            case "feed" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_FEED, messages)) return true;
                playerService.feed(sender, target);
            }
            case "fly" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_FLY, messages)) return true;
                playerService.toggleFly(sender, target);
            }
            case "god" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_GOD, messages)) return true;
                playerService.toggleGod(sender, target);
            }
            case "gamemode", "gm" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_GAMEMODE, messages)) return true;
                if (args.length < 2) {
                    usage(sender, "/gm <mode> [player]  or  /player gamemode <mode> [player]");
                    return true;
                }
                org.bukkit.GameMode mode = PlayerUtil.parseGameMode(args[1]);
                if (mode == null) {
                    messages.send(sender, "invalid-argument", MessageManager.placeholders("input", args[1]));
                    return true;
                }
                playerService.setGameMode(sender, target, mode);
            }
            case "speed" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_SPEED, messages)) return true;
                if (args.length < 2) {
                    usage(sender, "/player speed <val> [player]");
                    return true;
                }
                Float speed = NumberUtil.parseFloat(args[1]);
                if (speed == null) {
                    messages.send(sender, "invalid-number", MessageManager.placeholders("input", args[1]));
                    return true;
                }
                playerService.setSpeed(target, Math.min(1.0f, Math.max(0.0f, speed)));
            }
            case "freeze" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_FREEZE, messages)) return true;
                playerService.freeze(sender, target);
            }
            case "unfreeze" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_UNFREEZE, messages)) return true;
                playerService.unfreeze(sender, target);
            }
            case "vanish" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_VANISH, messages)) return true;
                // /player vanish [name] can target another player; /vanish is self-only via handleSelfAction
                playerService.toggleVanish(target);
            }
            case "cleareffects", "clearpotions" -> {
                if (!checkOpPerm(sender, PermissionNodes.PLAYER_CLEAREFFECTS, messages)) return true;
                playerService.clearEffects(sender, target);
            }
            case "ping" -> {
                if (!checkPerm(sender, PermissionNodes.PLAYER_PING, messages)) return true;
                playerService.sendPing(sender, target);
            }
            default -> usage(sender, "/player <info|heal|feed|fly|god|gamemode|speed|freeze|unfreeze|vanish|cleareffects|ping> [player]");
        }
        return true;
    }

    private boolean handleInventory(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        if (!PermissionUtil.has(sender, PermissionNodes.INVENTORY)) {
            messages.send(sender, "no-permission");
            return true;
        }
        Player viewer = requirePlayer(sender, messages);
        if (viewer == null) {
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx inventory <see|ender|clear|repair> [player]");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        Player target = resolveTarget(sender, args, 1);
        if (target == null) {
            return true;
        }
        var inventoryService = plugin.getInventoryService();
        switch (sub) {
            case "see" -> {
                if (!checkOpPerm(sender, PermissionNodes.INVENTORY_SEE, messages)) return true;
                inventoryService.openInventory(sender, viewer, target);
            }
            case "ender" -> {
                if (!checkOpPerm(sender, PermissionNodes.INVENTORY_ENDER, messages)) return true;
                inventoryService.openEnderChest(sender, viewer, target);
            }
            case "clear" -> {
                if (!checkOpPerm(sender, PermissionNodes.INVENTORY_CLEAR, messages)) return true;
                inventoryService.clear(target);
            }
            case "repair" -> {
                if (!checkOpPerm(sender, PermissionNodes.INVENTORY_REPAIR, messages)) return true;
                inventoryService.repairAll(target);
            }
            default -> usage(sender, "/rx inventory <see|ender|clear|repair> [player]");
        }
        return true;
    }

    private boolean handleItem(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.ITEM)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx item <info|give|rename|lore|enchant|repair>");
            return true;
        }
        var itemService = plugin.getItemService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "info" -> {
                    if (!checkPerm(player, PermissionNodes.ITEM_INFO, messages)) return true;
                    itemService.sendInfo(player);
                }
                case "give" -> {
                    if (!checkOpPerm(player, PermissionNodes.ITEM_GIVE, messages)) return true;
                    if (args.length < 2) {
                        usage(sender, "/rx item give <material|tipped_arrow:strong_harming> [amount] [player]");
                        return true;
                    }
                    int amount = 1;
                    Player giveTarget = player;
                    if (args.length >= 3) {
                        Integer parsed = NumberUtil.parseInt(args[2]);
                        if (parsed != null) {
                            amount = parsed;
                            if (args.length >= 4) {
                                if (!checkOpPerm(player, PermissionNodes.ITEM_GIVE_OTHERS, messages)) return true;
                                giveTarget = PlayerUtil.findPlayer(args[3]);
                                if (giveTarget == null) {
                                    messages.send(sender, "player-not-found", MessageManager.placeholders("player", args[3]));
                                    return true;
                                }
                            }
                        } else {
                            if (!checkOpPerm(player, PermissionNodes.ITEM_GIVE_OTHERS, messages)) return true;
                            giveTarget = PlayerUtil.findPlayer(args[2]);
                            if (giveTarget == null) {
                                messages.send(sender, "player-not-found", MessageManager.placeholders("player", args[2]));
                                return true;
                            }
                        }
                    }
                    itemService.give(sender, giveTarget, args[1], amount);
                }
                case "rename" -> {
                    if (!checkOpPerm(player, PermissionNodes.ITEM_RENAME, messages)) return true;
                    if (args.length < 2) {
                        usage(sender, "/rx item rename <name...>");
                        return true;
                    }
                    itemService.rename(player, TextUtil.joinFrom(1, " ", args));
                }
                case "lore" -> {
                    if (!checkOpPerm(player, PermissionNodes.ITEM_LORE, messages)) return true;
                    if (args.length < 2) {
                        usage(sender, "/rx item lore <line...>");
                        return true;
                    }
                    itemService.setLore(player, TextUtil.joinFrom(1, " ", args));
                }
                case "enchant" -> {
                    if (!checkOpPerm(player, PermissionNodes.ITEM_ENCHANT, messages)) return true;
                    if (args.length < 2) {
                        usage(sender, "/rx item enchant <ench> [level]");
                        return true;
                    }
                    int level = args.length > 2 ? NumberUtil.parseInt(args[2]) == null ? 1 : NumberUtil.parseInt(args[2]) : 1;
                    itemService.enchant(player, args[1], level);
                }
                case "repair" -> {
                    if (!checkOpPerm(player, PermissionNodes.ITEM_REPAIR, messages)) return true;
                    itemService.repairHand(player);
                }
                default -> usage(sender, "/rx item <info|give|rename|lore|enchant|repair>");
            }
        } catch (IllegalStateException ex) {
            if ("empty".equals(ex.getMessage())) {
                messages.send(player, "item-empty");
            }
        }
        return true;
    }

    private boolean handleSearch(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            @NotNull MessageManager messages,
            @NotNull CooldownManager cooldowns
    ) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.SEARCH)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx search <slime|cave|lava|water|spawner|village> [radius]");
            return true;
        }
        if (checkCooldown(player, cooldowns, "search", messages)) {
            return true;
        }
        if (plugin.getAsyncTaskTracker().hasActive(player.getUniqueId())) {
            messages.send(player, "search-already-running");
            return true;
        }

        String typeName = args[0].toLowerCase(Locale.ROOT);
        Integer radius = args.length > 1 ? NumberUtil.parseInt(args[1]) : null;
        if (args.length > 1 && radius == null) {
            invalidNumber(sender, messages, args[1]);
            return true;
        }

        SchedulerUtil scheduler = plugin.getSchedulerUtil();
        BlockSearchService blockSearch = plugin.getBlockSearchService();
        SlimeService slime = plugin.getSlimeService();

        if (typeName.equals("slime")) {
            if (!checkPerm(player, PermissionNodes.SEARCH_SLIME, messages)) return true;
            int finalRadius = slime.clampRadius(radius == null ? 0 : radius);
            slime.searchAsync(player, finalRadius).thenAccept(results -> scheduler.runSync(() -> {
                if (results.isEmpty()) {
                    messages.send(player, "search-no-results", MessageManager.placeholders("radius", finalRadius));
                } else {
                    messages.send(player, "search-complete", MessageManager.placeholders("count", results.size()));
                    for (ChunkCoord coord : results) {
                        slime.cacheSlimeResult(player, coord);
                        messages.send(player, "search-result", MessageManager.placeholders(
                                "x", coord.centerBlockX(), "y", player.getLocation().getBlockY(), "z", coord.centerBlockZ(),
                                "distance", String.format("%.0f", ChunkCoord.fromLocation(player.getLocation()).distance(coord))
                        ));
                    }
                }
            }));
        } else {
            BlockSearchService.SearchType type = mapBlockSearchType(typeName);
            if (type == null) {
                usage(sender, "/rx search <slime|cave|lava|water|spawner|village> [radius]");
                return true;
            }
            if (!checkPerm(player, permissionForSearch(type), messages)) {
                return true;
            }
            int finalRadius = blockSearch.clampRadius(radius == null ? 0 : radius);
            blockSearch.searchAsync(player, type, finalRadius).thenAccept(results ->
                    scheduler.runSync(() -> blockSearch.sendResults(player, results, finalRadius)));
        }
        cooldowns.setCooldown(player, "search");
        return true;
    }

    private boolean handleServer(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        if (!PermissionUtil.has(sender, PermissionNodes.SERVER)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx server <info|tps|mspt|memory|uptime>");
            return true;
        }
        var perf = plugin.getPerformanceService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info" -> {
                if (!checkPerm(sender, PermissionNodes.SERVER_INFO, messages)) return true;
                perf.sendServerInfo(sender);
            }
            case "tps" -> {
                if (!checkPerm(sender, PermissionNodes.SERVER_TPS, messages)) return true;
                perf.sendTps(sender);
            }
            case "mspt" -> {
                if (!checkPerm(sender, PermissionNodes.SERVER_MSPT, messages)) return true;
                perf.sendMspt(sender);
            }
            case "memory" -> {
                if (!checkPerm(sender, PermissionNodes.SERVER_MEMORY, messages)) return true;
                perf.sendMemory(sender);
            }
            case "uptime" -> {
                if (!checkPerm(sender, PermissionNodes.SERVER_UPTIME, messages)) return true;
                perf.sendUptime(sender);
            }
            default -> usage(sender, "/rx server <info|tps|mspt|memory|uptime>");
        }
        return true;
    }

    private boolean handlePerformance(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        Player player = requirePlayer(sender, messages);
        if (player == null) {
            return true;
        }
        if (!PermissionUtil.has(player, PermissionNodes.PERFORMANCE)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx performance <chunks|entities>");
            return true;
        }
        var perf = plugin.getPerformanceService();
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "chunks" -> {
                if (!checkPerm(player, PermissionNodes.PERFORMANCE_CHUNKS, messages)) return true;
                perf.sendNearbyChunks(player);
            }
            case "entities" -> {
                if (!checkPerm(player, PermissionNodes.PERFORMANCE_ENTITIES, messages)) return true;
                perf.sendEntityBreakdown(player);
            }
            default -> usage(sender, "/rx performance <chunks|entities>");
        }
        return true;
    }

    private boolean handleAdmin(@NotNull CommandSender sender, @NotNull String[] args, @NotNull MessageManager messages) {
        if (!PermissionUtil.has(sender, PermissionNodes.ADMIN)) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/rx admin <reload|debug|cache|config|cancel>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                messages.send(sender, "reloaded");
            }
            case "debug" -> {
                boolean next = !plugin.getConfigManager().isDebug();
                plugin.getConfig().set("general.debug", next);
                plugin.saveConfig();
                plugin.getConfigManager().reload();
                messages.send(sender, next ? "admin-debug-on" : "admin-debug-off");
            }
            case "cache" -> {
                plugin.getSearchCache().clear();
                messages.send(sender, "admin-cache-cleared");
            }
            case "config" -> {
                var config = plugin.getConfigManager().raw();
                for (String key : config.getKeys(true)) {
                    if (!config.isConfigurationSection(key)) {
                        messages.send(sender, "admin-config-dump", MessageManager.placeholders(
                                "key", key,
                                "value", String.valueOf(config.get(key))
                        ));
                    }
                }
            }
            case "cancel" -> {
                if (sender instanceof Player player) {
                    if (plugin.getAsyncTaskTracker().cancel(player.getUniqueId())) {
                        messages.send(player, "search-cancelled");
                    } else {
                        messages.send(player, "search-none-active");
                    }
                }
            }
            default -> usage(sender, "/rx admin <reload|debug|cache|config|cancel>");
        }
        return true;
    }

    private @NotNull String[] shift(@NotNull String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return rest;
    }

    private @Nullable Player requirePlayer(@NotNull CommandSender sender, @NotNull MessageManager messages) {
        return CommandSupport.requirePlayer(sender, messages);
    }

    private @Nullable Player resolveTarget(@NotNull CommandSender sender, @NotNull String[] args, int nameIndex) {
        return CommandSupport.resolveSelfOrNamed(sender, args, nameIndex, plugin.getMessageManager());
    }

    private @Nullable Player resolveSelfOrNamed(
            @NotNull CommandSender sender,
            @NotNull String[] args,
            int nameIndex,
            @NotNull MessageManager messages
    ) {
        return CommandSupport.resolveSelfOrNamed(sender, args, nameIndex, messages);
    }

    private @Nullable Player resolveNamedPlayer(
            @NotNull CommandSender sender,
            @NotNull String name,
            @NotNull MessageManager messages
    ) {
        return CommandSupport.resolveNamedPlayer(sender, name, messages);
    }

    private boolean checkPerm(@NotNull CommandSender sender, @NotNull String permission, @NotNull MessageManager messages) {
        return CommandSupport.checkPerm(sender, permission, messages);
    }

    private boolean checkOpPerm(@NotNull CommandSender sender, @NotNull String permission, @NotNull MessageManager messages) {
        return CommandSupport.checkOpPerm(sender, permission, messages);
    }

    private boolean checkCooldown(
            @NotNull Player player,
            @NotNull CooldownManager cooldowns,
            @NotNull String key,
            @NotNull MessageManager messages
    ) {
        if (cooldowns.isOnCooldown(player, key)) {
            messages.send(player, "cooldown", MessageManager.placeholders(
                    "seconds", cooldowns.getRemainingSeconds(player, key)
            ));
            return true;
        }
        return false;
    }

    private void usage(@NotNull CommandSender sender, @NotNull String usage) {
        CommandSupport.usage(plugin.getMessageManager(), sender, usage);
    }

    private void invalidNumber(@NotNull CommandSender sender, @NotNull MessageManager messages, @NotNull String input) {
        CommandSupport.invalidNumber(sender, messages, input);
    }

    private @Nullable BlockSearchService.SearchType mapBlockSearchType(@NotNull String typeName) {
        return switch (typeName) {
            case "cave" -> BlockSearchService.SearchType.CAVE;
            case "lava" -> BlockSearchService.SearchType.LAVA;
            case "water" -> BlockSearchService.SearchType.WATER;
            case "spawner" -> BlockSearchService.SearchType.SPAWNER;
            case "village" -> BlockSearchService.SearchType.VILLAGE;
            default -> null;
        };
    }

    private @NotNull String permissionForSearch(@NotNull BlockSearchService.SearchType type) {
        return switch (type) {
            case CAVE -> PermissionNodes.SEARCH_CAVE;
            case LAVA -> PermissionNodes.SEARCH_LAVA;
            case WATER -> PermissionNodes.SEARCH_WATER;
            case SPAWNER -> PermissionNodes.SEARCH_SPAWNER;
            case VILLAGE -> PermissionNodes.SEARCH_VILLAGE;
        };
    }
}
