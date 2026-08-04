package com.rihanx.utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Player state helpers for heal, feed, fly, repair, and lookups.
 */
public final class PlayerUtil {

    private PlayerUtil() {
    }

    public static @Nullable Player findPlayer(@NotNull String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(trimmed);
        if (exact != null) {
            return exact;
        }
        // Partial / case-insensitive match — only if exactly one player matches
        String needle = trimmed.toLowerCase(Locale.ROOT);
        Player match = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            String onlineName = online.getName().toLowerCase(Locale.ROOT);
            if (onlineName.equals(needle) || onlineName.startsWith(needle)) {
                if (match != null) {
                    return null; // ambiguous
                }
                match = online;
            }
        }
        return match;
    }

    public static @Nullable Player findPlayer(@NotNull UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    public static @NotNull List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static @NotNull List<String> onlineNames(@Nullable String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (normalized.isEmpty() || player.getName().toLowerCase(Locale.ROOT).startsWith(normalized)) {
                names.add(player.getName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static void feed(@NotNull Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
    }

    public static void heal(@NotNull Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : 20.0;
        player.setHealth(max);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        feed(player);
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            PotionEffectType type = effect.getType();
            if (isNegative(type)) {
                player.removePotionEffect(type);
            }
        }
    }

    public static boolean isNegative(@NotNull PotionEffectType type) {
        return type.equals(PotionEffectType.POISON)
                || type.equals(PotionEffectType.WITHER)
                || type.equals(PotionEffectType.HUNGER)
                || type.equals(PotionEffectType.SLOWNESS)
                || type.equals(PotionEffectType.MINING_FATIGUE)
                || type.equals(PotionEffectType.NAUSEA)
                || type.equals(PotionEffectType.BLINDNESS)
                || type.equals(PotionEffectType.WEAKNESS)
                || type.equals(PotionEffectType.LEVITATION)
                || type.equals(PotionEffectType.UNLUCK)
                || type.equals(PotionEffectType.DARKNESS)
                || type.equals(PotionEffectType.INFESTED)
                || type.equals(PotionEffectType.OOZING)
                || type.equals(PotionEffectType.WEAVING)
                || type.equals(PotionEffectType.WIND_CHARGED);
    }

    public static boolean toggleFlight(@NotNull Player player) {
        boolean enable = !player.getAllowFlight();
        player.setAllowFlight(enable);
        if (!enable) {
            player.setFlying(false);
        }
        return enable;
    }

    public static void setFlight(@NotNull Player player, boolean enabled) {
        player.setAllowFlight(enabled);
        if (!enabled) {
            player.setFlying(false);
        }
    }

    public static boolean repairHand(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return repairItem(item);
    }

    public static int repairAll(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        int repaired = 0;
        for (ItemStack item : inventory.getContents()) {
            if (repairItem(item)) {
                repaired++;
            }
        }
        for (ItemStack item : inventory.getArmorContents()) {
            if (repairItem(item)) {
                repaired++;
            }
        }
        if (repairItem(inventory.getItemInOffHand())) {
            repaired++;
        }
        return repaired;
    }

    public static boolean repairItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }
        if (damageable.getDamage() <= 0) {
            return false;
        }
        damageable.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }

    public static void clearInventory(@NotNull Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
    }

    public static @Nullable GameMode parseGameMode(@NotNull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "s", "survival", "0" -> GameMode.SURVIVAL;
            case "c", "creative", "1" -> GameMode.CREATIVE;
            case "a", "adventure", "2" -> GameMode.ADVENTURE;
            case "sp", "spectator", "3" -> GameMode.SPECTATOR;
            default -> {
                try {
                    yield GameMode.valueOf(normalized.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }

    public static @NotNull String formatGameMode(@NotNull GameMode mode) {
        return mode.name().charAt(0) + mode.name().substring(1).toLowerCase(Locale.ROOT);
    }

    public static double getMaxHealth(@NotNull Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 20.0;
    }
}
