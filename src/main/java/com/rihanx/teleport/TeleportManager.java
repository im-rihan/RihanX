package com.rihanx.teleport;

import com.rihanx.managers.BackLocationManager;
import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.models.PendingTeleport;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.utils.PermissionUtil;
import com.rihanx.utils.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles delayed teleports with move/damage cancellation.
 */
public final class TeleportManager {

    private final @NotNull ConfigManager config;
    private final @NotNull MessageManager messages;
    private final @NotNull SchedulerUtil scheduler;
    private final @NotNull BackLocationManager backLocationManager;
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public TeleportManager(
            @NotNull ConfigManager config,
            @NotNull MessageManager messages,
            @NotNull SchedulerUtil scheduler,
            @NotNull BackLocationManager backLocationManager
    ) {
        this.config = config;
        this.messages = messages;
        this.scheduler = scheduler;
        this.backLocationManager = backLocationManager;
    }

    public void teleport(@NotNull Player player, @NotNull Location destination, @NotNull String reason) {
        teleport(player, destination, reason, false);
    }

    /**
     * @param preferExact when true, keep exact coords if already safe (spawn/warps)
     */
    public void teleportPreferExact(@NotNull Player player, @NotNull Location destination, @NotNull String reason) {
        teleport(player, destination, reason, true);
    }

    private void teleport(
            @NotNull Player player,
            @NotNull Location destination,
            @NotNull String reason,
            boolean preferExact
    ) {
        Location prepared = preferExact
                ? TeleportUtil.prepareDestinationPreferExact(
                        destination,
                        config.safeTeleport(),
                        config.getSafeTeleportMaxY()
                )
                : TeleportUtil.prepareDestination(
                        destination,
                        config.safeTeleport(),
                        config.getSafeTeleportMaxY()
                );
        if (prepared == null) {
            messages.send(player, "teleport-unsafe");
            return;
        }

        int delaySeconds = PermissionUtil.bypassTeleportDelay(player) ? 0 : config.getTeleportDelaySeconds();
        if (delaySeconds <= 0) {
            finishTeleport(player, prepared, reason);
            return;
        }

        cancel(player.getUniqueId(), false);
        PendingTeleport pendingTeleport = new PendingTeleport(player, prepared, delaySeconds * 20, reason);
        pending.put(player.getUniqueId(), pendingTeleport);
        messages.send(player, "teleport-warmup", MessageManager.placeholders("seconds", delaySeconds));

        final int[] remaining = {delaySeconds};
        BukkitTask task = scheduler.runSyncTimer(() -> {
            if (!player.isOnline()) {
                cancel(player.getUniqueId(), false);
                return;
            }
            PendingTeleport current = pending.get(player.getUniqueId());
            if (current == null) {
                return;
            }
            if (config.cancelTeleportOnMove() && TeleportUtil.hasMovedBeyond(current, player, 0.5)) {
                cancel(player.getUniqueId(), true);
                messages.send(player, "teleport-cancelled-move");
                return;
            }
            if (config.particlesEnabled() && config.teleportParticles()) {
                TeleportUtil.playWarmupParticles(
                        player,
                        config.getParticle("teleport-warmup"),
                        config.getParticleCount("teleport-warmup-count")
                );
            }
            remaining[0]--;
            if (remaining[0] <= 0) {
                BukkitTask self = tasks.remove(player.getUniqueId());
                pending.remove(player.getUniqueId());
                if (self != null) {
                    self.cancel();
                }
                finishTeleport(player, current.getDestination(), reason);
            } else {
                messages.sendActionBar(player, "teleport-warmup",
                        MessageManager.placeholders("seconds", remaining[0]));
            }
        }, 20L, 20L);
        tasks.put(player.getUniqueId(), task);
    }

    public void finishTeleport(@NotNull Player player, @NotNull Location destination) {
        finishTeleport(player, destination, "");
    }

    public void finishTeleport(@NotNull Player player, @NotNull Location destination, @NotNull String reason) {
        backLocationManager.push(player, player.getLocation());
        player.teleportAsync(destination).thenAccept(success -> scheduler.runSync(() -> {
            if (!Boolean.TRUE.equals(success)) {
                messages.send(player, "teleport-unsafe");
                return;
            }
            if (config.particlesEnabled()) {
                TeleportUtil.playArriveEffects(
                        player,
                        destination,
                        config.teleportParticles(),
                        config.teleportSound(),
                        config.getParticle("teleport-arrive"),
                        config.getParticleCount("teleport-arrive-count"),
                        config.getTeleportSound(),
                        config.getTeleportSoundVolume(),
                        config.getTeleportSoundPitch()
                );
            }
            if ("spawn".equals(reason)) {
                messages.send(player, "spawn-teleport");
            } else {
                messages.send(player, "teleport-success");
            }
        }));
    }

    public void cancelOnDamage(@NotNull Player player) {
        if (!config.cancelTeleportOnDamage()) {
            return;
        }
        if (pending.containsKey(player.getUniqueId())) {
            cancel(player.getUniqueId(), true);
            messages.send(player, "teleport-cancelled-damage");
        }
    }

    public boolean cancel(@NotNull UUID playerId, boolean notify) {
        BukkitTask task = tasks.remove(playerId);
        PendingTeleport removed = pending.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        return removed != null;
    }

    public boolean isPending(@NotNull UUID playerId) {
        return pending.containsKey(playerId);
    }

    public @Nullable PendingTeleport getPending(@NotNull UUID playerId) {
        return pending.get(playerId);
    }

    public void clear() {
        for (UUID uuid : tasks.keySet()) {
            cancel(uuid, false);
        }
        pending.clear();
    }
}
