package com.rihanx.station;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Pure validation for station stop names and link pairing (unit-tested, no Bukkit).
 */
public final class StationLinkLogic {

    public enum LinkResult {
        OK,
        EMPTY_NAME,
        SELF,
        MISSING_LEFT,
        MISSING_RIGHT
    }

    public enum RegisterResult {
        OK,
        EMPTY_NAME,
        EXISTS
    }

    private StationLinkLogic() {
    }

    public static @NotNull String normalize(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public static @NotNull RegisterResult canRegister(
            @NotNull Set<String> existingNormalized,
            @NotNull String rawName
    ) {
        String id = normalize(rawName);
        if (id.isEmpty()) {
            return RegisterResult.EMPTY_NAME;
        }
        if (existingNormalized.contains(id)) {
            return RegisterResult.EXISTS;
        }
        return RegisterResult.OK;
    }

    public static @NotNull LinkResult canLink(
            @NotNull Set<String> existingNormalized,
            @NotNull String rawLeft,
            @NotNull String rawRight
    ) {
        String left = normalize(rawLeft);
        String right = normalize(rawRight);
        if (left.isEmpty() || right.isEmpty()) {
            return LinkResult.EMPTY_NAME;
        }
        if (left.equals(right)) {
            return LinkResult.SELF;
        }
        if (!existingNormalized.contains(left)) {
            return LinkResult.MISSING_LEFT;
        }
        if (!existingNormalized.contains(right)) {
            return LinkResult.MISSING_RIGHT;
        }
        return LinkResult.OK;
    }

    /** Portal id used for a named station stop (same name — one namespace with portals). */
    public static @NotNull String portalIdForStop(@NotNull String stopName) {
        return normalize(stopName);
    }

    public static @Nullable String missingName(
            @NotNull LinkResult result,
            @NotNull String rawLeft,
            @NotNull String rawRight
    ) {
        return switch (result) {
            case MISSING_LEFT -> normalize(rawLeft);
            case MISSING_RIGHT -> normalize(rawRight);
            default -> null;
        };
    }
}
