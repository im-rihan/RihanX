package com.rihanx.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Thin wrapper around the Bukkit scheduler for sync/async work.
 */
public final class SchedulerUtil {

    private final @NotNull Plugin plugin;
    private final @NotNull BukkitScheduler scheduler;

    public SchedulerUtil(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }

    public @NotNull BukkitTask runSync(@NotNull Runnable task) {
        return scheduler.runTask(plugin, task);
    }

    public @NotNull BukkitTask runSyncLater(@NotNull Runnable task, long delayTicks) {
        return scheduler.runTaskLater(plugin, task, delayTicks);
    }

    public @NotNull BukkitTask runSyncTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public @NotNull BukkitTask runAsync(@NotNull Runnable task) {
        return scheduler.runTaskAsynchronously(plugin, task);
    }

    public <T> @NotNull CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runAsync(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public <T> @NotNull CompletableFuture<T> supplySync(@NotNull Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runSync(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public <T> void thenSync(@NotNull CompletableFuture<T> future, @NotNull Consumer<T> consumer) {
        future.whenComplete((result, error) -> runSync(() -> {
            if (error != null) {
                plugin.getLogger().severe("Async task failed: " + error.getMessage());
                error.printStackTrace();
                return;
            }
            consumer.accept(result);
        }));
    }

    public void cancelAll() {
        scheduler.cancelTasks(plugin);
    }

    public @NotNull Plugin getPlugin() {
        return plugin;
    }
}
