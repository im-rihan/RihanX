package com.rihanx.protection;

import com.rihanx.RihanX;
import com.rihanx.edit.Cuboid;
import com.rihanx.edit.SelectionManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.MessageUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * World and region protection facade.
 */
public final class ProtectionService {

    public static final String WAND_NAME = "RihanX Protect Wand";

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull WorldFlagStore worldFlags;
    private final @NotNull RegionStore regions;
    private final @NotNull SelectionManager selections;
    private final @NotNull Set<UUID> bypass = ConcurrentHashMap.newKeySet();

    public ProtectionService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull SelectionManager selections
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.selections = selections;
        this.worldFlags = new WorldFlagStore(plugin);
        this.regions = new RegionStore(plugin);
        loadBypass();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("protection.enabled", true);
    }

    public int getMaxRegionsPerWorld() {
        return plugin.getConfig().getInt("protection.max-regions-per-world", 200);
    }

    public void reload() {
        worldFlags.reload();
        regions.load();
        loadBypass();
    }

    public void save() {
        worldFlags.save();
        regions.save();
        saveBypass();
    }

    public @NotNull MessageManager getPluginMessages() {
        return messages;
    }

    public @NotNull WorldFlagStore getWorldFlags() {
        return worldFlags;
    }

    public @NotNull RegionStore getRegions() {
        return regions;
    }

    public @NotNull SelectionManager getSelections() {
        return selections;
    }

    public boolean toggleBypass(@NotNull Player player) {
        UUID id = player.getUniqueId();
        boolean enabled;
        if (bypass.contains(id)) {
            bypass.remove(id);
            enabled = false;
        } else {
            bypass.add(id);
            enabled = true;
        }
        saveBypass();
        return enabled;
    }

    public boolean hasBypass(@Nullable Player player) {
        return player != null && bypass.contains(player.getUniqueId());
    }

    private void loadBypass() {
        bypass.clear();
        File file = new File(plugin.getDataFolder(), "bypass.yml");
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String raw : yaml.getStringList("players")) {
            try {
                bypass.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveBypass() {
        File file = new File(plugin.getDataFolder(), "bypass.yml");
        FileConfiguration yaml = new YamlConfiguration();
        List<String> ids = new ArrayList<>();
        for (UUID id : bypass) {
            ids.add(id.toString());
        }
        yaml.set("players", ids);
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for bypass.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save bypass.yml", ex);
        }
    }

    /**
     * Resolves whether an action is allowed at a location.
     * Region (smallest) flag → world flag → allow.
     * Members bypass build/break/place/entry denies for their regions.
     */
    public boolean isAllowed(
            @Nullable Player player,
            @NotNull Location location,
            @NotNull ProtectionFlag flag
    ) {
        if (!isEnabled()) {
            return true;
        }
        if (hasBypass(player)) {
            return true;
        }
        World world = location.getWorld();
        if (world == null) {
            return true;
        }

        List<Region> containing = regions.findContaining(
                world.getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        for (Region region : containing) {
            FlagValue regionValue = region.getFlag(flag);
            if (regionValue != FlagValue.UNSET) {
                if (regionValue.isDeny() && player != null && isMemberBypassFlag(flag) && region.isMember(player)) {
                    return true;
                }
                return regionValue.isAllow();
            }
        }

        // Member of any containing region can build even if only world flag denies
        if (player != null && isMemberBypassFlag(flag)) {
            for (Region region : containing) {
                if (region.isMember(player)) {
                    return true;
                }
            }
        }

        return worldFlags.resolve(world.getName(), flag).isAllow();
    }

    public boolean isAllowedAtBlock(
            @Nullable Player player,
            @NotNull org.bukkit.block.Block block,
            @NotNull ProtectionFlag flag
    ) {
        return isAllowed(player, block.getLocation(), flag);
    }

    private boolean isMemberBypassFlag(@NotNull ProtectionFlag flag) {
        return flag == ProtectionFlag.BUILD
                || flag == ProtectionFlag.BREAK
                || flag == ProtectionFlag.PLACE
                || flag == ProtectionFlag.ENTRY;
    }

    public void giveWand(@NotNull Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<aqua>" + WAND_NAME + "</aqua>"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Left-click: pos1</gray>"),
                    MessageUtil.parse("<gray>Right-click: pos2</gray>")
            ));
            meta.getPersistentDataContainer().set(wandKey(), PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        messages.send(player, "protect-wand-given");
    }

    public boolean isProtectWand(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (meta.getPersistentDataContainer().has(wandKey(), PersistentDataType.BYTE)) {
            return true;
        }
        if (meta.hasDisplayName()) {
            String plain = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            return plain.contains(WAND_NAME);
        }
        return false;
    }

    private @NotNull NamespacedKey wandKey() {
        return new NamespacedKey(plugin, "protect_wand");
    }

    public void setPos(@NotNull Player player, int which, @NotNull Location location) {
        if (which == 1) {
            selections.setPos1(player, SelectionManager.Channel.PROTECT, location);
            messages.send(player, "protect-pos1", MessageManager.placeholders(
                    "x", location.getBlockX(),
                    "y", location.getBlockY(),
                    "z", location.getBlockZ()
            ));
        } else {
            selections.setPos2(player, SelectionManager.Channel.PROTECT, location);
            messages.send(player, "protect-pos2", MessageManager.placeholders(
                    "x", location.getBlockX(),
                    "y", location.getBlockY(),
                    "z", location.getBlockZ()
            ));
        }
        Cuboid cuboid = selections.getCuboid(player, SelectionManager.Channel.PROTECT);
        if (cuboid != null) {
            messages.send(player, "protect-selection-size", MessageManager.placeholders("volume", cuboid.volume()));
        }
    }

    public void setWorldFlag(
            @NotNull CommandSender sender,
            @NotNull World world,
            @NotNull ProtectionFlag flag,
            @NotNull FlagValue value
    ) {
        worldFlags.set(world.getName(), flag, value);
        messages.send(sender, "protect-flag-set", MessageManager.placeholders(
                "flag", flag.key(),
                "value", value.display(),
                "world", world.getName()
        ));
    }

    public void listWorldFlags(@NotNull CommandSender sender, @NotNull World world) {
        messages.send(sender, "protect-flags-header", MessageManager.placeholders("world", world.getName()));
        for (Map.Entry<ProtectionFlag, FlagValue> entry : worldFlags.getEffective(world.getName()).entrySet()) {
            messages.send(sender, "protect-flags-line", MessageManager.placeholders(
                    "flag", entry.getKey().key(),
                    "value", entry.getValue().display()
            ));
        }
    }

    public boolean define(@NotNull Player player, @NotNull String name) {
        Cuboid cuboid = selections.getCuboid(player, SelectionManager.Channel.PROTECT);
        if (cuboid == null) {
            messages.send(player, "protect-selection-incomplete");
            return false;
        }
        String worldName = cuboid.getWorld().getName();
        if (regions.get(worldName, name) != null) {
            messages.send(player, "protect-region-exists", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        if (regions.count(worldName) >= getMaxRegionsPerWorld()) {
            messages.send(player, "protect-region-limit");
            return false;
        }
        Region region = new Region(name, cuboid);
        region.addOwner(player.getUniqueId());
        regions.put(region);
        messages.send(player, "protect-region-defined", MessageManager.placeholders(
                "name", region.getName(),
                "volume", region.volume()
        ));
        return true;
    }

    public boolean redefine(@NotNull Player player, @NotNull String name) {
        Cuboid cuboid = selections.getCuboid(player, SelectionManager.Channel.PROTECT);
        if (cuboid == null) {
            messages.send(player, "protect-selection-incomplete");
            return false;
        }
        Region region = regions.get(cuboid.getWorld().getName(), name);
        if (region == null) {
            messages.send(player, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        region.setBounds(cuboid);
        regions.put(region);
        messages.send(player, "protect-region-redefined", MessageManager.placeholders(
                "name", region.getName(),
                "volume", region.volume()
        ));
        return true;
    }

    public boolean delete(@NotNull CommandSender sender, @NotNull World world, @NotNull String name) {
        if (!regions.remove(world.getName(), name)) {
            messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        messages.send(sender, "protect-region-deleted", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
        return true;
    }

    public void listRegions(@NotNull CommandSender sender, @NotNull World world) {
        List<Region> list = new ArrayList<>(regions.getRegions(world.getName()));
        if (list.isEmpty()) {
            messages.send(sender, "protect-region-list-empty", MessageManager.placeholders("world", world.getName()));
            return;
        }
        messages.send(sender, "protect-region-list-header", MessageManager.placeholders(
                "world", world.getName(),
                "count", list.size()
        ));
        for (Region region : list) {
            messages.send(sender, "protect-region-list-line", MessageManager.placeholders(
                    "name", region.getName(),
                    "volume", region.volume()
            ));
        }
    }

    public void sendInfo(@NotNull CommandSender sender, @NotNull Player player, @Nullable String name) {
        Region region;
        if (name != null) {
            region = regions.get(player.getWorld().getName(), name);
            if (region == null) {
                messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
                return;
            }
        } else {
            List<Region> containing = regions.findContaining(
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ()
            );
            if (containing.isEmpty()) {
                messages.send(sender, "protect-region-none-here");
                return;
            }
            region = containing.get(0);
        }
        messages.send(sender, "protect-region-info-header", MessageManager.placeholders("name", region.getName()));
        messages.send(sender, "protect-region-info-line", MessageManager.placeholders(
                "key", "Bounds",
                "value", region.getMinX() + "," + region.getMinY() + "," + region.getMinZ()
                        + " → " + region.getMaxX() + "," + region.getMaxY() + "," + region.getMaxZ()
        ));
        messages.send(sender, "protect-region-info-line", MessageManager.placeholders("key", "Volume", "value", region.volume()));
        messages.send(sender, "protect-region-info-line", MessageManager.placeholders(
                "key", "Priority",
                "value", region.getPriority()
        ));
        messages.send(sender, "protect-region-info-line", MessageManager.placeholders(
                "key", "Owners",
                "value", formatUuids(region.getOwners())
        ));
        messages.send(sender, "protect-region-info-line", MessageManager.placeholders(
                "key", "Members",
                "value", formatUuids(region.getMembers())
        ));
        for (Map.Entry<ProtectionFlag, FlagValue> entry : region.getFlags().entrySet()) {
            messages.send(sender, "protect-region-info-line", MessageManager.placeholders(
                    "key", "Flag " + entry.getKey().key(),
                    "value", entry.getValue().display()
            ));
        }
    }

    public boolean setRegionFlag(
            @NotNull CommandSender sender,
            @NotNull World world,
            @NotNull String name,
            @NotNull ProtectionFlag flag,
            @NotNull FlagValue value
    ) {
        Region region = regions.get(world.getName(), name);
        if (region == null) {
            messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        region.setFlag(flag, value);
        regions.put(region);
        messages.send(sender, "protect-region-flag-set", MessageManager.placeholders(
                "name", region.getName(),
                "flag", flag.key(),
                "value", value.display()
        ));
        return true;
    }

    public boolean addMember(
            @NotNull CommandSender sender,
            @NotNull World world,
            @NotNull String name,
            @NotNull UUID target,
            @NotNull String targetName
    ) {
        Region region = regions.get(world.getName(), name);
        if (region == null) {
            messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        region.addMember(target);
        regions.put(region);
        messages.send(sender, "protect-member-added", MessageManager.placeholders(
                "player", targetName,
                "name", region.getName()
        ));
        return true;
    }

    public boolean removeMember(
            @NotNull CommandSender sender,
            @NotNull World world,
            @NotNull String name,
            @NotNull UUID target,
            @NotNull String targetName
    ) {
        Region region = regions.get(world.getName(), name);
        if (region == null) {
            messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        region.removeMember(target);
        regions.put(region);
        messages.send(sender, "protect-member-removed", MessageManager.placeholders(
                "player", targetName,
                "name", region.getName()
        ));
        return true;
    }

    public boolean addOwner(
            @NotNull CommandSender sender,
            @NotNull World world,
            @NotNull String name,
            @NotNull UUID target,
            @NotNull String targetName
    ) {
        Region region = regions.get(world.getName(), name);
        if (region == null) {
            messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        region.addOwner(target);
        regions.put(region);
        messages.send(sender, "protect-owner-added", MessageManager.placeholders(
                "player", targetName,
                "name", region.getName()
        ));
        return true;
    }

    public boolean removeOwner(
            @NotNull CommandSender sender,
            @NotNull World world,
            @NotNull String name,
            @NotNull UUID target,
            @NotNull String targetName
    ) {
        Region region = regions.get(world.getName(), name);
        if (region == null) {
            messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        region.removeOwner(target);
        regions.put(region);
        messages.send(sender, "protect-owner-removed", MessageManager.placeholders(
                "player", targetName,
                "name", region.getName()
        ));
        return true;
    }

    public boolean setPriority(
            @NotNull CommandSender sender,
            @NotNull World world,
            @NotNull String name,
            int priority
    ) {
        Region region = regions.get(world.getName(), name);
        if (region == null) {
            messages.send(sender, "protect-region-missing", MessageManager.placeholders("name", name.toLowerCase(Locale.ROOT)));
            return false;
        }
        region.setPriority(priority);
        regions.put(region);
        messages.send(sender, "protect-priority-set", MessageManager.placeholders(
                "name", region.getName(),
                "priority", priority
        ));
        return true;
    }

    private @NotNull String formatUuids(@NotNull Set<UUID> ids) {
        if (ids.isEmpty()) {
            return "none";
        }
        List<String> names = new ArrayList<>();
        for (UUID id : ids) {
            String name = Bukkit.getOfflinePlayer(id).getName();
            names.add(name == null ? id.toString().substring(0, 8) : name);
        }
        return String.join(", ", names);
    }
}
