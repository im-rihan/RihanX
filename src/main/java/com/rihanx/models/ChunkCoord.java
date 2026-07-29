package com.rihanx.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable chunk coordinate pair with optional world reference.
 */
public final class ChunkCoord {

    private final int x;
    private final int z;
    private final @Nullable World world;

    public ChunkCoord(int x, int z) {
        this(x, z, null);
    }

    public ChunkCoord(int x, int z, @Nullable World world) {
        this.x = x;
        this.z = z;
        this.world = world;
    }

    public static @NotNull ChunkCoord fromLocation(@NotNull Location location) {
        return new ChunkCoord(location.getBlockX() >> 4, location.getBlockZ() >> 4, location.getWorld());
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public @Nullable World getWorld() {
        return world;
    }

    public int distanceSquared(@NotNull ChunkCoord other) {
        int dx = this.x - other.x;
        int dz = this.z - other.z;
        return dx * dx + dz * dz;
    }

    public double distance(@NotNull ChunkCoord other) {
        return Math.sqrt(distanceSquared(other));
    }

    public int blockX() {
        return x << 4;
    }

    public int blockZ() {
        return z << 4;
    }

    public int centerBlockX() {
        return blockX() + 8;
    }

    public int centerBlockZ() {
        return blockZ() + 8;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChunkCoord other)) {
            return false;
        }
        return x == other.x && z == other.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }

    @Override
    public String toString() {
        return "ChunkCoord{x=" + x + ", z=" + z + "}";
    }
}
