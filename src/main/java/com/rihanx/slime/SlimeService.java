package com.rihanx.slime;

import com.rihanx.cache.SearchCache;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.models.ChunkCoord;
import com.rihanx.models.SearchResult;
import com.rihanx.scheduler.AsyncTaskTracker;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.utils.ChunkUtil;
import com.rihanx.utils.LocationUtil;
import com.rihanx.utils.SlimeUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Slime chunk detection, search, density, and map services.
 */
public final class SlimeService {

    private final @NotNull ConfigManager config;
    private final @NotNull MessageManager messages;
    private final @NotNull SchedulerUtil scheduler;
    private final @NotNull AsyncTaskTracker tracker;
    private final @NotNull SearchCache searchCache;

    public SlimeService(
            @NotNull ConfigManager config,
            @NotNull MessageManager messages,
            @NotNull SchedulerUtil scheduler,
            @NotNull AsyncTaskTracker tracker,
            @NotNull SearchCache searchCache
    ) {
        this.config = config;
        this.messages = messages;
        this.scheduler = scheduler;
        this.tracker = tracker;
        this.searchCache = searchCache;
    }

    public boolean isSlime(@NotNull Player player) {
        return SlimeUtil.isSlimeChunk(player.getWorld(), ChunkUtil.coordOf(player.getLocation()));
    }

    public void check(@NotNull Player player) {
        ChunkCoord coord = ChunkUtil.coordOf(player.getLocation());
        boolean slime = SlimeUtil.isSlimeChunk(player.getWorld(), coord);
        messages.send(player, slime ? "slime-yes" : "slime-no",
                MessageManager.placeholders("x", coord.getX(), "z", coord.getZ()));
    }

    public @NotNull CompletableFuture<ChunkCoord> findNearestAsync(@NotNull Player player, int radius) {
        World world = player.getWorld();
        ChunkCoord origin = ChunkUtil.coordOf(player.getLocation());
        AsyncTaskTracker.TrackedTask task = tracker.start(player.getUniqueId(), "slime-nearest");
        return scheduler.supplyAsync(() -> SlimeUtil.findNearest(world, origin, radius, task::isCancelled))
                .whenComplete((result, error) -> tracker.complete(player.getUniqueId()));
    }

    public @NotNull CompletableFuture<List<ChunkCoord>> searchAsync(@NotNull Player player, int radius) {
        World world = player.getWorld();
        ChunkCoord origin = ChunkUtil.coordOf(player.getLocation());
        int maxResults = config.getMaxSearchResults();
        AsyncTaskTracker.TrackedTask task = tracker.start(player.getUniqueId(), "slime-search");
        messages.send(player, "search-started", MessageManager.placeholders("radius", radius));
        return scheduler.supplyAsync(() -> SlimeUtil.search(world, origin, radius, maxResults, task::isCancelled))
                .whenComplete((result, error) -> tracker.complete(player.getUniqueId()));
    }

    public @NotNull CompletableFuture<DensityResult> densityAsync(@NotNull Player player, int radius) {
        World world = player.getWorld();
        ChunkCoord origin = ChunkUtil.coordOf(player.getLocation());
        AsyncTaskTracker.TrackedTask task = tracker.start(player.getUniqueId(), "slime-density");
        return scheduler.supplyAsync(() -> {
            if (task.isCancelled()) {
                return new DensityResult(0, 0, 0.0, null, 0);
            }
            int total = ChunkUtil.countChunksInRadius(radius);
            int count = SlimeUtil.countSlime(world, origin, radius);
            double percent = total == 0 ? 0.0 : (count * 100.0) / total;
            FarmSpot best = findBestFarmSpot(world, origin, radius, 1);
            return new DensityResult(
                    count,
                    total,
                    percent,
                    best == null ? null : best.center(),
                    best == null ? 0 : best.slimeCount()
            );
        }).whenComplete((result, error) -> tracker.complete(player.getUniqueId()));
    }

    public @Nullable FarmSpot findBestFarmSpot(
            @NotNull World world,
            @NotNull ChunkCoord origin,
            int searchRadius,
            int farmRadius
    ) {
        FarmSpot best = null;
        for (int cx = origin.getX() - searchRadius; cx <= origin.getX() + searchRadius; cx++) {
            for (int cz = origin.getZ() - searchRadius; cz <= origin.getZ() + searchRadius; cz++) {
                int slime = 0;
                for (int dx = -farmRadius; dx <= farmRadius; dx++) {
                    for (int dz = -farmRadius; dz <= farmRadius; dz++) {
                        if (SlimeUtil.isSlimeChunk(world, cx + dx, cz + dz)) {
                            slime++;
                        }
                    }
                }
                if (best == null || slime > best.slimeCount()) {
                    best = new FarmSpot(new ChunkCoord(cx, cz, world), slime);
                }
            }
        }
        return best;
    }

    public @NotNull String buildMap(@NotNull Player player, int radius) {
        ChunkCoord origin = ChunkUtil.coordOf(player.getLocation());
        return SlimeUtil.buildAsciiMap(
                player.getWorld(),
                origin,
                radius,
                config.getMapSlimeChar(),
                config.getMapEmptyChar(),
                config.getMapPlayerChar()
        );
    }

    public void cacheSlimeResult(@NotNull Player player, @NotNull ChunkCoord coord) {
        Location location = LocationUtil.centerOfChunk(player.getWorld(), coord.getX(), coord.getZ());
        double distance = ChunkUtil.coordOf(player.getLocation()).distance(coord);
        searchCache.put(player.getUniqueId(), new SearchResult(
                SearchResult.Type.SLIME,
                "slime",
                location,
                distance
        ));
    }

    public int clampRadius(int requested) {
        int max = config.getMaxSearchRadius();
        int value = requested <= 0 ? config.getDefaultSearchRadius() : requested;
        return Math.min(Math.max(1, value), max);
    }

    public int clampMapRadius(int requested) {
        int max = config.getSlimeMaxMapRadius();
        int value = requested <= 0 ? config.getSlimeDefaultMapRadius() : requested;
        return Math.min(Math.max(0, value), max);
    }

    public @NotNull String formatPercent(double percent) {
        return String.format(Locale.US, "%.2f", percent);
    }

    public record FarmSpot(@NotNull ChunkCoord center, int slimeCount) {
    }

    public record DensityResult(
            int count,
            int total,
            double percent,
            @Nullable ChunkCoord bestFarmChunk,
            int bestFarmSlimeCount
    ) {
    }
}
