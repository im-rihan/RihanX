package com.rihanx.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * String manipulation helpers.
 */
public final class TextUtil {

    private TextUtil() {
    }

    public static @NotNull String join(@NotNull String delimiter, @NotNull String... parts) {
        return String.join(delimiter, parts);
    }

    public static @NotNull String joinFrom(int startIndex, @NotNull String delimiter, @NotNull String[] args) {
        if (startIndex >= args.length) {
            return "";
        }
        return Arrays.stream(args, startIndex, args.length).collect(Collectors.joining(delimiter));
    }

    public static @NotNull String capitalize(@NotNull String input) {
        if (input.isEmpty()) {
            return input;
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    public static @NotNull String truncate(@NotNull String input, int maxLength) {
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public static @NotNull String normalizeKey(@NotNull String input) {
        return input.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    public static boolean isBlank(@Nullable String input) {
        return input == null || input.isBlank();
    }
}
