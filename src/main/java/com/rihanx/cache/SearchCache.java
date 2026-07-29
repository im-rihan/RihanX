package com.rihanx.cache;

import com.rihanx.models.SearchResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived cache of the last successful search result per player.
 */
public final class SearchCache {

    private final Map<UUID, SearchResult> lastResults = new ConcurrentHashMap<>();
    private final Map<UUID, Long> timestamps = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final int maxEntries;

    public SearchCache(long ttlMillis, int maxEntries) {
        this.ttlMillis = ttlMillis;
        this.maxEntries = Math.max(1, maxEntries);
    }

    public void put(@NotNull UUID playerId, @NotNull SearchResult result) {
        if (lastResults.size() >= maxEntries) {
            evictOldest();
        }
        lastResults.put(playerId, result);
        timestamps.put(playerId, System.currentTimeMillis());
    }

    public @NotNull Optional<SearchResult> get(@NotNull UUID playerId) {
        Long stamped = timestamps.get(playerId);
        if (stamped == null) {
            return Optional.empty();
        }
        if (System.currentTimeMillis() - stamped > ttlMillis) {
            invalidate(playerId);
            return Optional.empty();
        }
        return Optional.ofNullable(lastResults.get(playerId));
    }

    public @Nullable SearchResult getOrNull(@NotNull UUID playerId) {
        return get(playerId).orElse(null);
    }

    public void invalidate(@NotNull UUID playerId) {
        lastResults.remove(playerId);
        timestamps.remove(playerId);
    }

    public void clear() {
        lastResults.clear();
        timestamps.clear();
    }

    private void evictOldest() {
        UUID oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<UUID, Long> entry : timestamps.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldest = entry.getKey();
            }
        }
        if (oldest != null) {
            invalidate(oldest);
        }
    }
}
