package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses compact durations such as {@code 10s}, {@code 2m}, {@code 500ms}.
 */
public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile(
            "^(\\d+)\\s*(ms|milliseconds|s|sec|secs|seconds|m|min|mins|minutes|h|hr|hrs|hours)?$",
            Pattern.CASE_INSENSITIVE
    );

    private DurationParser() {
    }

    /**
     * @return duration in milliseconds, or {@code -1} if the input is invalid
     */
    public static long millis(@Nullable String input, long defaultMillis) {
        if (input == null || input.isBlank()) {
            return defaultMillis;
        }
        Matcher matcher = PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            return -1L;
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        if (unit == null) {
            return amount * 1000L;
        }
        return switch (unit.toLowerCase(Locale.ROOT)) {
            case "ms", "milliseconds" -> amount;
            case "s", "sec", "secs", "seconds" -> amount * 1000L;
            case "m", "min", "mins", "minutes" -> amount * 60_000L;
            case "h", "hr", "hrs", "hours" -> amount * 3_600_000L;
            default -> amount * 1000L;
        };
    }

    public static @NotNull String format(long millis) {
        if (millis < 1000L) {
            return millis + "ms";
        }
        if (millis % 3_600_000L == 0) {
            return (millis / 3_600_000L) + "h";
        }
        if (millis % 60_000L == 0) {
            return (millis / 60_000L) + "m";
        }
        if (millis % 1000L == 0) {
            return (millis / 1000L) + "s";
        }
        return millis + "ms";
    }
}
