package com.rihanx.api;

import com.rihanx.cache.SearchCache;
import com.rihanx.chunk.ChunkService;
import com.rihanx.database.DatabaseManager;
import com.rihanx.edit.EditService;
import com.rihanx.home.HomeService;
import com.rihanx.inventory.InventoryService;
import com.rihanx.items.ItemService;
import com.rihanx.kits.KitService;
import com.rihanx.managers.BackLocationManager;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.CooldownManager;
import com.rihanx.managers.FreezeManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.managers.VanishManager;
import com.rihanx.performance.PerformanceService;
import com.rihanx.player.PlayerService;
import com.rihanx.protection.ProtectionService;
import com.rihanx.scheduler.AsyncTaskTracker;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.search.BlockSearchService;
import com.rihanx.search.FindService;
import com.rihanx.slime.SlimeService;
import com.rihanx.teleport.TeleportService;
import com.rihanx.teleport.TpaService;
import com.rihanx.warp.WarpService;
import com.rihanx.world.WorldService;
import org.jetbrains.annotations.NotNull;

/**
 * Public facade exposing RihanX services.
 */
public final class RihanXAPI {

    private final @NotNull ConfigManager configManager;
    private final @NotNull MessageManager messageManager;
    private final @NotNull CooldownManager cooldownManager;
    private final @NotNull FreezeManager freezeManager;
    private final @NotNull VanishManager vanishManager;
    private final @NotNull BackLocationManager backLocationManager;
    private final @NotNull DatabaseManager databaseManager;
    private final @NotNull SchedulerUtil schedulerUtil;
    private final @NotNull AsyncTaskTracker asyncTaskTracker;
    private final @NotNull SearchCache searchCache;
    private final @NotNull SlimeService slimeService;
    private final @NotNull WorldService worldService;
    private final @NotNull FindService findService;
    private final @NotNull BlockSearchService blockSearchService;
    private final @NotNull ChunkService chunkService;
    private final @NotNull TeleportService teleportService;
    private final @NotNull PlayerService playerService;
    private final @NotNull InventoryService inventoryService;
    private final @NotNull ItemService itemService;
    private final @NotNull PerformanceService performanceService;
    private final @NotNull ProtectionService protectionService;
    private final @NotNull EditService editService;
    private final @NotNull HomeService homeService;
    private final @NotNull WarpService warpService;
    private final @NotNull TpaService tpaService;
    private final @NotNull KitService kitService;

    public RihanXAPI(
            @NotNull ConfigManager configManager,
            @NotNull MessageManager messageManager,
            @NotNull CooldownManager cooldownManager,
            @NotNull FreezeManager freezeManager,
            @NotNull VanishManager vanishManager,
            @NotNull BackLocationManager backLocationManager,
            @NotNull DatabaseManager databaseManager,
            @NotNull SchedulerUtil schedulerUtil,
            @NotNull AsyncTaskTracker asyncTaskTracker,
            @NotNull SearchCache searchCache,
            @NotNull SlimeService slimeService,
            @NotNull WorldService worldService,
            @NotNull FindService findService,
            @NotNull BlockSearchService blockSearchService,
            @NotNull ChunkService chunkService,
            @NotNull TeleportService teleportService,
            @NotNull PlayerService playerService,
            @NotNull InventoryService inventoryService,
            @NotNull ItemService itemService,
            @NotNull PerformanceService performanceService,
            @NotNull ProtectionService protectionService,
            @NotNull EditService editService,
            @NotNull HomeService homeService,
            @NotNull WarpService warpService,
            @NotNull TpaService tpaService,
            @NotNull KitService kitService
    ) {
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.cooldownManager = cooldownManager;
        this.freezeManager = freezeManager;
        this.vanishManager = vanishManager;
        this.backLocationManager = backLocationManager;
        this.databaseManager = databaseManager;
        this.schedulerUtil = schedulerUtil;
        this.asyncTaskTracker = asyncTaskTracker;
        this.searchCache = searchCache;
        this.slimeService = slimeService;
        this.worldService = worldService;
        this.findService = findService;
        this.blockSearchService = blockSearchService;
        this.chunkService = chunkService;
        this.teleportService = teleportService;
        this.playerService = playerService;
        this.inventoryService = inventoryService;
        this.itemService = itemService;
        this.performanceService = performanceService;
        this.protectionService = protectionService;
        this.editService = editService;
        this.homeService = homeService;
        this.warpService = warpService;
        this.tpaService = tpaService;
        this.kitService = kitService;
    }

    public @NotNull ConfigManager config() {
        return configManager;
    }

    public @NotNull MessageManager messages() {
        return messageManager;
    }

    public @NotNull CooldownManager cooldowns() {
        return cooldownManager;
    }

    public @NotNull FreezeManager freeze() {
        return freezeManager;
    }

    public @NotNull VanishManager vanish() {
        return vanishManager;
    }

    public @NotNull BackLocationManager backs() {
        return backLocationManager;
    }

    public @NotNull DatabaseManager database() {
        return databaseManager;
    }

    public @NotNull SchedulerUtil scheduler() {
        return schedulerUtil;
    }

    public @NotNull AsyncTaskTracker tasks() {
        return asyncTaskTracker;
    }

    public @NotNull SearchCache searchCache() {
        return searchCache;
    }

    public @NotNull SlimeService slime() {
        return slimeService;
    }

    public @NotNull WorldService world() {
        return worldService;
    }

    public @NotNull FindService find() {
        return findService;
    }

    public @NotNull BlockSearchService blockSearch() {
        return blockSearchService;
    }

    public @NotNull ChunkService chunk() {
        return chunkService;
    }

    public @NotNull TeleportService teleport() {
        return teleportService;
    }

    public @NotNull PlayerService player() {
        return playerService;
    }

    public @NotNull InventoryService inventory() {
        return inventoryService;
    }

    public @NotNull ItemService items() {
        return itemService;
    }

    public @NotNull PerformanceService performance() {
        return performanceService;
    }

    public @NotNull ProtectionService protection() {
        return protectionService;
    }

    public @NotNull EditService edit() {
        return editService;
    }

    public @NotNull HomeService home() {
        return homeService;
    }

    public @NotNull WarpService warp() {
        return warpService;
    }

    public @NotNull TpaService tpa() {
        return tpaService;
    }

    public @NotNull KitService kit() {
        return kitService;
    }
}
