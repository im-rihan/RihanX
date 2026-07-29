package com.rihanx.edit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable block change record for undo/clipboard.
 */
public final class BlockSnapshot {

    private final int x;
    private final int y;
    private final int z;
    private final @NotNull BlockData data;

    public BlockSnapshot(int x, int y, int z, @NotNull BlockData data) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data.clone();
    }

    public static @NotNull BlockSnapshot from(@NotNull Block block) {
        return new BlockSnapshot(block.getX(), block.getY(), block.getZ(), block.getBlockData());
    }

    public static @NotNull BlockSnapshot from(@NotNull BlockState state) {
        return new BlockSnapshot(state.getX(), state.getY(), state.getZ(), state.getBlockData());
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public @NotNull BlockData getData() {
        return data;
    }

    public @NotNull Material getType() {
        return data.getMaterial();
    }

    public @NotNull Location toLocation(@NotNull org.bukkit.World world) {
        return new Location(world, x, y, z);
    }

    public void apply(@NotNull org.bukkit.World world, boolean physics) {
        world.getBlockAt(x, y, z).setBlockData(data, physics);
    }
}
