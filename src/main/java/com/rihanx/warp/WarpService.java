package com.rihanx.warp;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import com.rihanx.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Server warp set / delete / list / teleport operations.
 */
public final class WarpService {

    private final @NotNull MessageManager messages;
    private final @NotNull TeleportManager teleportManager;
    private final @NotNull WarpStore store;

    public WarpService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager
    ) {
        this(messages, teleportManager, new WarpStore(plugin));
    }

    public WarpService(
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager,
            @NotNull WarpStore store
    ) {
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.store = store;
    }

    public @NotNull WarpStore getStore() {
        return store;
    }

    public void setWarp(@NotNull Player player, @NotNull String name) {
        String warpName = normalize(name);
        if (warpName.isEmpty()) {
            messages.send(player, "invalid-argument", MessageManager.placeholders("input", name));
            return;
        }
        if (!store.set(warpName, player.getLocation())) {
            messages.send(player, "internal-error");
            return;
        }
        messages.send(player, "warp-set", MessageManager.placeholders("warp", warpName));
    }

    public void delWarp(@NotNull Player player, @NotNull String name) {
        String warpName = normalize(name);
        if (!store.delete(warpName)) {
            messages.send(player, "warp-missing", MessageManager.placeholders("warp", warpName));
            return;
        }
        messages.send(player, "warp-deleted", MessageManager.placeholders("warp", warpName));
    }

    public @Nullable Location getWarp(@NotNull String name) {
        return store.get(normalize(name));
    }

    public @NotNull List<String> listWarps() {
        return store.list();
    }

    public void sendWarpList(@NotNull Player player) {
        List<String> warps = listWarps();
        String joined = warps.isEmpty() ? "none" : String.join(", ", warps);
        messages.send(player, "warp-list", MessageManager.placeholders(
                "warps", joined,
                "count", warps.size()
        ));
    }

    public void teleportWarp(@NotNull Player player, @NotNull String name) {
        String warpName = normalize(name);
        Location location = store.get(warpName);
        if (location == null) {
            messages.send(player, "warp-missing", MessageManager.placeholders("warp", warpName));
            return;
        }
        teleportManager.teleport(player, location, "warp");
        messages.send(player, "warp-teleport", MessageManager.placeholders("warp", warpName));
    }

    public void reload() {
        store.load();
    }

    private static @NotNull String normalize(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
