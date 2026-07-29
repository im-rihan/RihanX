package com.rihanx.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Numeric parsing and formatting helpers.
 */
public final class NumberUtil {

    private NumberUtil() {
    }

    public static @Nullable Integer parseInt(@NotNull String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static @Nullable Double parseDouble(@NotNull String input) {
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static @Nullable Float parseFloat(@NotNull String input) {
        try {
            return Float.parseFloat(input.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static @NotNull String format(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    public static @NotNull String formatPercent(double percent) {
        return String.format(Locale.US, "%.2f", percent);
    }
}
