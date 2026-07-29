package com.rihanx.search;

import com.rihanx.cache.SearchCache;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.models.SearchResult;
import com.rihanx.scheduler.AsyncTaskTracker;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.utils.StructureUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/**
 * Async block-based searches in loaded chunks.
 */
public final class BlockSearchService {

    public enum SearchType {
        CAVE,
        LAVA,
        WATER,
        SPAWNER,
        VILLAGE
    }

    private final @NotNull ConfigManager config;
    private final @NotNull MessageManager messages;
    private final @NotNull SchedulerUtil scheduler;
    private final @NotNull AsyncTaskTracker tracker;
    private final @NotNull SearchCache searchCache;
    private final @NotNull FindService findService;

    public BlockSearchService(
            @NotNull ConfigManager config,
            @NotNull MessageManager messages,
            @NotNull SchedulerUtil scheduler,
            @NotNull AsyncTaskTracker tracker,
            @NotNull SearchCache searchCache,
            @NotNull FindService findService
    ) {
        this.config = config;
        this.messages = messages;
        this.scheduler = scheduler;
        this.tracker = tracker;
        this.searchCache = searchCache;
        this.findService = findService;
    }

    public @NotNull CompletableFuture<List<SearchResult>> searchAsync(
            @NotNull Player player,
            @NotNull SearchType type,
            int radius
    ) {
        if (type == SearchType.VILLAGE) {
            return searchVillageAsync(player);
        }

        Location origin = player.getLocation();
        int blockRadius = radius <= 0 ? config.getBlockSearchRadius() : radius;
        int step = config.getBlockSearchStep();
        int maxResults = config.getMaxSearchResults();
        AsyncTaskTracker.TrackedTask task = tracker.start(player.getUniqueId(), "block:" + type.name().toLowerCase());
        messages.send(player, "search-started", MessageManager.placeholders("radius", blockRadius));

        return scheduler.supplyAsync(() -> sampleLoadedChunks(
                player.getWorld(),
                origin,
                blockRadius,
                step,
                maxResults,
                type,
                task::isCancelled
        )).whenComplete((result, error) -> tracker.complete(player.getUniqueId()));
    }

    private @NotNull CompletableFuture<List<SearchResult>> searchVillageAsync(@NotNull Player player) {
        Structure village = StructureUtil.match("village");
        if (village == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return findService.locateStructureAsync(player, village).thenApply(found -> {
            if (found == null) {
                return List.<SearchResult>of();
            }
            double distance = player.getLocation().distance(found);
            SearchResult result = new SearchResult(SearchResult.Type.VILLAGE, "village", found, distance);
            searchCache.put(player.getUniqueId(), result);
            return List.of(result);
        });
    }

    private @NotNull List<SearchResult> sampleLoadedChunks(
            @NotNull World world,
            @NotNull Location origin,
            int radius,
            int step,
            int maxResults,
            @NotNull SearchType type,
            @NotNull BooleanSupplier cancelled
    ) {
        List<SearchResult> results = new ArrayList<>();
        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        for (int x = ox - radius; x <= ox + radius; x += step) {
            for (int z = oz - radius; z <= oz + radius; z += step) {
                if (cancelled.getAsBoolean()) {
                    return results;
                }
                Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
                if (!chunk.isLoaded()) {
                    continue;
                }
                for (int y = Math.max(minY, oy - radius); y <= Math.min(maxY, oy + radius); y += step) {
                    if (cancelled.getAsBoolean()) {
                        return results;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    if (!matches(type, block, world)) {
                        continue;
                    }
                    Location loc = block.getLocation().add(0.5, 0, 0.5);
                    double distance = origin.distance(loc);
                    results.add(new SearchResult(mapType(type), type.name().toLowerCase(), loc, distance));
                    if (results.size() >= maxResults) {
                        return results;
                    }
                }
            }
        }
        return results;
    }

    private boolean matches(@NotNull SearchType type, @NotNull Block block, @NotNull World world) {
        Material material = block.getType();
        return switch (type) {
            case CAVE -> isCaveAir(block, world);
            case LAVA -> material == Material.LAVA;
            case WATER -> material == Material.WATER;
            case SPAWNER -> material == Material.SPAWNER;
            case VILLAGE -> false;
        };
    }

    private boolean isCaveAir(@NotNull Block block, @NotNull World world) {
        if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR) {
            return false;
        }
        int y = block.getY();
        if (y >= world.getHighestBlockYAt(block.getX(), block.getZ())) {
            return false;
        }
        Block below = block.getRelative(0, -1, 0);
        return below.getType().isSolid();
    }

    private @NotNull SearchResult.Type mapType(@NotNull SearchType type) {
        return switch (type) {
            case CAVE -> SearchResult.Type.CAVE;
            case LAVA -> SearchResult.Type.LAVA;
            case WATER -> SearchResult.Type.WATER;
            case SPAWNER -> SearchResult.Type.SPAWNER;
            case VILLAGE -> SearchResult.Type.VILLAGE;
        };
    }

    public void sendResults(@NotNull Player player, @NotNull List<SearchResult> results, int radius) {
        if (results.isEmpty()) {
            messages.send(player, "search-no-results", MessageManager.placeholders("radius", radius));
            return;
        }
        messages.send(player, "search-complete", MessageManager.placeholders("count", results.size()));
        for (SearchResult result : results) {
            searchCache.put(player.getUniqueId(), result);
            messages.send(player, "search-result", MessageManager.placeholders(
                    "x", result.getBlockX(),
                    "y", result.getBlockY(),
                    "z", result.getBlockZ(),
                    "distance", String.format("%.0f", result.getDistance())
            ));
        }
    }

    public int clampRadius(int requested) {
        int max = config.getMaxSearchRadius();
        int value = requested <= 0 ? config.getBlockSearchRadius() : requested;
        return Math.min(Math.max(1, value), max);
    }
}
