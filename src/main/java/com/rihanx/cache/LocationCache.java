package com.rihanx.cache;

import com.rihanx.models.BackLocation;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of recent /back locations per player.
 */
public final class LocationCache {

    private final Map<UUID, Deque<BackLocation>> history = new ConcurrentHashMap<>();
    private final int maxHistory;

    public LocationCache(int maxHistory) {
        this.maxHistory = Math.max(1, maxHistory);
    }

    public void push(@NotNull UUID playerId, @NotNull Location location) {
        Deque<BackLocation> deque = history.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addFirst(new BackLocation(playerId, location, System.currentTimeMillis()));
            while (deque.size() > maxHistory) {
                deque.removeLast();
            }
        }
    }

    public @Nullable BackLocation peek(@NotNull UUID playerId) {
        Deque<BackLocation> deque = history.get(playerId);
        if (deque == null) {
            return null;
        }
        synchronized (deque) {
            return deque.peekFirst();
        }
    }

    public @Nullable BackLocation pop(@NotNull UUID playerId) {
        Deque<BackLocation> deque = history.get(playerId);
        if (deque == null) {
            return null;
        }
        synchronized (deque) {
            return deque.pollFirst();
        }
    }

    public void clear(@NotNull UUID playerId) {
        history.remove(playerId);
    }

    public void clearAll() {
        history.clear();
    }

    public @NotNull Map<UUID, Deque<BackLocation>> snapshot() {
        return Map.copyOf(history);
    }

    public void restore(@NotNull UUID playerId, @NotNull Deque<BackLocation> locations) {
        Deque<BackLocation> deque = new ArrayDeque<>();
        int count = 0;
        for (BackLocation location : locations) {
            if (count >= maxHistory) {
                break;
            }
            deque.addLast(location);
            count++;
        }
        history.put(playerId, deque);
    }
}
