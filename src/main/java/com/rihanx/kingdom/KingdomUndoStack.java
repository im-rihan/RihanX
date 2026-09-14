package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player LIFO undo for kingdom masonry (towers, drain, gate lanterns).
 */
public final class KingdomUndoStack {

    public record Snapshot(int x, int y, int z, @NotNull String beforeMaterial) {
    }

    public record Session(
            @NotNull String tool,
            @NotNull String kingdomId,
            @NotNull UUID worldId,
            @NotNull List<Snapshot> snapshots
    ) {
        public Session {
            snapshots = List.copyOf(snapshots);
        }

        public int size() {
            return snapshots.size();
        }
    }

    private final @NotNull Map<UUID, Deque<Session>> stacks = new ConcurrentHashMap<>();
    private final int max;

    public KingdomUndoStack(int max) {
        this.max = Math.max(1, max);
    }

    public void push(@NotNull UUID playerId, @NotNull Session session) {
        if (session.snapshots().isEmpty()) {
            return;
        }
        Deque<Session> stack = stacks.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        stack.push(session);
        while (stack.size() > max) {
            stack.removeLast();
        }
    }

    public @org.jetbrains.annotations.Nullable Session pop(@NotNull UUID playerId) {
        Deque<Session> stack = stacks.get(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    public boolean isEmpty(@NotNull UUID playerId) {
        Deque<Session> stack = stacks.get(playerId);
        return stack == null || stack.isEmpty();
    }

    public int depth(@NotNull UUID playerId) {
        Deque<Session> stack = stacks.get(playerId);
        return stack == null ? 0 : stack.size();
    }

    public void clear(@NotNull UUID playerId) {
        stacks.remove(playerId);
    }
}
