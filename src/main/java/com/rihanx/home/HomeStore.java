package com.rihanx.home;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * YAML-backed player home storage ({@code homes.yml}).
 * <p>Path: {@code players.<uuid>.homes.<name>} → world, x, y, z, yaw, pitch
 */
public final class HomeStore {

    private final @NotNull RihanX plugin;
    private final @NotNull File file;
    private final @NotNull Map<UUID, Map<String, StoredHome>> homes = new ConcurrentHashMap<>();

    public HomeStore(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    public void load() {
        homes.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuidString : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection homesSection = players.getConfigurationSection(uuidString + ".homes");
            if (homesSection == null) {
                continue;
            }
            Map<String, StoredHome> playerHomes = new ConcurrentHashMap<>();
            for (String name : homesSection.getKeys(false)) {
                ConfigurationSection section = homesSection.getConfigurationSection(name);
                if (section == null) {
                    continue;
                }
                StoredHome home = StoredHome.read(section);
                if (home != null) {
                    playerHomes.put(normalize(name), home);
                }
            }
            if (!playerHomes.isEmpty()) {
                homes.put(uuid, playerHomes);
            }
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, StoredHome>> playerEntry : homes.entrySet()) {
            String playerPath = "players." + playerEntry.getKey() + ".homes";
            for (Map.Entry<String, StoredHome> homeEntry : playerEntry.getValue().entrySet()) {
                homeEntry.getValue().write(yaml, playerPath + "." + homeEntry.getKey());
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for homes.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save homes.yml", ex);
        }
    }

    public @Nullable Location get(@NotNull UUID playerId, @NotNull String name) {
        Map<String, StoredHome> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return null;
        }
        StoredHome home = playerHomes.get(normalize(name));
        return home == null ? null : home.toLocation();
    }

    public boolean set(@NotNull UUID playerId, @NotNull String name, @NotNull Location location) {
        StoredHome home = StoredHome.from(location);
        if (home == null) {
            return false;
        }
        homes.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(normalize(name), home);
        save();
        return true;
    }

    public boolean delete(@NotNull UUID playerId, @NotNull String name) {
        Map<String, StoredHome> playerHomes = homes.get(playerId);
        if (playerHomes == null) {
            return false;
        }
        StoredHome removed = playerHomes.remove(normalize(name));
        if (removed == null) {
            return false;
        }
        if (playerHomes.isEmpty()) {
            homes.remove(playerId);
        }
        save();
        return true;
    }

    public boolean has(@NotNull UUID playerId, @NotNull String name) {
        Map<String, StoredHome> playerHomes = homes.get(playerId);
        return playerHomes != null && playerHomes.containsKey(normalize(name));
    }

    public int count(@NotNull UUID playerId) {
        Map<String, StoredHome> playerHomes = homes.get(playerId);
        return playerHomes == null ? 0 : playerHomes.size();
    }

    public @NotNull List<String> list(@NotNull UUID playerId) {
        Map<String, StoredHome> playerHomes = homes.get(playerId);
        if (playerHomes == null || playerHomes.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>(playerHomes.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(names);
    }

    private static @NotNull String normalize(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private record StoredHome(
            @NotNull String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        static @Nullable StoredHome from(@NotNull Location location) {
            World world = location.getWorld();
            if (world == null) {
                return null;
            }
            return new StoredHome(
                    world.getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch()
            );
        }

        static @Nullable StoredHome read(@NotNull ConfigurationSection section) {
            String world = section.getString("world");
            if (world == null || world.isBlank()) {
                return null;
            }
            return new StoredHome(
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
