package com.rihanx.models;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single search hit.
 */
public final class SearchResult {

    public enum Type {
        SLIME,
        STRUCTURE,
        BIOME,
        CAVE,
        LAVA,
        WATER,
        SPAWNER,
        VILLAGE
    }

    private final @NotNull Type type;
    private final @NotNull String name;
    private final @NotNull Location location;
    private final double distance;

    public SearchResult(@NotNull Type type, @NotNull String name, @NotNull Location location, double distance) {
        this.type = type;
        this.name = name;
        this.location = location.clone();
        this.distance = distance;
    }

    public @NotNull Type getType() {
        return type;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull Location getLocation() {
        return location.clone();
    }

    public double getDistance() {
        return distance;
    }

    public int getBlockX() {
        return location.getBlockX();
    }

    public int getBlockY() {
        return location.getBlockY();
    }

    public int getBlockZ() {
        return location.getBlockZ();
    }

    public @Nullable String getWorldName() {
        return location.getWorld() != null ? location.getWorld().getName() : null;
    }
}
