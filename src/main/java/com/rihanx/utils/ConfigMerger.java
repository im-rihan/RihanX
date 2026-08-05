package com.rihanx.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Merges missing default keys from the jar {@code config.yml} into the live plugin config.
 */
public final class ConfigMerger {

    private ConfigMerger() {
    }

    /**
     * Load default {@code config.yml} from the plugin jar and recursively set any keys
     * that are absent from {@link Plugin#getConfig()}, then {@link Plugin#saveConfig()}.
     */
    public static void mergeMissingDefaults(@NotNull Plugin plugin) {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) {
                plugin.getLogger().warning("No default config.yml found in jar; skip merge");
                return;
            }
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            FileConfiguration config = plugin.getConfig();
            mergeSection(defaults, config);
            plugin.saveConfig();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not merge default config keys", ex);
        }
    }

    private static void mergeSection(@NotNull ConfigurationSection defaults, @NotNull ConfigurationSection target) {
        for (String key : defaults.getKeys(false)) {
            if (defaults.isConfigurationSection(key)) {
                ConfigurationSection defaultChild = defaults.getConfigurationSection(key);
                if (defaultChild == null) {
                    continue;
                }
                if (!target.isConfigurationSection(key)) {
                    if (target.contains(key)) {
                        // Existing scalar/list at this path — leave it alone
                        continue;
                    }
                    target.createSection(key);
                }
                ConfigurationSection targetChild = target.getConfigurationSection(key);
                if (targetChild != null) {
                    mergeSection(defaultChild, targetChild);
                }
            } else if (!target.contains(key)) {
                target.set(key, defaults.get(key));
            }
        }
    }
}
