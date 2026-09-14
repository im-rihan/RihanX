package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Membership ranks inside a sealed kingdom.
 */
public enum KingdomRole {
    SOVEREIGN(3),
    WARDEN(2),
    CITIZEN(1),
    GUEST(0);

    private final int rank;

    KingdomRole(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean canManage() {
        return rank >= WARDEN.rank;
    }

    public boolean canBuild() {
        return rank >= CITIZEN.rank;
    }

    public boolean canInvite() {
        return rank >= WARDEN.rank;
    }

    public boolean receivesAlerts() {
        return rank >= WARDEN.rank;
    }

    public boolean isSovereign() {
        return this == SOVEREIGN;
    }

    public boolean atLeast(@NotNull KingdomRole other) {
        return rank >= other.rank;
    }

    public static @Nullable KingdomRole fromKey(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String key = input.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "sovereign", "king", "queen", "owner", "lord" -> SOVEREIGN;
            case "warden", "guard", "mod", "moderator" -> WARDEN;
            case "citizen", "member", "resident" -> CITIZEN;
            case "guest", "visitor" -> GUEST;
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
