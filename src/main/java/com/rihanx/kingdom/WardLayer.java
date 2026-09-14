package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Concentric citadel wards. Each layer can be sealed or lifted independently.
 */
public enum WardLayer {
    OUTER("outer", "Boundary Ring"),
    ELEMENTAL("elemental", "Water Ward"),
    SKY("sky", "Ceiling"),
    DEEP("deep", "Foundation"),
    INNER("inner", "Core Zone"),
    BEACON("beacon", "Towers");

    private final @NotNull String key;
    private final @NotNull String display;

    WardLayer(@NotNull String key, @NotNull String display) {
        this.key = key;
        this.display = display;
    }

    public @NotNull String key() {
        return key;
    }

    public @NotNull String display() {
        return display;
    }

    public static @Nullable WardLayer fromKey(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String key = input.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (key) {
            case "outer", "ward", "boundary", "ring", "horizontal" -> OUTER;
            case "elemental", "water", "lava", "fluid", "seal" -> ELEMENTAL;
            case "sky", "ceiling", "roof", "above" -> SKY;
            case "deep", "foundation", "below", "bedrock" -> DEEP;
            case "inner", "core", "sanctum" -> INNER;
            case "beacon", "towers", "crown", "spire" -> BEACON;
            default -> {
                for (WardLayer layer : values()) {
                    if (layer.key.equals(key) || layer.name().equalsIgnoreCase(key)) {
                        yield layer;
                    }
                }
                yield null;
            }
        };
    }

    public static @NotNull String[] keys() {
        WardLayer[] values = values();
        String[] keys = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            keys[i] = values[i].key;
        }
        return keys;
    }
}
