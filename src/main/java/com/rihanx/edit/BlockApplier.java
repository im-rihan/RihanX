package com.rihanx.edit;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Applies block changes and collects undo snapshots (sync or chunked).
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

    /**
     * Applies a fill over multiple ticks to avoid main-thread stalls.
     *
     * @return the repeating task, or null if completed immediately
     */
    public static @Nullable BukkitTask fillChunked(
            @NotNull Plugin plugin,
            @NotNull Cuboid cuboid,
            @NotNull BlockData data,
            @NotNull Predicate<Block> filter,
            boolean physics,
            int blocksPerTick,
            @NotNull Consumer<EditHistory.EditSession> onComplete,
            @NotNull Consumer<Integer> onProgress
    ) {
        int perTick = Math.max(1, blocksPerTick);
        List<BlockSnapshot> before = new ArrayList<>();
        List<BlockSnapshot> after = new ArrayList<>();
        Iterator<Block> iterator = cuboid.iterator();
        final int[] processed = {0};

        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int budget = perTick;
            while (budget-- > 0 && iterator.hasNext()) {
                Block block = iterator.next();
                processed[0]++;
                if (!filter.test(block) || block.getBlockData().matches(data)) {
                    continue;
                }
                before.add(BlockSnapshot.from(block));
                block.setBlockData(data, physics);
                after.add(new BlockSnapshot(block.getX(), block.getY(), block.getZ(), data));
            }
            if (processed[0] % (perTick * 5) == 0 || !iterator.hasNext()) {
                onProgress.accept(processed[0]);
            }
            if (!iterator.hasNext()) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                onComplete.accept(new EditHistory.EditSession(before, after));
            }
        }, 1L, 1L);
        return holder[0];
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

    public static @Nullable BukkitTask applySnapshotsChunked(
            @NotNull Plugin plugin,
            @NotNull World world,
            @NotNull List<BlockSnapshot> snapshots,
            boolean physics,
            int blocksPerTick,
            @NotNull Runnable onComplete
    ) {
        int perTick = Math.max(1, blocksPerTick);
        final int[] index = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int budget = perTick;
            while (budget-- > 0 && index[0] < snapshots.size()) {
                snapshots.get(index[0]++).apply(world, physics);
            }
            if (index[0] >= snapshots.size()) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                onComplete.run();
            }
        }, 1L, 1L);
        return holder[0];
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

    public static @Nullable BukkitTask pasteChunked(
            @NotNull Plugin plugin,
            @NotNull World world,
            int baseX,
            int baseY,
            int baseZ,
            @NotNull Clipboard clipboard,
            boolean physics,
            @NotNull BiPredicate<Block, BlockData> allow,
            int blocksPerTick,
            @NotNull Consumer<EditHistory.EditSession> onComplete
    ) {
        int perTick = Math.max(1, blocksPerTick);
        List<Clipboard.RelativeBlock> blocks = clipboard.getBlocks();
        List<BlockSnapshot> before = new ArrayList<>();
        List<BlockSnapshot> after = new ArrayList<>();
        final int[] index = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int budget = perTick;
            while (budget-- > 0 && index[0] < blocks.size()) {
                Clipboard.RelativeBlock relative = blocks.get(index[0]++);
                int x = baseX + relative.dx();
                int y = baseY + relative.dy();
                int z = baseZ + relative.dz();
                Block block = world.getBlockAt(x, y, z);
                if (!allow.test(block, relative.data()) || block.getBlockData().matches(relative.data())) {
                    continue;
                }
                before.add(BlockSnapshot.from(block));
                block.setBlockData(relative.data(), physics);
                after.add(new BlockSnapshot(x, y, z, relative.data()));
            }
            if (index[0] >= blocks.size()) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                onComplete.accept(new EditHistory.EditSession(before, after));
            }
        }, 1L, 1L);
        return holder[0];
    }
}
