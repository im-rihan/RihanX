package com.rihanx.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates recursive missing-key merge behavior used by ConfigMerger.
 */
class ConfigMergeLogicTest {

    @TempDir
    Path tempDir;

    @Test
    void mergesMissingNestedKeysWithoutOverwriting() throws Exception {
        File existing = tempDir.resolve("config.yml").toFile();
        try (FileWriter writer = new FileWriter(existing, StandardCharsets.UTF_8)) {
            writer.write("""
                    general:
                      debug: true
                    search:
                      default-radius: 16
                    """);
        }

        YamlConfiguration target = YamlConfiguration.loadConfiguration(existing);
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.loadFromString("""
                general:
                  debug: false
                  timezone: Asia/Kolkata
                search:
                  default-radius: 32
                  max-radius: 128
                sleep:
                  enabled: true
                """);

        mergeSection(defaults, target);

        assertTrue(target.getBoolean("general.debug")); // preserved
        assertEquals("Asia/Kolkata", target.getString("general.timezone"));
        assertEquals(16, target.getInt("search.default-radius")); // preserved
        assertEquals(128, target.getInt("search.max-radius"));
        assertTrue(target.getBoolean("sleep.enabled"));
        assertFalse(target.contains("missing"));
    }

    private static void mergeSection(
            org.bukkit.configuration.ConfigurationSection defaults,
            org.bukkit.configuration.ConfigurationSection target
    ) {
        for (String key : defaults.getKeys(false)) {
            if (defaults.isConfigurationSection(key)) {
                var defaultChild = defaults.getConfigurationSection(key);
                if (defaultChild == null) {
                    continue;
                }
                if (!target.isConfigurationSection(key)) {
                    if (target.contains(key)) {
                        continue;
                    }
                    target.createSection(key);
                }
                var targetChild = target.getConfigurationSection(key);
                if (targetChild != null) {
                    mergeSection(defaultChild, targetChild);
                }
            } else if (!target.contains(key)) {
                target.set(key, defaults.get(key));
            }
        }
    }
}
