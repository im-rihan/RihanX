package com.rihanx.warp;

import com.rihanx.RihanX;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * YAML-backed warp storage ({@code warps.yml}).
 * <p>Path: {@code warps.<name>} → world, x, y, z, yaw, pitch
 */
public final class WarpStore {

    private final @NotNull RihanX plugin;
    private final @NotNull File file;
    private final @NotNull Map<String, StoredWarp> warps = new ConcurrentHashMap<>();

    public WarpStore(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        load();
    }

    public void load() {
        warps.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("warps");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            StoredWarp warp = StoredWarp.read(section);
            if (warp != null) {
                warps.put(normalize(name), warp);
            }
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, StoredWarp> entry : warps.entrySet()) {
            entry.getValue().write(yaml, "warps." + entry.getKey());
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for warps.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save warps.yml", ex);
        }
    }

    public @Nullable Location get(@NotNull String name) {
        StoredWarp warp = warps.get(normalize(name));
        return warp == null ? null : warp.toLocation();
    }

    public boolean set(@NotNull String name, @NotNull Location location) {
        StoredWarp warp = StoredWarp.from(location);
        if (warp == null) {
            return false;
        }
        warps.put(normalize(name), warp);
        save();
        return true;
    }

    public boolean delete(@NotNull String name) {
        StoredWarp removed = warps.remove(normalize(name));
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public boolean has(@NotNull String name) {
        return warps.containsKey(normalize(name));
    }

    public @NotNull List<String> list() {
        if (warps.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>(warps.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(names);
    }

    private static @NotNull String normalize(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private record StoredWarp(
            @NotNull String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        static @Nullable StoredWarp from(@NotNull Location location) {
            World world = location.getWorld();
            if (world == null) {
                return null;
            }
            return new StoredWarp(
                    world.getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch()
            );
        }

        static @Nullable StoredWarp read(@NotNull ConfigurationSection section) {
            String world = section.getString("world");
            if (world == null || world.isBlank()) {
                return null;
            }
            return new StoredWarp(
                    world,
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch")
            );
        }

        void write(@NotNull FileConfiguration yaml, @NotNull String path) {
            yaml.set(path + ".world", worldName);
            yaml.set(path + ".x", x);
            yaml.set(path + ".y", y);
            yaml.set(path + ".z", z);
            yaml.set(path + ".yaw", yaw);
            yaml.set(path + ".pitch", pitch);
        }

        @Nullable Location toLocation() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        }
    }
}
