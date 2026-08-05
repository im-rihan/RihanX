package com.rihanx.kits;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.MaterialUtil;
import com.rihanx.utils.MessageUtil;
import com.rihanx.utils.PermissionUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Kit definitions from {@code kits.yml} (preferred) or {@code kits.definitions} in config.
 * Cooldowns tracked in memory with optional YAML persistence.
 */
public final class KitService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull File kitsFile;
    private final @NotNull File cooldownFile;
    private final @NotNull Map<String, KitDefinition> kits = new LinkedHashMap<>();
    /** player → kit → expiry epoch ms */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final boolean persistCooldowns;

    public KitService(@NotNull RihanX plugin, @NotNull MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        this.cooldownFile = new File(plugin.getDataFolder(), "kit-cooldowns.yml");
        this.persistCooldowns = plugin.getConfig().getBoolean("kits.persist-cooldowns", true);
        reload();
        if (persistCooldowns) {
            loadCooldowns();
        }
    }

    public void reload() {
        kits.clear();
        ensureKitsFile();
        // Refresh bundled starter/survival kits from the jar (keeps custom kits intact)
        mergeBundledKits();
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection root = yaml.getConfigurationSection("kits");
        if (root == null || root.getKeys(false).isEmpty()) {
            root = plugin.getConfig().getConfigurationSection("kits.definitions");
        }
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            KitDefinition definition = KitDefinition.read(normalize(name), section);
            if (definition != null) {
                kits.put(definition.name(), definition);
            }
        }
    }

    /**
     * Overwrites {@code starter} and {@code survival} from the jar kits.yml so updates ship.
     * Other custom kit names are left alone. Set {@code kits.sync-bundled: false} to disable.
     */
    private void mergeBundledKits() {
        if (!plugin.getConfig().getBoolean("kits.sync-bundled", true)) {
            return;
        }
        try (var stream = plugin.getResource("kits.yml")) {
            if (stream == null) {
                return;
            }
            FileConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
            ConfigurationSection bundledKits = bundled.getConfigurationSection("kits");
            if (bundledKits == null) {
                return;
            }
            FileConfiguration live = kitsFile.exists()
                    ? YamlConfiguration.loadConfiguration(kitsFile)
                    : new YamlConfiguration();
            boolean changed = false;
            for (String name : List.of("starter", "survival", "pro")) {
                ConfigurationSection section = bundledKits.getConfigurationSection(name);
                if (section == null) {
                    continue;
                }
                live.set("kits." + name + ".cooldown-seconds", section.getInt("cooldown-seconds", 0));
                live.set("kits." + name + ".items", section.getStringList("items"));
                changed = true;
            }
            if (changed) {
                live.save(kitsFile);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not sync bundled kits.yml", ex);
        }
    }

    public void giveKit(@NotNull Player player, @NotNull String name) {
        String kitName = normalize(name);
        KitDefinition kit = kits.get(kitName);
        if (kit == null) {
            messages.send(player, "kit-missing", MessageManager.placeholders("kit", kitName));
            return;
        }

        if (!PermissionUtil.bypassCooldown(player)) {
            long remaining = getRemainingSeconds(player.getUniqueId(), kitName);
            if (remaining > 0) {
                messages.send(player, "kit-cooldown", MessageManager.placeholders(
                        "kit", kitName,
                        "seconds", remaining
                ));
                return;
            }
        }

        // auto/chest/place = plugin creates filled chests for you (nothing to craft/place first)
        // shulker = portable kit chest item in inventory
        // inventory = loose items
        String delivery = plugin.getConfig().getString("kits.delivery", "auto");
        if (delivery == null || delivery.isBlank()) {
            delivery = "auto";
        }
        delivery = delivery.toLowerCase(Locale.ROOT);

        int chests;
        switch (delivery) {
            case "inventory", "inv" -> {
                giveLooseItems(player, kit.items());
                chests = 0;
            }
            case "shulker", "portable" -> chests = giveKitChests(player, kit);
            default -> chests = placeKitChests(player, kit); // auto / chest / place / world
        }

        if (kit.cooldownSeconds() > 0 && !PermissionUtil.bypassCooldown(player)) {
            long expiresAt = System.currentTimeMillis() + (kit.cooldownSeconds() * 1000L);
            cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                    .put(kitName, expiresAt);
            if (persistCooldowns) {
                saveCooldowns();
            }
        }

        if (chests > 0) {
            messages.send(player, "kit-given-chest", MessageManager.placeholders(
                    "kit", kitName,
                    "count", chests
            ));
        } else {
            messages.send(player, "kit-given", MessageManager.placeholders("kit", kitName));
        }
    }

    private void giveLooseItems(@NotNull Player player, @NotNull List<ItemStack> items) {
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    /**
     * Packs kit items into named shulker "kit chests" and gives them to the player.
     * (Portable chests — place and open, or open from inventory.)
     */
    private int giveKitChests(@NotNull Player player, @NotNull KitDefinition kit) {
        List<ItemStack> chests = packIntoShulkerChests(kit);
        for (ItemStack chest : chests) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(chest);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        return chests.size();
    }

    /**
     * Automatically creates real chest blocks near the player and fills them.
     * Player does not need to craft or place anything first.
     */
    private int placeKitChests(@NotNull Player player, @NotNull KitDefinition kit) {
        List<List<ItemStack>> pages = paginate(kit.items(), 27);
        List<Block> spots = findChestSpots(player, pages.size());
        int placed = 0;
        for (int i = 0; i < pages.size(); i++) {
            Block target = i < spots.size() ? spots.get(i) : null;
            if (target == null || (!target.getType().isAir() && !target.isReplaceable())) {
                // Still automatic: give a filled portable kit chest instead
                ItemStack shulker = createShulkerChest(kit.name(), i + 1, pages.size(), pages.get(i));
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(shulker);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                placed++;
                continue;
            }
            target.setType(Material.CHEST, false);
            if (target.getState() instanceof Chest chest) {
                Inventory inv = chest.getBlockInventory();
                inv.clear();
                for (ItemStack stack : pages.get(i)) {
                    inv.addItem(stack.clone());
                }
                chest.update(true, false);
                placed++;
            }
        }
        return placed;
    }

    /** Finds empty blocks around the player to auto-create kit chests. */
    private @NotNull List<Block> findChestSpots(@NotNull Player player, int needed) {
        List<Block> spots = new ArrayList<>();
        Block origin = player.getLocation().getBlock();
        BlockFace facing = player.getFacing();
        BlockFace left = rotateLeft(facing);
        BlockFace right = rotateRight(facing);

        // Prefer in front of the player, then sides, then up
        List<Block> candidates = new ArrayList<>();
        for (int dist = 1; dist <= Math.max(3, needed + 1); dist++) {
            candidates.add(origin.getRelative(facing, dist));
        }
        candidates.add(origin.getRelative(left));
        candidates.add(origin.getRelative(right));
        candidates.add(origin.getRelative(facing).getRelative(left));
        candidates.add(origin.getRelative(facing).getRelative(right));
        for (int up = 0; up <= 2; up++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0 && up == 0) {
                        continue;
                    }
                    candidates.add(origin.getRelative(dx, up, dz));
                }
            }
        }

        for (Block candidate : candidates) {
            if (spots.size() >= needed) {
                break;
            }
            if (spots.contains(candidate)) {
                continue;
            }
            if (candidate.getType().isAir() || candidate.isReplaceable()) {
                spots.add(candidate);
            }
        }
        return spots;
    }

    private static @NotNull BlockFace rotateLeft(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.WEST;
        };
    }

    private static @NotNull BlockFace rotateRight(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private @NotNull List<ItemStack> packIntoShulkerChests(@NotNull KitDefinition kit) {
        List<List<ItemStack>> pages = paginate(kit.items(), 27);
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            result.add(createShulkerChest(kit.name(), i + 1, pages.size(), pages.get(i)));
        }
        return result;
    }

    private @NotNull ItemStack createShulkerChest(
            @NotNull String kitName,
            int page,
            int totalPages,
            @NotNull List<ItemStack> contents
    ) {
        Material boxType = shulkerForKit(kitName);
        ItemStack item = new ItemStack(boxType);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof BlockStateMeta meta)) {
            return item;
        }
        String title = capitalize(kitName) + " Kit Chest"
                + (totalPages > 1 ? " (" + page + "/" + totalPages + ")" : "");
        meta.displayName(MessageUtil.parse("<gold>" + title + "</gold>"));
        meta.lore(List.of(
                MessageUtil.parse("<gray>Place or open to claim kit items.</gray>"),
                MessageUtil.parse("<dark_gray>RihanX /kit " + kitName + "</dark_gray>")
        ));
        if (meta.getBlockState() instanceof ShulkerBox shulker) {
            Inventory inv = shulker.getInventory();
            inv.clear();
            for (ItemStack stack : contents) {
                inv.addItem(stack.clone());
            }
            meta.setBlockState(shulker);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static @NotNull Material shulkerForKit(@NotNull String kitName) {
        return switch (kitName.toLowerCase(Locale.ROOT)) {
            case "starter" -> Material.WHITE_SHULKER_BOX;
            case "survival" -> Material.LIME_SHULKER_BOX;
            case "pro" -> Material.PURPLE_SHULKER_BOX;
            default -> Material.CYAN_SHULKER_BOX;
        };
    }

    private static @NotNull String capitalize(@NotNull String name) {
        if (name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private static @NotNull List<List<ItemStack>> paginate(@NotNull List<ItemStack> items, int pageSize) {
        List<List<ItemStack>> pages = new ArrayList<>();
        List<ItemStack> current = new ArrayList<>();
        int used = 0;
        for (ItemStack item : items) {
            ItemStack stack = item.clone();
            // Rough slot estimate: each stack needs one slot
            if (used >= pageSize) {
                pages.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(stack);
            used++;
        }
        if (!current.isEmpty()) {
            pages.add(current);
        }
        if (pages.isEmpty()) {
            pages.add(List.of());
        }
        return pages;
    }

    public @NotNull List<String> listKits() {
        if (kits.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>(kits.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(names);
    }

    public void sendKitList(@NotNull Player player) {
        List<String> names = listKits();
        String joined = names.isEmpty() ? "none" : String.join(", ", names);
        messages.send(player, "kit-list", MessageManager.placeholders(
                "kits", joined,
                "count", names.size()
        ));
    }

    public @Nullable KitDefinition getKit(@NotNull String name) {
        return kits.get(normalize(name));
    }

    public long getRemainingSeconds(@NotNull UUID playerId, @NotNull String kitName) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0L;
        }
        Long expires = playerCooldowns.get(normalize(kitName));
        if (expires == null) {
            return 0L;
        }
        long remaining = expires - System.currentTimeMillis();
        if (remaining <= 0) {
            playerCooldowns.remove(normalize(kitName));
            return 0L;
        }
        return (remaining + 999L) / 1000L;
    }

    public void clearCooldown(@NotNull UUID playerId) {
        cooldowns.remove(playerId);
        if (persistCooldowns) {
            saveCooldowns();
        }
    }

    private void ensureKitsFile() {
        if (kitsFile.exists()) {
            return;
        }
        if (plugin.getResource("kits.yml") != null) {
            plugin.saveResource("kits.yml", false);
            return;
        }
        // Prefer config kits.definitions when present — skip generating a data-folder file
        ConfigurationSection fromConfig = plugin.getConfig().getConfigurationSection("kits.definitions");
        if (fromConfig != null && !fromConfig.getKeys(false).isEmpty()) {
            return;
        }
        FileConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "RihanX kits — kits.<name>.cooldown-seconds, kits.<name>.items: MATERIAL:amount"
        ));
        yaml.set("kits.starter.cooldown-seconds", 3600);
        yaml.set("kits.starter.items", List.of("STONE_SWORD:1", "BREAD:16", "TORCH:32"));
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for kits.yml");
            }
            yaml.save(kitsFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not create default kits.yml", ex);
        }
    }

    private void loadCooldowns() {
        cooldowns.clear();
        if (!cooldownFile.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(cooldownFile);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String uuidString : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(uuidString);
            if (section == null) {
                continue;
            }
            Map<String, Long> map = new ConcurrentHashMap<>();
            for (String kit : section.getKeys(false)) {
                long expires = section.getLong(kit);
                if (expires > now) {
                    map.put(normalize(kit), expires);
                }
            }
            if (!map.isEmpty()) {
                cooldowns.put(uuid, map);
            }
        }
    }

    private void saveCooldowns() {
        FileConfiguration yaml = new YamlConfiguration();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String, Long>> playerEntry : cooldowns.entrySet()) {
            for (Map.Entry<String, Long> kitEntry : playerEntry.getValue().entrySet()) {
                if (kitEntry.getValue() > now) {
                    yaml.set("players." + playerEntry.getKey() + "." + kitEntry.getKey(), kitEntry.getValue());
                }
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for kit-cooldowns.yml");
            }
            yaml.save(cooldownFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save kit-cooldowns.yml", ex);
        }
    }

    private static @NotNull String normalize(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public record KitDefinition(
            @NotNull String name,
            int cooldownSeconds,
            @NotNull List<ItemStack> items
    ) {
        static @Nullable KitDefinition read(@NotNull String name, @NotNull ConfigurationSection section) {
            int cooldown = section.getInt("cooldown-seconds", 0);
            List<String> rawItems = section.getStringList("items");
            List<ItemStack> items = new ArrayList<>();
            for (String entry : rawItems) {
                ItemStack stack = parseItem(entry);
                if (stack != null) {
                    items.add(stack);
                }
            }
            if (items.isEmpty()) {
                return null;
            }
            return new KitDefinition(name, Math.max(0, cooldown), List.copyOf(items));
        }

        private static @Nullable ItemStack parseItem(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String[] parts = raw.trim().split(":");
            Material material = MaterialUtil.match(parts[0]);
            if (material == null || !material.isItem() || material.isAir()) {
                return null;
            }
            int amount = 1;
            if (parts.length >= 2) {
                try {
                    amount = Math.max(1, Math.min(material.getMaxStackSize(), Integer.parseInt(parts[1].trim())));
                } catch (NumberFormatException ignored) {
                    amount = 1;
                }
            }
            return new ItemStack(material, amount);
        }
    }
}
