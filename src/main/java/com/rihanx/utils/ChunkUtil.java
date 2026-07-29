package com.rihanx.utils;

import com.rihanx.models.ChunkCoord;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Chunk-related helpers.
 */
public final class ChunkUtil {

    private ChunkUtil() {
    }

    public static int toChunk(int blockCoord) {
        return blockCoord >> 4;
    }

    public static int toBlock(int chunkCoord) {
        return chunkCoord << 4;
    }

    public static @NotNull ChunkCoord coordOf(@NotNull Location location) {
        return ChunkCoord.fromLocation(location);
    }

    public static @NotNull ChunkCoord coordOf(@NotNull Chunk chunk) {
        return new ChunkCoord(chunk.getX(), chunk.getZ(), chunk.getWorld());
    }

    public static boolean isLoaded(@NotNull World world, int chunkX, int chunkZ) {
        return world.isChunkLoaded(chunkX, chunkZ);
    }

    public static @NotNull Chunk getChunk(@NotNull World world, int chunkX, int chunkZ) {
        return world.getChunkAt(chunkX, chunkZ);
    }

    public static int getLightLevel(@NotNull Location location) {
        Block block = location.getBlock();
        return Math.max(block.getLightFromBlocks(), block.getLightFromSky());
    }

    public static @NotNull List<Player> getNearbyPlayers(@NotNull Location origin, double radius) {
        List<Player> result = new ArrayList<>();
        World world = origin.getWorld();
        if (world == null) {
            return result;
        }
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(origin) <= radiusSq) {
                result.add(player);
            }
        }
        return result;
    }

    public static @NotNull Collection<ChunkCoord> spiral(@NotNull ChunkCoord center, int radiusChunks) {
        List<ChunkCoord> coords = new ArrayList<>();
        coords.add(center);
        for (int ring = 1; ring <= radiusChunks; ring++) {
            int minX = center.getX() - ring;
            int maxX = center.getX() + ring;
            int minZ = center.getZ() - ring;
            int maxZ = center.getZ() + ring;
            for (int x = minX; x <= maxX; x++) {
                coords.add(new ChunkCoord(x, minZ, center.getWorld()));
                coords.add(new ChunkCoord(x, maxZ, center.getWorld()));
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                coords.add(new ChunkCoord(minX, z, center.getWorld()));
                coords.add(new ChunkCoord(maxX, z, center.getWorld()));
            }
        }
        return coords;
    }

    public static int countChunksInRadius(int radius) {
        int side = radius * 2 + 1;
        return side * side;
    }

    public static int[] borderBlocks(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        return new int[]{minX, maxX, minZ, maxZ};
    }
}
