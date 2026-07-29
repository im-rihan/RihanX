package com.rihanx.protection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Protection flags for worlds and regions.
 */
public enum ProtectionFlag {
    TNT("tnt"),
    CREEPER_EXPLOSION("creeper-explosion"),
    OTHER_EXPLOSION("other-explosion"),
    FIRE_SPREAD("fire-spread"),
    FIRE_DESTROY("fire-destroy"),
    LAVA_FIRE("lava-fire"),
    BUILD("build"),
    BREAK("break"),
    PLACE("place"),
    PVP("pvp"),
    MOB_GRIEF("mob-grief"),
    ENDERMAN_GRIEF("enderman-grief"),
    LEAF_DECAY("leaf-decay"),
    ICE_MELT("ice-melt"),
    CROP_TRAMPLE("crop-trample"),
    ENTRY("entry");

    private final @NotNull String key;

    ProtectionFlag(@NotNull String key) {
        this.key = key;
    }

    public @NotNull String key() {
        return key;
    }

    public static @Nullable ProtectionFlag fromKey(@NotNull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (ProtectionFlag flag : values()) {
            if (flag.key.equals(normalized) || flag.name().equalsIgnoreCase(input.replace('-', '_'))) {
                return flag;
            }
        }
        return null;
    }

    public static @NotNull String[] keys() {
        ProtectionFlag[] values = values();
        String[] keys = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            keys[i] = values[i].key;
        }
        return keys;
    }
}
