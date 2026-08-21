package com.rihanx.protection;

import com.rihanx.RihanX;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Per-world default protection flags persisted to protection.yml.
 */
public final class WorldFlagStore {

    private final @NotNull RihanX plugin;
    private final @NotNull File file;
    private final @NotNull Map<String, Map<ProtectionFlag, FlagValue>> worldFlags = new ConcurrentHashMap<>();
    private final @NotNull Map<ProtectionFlag, FlagValue> defaults = new EnumMap<>(ProtectionFlag.class);

    public WorldFlagStore(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "protection.yml");
        loadDefaultsFromConfig();
        load();
    }

    public void loadDefaultsFromConfig() {
        defaults.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("protection.default-world-flags");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ProtectionFlag flag = ProtectionFlag.fromKey(key);
                FlagValue value = FlagValue.parse(section.getString(key, "allow"));
                if (flag != null && value != null && value != FlagValue.UNSET) {
                    defaults.put(flag, value);
                }
            }
        }
        defaults.putIfAbsent(ProtectionFlag.TNT, FlagValue.ALLOW);
        defaults.putIfAbsent(ProtectionFlag.CREEPER_EXPLOSION, FlagValue.DENY);
        defaults.putIfAbsent(ProtectionFlag.OTHER_EXPLOSION, FlagValue.DENY);
        defaults.putIfAbsent(ProtectionFlag.FIRE_SPREAD, FlagValue.DENY);
        defaults.putIfAbsent(ProtectionFlag.MOB_GRIEF, FlagValue.DENY);
        defaults.putIfAbsent(ProtectionFlag.BUILD, FlagValue.ALLOW);
        defaults.putIfAbsent(ProtectionFlag.PVP, FlagValue.ALLOW);
    }

    public void load() {
        worldFlags.clear();
        loadDefaultsFromConfig();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection worlds = yaml.getConfigurationSection("worlds");
        if (worlds == null) {
            return;
        }
        for (String worldName : worlds.getKeys(false)) {
            ConfigurationSection flags = worlds.getConfigurationSection(worldName);
            if (flags == null) {
                continue;
            }
            Map<ProtectionFlag, FlagValue> map = new EnumMap<>(ProtectionFlag.class);
            for (String key : flags.getKeys(false)) {
                ProtectionFlag flag = ProtectionFlag.fromKey(key);
                FlagValue value = FlagValue.parse(flags.getString(key, "unset"));
                if (flag != null && value != null && value != FlagValue.UNSET) {
                    map.put(flag, value);
                }
            }
            worldFlags.put(worldName, map);
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Map<ProtectionFlag, FlagValue>> entry : worldFlags.entrySet()) {
            for (Map.Entry<ProtectionFlag, FlagValue> flagEntry : entry.getValue().entrySet()) {
                yaml.set("worlds." + entry.getKey() + "." + flagEntry.getKey().key(), flagEntry.getValue().display());
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for protection.yml");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save protection.yml", ex);
        }
    }

    public void set(@NotNull String worldName, @NotNull ProtectionFlag flag, @NotNull FlagValue value) {
        Map<ProtectionFlag, FlagValue> map = worldFlags.computeIfAbsent(worldName, w -> new EnumMap<>(ProtectionFlag.class));
        if (value == FlagValue.UNSET) {
            map.remove(flag);
        } else {
            map.put(flag, value);
        }
        save();
    }

    public @NotNull FlagValue getRaw(@NotNull String worldName, @NotNull ProtectionFlag flag) {
        Map<ProtectionFlag, FlagValue> map = worldFlags.get(worldName);
        if (map != null && map.containsKey(flag)) {
            return map.get(flag);
        }
        return FlagValue.UNSET;
    }

    public @NotNull FlagValue resolve(@NotNull String worldName, @NotNull ProtectionFlag flag) {
        FlagValue raw = getRaw(worldName, flag);
        if (raw != FlagValue.UNSET) {
            return raw;
        }
        return defaults.getOrDefault(flag, FlagValue.ALLOW);
    }

    public @NotNull Map<ProtectionFlag, FlagValue> getEffective(@NotNull String worldName) {
        Map<ProtectionFlag, FlagValue> result = new EnumMap<>(ProtectionFlag.class);
        for (ProtectionFlag flag : ProtectionFlag.values()) {
            result.put(flag, resolve(worldName, flag));
        }
        return Collections.unmodifiableMap(result);
    }

    public void reload() {
        load();
    }
}
