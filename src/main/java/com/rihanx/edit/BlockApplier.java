package com.rihanx.edit;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Applies block changes and collects undo snapshots.
 */
public final class BlockApplier {

    private BlockApplier() {
    }

    public static @NotNull EditHistory.EditSession fill(
            @NotNull Cuboid cuboid,
            @NotNull BlockData data,
            @NotNull Predicate<Block> filter,
            boolean physics
    ) {
        List<BlockSnapshot> before = new ArrayList<>();
        List<BlockSnapshot> after = new ArrayList<>();
        World world = cuboid.getWorld();
        for (Block block : cuboid) {
            if (!filter.test(block)) {
                continue;
            }
            if (block.getBlockData().matches(data)) {
                continue;
            }
            before.add(BlockSnapshot.from(block));
            block.setBlockData(data, physics);
            after.add(new BlockSnapshot(block.getX(), block.getY(), block.getZ(), data));
        }
        return new EditHistory.EditSession(before, after);
    }

    public static @NotNull EditHistory.EditSession replace(
            @NotNull Cuboid cuboid,
            @NotNull Predicate<Block> from,
            @NotNull BlockData to,
            boolean physics
    ) {
        return fill(cuboid, to, from, physics);
    }

    public static void applySnapshots(@NotNull World world, @NotNull List<BlockSnapshot> snapshots, boolean physics) {
        for (BlockSnapshot snapshot : snapshots) {
            snapshot.apply(world, physics);
        }
    }

    public static @NotNull EditHistory.EditSession paste(
            @NotNull World world,
            int baseX,
            int baseY,
            int baseZ,
            @NotNull Clipboard clipboard,
            boolean physics,
            @NotNull BiPredicate<Block, BlockData> allow
    ) {
        List<BlockSnapshot> before = new ArrayList<>();
        List<BlockSnapshot> after = new ArrayList<>();
        for (Clipboard.RelativeBlock relative : clipboard.getBlocks()) {
            int x = baseX + relative.dx();
            int y = baseY + relative.dy();
            int z = baseZ + relative.dz();
            Block block = world.getBlockAt(x, y, z);
            if (!allow.test(block, relative.data())) {
                continue;
            }
            if (block.getBlockData().matches(relative.data())) {
                continue;
            }
            before.add(BlockSnapshot.from(block));
            block.setBlockData(relative.data(), physics);
            after.add(new BlockSnapshot(x, y, z, relative.data()));
        }
        return new EditHistory.EditSession(before, after);
    }
}
