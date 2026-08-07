package com.rihanx.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Safe location and distance helpers.
 */
public final class LocationUtil {

    private LocationUtil() {
    }

    public static double distance(@NotNull Location a, @NotNull Location b) {
        if (a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return Double.MAX_VALUE;
        }
        return a.distance(b);
    }

    public static @NotNull String format(@NotNull Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    public static @Nullable Location findSafe(@NotNull Location destination, int maxYOffset) {
        World world = destination.getWorld();
        if (world == null) {
            return null;
        }
        int x = destination.getBlockX();
        int z = destination.getBlockZ();
        int startY = destination.getBlockY();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 2;

        for (int offset = 0; offset <= maxYOffset; offset++) {
            int up = startY + offset;
            int down = startY - offset;
            if (up <= maxY && isSafe(world, x, up, z)) {
                return centered(world, x, up, z, destination.getYaw(), destination.getPitch());
            }
            if (offset != 0 && down >= minY && isSafe(world, x, down, z)) {
                return centered(world, x, down, z, destination.getYaw(), destination.getPitch());
            }
        }

        int highest = world.getHighestBlockYAt(x, z);
        int feetY = highest + 1;
        if (feetY <= maxY && isSafe(world, x, feetY, z)) {
            return centered(world, x, feetY, z, destination.getYaw(), destination.getPitch());
        }
        return null;
    }

    public static boolean isSafe(@NotNull World world, int x, int y, int z) {
        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 1) {
            return false;
        }
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);
        return isPassable(feet) && isPassable(head) && isSolid(ground) && !isDangerous(ground) && !isDangerous(feet);
    }

    public static boolean isSafeStanding(@NotNull Block ground) {
        Block feet = ground.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        return isSolid(ground) && isPassable(feet) && isPassable(head) && !isDangerous(ground) && !isDangerous(feet);
    }

    public static boolean isPassable(@NotNull Block block) {
        Material type = block.getType();
        return !type.isSolid() || type == Material.COBWEB;
    }

    public static boolean isSolid(@NotNull Block block) {
        Material type = block.getType();
        return type.isSolid() && type != Material.LAVA && type != Material.MAGMA_BLOCK;
    }

    public static boolean isDangerous(@NotNull Block block) {
        Material type = block.getType();
        return type == Material.LAVA
                || type == Material.FIRE
                || type == Material.SOUL_FIRE
                || type == Material.MAGMA_BLOCK
                || type == Material.CACTUS
                || type == Material.SWEET_BERRY_BUSH
                || type == Material.WITHER_ROSE;
    }

    public static @NotNull Location centered(
            @NotNull World world,
            int x,
            int y,
            int z,
            float yaw,
            float pitch
    ) {
        return new Location(world, x + 0.5, y, z + 0.5, yaw, pitch);
    }

    public static @NotNull Location centerOfChunk(@NotNull World world, int chunkX, int chunkZ) {
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        int y = world.getHighestBlockYAt(x, z) + 1;
        return centered(world, x, y, z, 0f, 0f);
    }

    public static boolean sameBlock(@NotNull Location a, @NotNull Location b) {
        return a.getWorld() != null
                && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
