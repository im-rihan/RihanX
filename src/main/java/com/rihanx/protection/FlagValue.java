package com.rihanx.protection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Allow / deny / unset for a protection flag.
 */
public enum FlagValue {
    ALLOW,
    DENY,
    UNSET;

    public boolean isAllow() {
        return this == ALLOW;
    }

    public boolean isDeny() {
        return this == DENY;
    }

    public static @Nullable FlagValue parse(@NotNull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "allow", "true", "yes", "on", "1" -> ALLOW;
            case "deny", "false", "no", "off", "0" -> DENY;
            case "unset", "none", "inherit", "default" -> UNSET;
            default -> null;
        };
    }

    public @NotNull String display() {
        return name().toLowerCase(Locale.ROOT);
    }
}
