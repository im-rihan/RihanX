package com.rihanx.teleport;

import com.rihanx.cache.SearchCache;
import com.rihanx.managers.BackLocationManager;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.models.ChunkCoord;
import com.rihanx.models.SearchResult;
import com.rihanx.search.FindService;
import com.rihanx.utils.BiomeUtil;
import com.rihanx.utils.LocationUtil;
import com.rihanx.utils.StructureUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * High-level teleport operations.
 */
public final class TeleportService {

    private final @NotNull TeleportManager teleportManager;
    private final @NotNull ConfigManager config;
    private final @NotNull MessageManager messages;
    private final @NotNull BackLocationManager backLocationManager;
    private final @NotNull FindService findService;
    private final @NotNull SearchCache searchCache;

    public TeleportService(
            @NotNull TeleportManager teleportManager,
            @NotNull ConfigManager config,
            @NotNull MessageManager messages,
            @NotNull BackLocationManager backLocationManager,
            @NotNull FindService findService,
            @NotNull SearchCache searchCache
    ) {
        this.teleportManager = teleportManager;
        this.config = config;
        this.messages = messages;
        this.backLocationManager = backLocationManager;
        this.findService = findService;
        this.searchCache = searchCache;
    }

    public void teleport(@NotNull Player player, @NotNull Location destination, @NotNull String reason) {
        teleportManager.teleport(player, destination, reason);
    }

    public void teleportToPos(@NotNull Player player, double x, double y, double z, @Nullable World world) {
        World targetWorld = world != null ? world : player.getWorld();
        Location destination = new Location(targetWorld, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
        teleport(player, destination, "pos");
    }

    public void teleportToPlayer(@NotNull Player player, @NotNull Player target) {
        teleport(player, target.getLocation(), "player");
    }

    public void teleportToWorldSpawn(@NotNull Player player, @NotNull World world) {
        teleport(player, world.getSpawnLocation(), "world-spawn");
    }

    public void teleportToChunk(@NotNull Player player, int chunkX, int chunkZ) {
        World world = player.getWorld();
        Location destination = LocationUtil.centerOfChunk(world, chunkX, chunkZ);
        teleport(player, destination, "chunk");
    }

    public void teleportToBiome(@NotNull Player player, @NotNull String biomeName) {
        Biome biome = BiomeUtil.match(biomeName);
        if (biome == null) {
            messages.send(player, "biome-invalid", MessageManager.placeholders("input", biomeName));
            return;
        }
        findService.locateBiomeAsync(player, biome).thenAccept(found -> {
            if (found != null) {
                teleport(player, found, "biome");
            }
        });
    }

    public void teleportToStructure(@NotNull Player player, @NotNull String structureName) {
        Structure structure = StructureUtil.match(structureName);
        if (structure == null) {
            messages.send(player, "structure-invalid", MessageManager.placeholders("input", structureName));
            return;
        }
        findService.locateStructureAsync(player, structure).thenAccept(found -> {
            if (found != null) {
                teleport(player, found, "structure");
            }
        });
    }

    public void teleportRandom(@NotNull Player player) {
        World world = player.getWorld();
        Random random = ThreadLocalRandom.current();
        int range = config.getRandomMaxRange();
        for (int attempt = 0; attempt < 25; attempt++) {
            int x = random.nextInt(range * 2 + 1) - range + player.getLocation().getBlockX();
            int z = random.nextInt(range * 2 + 1) - range + player.getLocation().getBlockZ();
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);
            Location safe = LocationUtil.findSafe(candidate, config.getSafeTeleportMaxY());
            if (safe != null) {
                teleport(player, safe, "random");
                return;
            }
        }
        messages.send(player, "teleport-unsafe");
    }

    public void teleportSafe(@NotNull Player player) {
        Location safe = LocationUtil.findSafe(player.getLocation(), config.getSafeTeleportMaxY());
        if (safe == null) {
            messages.send(player, "teleport-unsafe");
            return;
        }
        teleport(player, safe, "safe");
    }

    public void teleportBack(@NotNull Player player) {
        Location back = backLocationManager.pop(player);
        if (back == null) {
            messages.send(player, "teleport-back-none");
            return;
        }
        teleportManager.finishTeleport(player, back);
        messages.send(player, "teleport-back");
    }

    public void teleportToSlime(@NotNull Player player, @NotNull ChunkCoord coord) {
        World world = player.getWorld();
        Location destination = LocationUtil.centerOfChunk(world, coord.getX(), coord.getZ());
        teleport(player, destination, "slime");
    }

    public void teleportToLastSearch(@NotNull Player player) {
        Optional<SearchResult> cached = searchCache.get(player.getUniqueId());
        if (cached.isEmpty()) {
            messages.send(player, "search-no-results", MessageManager.placeholders("radius", 0));
            return;
        }
        teleport(player, cached.get().getLocation(), "search");
    }

    public @NotNull TeleportManager manager() {
        return teleportManager;
    }
}
