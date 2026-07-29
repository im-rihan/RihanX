package com.rihanx.edit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Inclusive axis-aligned cuboid in a single world.
 */
public final class Cuboid implements Iterable<Block> {

    private final @NotNull World world;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Cuboid(@NotNull Location a, @NotNull Location b) {
        if (a.getWorld() == null || b.getWorld() == null || !Objects.equals(a.getWorld(), b.getWorld())) {
            throw new IllegalArgumentException("Cuboid corners must be in the same world");
        }
        this.world = a.getWorld();
        this.minX = Math.min(a.getBlockX(), b.getBlockX());
        this.minY = Math.min(a.getBlockY(), b.getBlockY());
        this.minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        this.maxX = Math.max(a.getBlockX(), b.getBlockX());
        this.maxY = Math.max(a.getBlockY(), b.getBlockY());
        this.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
    }

    public Cuboid(@NotNull World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public @NotNull World getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean contains(@NotNull Location location) {
        if (location.getWorld() == null || !location.getWorld().equals(world)) {
            return false;
        }
        return contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean contains(@NotNull Block block) {
        return block.getWorld().equals(world) && contains(block.getX(), block.getY(), block.getZ());
    }

    public @NotNull Location getCenter() {
        return new Location(world, (minX + maxX) / 2.0 + 0.5, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0 + 0.5);
    }

    public boolean isWall(int x, int y, int z) {
        return x == minX || x == maxX || z == minZ || z == maxZ;
    }

    public boolean isOutline(int x, int y, int z) {
        boolean onX = x == minX || x == maxX;
        boolean onY = y == minY || y == maxY;
        boolean onZ = z == minZ || z == maxZ;
        int faces = (onX ? 1 : 0) + (onY ? 1 : 0) + (onZ ? 1 : 0);
        return faces >= 1 && (onX || onY || onZ) && (onX || onZ || onY);
    }

    public boolean isHollowShell(int x, int y, int z) {
        return x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
    }

    @Override
    public @NotNull Iterator<Block> iterator() {
        return new Iterator<>() {
            private int x = minX;
            private int y = minY;
            private int z = minZ;
            private boolean done = volume() <= 0;

            @Override
            public boolean hasNext() {
                return !done;
            }

            @Override
            public Block next() {
                if (done) {
                    throw new NoSuchElementException();
                }
                Block block = world.getBlockAt(x, y, z);
                z++;
                if (z > maxZ) {
                    z = minZ;
                    y++;
                    if (y > maxY) {
                        y = minY;
                        x++;
                        if (x > maxX) {
                            done = true;
                        }
                    }
                }
                return block;
            }
        };
    }

    public @Nullable Cuboid intersect(@NotNull Cuboid other) {
        if (!world.equals(other.world)) {
            return null;
        }
        int nx1 = Math.max(minX, other.minX);
        int ny1 = Math.max(minY, other.minY);
        int nz1 = Math.max(minZ, other.minZ);
        int nx2 = Math.min(maxX, other.maxX);
        int ny2 = Math.min(maxY, other.maxY);
        int nz2 = Math.min(maxZ, other.maxZ);
        if (nx1 > nx2 || ny1 > ny2 || nz1 > nz2) {
            return null;
        }
        return new Cuboid(world, nx1, ny1, nz1, nx2, ny2, nz2);
    }
}
