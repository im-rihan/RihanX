package com.rihanx.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks frozen players and applies/removes freeze state.
 */
public final class FreezeManager {

    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();

    public boolean freeze(@NotNull Player player) {
        if (!frozen.add(player.getUniqueId())) {
            return false;
        }
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false, false));
        return true;
    }

    public boolean unfreeze(@NotNull Player player) {
        if (!frozen.remove(player.getUniqueId())) {
            return false;
        }
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        return true;
    }

    public boolean isFrozen(@NotNull UUID playerId) {
        return frozen.contains(playerId);
    }

    public boolean isFrozen(@NotNull Player player) {
        return isFrozen(player.getUniqueId());
    }

    public @NotNull Set<UUID> getFrozen() {
        return Collections.unmodifiableSet(frozen);
    }

    public void unfreezeAll() {
        for (UUID uuid : Set.copyOf(frozen)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                unfreeze(player);
            } else {
                frozen.remove(uuid);
            }
        }
    }

    public void handleQuit(@NotNull Player player, boolean unfreezeOnQuit) {
        if (unfreezeOnQuit) {
            unfreeze(player);
        }
    }
}
