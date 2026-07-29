package com.rihanx.edit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player undo/redo stacks of block changes.
 */
public final class EditHistory {

    public record EditSession(@NotNull List<BlockSnapshot> before, @NotNull List<BlockSnapshot> after) {
        public EditSession {
            before = List.copyOf(before);
            after = List.copyOf(after);
        }
    }

    private final int maxStack;
    private final Map<UUID, Deque<EditSession>> undo = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<EditSession>> redo = new ConcurrentHashMap<>();

    public EditHistory(int maxStack) {
        this.maxStack = Math.max(1, maxStack);
    }

    public void push(@NotNull UUID playerId, @NotNull EditSession session) {
        Deque<EditSession> stack = undo.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        stack.push(session);
        while (stack.size() > maxStack) {
            stack.removeLast();
        }
        redo.remove(playerId);
    }

    public @Nullable EditSession popUndo(@NotNull UUID playerId) {
        Deque<EditSession> stack = undo.get(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        EditSession session = stack.pop();
        redo.computeIfAbsent(playerId, id -> new ArrayDeque<>()).push(session);
        return session;
    }

    public @Nullable EditSession popRedo(@NotNull UUID playerId) {
        Deque<EditSession> stack = redo.get(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        EditSession session = stack.pop();
        undo.computeIfAbsent(playerId, id -> new ArrayDeque<>()).push(session);
        return session;
    }

    public void clear(@NotNull UUID playerId) {
        undo.remove(playerId);
        redo.remove(playerId);
    }
}
