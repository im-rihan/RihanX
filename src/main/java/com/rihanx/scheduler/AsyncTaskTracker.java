package com.rihanx.scheduler;

import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks cancellable async searches per player.
 */
public final class AsyncTaskTracker {

    public static final class TrackedTask {
        private final @NotNull UUID owner;
        private final @NotNull AtomicBoolean cancelled = new AtomicBoolean(false);
        private final @NotNull AtomicBoolean completed = new AtomicBoolean(false);
        private volatile @Nullable BukkitTask bukkitTask;
        private volatile @Nullable String label;

        public TrackedTask(@NotNull UUID owner, @Nullable String label) {
            this.owner = owner;
            this.label = label;
        }

        public @NotNull UUID getOwner() {
            return owner;
        }

        public @Nullable String getLabel() {
            return label;
        }

        public void attach(@NotNull BukkitTask task) {
            this.bukkitTask = task;
        }

        public boolean cancel() {
            if (!cancelled.compareAndSet(false, true)) {
                return false;
            }
            BukkitTask task = bukkitTask;
            if (task != null) {
                task.cancel();
            }
            return true;
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        public boolean complete() {
            return completed.compareAndSet(false, true);
        }

        public boolean isCompleted() {
            return completed.get();
        }

        public boolean isActive() {
            return !cancelled.get() && !completed.get();
        }
    }

    private final Map<UUID, TrackedTask> active = new ConcurrentHashMap<>();

    public @NotNull TrackedTask start(@NotNull UUID playerId, @Nullable String label) {
        TrackedTask previous = active.remove(playerId);
        if (previous != null) {
            previous.cancel();
        }
        TrackedTask task = new TrackedTask(playerId, label);
        active.put(playerId, task);
        return task;
    }

    public boolean cancel(@NotNull UUID playerId) {
        TrackedTask task = active.remove(playerId);
        if (task == null) {
            return false;
        }
        return task.cancel();
    }

    public void complete(@NotNull UUID playerId) {
        TrackedTask task = active.get(playerId);
        if (task != null && task.complete()) {
            active.remove(playerId, task);
        }
    }

    public boolean hasActive(@NotNull UUID playerId) {
        TrackedTask task = active.get(playerId);
        return task != null && task.isActive();
    }

    public @NotNull Optional<TrackedTask> get(@NotNull UUID playerId) {
        return Optional.ofNullable(active.get(playerId));
    }

    public void clear() {
        for (TrackedTask task : active.values()) {
            task.cancel();
        }
        active.clear();
    }
}
