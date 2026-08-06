package com.rihanx.managers;

import com.rihanx.RihanX;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Loads and exposes strongly-typed configuration values matching config.yml.
 */
public final class ConfigManager {

    private final @NotNull RihanX plugin;
    private FileConfiguration config;

    public ConfigManager(@NotNull RihanX plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        // Always keep server clock on IST
        if (!"Asia/Kolkata".equals(config.getString("general.timezone"))) {
            config.set("general.timezone", "Asia/Kolkata");
            plugin.saveConfig();
        }
    }

    public @NotNull FileConfiguration raw() {
        return config;
    }

    public @NotNull String getPrefix() {
        return config.getString("prefix", "<gradient:#7C4DFF:#00E5FF><bold>RihanX</bold></gradient> <dark_gray>»</dark_gray> ");
    }

    public int getDefaultSearchRadius() {
        return config.getInt("search.default-radius", 32);
    }

    public int getMaxSearchRadius() {
        return config.getInt("search.max-radius", 128);
    }

    public int getMaxSearchResults() {
        return config.getInt("search.max-results", 50);
    }

    public int getProgressInterval() {
        return config.getInt("search.progress-interval", 64);
    }

    public int getSearchTimeoutSeconds() {
        return config.getInt("search.timeout-seconds", 120);
    }

    public int getStructureRadiusBlocks() {
        return config.getInt("search.structure-radius-blocks", 6400);
    }

    public int getBiomeStepBlocks() {
        return config.getInt("search.biome-step-blocks", 32);
    }

    public int getBiomeMaxRadiusBlocks() {
        return config.getInt("search.biome-max-radius-blocks", 6400);
    }

    public int getBlockSearchRadius() {
        return config.getInt("search.block-search-radius", 64);
    }

    public int getBlockSearchStep() {
        return config.getInt("search.block-search-step", 4);
    }

    public int getTeleportDelaySeconds() {
        return config.getInt("teleport.delay-seconds", 3);
    }

    public boolean cancelTeleportOnMove() {
        return config.getBoolean("teleport.cancel-on-move", true);
    }

    public boolean cancelTeleportOnDamage() {
        return config.getBoolean("teleport.cancel-on-damage", true);
    }

    public boolean teleportParticles() {
        return config.getBoolean("teleport.particles", true);
    }

    public boolean teleportSound() {
        return config.getBoolean("teleport.sound", true);
    }

    public @Nullable Sound getTeleportSound() {
        return parseSound(config.getString("teleport.sound-name", "ENTITY_ENDERMAN_TELEPORT"));
    }

    public float getTeleportSoundVolume() {
        return (float) config.getDouble("teleport.sound-volume", 1.0);
    }

    public float getTeleportSoundPitch() {
        return (float) config.getDouble("teleport.sound-pitch", 1.0);
    }

    public boolean safeTeleport() {
        return config.getBoolean("teleport.safe-teleport", true);
    }

    public int getSafeTeleportMaxY() {
        return config.getInt("teleport.safe-teleport-max-y", 32);
    }

    public int getRandomMaxRange() {
        return config.getInt("teleport.random-max-range", 2000);
    }

    public int getCooldown(@NotNull String key) {
        if (config.contains("cooldowns." + key)) {
            return config.getInt("cooldowns." + key, 0);
        }
        if ("tpa".equals(key)) {
            return config.getInt("tpa.cooldown-seconds", 5);
        }
        return 0;
    }

    public int getSlimeNearestRadius() {
        return config.getInt("slime.nearest-radius", 64);
    }

    public int getSlimeDefaultMapRadius() {
        return config.getInt("slime.default-map-radius", 8);
    }

    public int getSlimeMaxMapRadius() {
        return config.getInt("slime.max-map-radius", 32);
    }

    public char getMapSlimeChar() {
        String value = config.getString("slime.map-slime-char", "S");
        return value == null || value.isEmpty() ? 'S' : value.charAt(0);
    }

    public char getMapEmptyChar() {
        String value = config.getString("slime.map-empty-char", ".");
        return value == null || value.isEmpty() ? '.' : value.charAt(0);
    }

    public char getMapPlayerChar() {
        String value = config.getString("slime.map-player-char", "P");
        return value == null || value.isEmpty() ? 'P' : value.charAt(0);
    }

    public boolean particlesEnabled() {
        return config.getBoolean("particles.enabled", true);
    }

    public @Nullable Particle getParticle(@NotNull String path) {
        return parseParticle(config.getString("particles." + path));
    }

    public int getParticleCount(@NotNull String path) {
        return config.getInt("particles." + path, 10);
    }

    public boolean databaseEnabled() {
        return config.getBoolean("database.enabled", false);
    }

    public @NotNull String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public @NotNull String getSqliteFile() {
        return config.getString("database.sqlite-file", "rihanx.db");
    }

    public @NotNull String getMysqlHost() {
        return config.getString("database.mysql-host", "localhost");
    }

    public int getMysqlPort() {
        return config.getInt("database.mysql-port", 3306);
    }

    public @NotNull String getMysqlDatabase() {
        return config.getString("database.mysql-database", "rihanx");
    }

    public @NotNull String getMysqlUsername() {
        return config.getString("database.mysql-username", "root");
    }

    public @NotNull String getMysqlPassword() {
        return config.getString("database.mysql-password", "");
    }

    public int getPoolSize() {
        return config.getInt("database.pool-size", 5);
    }

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    /**
     * Server display timezone — always India Standard Time (Asia/Kolkata).
     * Config key is kept for readability but is forced to IST on reload.
     */
    public @NotNull ZoneId getTimezone() {
        return ZoneId.of("Asia/Kolkata");
    }

    public void forceIstTimezone() {
        config.set("general.timezone", "Asia/Kolkata");
        plugin.saveConfig();
    }

    public boolean persistentBack() {
        return config.getBoolean("general.persistent-back", true);
    }

    public int getMaxBackHistory() {
        return config.getInt("general.max-back-history", 10);
    }

    public boolean unfreezeOnQuit() {
        return config.getBoolean("general.unfreeze-on-quit", true);
    }

    public boolean unvanishOnQuit() {
        return config.getBoolean("general.unvanish-on-quit", false);
    }

    public boolean ungodOnQuit() {
        return config.getBoolean("general.ungod-on-quit", false);
    }

    public boolean unflyOnQuit() {
        return config.getBoolean("general.unfly-on-quit", false);
    }

    public @NotNull String getLanguageFile() {
        return config.getString("general.language-file", "messages.yml");
    }

    public long getSearchCacheTtlMillis() {
        return config.getLong("cache.search-ttl-seconds", 300L) * 1000L;
    }

    public long getLocationCacheTtlMillis() {
        return config.getLong("cache.location-ttl-seconds", 600L) * 1000L;
    }

    public int getCacheMaxEntries() {
        return config.getInt("cache.max-entries", 1000);
    }

    public int getEntityReportLimit() {
        return config.getInt("performance.entity-report-limit", 25);
    }

    public int getChunkReportRadius() {
        return config.getInt("performance.chunk-report-radius", 8);
    }

    public void debug(@NotNull String message) {
        if (isDebug()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    private @Nullable Particle parseParticle(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Unknown particle: " + name);
            return null;
        }
    }

    private @Nullable Sound parseSound(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Unknown sound: " + name);
            return null;
        }
    }
}
