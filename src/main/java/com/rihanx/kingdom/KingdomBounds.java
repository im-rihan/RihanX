package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;

/**
 * Axis-aligned sealed volume: a square footprint of {@code radius} from the
 * center, stretched from foundation Y to ceiling Y.
 */
public final class KingdomBounds {

    private final @NotNull String worldName;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int minY;
    private final int maxY;

    public KingdomBounds(
            @NotNull String worldName,
            int centerX,
            int centerZ,
            int radius,
            int minY,
            int maxY
    ) {
        if (radius < 1) {
            throw new IllegalArgumentException("radius must be >= 1");
        }
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    public @NotNull String worldName() {
        return worldName;
    }

    public int centerX() {
        return centerX;
    }

    public int centerZ() {
        return centerZ;
    }

    public int radius() {
        return radius;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int minX() {
        return centerX - radius;
    }

    public int maxX() {
        return centerX + radius;
    }

    public int minZ() {
        return centerZ - radius;
    }

    public int maxZ() {
        return centerZ + radius;
    }

    public boolean containsHorizontal(int x, int z) {
        return Math.abs(x - centerX) <= radius && Math.abs(z - centerZ) <= radius;
    }

    public boolean contains(int x, int y, int z) {
        return containsHorizontal(x, z) && y >= minY && y <= maxY;
    }

    public boolean containsWorld(@NotNull String world, int x, int y, int z) {
        return worldName.equals(world) && contains(x, y, z);
    }

    public boolean crosses(int x1, int y1, int z1, int x2, int y2, int z2) {
        return contains(x1, y1, z1) != contains(x2, y2, z2);
    }

    /**
     * True when a fluid (or entity) would leave or enter the sealed volume.
     */
    public boolean fluidWouldCross(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return contains(fromX, fromY, fromZ) != contains(toX, toY, toZ);
    }

    public boolean isOnPerimeter(int x, int z) {
        if (!containsHorizontal(x, z)) {
            return false;
        }
        return Math.abs(x - centerX) == radius || Math.abs(z - centerZ) == radius;
    }

    public boolean isOnCeiling(int y) {
        return y >= maxY;
    }

    public boolean isOnFoundation(int y) {
        return y <= minY;
    }

    public @NotNull KingdomBounds expanded(int blocks) {
        int next = Math.max(1, radius + blocks);
        return new KingdomBounds(worldName, centerX, centerZ, next, minY, maxY);
    }

    public @NotNull KingdomBounds withHeight(int newMinY, int newMaxY) {
        return new KingdomBounds(worldName, centerX, centerZ, radius, newMinY, newMaxY);
    }

    public boolean overlaps(@NotNull KingdomBounds other) {
        if (!worldName.equals(other.worldName)) {
            return false;
        }
        return minX() <= other.maxX() && maxX() >= other.minX()
                && minZ() <= other.maxZ() && maxZ() >= other.minZ()
                && minY <= other.maxY && maxY >= other.minY;
    }

    public long footprint() {
        long side = (long) radius * 2L + 1L;
        return side * side;
    }

    public long volume() {
        return footprint() * (maxY - minY + 1L);
    }

    public int distanceToEdge(int x, int z) {
        int dx = radius - Math.abs(x - centerX);
        int dz = radius - Math.abs(z - centerZ);
        return Math.min(dx, dz);
    }

    public @NotNull GateSide nearestHorizontalFace(int x, int z) {
        int north = Math.abs(z - minZ());
        int south = Math.abs(z - maxZ());
        int west = Math.abs(x - minX());
        int east = Math.abs(x - maxX());
        int best = Math.min(Math.min(north, south), Math.min(west, east));
        if (best == north) {
            return GateSide.NORTH;
        }
        if (best == south) {
            return GateSide.SOUTH;
        }
        if (best == west) {
            return GateSide.WEST;
        }
        return GateSide.EAST;
    }

    /**
     * Block coordinates of a gate cut into {@code side} at standing Y (or ceiling/foundation).
     */
    public int[] gateAnchor(int standingY, @NotNull GateSide side) {
        int y = Math.max(minY, Math.min(maxY, standingY));
        return switch (side) {
            case NORTH -> new int[]{centerX, y, minZ()};
            case SOUTH -> new int[]{centerX, y, maxZ()};
            case WEST -> new int[]{minX(), y, centerZ};
            case EAST -> new int[]{maxX(), y, centerZ};
            case ABOVE -> new int[]{centerX, maxY, centerZ};
            case BELOW -> new int[]{centerX, minY, centerZ};
        };
    }

    public int[] towerAnchor(@NotNull TowerType type, int inset) {
        int pad = Math.max(0, Math.min(inset, Math.max(0, radius - 2)));
        return switch (type) {
            case NORTH_WEST -> new int[]{minX() + pad, minZ() + pad};
            case NORTH_EAST -> new int[]{maxX() - pad, minZ() + pad};
            case SOUTH_WEST -> new int[]{minX() + pad, maxZ() - pad};
            case SOUTH_EAST -> new int[]{maxX() - pad, maxZ() - pad};
            case CENTRAL -> new int[]{centerX, centerZ};
        };
    }
}
