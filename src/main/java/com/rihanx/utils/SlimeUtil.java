package com.rihanx.utils;

import com.rihanx.models.ChunkCoord;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Slime-chunk detection and search helpers.
 */
public final class SlimeUtil {

    private SlimeUtil() {
    }

    public static boolean isSlimeChunk(@NotNull World world, int chunkX, int chunkZ) {
        return isSlimeChunk(world.getSeed(), chunkX, chunkZ);
    }

    public static boolean isSlimeChunk(@NotNull World world, @NotNull ChunkCoord coord) {
        return isSlimeChunk(world, coord.getX(), coord.getZ());
    }

    public static boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
        long rngSeed = seed
                + (long) chunkX * chunkX * 4987142L
                + (long) chunkX * 5947611L
                + (long) chunkZ * chunkZ * 4392871L
                + (long) chunkZ * 389711L
                ^ 987234911L;
        java.util.Random random = new java.util.Random(rngSeed);
        return random.nextInt(10) == 0;
    }

    public static @Nullable ChunkCoord findNearest(
            @NotNull World world,
            @NotNull ChunkCoord origin,
            int radius,
            @Nullable BooleanSupplier cancelled
    ) {
        if (isSlimeChunk(world, origin)) {
            return origin;
        }
        ChunkCoord best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    return null;
                }
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int cx = origin.getX() + dx;
                int cz = origin.getZ() + dz;
                if (!isSlimeChunk(world, cx, cz)) {
                    continue;
                }
                int dist = dx * dx + dz * dz;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new ChunkCoord(cx, cz, world);
                }
            }
        }
        return best;
    }

    public static @NotNull List<ChunkCoord> search(
            @NotNull World world,
            @NotNull ChunkCoord origin,
            int radius,
            int maxResults,
            @Nullable BooleanSupplier cancelled
    ) {
        List<ChunkCoord> results = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    return results;
                }
                int cx = origin.getX() + dx;
                int cz = origin.getZ() + dz;
                if (isSlimeChunk(world, cx, cz)) {
                    results.add(new ChunkCoord(cx, cz, world));
                }
            }
        }
        results.sort(Comparator.comparingInt(c -> c.distanceSquared(origin)));
        if (results.size() > maxResults) {
            return new ArrayList<>(results.subList(0, maxResults));
        }
        return results;
    }

    public static int countSlime(@NotNull World world, @NotNull ChunkCoord origin, int radius) {
        int slime = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (isSlimeChunk(world, origin.getX() + dx, origin.getZ() + dz)) {
                    slime++;
                }
            }
        }
        return slime;
    }

    public static @NotNull String buildAsciiMap(
            @NotNull World world,
            @NotNull ChunkCoord origin,
            int radius,
            char slimeChar,
            char emptyChar,
            char playerChar
    ) {
        StringBuilder builder = new StringBuilder();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dz == 0) {
                    builder.append(playerChar);
                } else if (isSlimeChunk(world, origin.getX() + dx, origin.getZ() + dz)) {
                    builder.append(slimeChar);
                } else {
                    builder.append(emptyChar);
                }
                if (dx < radius) {
                    builder.append(' ');
                }
            }
            if (dz < radius) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }
}
