package com.rihanx.teleport;

import com.rihanx.RihanX;
import com.rihanx.managers.CooldownManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.managers.VanishManager;
import com.rihanx.scheduler.SchedulerUtil;
import com.rihanx.utils.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-to-player teleport requests (tpa / tpahere) with timeout and vanish checks.
 */
public final class TpaService {

    private static final @NotNull String COOLDOWN_KEY = "tpa";

    public enum RequestType {
        TPA,
        TPAHERE
    }

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull TeleportManager teleportManager;
    private final @NotNull CooldownManager cooldownManager;
    private final @NotNull VanishManager vanishManager;
    private final @NotNull SchedulerUtil scheduler;

    /** Target UUID → pending request (target is the player who must accept). */
    private final Map<UUID, PendingRequest> pendingByTarget = new ConcurrentHashMap<>();
    /** Sender UUID → target UUID for cancel lookups. */
    private final Map<UUID, UUID> pendingBySender = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> expireTasks = new ConcurrentHashMap<>();

    public TpaService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager,
            @NotNull CooldownManager cooldownManager,
            @NotNull VanishManager vanishManager,
            @NotNull SchedulerUtil scheduler
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.cooldownManager = cooldownManager;
        this.vanishManager = vanishManager;
        this.scheduler = scheduler;
    }

    public void tpa(@NotNull Player sender, @NotNull Player target) {
        request(sender, target, RequestType.TPA);
    }

    public void tpahere(@NotNull Player sender, @NotNull Player target) {
        request(sender, target, RequestType.TPAHERE);
    }

    public void accept(@NotNull Player target) {
        PendingRequest request = peekValid(target.getUniqueId());
        if (request == null) {
            messages.send(target, "tpa-none");
            return;
        }
        Player sender = Bukkit.getPlayer(request.senderId());
        if (sender == null || !sender.isOnline()) {
            clear(request);
            messages.send(target, "tpa-offline");
            return;
        }

        clear(request);
        messages.send(target, "tpa-accepted", MessageManager.placeholders("player", sender.getName()));
        messages.send(sender, "tpa-accepted", MessageManager.placeholders("player", target.getName()));

        if (request.type() == RequestType.TPA) {
            teleportManager.teleport(sender, target.getLocation(), "tpa");
        } else {
            teleportManager.teleport(target, sender.getLocation(), "tpahere");
        }
    }

    public void deny(@NotNull Player target) {
        PendingRequest request = peekValid(target.getUniqueId());
        if (request == null) {
            messages.send(target, "tpa-none");
            return;
        }
        Player sender = Bukkit.getPlayer(request.senderId());
        clear(request);
        messages.send(target, "tpa-denied", MessageManager.placeholders(
                "player", sender != null ? sender.getName() : "unknown"));
        if (sender != null && sender.isOnline()) {
            messages.send(sender, "tpa-denied", MessageManager.placeholders("player", target.getName()));
        }
    }

    public void cancel(@NotNull Player sender) {
        UUID targetId = pendingBySender.get(sender.getUniqueId());
        if (targetId == null) {
            messages.send(sender, "tpa-none");
            return;
        }
        PendingRequest request = pendingByTarget.get(targetId);
        if (request == null || !request.senderId().equals(sender.getUniqueId())) {
            pendingBySender.remove(sender.getUniqueId());
            messages.send(sender, "tpa-none");
            return;
        }
        if (request.isExpired()) {
            clear(request);
            messages.send(sender, "tpa-expired");
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        clear(request);
        messages.send(sender, "tpa-cancelled", MessageManager.placeholders(
                "player", target != null ? target.getName() : "unknown"));
        if (target != null && target.isOnline()) {
            messages.send(target, "tpa-cancelled", MessageManager.placeholders("player", sender.getName()));
        }
    }

    public @Nullable PendingRequest getPendingForTarget(@NotNull UUID targetId) {
        return peekValid(targetId);
    }

    public void clearAll() {
        for (BukkitTask task : expireTasks.values()) {
            task.cancel();
        }
        expireTasks.clear();
        pendingByTarget.clear();
        pendingBySender.clear();
    }

    private void request(@NotNull Player sender, @NotNull Player target, @NotNull RequestType type) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            messages.send(sender, "tpa-self");
            return;
        }
        if (!isVisibleTarget(sender, target)) {
            messages.send(sender, "tpa-offline", MessageManager.placeholders("player", target.getName()));
            return;
        }
        if (cooldownManager.isOnCooldown(sender, COOLDOWN_KEY)) {
            messages.send(sender, "cooldown", MessageManager.placeholders(
                    "seconds", cooldownManager.getRemainingSeconds(sender, COOLDOWN_KEY)));
            return;
        }

        // Replace any existing outgoing request from this sender
        UUID previousTarget = pendingBySender.remove(sender.getUniqueId());
        if (previousTarget != null) {
            PendingRequest previous = pendingByTarget.get(previousTarget);
            if (previous != null && previous.senderId().equals(sender.getUniqueId())) {
                clear(previous);
            }
        }

        long timeoutSeconds = Math.max(1L, plugin.getConfig().getLong("tpa.timeout-seconds", 60L));
        long expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        PendingRequest request = new PendingRequest(sender.getUniqueId(), target.getUniqueId(), type, expiresAt);

        // Replace incoming request on target
        PendingRequest existing = pendingByTarget.remove(target.getUniqueId());
        if (existing != null) {
            clearMapsOnly(existing);
            cancelExpireTask(existing.targetId());
        }

        pendingByTarget.put(target.getUniqueId(), request);
        pendingBySender.put(sender.getUniqueId(), target.getUniqueId());
        scheduleExpire(request);
        cooldownManager.setCooldown(sender, COOLDOWN_KEY);

        if (type == RequestType.TPA) {
            messages.send(sender, "tpa-sent", MessageManager.placeholders("player", target.getName()));
            messages.send(target, "tpa-received", MessageManager.placeholders("player", sender.getName()));
        } else {
            messages.send(sender, "tpahere-sent", MessageManager.placeholders("player", target.getName()));
            messages.send(target, "tpahere-received", MessageManager.placeholders("player", sender.getName()));
        }
    }

    private boolean isVisibleTarget(@NotNull Player sender, @NotNull Player target) {
        if (!target.isOnline()) {
            return false;
        }
        if (vanishManager.isVanished(target) && !PermissionUtil.canSeeVanished(sender)) {
            return false;
        }
        return true;
    }

    private @Nullable PendingRequest peekValid(@NotNull UUID targetId) {
        PendingRequest request = pendingByTarget.get(targetId);
        if (request == null) {
            return null;
        }
        if (request.isExpired()) {
            clear(request);
            return null;
        }
        return request;
    }

    private void scheduleExpire(@NotNull PendingRequest request) {
        cancelExpireTask(request.targetId());
        long delayTicks = Math.max(1L, (request.expiresAt() - System.currentTimeMillis() + 50L) / 50L);
        BukkitTask task = scheduler.runSyncLater(() -> {
            PendingRequest current = pendingByTarget.get(request.targetId());
            if (current == null || current.expiresAt() != request.expiresAt()) {
                return;
            }
            if (!current.isExpired()) {
                return;
            }
            Player sender = Bukkit.getPlayer(current.senderId());
            Player target = Bukkit.getPlayer(current.targetId());
            clear(current);
            if (sender != null && sender.isOnline()) {
                messages.send(sender, "tpa-expired");
            }
            if (target != null && target.isOnline()) {
                messages.send(target, "tpa-expired");
            }
        }, delayTicks);
        expireTasks.put(request.targetId(), task);
    }

    private void clear(@NotNull PendingRequest request) {
        clearMapsOnly(request);
        cancelExpireTask(request.targetId());
    }

    private void clearMapsOnly(@NotNull PendingRequest request) {
        pendingByTarget.remove(request.targetId(), request);
        pendingBySender.remove(request.senderId(), request.targetId());
    }

    private void cancelExpireTask(@NotNull UUID targetId) {
        BukkitTask task = expireTasks.remove(targetId);
        if (task != null) {
            task.cancel();
        }
    }

    /** Periodic sweep for safety (optional call from listeners). */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingRequest>> iterator = pendingByTarget.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingRequest request = iterator.next().getValue();
            if (request.expiresAt() <= now) {
                iterator.remove();
                pendingBySender.remove(request.senderId(), request.targetId());
                cancelExpireTask(request.targetId());
            }
        }
    }

    public record PendingRequest(
            @NotNull UUID senderId,
            @NotNull UUID targetId,
            @NotNull RequestType type,
            long expiresAt
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
