package com.rihanx.kingdom;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Strongly-typed kingdom / lighting / theme settings from {@code config.yml}.
 */
public final class KingdomSettings {

    private @NotNull FileConfiguration config;

    public KingdomSettings(@NotNull FileConfiguration config) {
        this.config = config;
    }

    public void refresh(@NotNull FileConfiguration config) {
        this.config = config;
    }

    public int defaultRadius() {
        return config.getInt("kingdom.defaults.radius", 128);
    }

    public int minRadius() {
        return config.getInt("kingdom.defaults.min-radius", 16);
    }

    public int maxRadius() {
        return config.getInt("kingdom.defaults.max-radius", 256);
    }

    public int maxHeight() {
        return config.getInt("kingdom.defaults.max-height", 320);
    }

    public int minHeight() {
        return config.getInt("kingdom.defaults.min-height", -64);
    }

    public int maxKingdomsPerPlayer() {
        return config.getInt("kingdom.defaults.max-per-player", 3);
    }

    public boolean sealWater() {
        return config.getBoolean("kingdom.defaults.seal-water", true);
    }

    public boolean sealLava() {
        return config.getBoolean("kingdom.defaults.seal-lava", true);
    }

    public boolean sealCeiling() {
        return config.getBoolean("kingdom.defaults.seal-ceiling", true);
    }

    public boolean sealFoundation() {
        return config.getBoolean("kingdom.defaults.seal-foundation", true);
    }

    public int ceilingY() {
        return config.getInt("kingdom.wards.ceiling-y", 320);
    }

    public int foundationY() {
        return config.getInt("kingdom.wards.foundation-y", -64);
    }

    public boolean breachAlert() {
        return config.getBoolean("kingdom.wards.breach-alert", true);
    }

    public boolean visualizeForMembers() {
        return config.getBoolean("kingdom.wards.visualize-for-members", true);
    }

    public int cornerCount() {
        return config.getInt("kingdom.towers.corner-count", 4);
    }

    public boolean centralSpire() {
        return config.getBoolean("kingdom.towers.central-spire", true);
    }

    public int defaultTowerHeight() {
        return config.getInt("kingdom.towers.default-height", 128);
    }

    public int maxTowerHeight() {
        return config.getInt("kingdom.towers.max-height", 320);
    }

    public boolean autoLightTiers() {
        return config.getBoolean("kingdom.towers.auto-light-tiers", true);
    }

    public boolean autoBuildOnCreate() {
        return config.getBoolean("kingdom.towers.auto-build", true);
    }

    public boolean citadelKeep() {
        return config.getBoolean("kingdom.citadel.enabled", true);
    }

    public @NotNull String citadelTemplate() {
        String id = config.getString("kingdom.citadel.template", "citadel");
        return id == null || id.isBlank() ? "citadel" : id.trim().toLowerCase(Locale.ROOT);
    }

    public int tierSpacing() {
        return config.getInt("kingdom.towers.tier-spacing", 32);
    }

    public int towerInset() {
        return config.getInt("kingdom.towers.inset", 3);
    }

    public boolean beaconEnabled() {
        return config.getBoolean("kingdom.beacon.enabled", true);
    }

    public @NotNull String beaconColor() {
        return config.getString("kingdom.beacon.color", "#B87333");
    }

    public boolean beaconRainbow() {
        return config.getBoolean("kingdom.beacon.rainbow", false);
    }

    public int beaconCycleSpeed() {
        return config.getInt("kingdom.beacon.cycle-speed", 20);
    }

    public int beaconVisibleDistance() {
        return config.getInt("kingdom.beacon.visible-distance", 256);
    }

    public int maxGates() {
        return config.getInt("kingdom.gates.max-gates", 8);
    }

    public @NotNull String defaultOpenDuration() {
        return config.getString("kingdom.gates.default-open-duration", "10s");
    }

    public boolean gateLanterns() {
        return config.getBoolean("kingdom.gates.lanterns", true);
    }

    public boolean gateGuards() {
        return config.getBoolean("kingdom.gates.guards", true);
    }

    public boolean dynamicPlayers() {
        return config.getBoolean("lighting.dynamic-players", true);
    }

    public int lightingUpdateInterval() {
        return Math.max(2, config.getInt("lighting.update-interval", 10));
    }

    public boolean waterExtinguish() {
        return config.getBoolean("lighting.water-extinguish", true);
    }

    public boolean particleAuras() {
        return config.getBoolean("lighting.particle-auras", true);
    }

    public int drainBlocksPerTick() {
        return Math.max(200, config.getInt("kingdom.drain.blocks-per-tick", 4000));
    }

    public int maxFluidHistory() {
        return config.getInt("kingdom.history.max-fluid-entries", 4000);
    }

    public int buildBlocksPerTick() {
        return Math.max(100, config.getInt("kingdom.towers.blocks-per-tick", 1500));
    }

    public long defaultGateOpenMillis() {
        long parsed = DurationParser.millis(defaultOpenDuration(), 10_000L);
        return parsed < 0 ? 10_000L : parsed;
    }
}
