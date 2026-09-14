package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;

/**
 * A controlled opening in a ward face.
 */
public final class KingdomGate {

    private final @NotNull String name;
    private final @NotNull GateSide side;
    private final int x;
    private final int y;
    private final int z;
    private long openUntilEpochMs;
    private boolean lanterns;

    public KingdomGate(@NotNull String name, @NotNull GateSide side, int x, int y, int z) {
        this.name = name.toLowerCase();
        this.side = side;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lanterns = true;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull GateSide side() {
        return side;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public boolean lanterns() {
        return lanterns;
    }

    public void setLanterns(boolean lanterns) {
        this.lanterns = lanterns;
    }

    public long openUntilEpochMs() {
        return openUntilEpochMs;
    }

    public void setOpenUntilEpochMs(long openUntilEpochMs) {
        this.openUntilEpochMs = openUntilEpochMs;
    }

    public void openFor(long durationMs, long nowMs) {
        this.openUntilEpochMs = nowMs + Math.max(0L, durationMs);
    }

    public void close() {
        this.openUntilEpochMs = 0L;
    }

    public boolean isOpen(long nowMs) {
        return openUntilEpochMs > nowMs;
    }

    public long remainingMs(long nowMs) {
        return Math.max(0L, openUntilEpochMs - nowMs);
    }

    /**
     * Passage corridor: 3 wide, 4 high, 2 deep around the anchor.
     */
    public boolean allowsPassage(int px, int py, int pz, long nowMs) {
        if (!isOpen(nowMs)) {
            return false;
        }
        if (Math.abs(px - x) > 2 || Math.abs(pz - z) > 2) {
            return false;
        }
        return py >= y - 1 && py <= y + 4;
    }
}
