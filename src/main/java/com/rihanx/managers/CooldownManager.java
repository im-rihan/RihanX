package com.rihanx.managers;

import com.rihanx.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player command cooldown tracking.
 */
public final class CooldownManager {

    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();
    private final @NotNull ConfigManager configManager;

    public CooldownManager(@NotNull ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean isOnCooldown(@NotNull CommandSender sender, @NotNull String key) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        if (PermissionUtil.bypassCooldown(sender)) {
            return false;
        }
        int seconds = configManager.getCooldown(key);
        if (seconds <= 0) {
            return false;
        }
        Map<UUID, Long> map = cooldowns.get(key);
        if (map == null) {
            return false;
        }
        Long expires = map.get(player.getUniqueId());
        if (expires == null) {
            return false;
        }
        return System.currentTimeMillis() < expires;
    }

    public long getRemainingSeconds(@NotNull Player player, @NotNull String key) {
        Map<UUID, Long> map = cooldowns.get(key);
        if (map == null) {
            return 0L;
        }
        Long expires = map.get(player.getUniqueId());
        if (expires == null) {
            return 0L;
        }
        long remaining = expires - System.currentTimeMillis();
        return Math.max(0L, (remaining + 999L) / 1000L);
    }

    public void setCooldown(@NotNull CommandSender sender, @NotNull String key) {
        if (!(sender instanceof Player player)) {
            return;
        }
        if (PermissionUtil.bypassCooldown(sender)) {
            return;
        }
        int seconds = configManager.getCooldown(key);
        if (seconds <= 0) {
            return;
        }
        cooldowns
                .computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public void clear(@NotNull UUID playerId) {
        for (Map<UUID, Long> map : cooldowns.values()) {
            map.remove(playerId);
        }
    }

    public void clearAll() {
        cooldowns.clear();
    }
}
