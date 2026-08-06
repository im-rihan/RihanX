package com.rihanx.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Persists fly/god (and similar) player flags across quit and restarts.
 */
public final class PlayerStateStore {

    private final @NotNull Plugin plugin;
    private final @NotNull File file;
    private final Set<UUID> fly = ConcurrentHashMap.newKeySet();
    private final Set<UUID> god = ConcurrentHashMap.newKeySet();

    public PlayerStateStore(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-states.yml");
        load();
    }

    public void load() {
        fly.clear();
        god.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String raw : yaml.getStringList("fly")) {
            try {
                fly.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (String raw : yaml.getStringList("god")) {
            try {
                god.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("fly", toList(fly));
        yaml.set("god", toList(god));
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for player-states.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player-states.yml", ex);
        }
    }

    public boolean hasFly(@NotNull UUID id) {
        return fly.contains(id);
    }

    public void setFly(@NotNull UUID id, boolean enabled) {
        if (enabled) {
            fly.add(id);
        } else {
            fly.remove(id);
        }
        save();
    }

    public boolean hasGod(@NotNull UUID id) {
        return god.contains(id);
    }

    public void setGod(@NotNull UUID id, boolean enabled) {
        if (enabled) {
            god.add(id);
        } else {
            god.remove(id);
        }
        save();
    }

    public @NotNull Set<UUID> getFly() {
        return Collections.unmodifiableSet(fly);
    }

    public @NotNull Set<UUID> getGod() {
        return Collections.unmodifiableSet(god);
    }

    private static @NotNull List<String> toList(@NotNull Set<UUID> ids) {
        List<String> list = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            list.add(id.toString());
        }
        return list;
    }
}
