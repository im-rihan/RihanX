package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * RGB beacon column color with optional rainbow cycling.
 */
public final class BeaconColor {

    public static final int DEFAULT_COPPER = 0xB87333;

    private final int rgb;
    private final boolean rainbow;

    public BeaconColor(int rgb, boolean rainbow) {
        this.rgb = rgb & 0xFFFFFF;
        this.rainbow = rainbow;
    }

    public int rgb() {
        return rgb;
    }

    public boolean rainbow() {
        return rainbow;
    }

    public int red() {
        return (rgb >> 16) & 0xFF;
    }

    public int green() {
        return (rgb >> 8) & 0xFF;
    }

    public int blue() {
        return rgb & 0xFF;
    }

    public @NotNull String hex() {
        return String.format("#%06X", rgb);
    }

    /**
     * Color at a scheduler tick. Rainbow walks HSV hue using {@code cycleSpeed} ticks per full cycle.
     */
    public int rgbAtTick(long tick, int cycleSpeed) {
        if (!rainbow) {
            return rgb;
        }
        int period = Math.max(1, cycleSpeed);
        float hue = (tick % period) / (float) period;
        return hsvToRgb(hue, 0.85f, 1.0f);
    }

    public static @NotNull BeaconColor parse(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return new BeaconColor(DEFAULT_COPPER, false);
        }
        String raw = input.trim().toLowerCase(Locale.ROOT);
        if (raw.equals("rainbow") || raw.equals("cycle") || raw.equals("prism")) {
            return new BeaconColor(DEFAULT_COPPER, true);
        }
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        } else if (raw.startsWith("0x")) {
            raw = raw.substring(2);
        }
        if (raw.length() == 3) {
            raw = "" + raw.charAt(0) + raw.charAt(0) + raw.charAt(1) + raw.charAt(1) + raw.charAt(2) + raw.charAt(2);
        }
        try {
            int value = Integer.parseInt(raw, 16);
            return new BeaconColor(value, false);
        } catch (NumberFormatException ex) {
            return new BeaconColor(DEFAULT_COPPER, false);
        }
    }

    public static boolean isValidHex(@Nullable String input) {
        if (input == null) {
            return false;
        }
        String raw = input.trim();
        if (raw.equalsIgnoreCase("rainbow") || raw.equalsIgnoreCase("cycle") || raw.equalsIgnoreCase("prism")) {
            return true;
        }
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        return raw.matches("(?i)[0-9a-f]{3}") || raw.matches("(?i)[0-9a-f]{6}");
    }

    static int hsvToRgb(float hue, float sat, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) h;
        float f = h - sector;
        float p = value * (1.0f - sat);
        float q = value * (1.0f - sat * f);
        float t = value * (1.0f - sat * (1.0f - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }
        int ri = Math.max(0, Math.min(255, Math.round(r * 255)));
        int gi = Math.max(0, Math.min(255, Math.round(g * 255)));
        int bi = Math.max(0, Math.min(255, Math.round(b * 255)));
        return (ri << 16) | (gi << 8) | bi;
    }
}
