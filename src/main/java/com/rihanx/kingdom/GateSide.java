package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Face of the six-directional ward a gate can be cut into.
 */
public enum GateSide {
    NORTH(0, -1),
    SOUTH(0, 1),
    WEST(-1, 0),
    EAST(1, 0),
    ABOVE(0, 0),
    BELOW(0, 0);

    private final int dx;
    private final int dz;

    GateSide(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    public boolean isVerticalFace() {
        return this == ABOVE || this == BELOW;
    }

    public static @Nullable GateSide fromKey(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String key = input.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "north", "n", "-z" -> NORTH;
            case "south", "s", "+z" -> SOUTH;
            case "west", "w", "-x" -> WEST;
            case "east", "e", "+x" -> EAST;
            case "above", "up", "sky", "ceiling", "top" -> ABOVE;
            case "below", "down", "foundation", "bottom" -> BELOW;
            default -> {
                try {
                    yield valueOf(key.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }

    public @NotNull String display() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
