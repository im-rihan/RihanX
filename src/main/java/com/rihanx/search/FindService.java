package com.rihanx.search;

import com.rihanx.cache.SearchCache;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.models.SearchResult;
import com.rihanx.scheduler.AsyncTaskTracker;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.utils.BiomeUtil;
import com.rihanx.utils.StructureUtil;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Async biome and structure locate service.
 */
public final class FindService {

    private final @NotNull ConfigManager config;
    private final @NotNull MessageManager messages;
    private final @NotNull SchedulerUtil scheduler;
    private final @NotNull AsyncTaskTracker tracker;
    private final @NotNull SearchCache searchCache;

    public FindService(
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

    public @Nullable Biome resolveBiome(@NotNull String input) {
        return BiomeUtil.match(input);
    }

    public @Nullable Structure resolveStructure(@NotNull String input) {
        return StructureUtil.match(input);
    }

    public @NotNull CompletableFuture<Location> locateBiomeAsync(@NotNull Player player, @NotNull Biome biome) {
        Location origin = player.getLocation().clone();
        int maxRadius = config.getBiomeMaxRadiusBlocks();
        int step = config.getBiomeStepBlocks();
        String name = BiomeUtil.displayName(biome);
        AsyncTaskTracker.TrackedTask task = tracker.start(player.getUniqueId(), "biome:" + name);
        messages.send(player, "biome-searching", MessageManager.placeholders("biome", name));

        return scheduler.supplyAsync(() -> BiomeUtil.locate(
                origin,
                biome,
                maxRadius,
                step,
                task::isCancelled,
                (scanned, total) -> {
                    if (scanned % Math.max(1, config.getProgressInterval()) == 0) {
                        int percent = total <= 0 ? 0 : (int) ((scanned * 100L) / total);
                        scheduler.runSync(() -> messages.sendActionBar(
                                player,
                                "search-progress",
                                MessageManager.placeholders("progress", percent)
                        ));
                    }
                }
        )).whenComplete((result, error) -> tracker.complete(player.getUniqueId()));
    }

    public @NotNull CompletableFuture<Location> locateStructureAsync(@NotNull Player player, @NotNull Structure structure) {
        Location origin = player.getLocation().clone();
        int radius = config.getStructureRadiusBlocks();
        String name = StructureUtil.displayName(structure);
        AsyncTaskTracker.TrackedTask task = tracker.start(player.getUniqueId(), "structure:" + name);
        messages.send(player, "structure-searching", MessageManager.placeholders("structure", name));

        return scheduler.supplyAsync(() -> {
            if (task.isCancelled()) {
                return null;
            }
            Location found = StructureUtil.locate(origin, structure, radius);
            if (found == null) {
                found = StructureUtil.locateLegacy(origin, StructureUtil.keyString(structure), radius);
            }
            return found;
        }).whenComplete((result, error) -> tracker.complete(player.getUniqueId()));
    }

    public void handleBiomeResult(@NotNull Player player, @NotNull Biome biome, @Nullable Location found) {
        String name = BiomeUtil.displayName(biome);
        if (found == null) {
            messages.send(player, "biome-not-found", MessageManager.placeholders("biome", name));
            return;
        }
        double distance = player.getLocation().distance(found);
        searchCache.put(player.getUniqueId(), new SearchResult(
                SearchResult.Type.BIOME,
                name,
                found,
                distance
        ));
        messages.send(player, "biome-found", MessageManager.placeholders(
                "biome", name,
                "x", found.getBlockX(),
                "y", found.getBlockY(),
                "z", found.getBlockZ(),
                "distance", String.format("%.0f", distance)
        ));
    }

    public void handleStructureResult(@NotNull Player player, @NotNull Structure structure, @Nullable Location found) {
        String name = StructureUtil.displayName(structure);
        if (found == null) {
            messages.send(player, "structure-not-found", MessageManager.placeholders("structure", name));
            return;
        }
        double distance = player.getLocation().distance(found);
        searchCache.put(player.getUniqueId(), new SearchResult(
                SearchResult.Type.STRUCTURE,
                name,
                found,
                distance
        ));
        messages.send(player, "structure-found", MessageManager.placeholders(
                "structure", name,
                "x", found.getBlockX(),
                "y", found.getBlockY(),
                "z", found.getBlockZ(),
                "distance", String.format("%.0f", distance)
        ));
    }
}
