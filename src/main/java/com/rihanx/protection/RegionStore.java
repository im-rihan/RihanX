package com.rihanx.protection;

import com.rihanx.RihanX;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * YAML-backed region storage.
 */
public final class RegionStore {

    private final @NotNull RihanX plugin;
    private final @NotNull File file;
    private final @NotNull Map<String, Map<String, Region>> byWorld = new ConcurrentHashMap<>();

    public RegionStore(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
        load();
    }

    public void load() {
        byWorld.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection worlds = yaml.getConfigurationSection("worlds");
        if (worlds == null) {
            return;
        }
        for (String worldName : worlds.getKeys(false)) {
            ConfigurationSection regions = worlds.getConfigurationSection(worldName);
            if (regions == null) {
                continue;
            }
            for (String regionName : regions.getKeys(false)) {
                ConfigurationSection section = regions.getConfigurationSection(regionName);
                if (section == null) {
                    continue;
                }
                Region region = new Region(
                        regionName,
                        worldName,
                        section.getInt("minX"),
                        section.getInt("minY"),
                        section.getInt("minZ"),
                        section.getInt("maxX"),
                        section.getInt("maxY"),
                        section.getInt("maxZ")
                );
                region.setPriority(section.getInt("priority", 0));
                for (String owner : section.getStringList("owners")) {
                    try {
                        region.addOwner(UUID.fromString(owner));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                for (String member : section.getStringList("members")) {
                    try {
                        region.addMember(UUID.fromString(member));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                ConfigurationSection flags = section.getConfigurationSection("flags");
                if (flags != null) {
                    for (String key : flags.getKeys(false)) {
                        ProtectionFlag flag = ProtectionFlag.fromKey(key);
                        FlagValue value = FlagValue.parse(flags.getString(key, "unset"));
                        if (flag != null && value != null && value != FlagValue.UNSET) {
                            region.setFlag(flag, value);
                        }
                    }
                }
                byWorld.computeIfAbsent(worldName, w -> new ConcurrentHashMap<>())
                        .put(region.getName(), region);
            }
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Map<String, Region>> worldEntry : byWorld.entrySet()) {
            String worldPath = "worlds." + worldEntry.getKey();
            for (Region region : worldEntry.getValue().values()) {
                String path = worldPath + "." + region.getName();
                yaml.set(path + ".minX", region.getMinX());
                yaml.set(path + ".minY", region.getMinY());
                yaml.set(path + ".minZ", region.getMinZ());
                yaml.set(path + ".maxX", region.getMaxX());
                yaml.set(path + ".maxY", region.getMaxY());
                yaml.set(path + ".maxZ", region.getMaxZ());
                yaml.set(path + ".priority", region.getPriority());
                List<String> owners = new ArrayList<>();
                region.getOwners().forEach(uuid -> owners.add(uuid.toString()));
                yaml.set(path + ".owners", owners);
                List<String> members = new ArrayList<>();
                region.getMembers().forEach(uuid -> members.add(uuid.toString()));
                yaml.set(path + ".members", members);
                for (Map.Entry<ProtectionFlag, FlagValue> flagEntry : region.getFlags().entrySet()) {
                    yaml.set(path + ".flags." + flagEntry.getKey().key(), flagEntry.getValue().display());
                }
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for regions.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save regions.yml", ex);
        }
    }

    public @Nullable Region get(@NotNull String worldName, @NotNull String name) {
        Map<String, Region> map = byWorld.get(worldName);
        if (map == null) {
            return null;
        }
        return map.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean put(@NotNull Region region) {
        Map<String, Region> map = byWorld.computeIfAbsent(region.getWorldName(), w -> new ConcurrentHashMap<>());
        map.put(region.getName(), region);
        save();
        return true;
    }

    public boolean remove(@NotNull String worldName, @NotNull String name) {
        Map<String, Region> map = byWorld.get(worldName);
        if (map == null) {
            return false;
        }
        Region removed = map.remove(name.toLowerCase(Locale.ROOT));
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public @NotNull Collection<Region> getRegions(@NotNull String worldName) {
        Map<String, Region> map = byWorld.get(worldName);
        if (map == null) {
            return List.of();
        }
        return Collections.unmodifiableCollection(map.values());
    }

    public int count(@NotNull String worldName) {
        Map<String, Region> map = byWorld.get(worldName);
        return map == null ? 0 : map.size();
    }

    public @NotNull List<Region> findContaining(@NotNull String worldName, int x, int y, int z) {
        List<Region> result = new ArrayList<>();
        for (Region region : getRegions(worldName)) {
            if (region.contains(x, y, z)) {
                result.add(region);
            }
        }
        // Highest priority first, then smallest volume
        result.sort((a, b) -> {
            int byPriority = Integer.compare(b.getPriority(), a.getPriority());
            if (byPriority != 0) {
                return byPriority;
            }
            return Long.compare(a.volume(), b.volume());
        });
        return result;
    }
}
