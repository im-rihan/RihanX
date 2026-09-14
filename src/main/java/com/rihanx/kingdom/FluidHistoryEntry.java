package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;

/**
 * One unauthorized (or contained) fluid placement, used by {@code /kingdom restore}.
 */
public final class FluidHistoryEntry {

    private final @NotNull String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final @NotNull String previousMaterial;
    private final long timestamp;

    public FluidHistoryEntry(
            @NotNull String worldName,
            int x,
            int y,
            int z,
            @NotNull String previousMaterial,
            long timestamp
    ) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.previousMaterial = previousMaterial;
        this.timestamp = timestamp;
    }

    public @NotNull String worldName() {
        return worldName;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public @NotNull String previousMaterial() {
        return previousMaterial;
    }

    public long timestamp() {
        return timestamp;
    }
}
