package com.rihanx;

import com.rihanx.api.RihanXAPI;
import com.rihanx.base.BaseService;
import com.rihanx.base.FarmService;
import com.rihanx.cache.SearchCache;
import com.rihanx.chat.ChatService;
import com.rihanx.chunk.ChunkService;
import com.rihanx.database.DatabaseManager;
import com.rihanx.edit.EditService;
import com.rihanx.edit.SelectionManager;
import com.rihanx.gui.GuiManager;
import com.rihanx.home.HomeService;
import com.rihanx.inventory.InventoryService;
import com.rihanx.items.ItemService;
import com.rihanx.kits.KitService;
import com.rihanx.listeners.AfkListener;
import com.rihanx.listeners.FirstJoinListener;
import com.rihanx.listeners.FreezeListener;
import com.rihanx.listeners.PlayerListener;
import com.rihanx.listeners.SleepListener;
import com.rihanx.listeners.TeleportListener;
import com.rihanx.listeners.WandListener;
import com.rihanx.managers.AfkManager;
import com.rihanx.managers.BackLocationManager;
import com.rihanx.managers.CommandManager;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.CooldownManager;
import com.rihanx.managers.FlyManager;
import com.rihanx.managers.FreezeManager;
import com.rihanx.managers.GodManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.managers.PlayerStateStore;
import com.rihanx.managers.VanishManager;
import com.rihanx.performance.PerformanceService;
import com.rihanx.placeholders.RihanXPlaceholders;
import com.rihanx.player.PlayerService;
import com.rihanx.protection.ProtectionListener;
import com.rihanx.protection.ProtectionService;
import com.rihanx.scheduler.AsyncTaskTracker;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.search.BlockSearchService;
import com.rihanx.search.FindService;
import com.rihanx.slime.SlimeService;
import com.rihanx.spawn.SpawnService;
import com.rihanx.teleport.TeleportManager;
import com.rihanx.teleport.TeleportService;
import com.rihanx.teleport.TpaService;
import com.rihanx.utils.ConfigMerger;
import com.rihanx.warp.WarpService;
import com.rihanx.world.WorldService;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Main plugin class for RihanX.
 */
