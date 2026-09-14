package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Architectural slot for a citadel tower.
 */
public enum TowerType {
    NORTH_WEST("nw", "NorthWest"),
    NORTH_EAST("ne", "NorthEast"),
    SOUTH_WEST("sw", "SouthWest"),
    SOUTH_EAST("se", "SouthEast"),
    CENTRAL("central", "Central");

    private final @NotNull String key;
    private final @NotNull String label;

    TowerType(@NotNull String key, @NotNull String label) {
        this.key = key;
        this.label = label;
    }

    public @NotNull String key() {
        return key;
    }

    public @NotNull String label() {
        return label;
    }

    public boolean isCentral() {
        return this == CENTRAL;
    }

    public static @Nullable TowerType fromKey(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String key = input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (key) {
            case "nw", "northwest", "north_west", "north-west" -> NORTH_WEST;
            case "ne", "northeast", "north_east", "north-east" -> NORTH_EAST;
            case "sw", "southwest", "south_west", "south-west" -> SOUTH_WEST;
            case "se", "southeast", "south_east", "south-east" -> SOUTH_EAST;
            case "central", "center", "centre", "spire", "heart" -> CENTRAL;
            default -> {
                try {
                    yield valueOf(key.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}
