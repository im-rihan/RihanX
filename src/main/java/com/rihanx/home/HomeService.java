package com.rihanx.home;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import com.rihanx.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Player home set / delete / list / teleport operations.
 */
public final class HomeService {

    public static final @NotNull String DEFAULT_HOME = "home";

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull TeleportManager teleportManager;
    private final @NotNull HomeStore store;

    public HomeService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager
    ) {
        this(plugin, messages, teleportManager, new HomeStore(plugin));
    }

    public HomeService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager,
            @NotNull HomeStore store
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.store = store;
    }

    public @NotNull HomeStore getStore() {
        return store;
    }

    public void setHome(@NotNull Player player, @Nullable String name) {
        String homeName = resolveName(name);
        boolean updating = store.has(player.getUniqueId(), homeName);
        if (!updating) {
            int max = getMaxHomes(player);
            if (store.count(player.getUniqueId()) >= max) {
                messages.send(player, "home-limit", MessageManager.placeholders("max", max));
                return;
            }
        }
        if (!store.set(player.getUniqueId(), homeName, player.getLocation())) {
            messages.send(player, "internal-error");
            return;
        }
        messages.send(player, "home-set", MessageManager.placeholders("home", homeName));
    }

    public void delHome(@NotNull Player player, @Nullable String name) {
        String homeName = resolveName(name);
        if (!store.delete(player.getUniqueId(), homeName)) {
            messages.send(player, "home-missing", MessageManager.placeholders("home", homeName));
            return;
        }
        messages.send(player, "home-deleted", MessageManager.placeholders("home", homeName));
    }

    public @Nullable Location getHome(@NotNull Player player, @Nullable String name) {
        return store.get(player.getUniqueId(), resolveName(name));
    }

    public @NotNull List<String> listHomes(@NotNull Player player) {
        return store.list(player.getUniqueId());
    }

    public void sendHomeList(@NotNull Player player) {
        List<String> homes = listHomes(player);
        String joined = homes.isEmpty() ? "none" : String.join(", ", homes);
        messages.send(player, "home-list", MessageManager.placeholders(
                "homes", joined,
                "count", homes.size(),
                "max", getMaxHomes(player)
        ));
    }

    public void teleportHome(@NotNull Player player, @Nullable String name) {
        String homeName = resolveName(name);
        Location location = store.get(player.getUniqueId(), homeName);
        if (location == null) {
            messages.send(player, "home-missing", MessageManager.placeholders("home", homeName));
            return;
        }
        teleportManager.teleport(player, location, "home");
        messages.send(player, "home-teleport", MessageManager.placeholders("home", homeName));
    }

    /**
     * Max homes: highest {@code rihanx.home.limit.<n>}, else {@code homes.max-by-permission},
     * else ops use {@code homes.max-op} (20), else {@code homes.max-default} (3).
     */
    public int getMaxHomes(@NotNull Player player) {
        int fromPermission = highestLimitPermission(player);
        int fromMap = highestMappedLimit(player);
        int resolved = Math.max(fromPermission, fromMap);
        if (resolved >= 0) {
            return resolved;
        }
        if (player.isOp()) {
            return plugin.getConfig().getInt("homes.max-op", 20);
        }
        return plugin.getConfig().getInt("homes.max-default", 3);
    }

    public void reload() {
        store.load();
    }

    private static @NotNull String resolveName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_HOME;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static int highestLimitPermission(@NotNull Player player) {
        int max = -1;
        String prefix = "rihanx.home.limit.";
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String permission = info.getPermission();
            if (!permission.startsWith(prefix)) {
                continue;
            }
            String suffix = permission.substring(prefix.length());
            try {
                max = Math.max(max, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    private int highestMappedLimit(@NotNull Player player) {
        ConfigurationSection map = plugin.getConfig().getConfigurationSection("homes.max-by-permission");
        if (map == null) {
            return -1;
        }
        int max = -1;
        for (String permission : map.getKeys(false)) {
            if (player.hasPermission(permission)) {
                max = Math.max(max, map.getInt(permission));
            }
        }
        return max;
    }
}
