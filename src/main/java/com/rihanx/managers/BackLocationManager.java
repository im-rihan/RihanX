package com.rihanx.managers;

import com.rihanx.RihanX;
import com.rihanx.cache.LocationCache;
import com.rihanx.database.DatabaseManager;
import com.rihanx.models.BackLocation;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages /back location history with optional persistence.
 */
public final class BackLocationManager {

    private final @NotNull RihanX plugin;
    private final @NotNull ConfigManager configManager;
    private final @NotNull LocationCache cache;
    private final @Nullable DatabaseManager databaseManager;
    private final @NotNull File storageFile;

    public BackLocationManager(
            @NotNull RihanX plugin,
            @NotNull ConfigManager configManager,
            @Nullable DatabaseManager databaseManager
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.cache = new LocationCache(configManager.getMaxBackHistory());
        this.storageFile = new File(plugin.getDataFolder(), "back-locations.yml");
        load();
    }

    public void push(@NotNull Player player, @NotNull Location location) {
        cache.push(player.getUniqueId(), location);
        persist(player.getUniqueId());
    }

    public @Nullable Location pop(@NotNull Player player) {
        BackLocation back = cache.pop(player.getUniqueId());
        if (back == null) {
            return null;
        }
        persist(player.getUniqueId());
        return back.toLocation();
    }

    public @Nullable Location peek(@NotNull Player player) {
        BackLocation back = cache.peek(player.getUniqueId());
        return back == null ? null : back.toLocation();
    }

    public void clear(@NotNull UUID playerId) {
        cache.clear(playerId);
        persist(playerId);
    }

    public void load() {
        if (databaseManager != null && databaseManager.isEnabled()) {
            databaseManager.loadBacks(cache);
            return;
        }
        if (!configManager.persistentBack() || !storageFile.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String uuidString : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection playerSection = root.getConfigurationSection(uuidString);
            if (playerSection == null) {
                continue;
            }
            Deque<BackLocation> deque = new ArrayDeque<>();
            for (String index : playerSection.getKeys(false)) {
                ConfigurationSection entry = playerSection.getConfigurationSection(index);
                if (entry == null) {
                    continue;
                }
                BackLocation location = BackLocation.read(entry);
                if (location != null) {
                    deque.addLast(location);
                }
            }
            cache.restore(uuid, deque);
        }
    }

    public void save() {
        if (databaseManager != null && databaseManager.isEnabled()) {
            for (UUID uuid : cache.snapshot().keySet()) {
                persist(uuid);
            }
            return;
        }
        if (!configManager.persistentBack()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : cache.snapshot().entrySet()) {
            String path = "players." + entry.getKey();
            int index = 0;
            synchronized (entry.getValue()) {
                for (BackLocation location : entry.getValue()) {
                    ConfigurationSection section = yaml.createSection(path + "." + index);
                    location.write(section);
                    index++;
                }
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder");
            }
            yaml.save(storageFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save back locations", ex);
        }
    }

    private void persist(@NotNull UUID playerId) {
        if (databaseManager != null && databaseManager.isEnabled()) {
            Deque<BackLocation> deque = cache.snapshot().get(playerId);
            if (deque != null) {
                databaseManager.saveBack(playerId, new ArrayDeque<>(deque));
            } else {
                databaseManager.saveBack(playerId, new ArrayDeque<>());
            }
            return;
        }
        if (configManager.persistentBack()) {
            save();
        }
    }
}