public final class RihanX extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;
    private FreezeManager freezeManager;
    private VanishManager vanishManager;
    private GodManager godManager;
    private FlyManager flyManager;
    private DatabaseManager databaseManager;
    private BackLocationManager backLocationManager;
    private SchedulerUtil schedulerUtil;
    private AsyncTaskTracker asyncTaskTracker;
    private SearchCache searchCache;
    private TeleportManager teleportManager;
    private TeleportService teleportService;
    private TpaService tpaService;
    private HomeService homeService;
    private WarpService warpService;
    private KitService kitService;
    private BaseService baseService;
    private FarmService farmService;
    private SlimeService slimeService;
    private WorldService worldService;
    private FindService findService;
    private BlockSearchService blockSearchService;
    private ChunkService chunkService;
    private PlayerService playerService;
    private InventoryService inventoryService;
    private ItemService itemService;
    private PerformanceService performanceService;
    private SelectionManager selectionManager;
    private ProtectionService protectionService;
    private EditService editService;
    private GuiManager guiManager;
    private CommandManager commandManager;
    private SleepListener sleepListener;
    private ChatService chatService;
    private AfkManager afkManager;
    private SpawnService spawnService;
    private RihanXAPI api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigMerger.mergeMissingDefaults(this);
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder");
        }

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this, configManager);
        this.cooldownManager = new CooldownManager(configManager);
        this.freezeManager = new FreezeManager();
        this.vanishManager = new VanishManager(this);
        this.godManager = new GodManager();
        this.flyManager = new FlyManager();
        PlayerStateStore playerStateStore = new PlayerStateStore(this);
        this.godManager.attachStore(playerStateStore);
        this.flyManager.attachStore(playerStateStore);
        this.databaseManager = new DatabaseManager(this, configManager);
        this.databaseManager.init();
        this.backLocationManager = new BackLocationManager(this, configManager, databaseManager);
        this.schedulerUtil = new SchedulerUtil(this);
        this.asyncTaskTracker = new AsyncTaskTracker();
        this.searchCache = new SearchCache(configManager.getSearchCacheTtlMillis(), configManager.getCacheMaxEntries());
        this.teleportManager = new TeleportManager(configManager, messageManager, schedulerUtil, backLocationManager);
        this.findService = new FindService(configManager, messageManager, schedulerUtil, asyncTaskTracker, searchCache);
        this.blockSearchService = new BlockSearchService(configManager, messageManager, schedulerUtil, asyncTaskTracker, searchCache, findService);
        this.teleportService = new TeleportService(teleportManager, configManager, messageManager, backLocationManager, findService, searchCache);
        this.tpaService = new TpaService(this, messageManager, teleportManager, cooldownManager, vanishManager, schedulerUtil);
        this.homeService = new HomeService(this, messageManager, teleportManager);
        this.warpService = new WarpService(this, messageManager, teleportManager);
        this.kitService = new KitService(this, messageManager);
        this.baseService = new BaseService(this, messageManager);
        this.farmService = new FarmService(this, messageManager);
        this.chatService = new ChatService(messageManager, vanishManager);
        this.afkManager = new AfkManager();
        this.spawnService = new SpawnService(messageManager, teleportManager, warpService);
        this.slimeService = new SlimeService(configManager, messageManager, schedulerUtil, asyncTaskTracker, searchCache);
        this.worldService = new WorldService(messageManager);
        this.chunkService = new ChunkService(messageManager);
        this.playerService = new PlayerService(messageManager, freezeManager, vanishManager, godManager, flyManager);
        this.inventoryService = new InventoryService(messageManager);
        this.itemService = new ItemService(this, messageManager);
        this.performanceService = new PerformanceService(messageManager, configManager);
        this.selectionManager = new SelectionManager();
        this.protectionService = new ProtectionService(this, messageManager, selectionManager);
        this.editService = new EditService(this, messageManager, selectionManager, protectionService);
        this.guiManager = new GuiManager(this);

        this.api = new RihanXAPI(
                configManager,
                messageManager,
                cooldownManager,
                freezeManager,
                vanishManager,
                backLocationManager,
                databaseManager,
                schedulerUtil,
                asyncTaskTracker,
                searchCache,
                slimeService,
                worldService,
                findService,
                blockSearchService,
                chunkService,
                teleportService,
                playerService,
                inventoryService,
                itemService,
                performanceService,
                protectionService,
                editService,
                homeService,
                warpService,
                tpaService,
                kitService
        );

        this.commandManager = new CommandManager(this);
        this.commandManager.register();

        getServer().getPluginManager().registerEvents(
                new PlayerListener(freezeManager, vanishManager, godManager, flyManager, teleportManager, configManager, cooldownManager, asyncTaskTracker, backLocationManager, chatService, afkManager),
                this
        );
        getServer().getPluginManager().registerEvents(new FreezeListener(freezeManager, messageManager), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(teleportManager, messageManager, configManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(protectionService), this);
        getServer().getPluginManager().registerEvents(new WandListener(protectionService, editService), this);
        getServer().getPluginManager().registerEvents(new AfkListener(afkManager, messageManager), this);
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this, kitService, schedulerUtil), this);
        this.sleepListener = new SleepListener(this, messageManager, vanishManager);
        vanishManager.setStateListener(sleepListener::syncSleepingIgnored);
        getServer().getPluginManager().registerEvents(sleepListener, this);

        RihanXPlaceholders.tryRegister(this);

        getLogger().info("RihanX v" + getPluginMeta().getVersion() + " enabled (Paper 26.2).");
    }

    @Override
    public void onDisable() {
        if (tpaService != null) {
            tpaService.clearAll();
        }
        if (teleportManager != null) {
            teleportManager.clear();
        }
        if (freezeManager != null) {
            freezeManager.unfreezeAll();
        }
        if (vanishManager != null) {
            vanishManager.unvanishAll();
        }
        if (godManager != null) {
            godManager.disableAllOnline();
        }
        if (flyManager != null) {
            flyManager.clearAll();
        }
        if (afkManager != null) {
            afkManager.clearAll();
        }
        if (protectionService != null) {
            protectionService.save();
        }
        if (backLocationManager != null) {
            backLocationManager.save();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        if (asyncTaskTracker != null) {
            asyncTaskTracker.clear();
        }
        if (schedulerUtil != null) {
            schedulerUtil.cancelAll();
        }
        if (searchCache != null) {
            searchCache.clear();
        }
        getLogger().info("RihanX disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        ConfigMerger.mergeMissingDefaults(this);
        configManager.reload();
        messageManager.reload();
        if (kitService != null) {
            kitService.reload();
        }
        if (homeService != null) {
            homeService.reload();
        }
        if (warpService != null) {
            warpService.reload();
        }
        if (protectionService != null) {
            protectionService.reload();
        }
        if (sleepListener != null) {
            sleepListener.refresh();
        }
        getLogger().info("RihanX configuration reloaded.");
    }

    public @NotNull RihanXAPI getAPI() {
        return api;
    }

    public @NotNull ConfigManager getConfigManager() {
        return configManager;
    }

    public @NotNull MessageManager getMessageManager() {
        return messageManager;
    }

    public @NotNull CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public @NotNull FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public @NotNull VanishManager getVanishManager() {
        return vanishManager;
    }

    public @NotNull GodManager getGodManager() {
        return godManager;
    }

    public @NotNull FlyManager getFlyManager() {
        return flyManager;
    }

    public @NotNull BackLocationManager getBackLocationManager() {
        return backLocationManager;
    }

    public @NotNull DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public @NotNull SchedulerUtil getSchedulerUtil() {
        return schedulerUtil;
    }

    public @NotNull AsyncTaskTracker getAsyncTaskTracker() {
        return asyncTaskTracker;
    }

    public @NotNull SearchCache getSearchCache() {
        return searchCache;
    }

    public @NotNull TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public @NotNull TeleportService getTeleportService() {
        return teleportService;
    }

    public @NotNull TpaService getTpaService() {
        return tpaService;
    }

    public @NotNull HomeService getHomeService() {
        return homeService;
    }

    public @NotNull WarpService getWarpService() {
        return warpService;
    }

    public @NotNull KitService getKitService() {
        return kitService;
    }

    public @NotNull BaseService getBaseService() {
        return baseService;
    }

    public @NotNull FarmService getFarmService() {
        return farmService;
    }

    public @NotNull SlimeService getSlimeService() {
        return slimeService;
    }

    public @NotNull WorldService getWorldService() {
        return worldService;
    }

    public @NotNull FindService getFindService() {
        return findService;
    }

    public @NotNull BlockSearchService getBlockSearchService() {
        return blockSearchService;
    }

    public @NotNull ChunkService getChunkService() {
        return chunkService;
    }

    public @NotNull PlayerService getPlayerService() {
        return playerService;
    }

    public @NotNull InventoryService getInventoryService() {
        return inventoryService;
    }

    public @NotNull ItemService getItemService() {
        return itemService;
    }

    public @NotNull PerformanceService getPerformanceService() {
        return performanceService;
    }

    public @NotNull ProtectionService getProtectionService() {
        return protectionService;
    }

    public @NotNull EditService getEditService() {
        return editService;
    }

    public @NotNull SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public @NotNull GuiManager getGuiManager() {
        return guiManager;
    }

    public @NotNull ChatService getChatService() {
        return chatService;
    }

    public @NotNull AfkManager getAfkManager() {
        return afkManager;
    }

    public @NotNull SpawnService getSpawnService() {
        return spawnService;
    }
}
