package com.rihanx.player;

import com.rihanx.managers.FlyManager;
import com.rihanx.managers.FreezeManager;
import com.rihanx.managers.GodManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.managers.VanishManager;
import com.rihanx.utils.PlayerUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Player utility operations (always scoped to a single target player).
 */
public final class PlayerService {

    private final @NotNull MessageManager messages;
    private final @NotNull FreezeManager freezeManager;
    private final @NotNull VanishManager vanishManager;
    private final @NotNull GodManager godManager;
    private final @NotNull FlyManager flyManager;

    public PlayerService(
            @NotNull MessageManager messages,
            @NotNull FreezeManager freezeManager,
            @NotNull VanishManager vanishManager,
            @NotNull GodManager godManager,
            @NotNull FlyManager flyManager
    ) {
        this.messages = messages;
        this.freezeManager = freezeManager;
        this.vanishManager = vanishManager;
        this.godManager = godManager;
        this.flyManager = flyManager;
    }

    public void sendInfo(@NotNull CommandSender sender, @NotNull Player target) {
        messages.send(sender, "player-info-header", MessageManager.placeholders("player", target.getName()));
        Location loc = target.getLocation();
        sendLine(sender, "UUID", target.getUniqueId().toString());
        sendLine(sender, "World", loc.getWorld() != null ? loc.getWorld().getName() : "unknown");
        sendLine(sender, "Position", loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        sendLine(sender, "Gamemode", PlayerUtil.formatGameMode(target.getGameMode()));
        sendLine(sender, "Health", String.format("%.1f / %.1f", target.getHealth(), PlayerUtil.getMaxHealth(target)));
        sendLine(sender, "Food", target.getFoodLevel() + " / 20");
        sendLine(sender, "Flying", (target.isFlying() || flyManager.isFlying(target)) ? "Yes" : "No");
        sendLine(sender, "God", godManager.isGod(target) ? "Yes" : "No");
        sendLine(sender, "Frozen", freezeManager.isFrozen(target) ? "Yes" : "No");
        sendLine(sender, "Vanished", vanishManager.isVanished(target) ? "Yes" : "No");
        sendLine(sender, "Ping", target.getPing() + "ms");
    }

    public void heal(@NotNull CommandSender sender, @NotNull Player target) {
        PlayerUtil.heal(target);
        messages.send(target, "player-healed");
        notifyOther(sender, target, "player-healed-other");
    }

    public void feed(@NotNull CommandSender sender, @NotNull Player target) {
        PlayerUtil.feed(target);
        messages.send(target, "player-fed");
        notifyOther(sender, target, "player-fed-other");
    }

    public void toggleFly(@NotNull CommandSender sender, @NotNull Player target) {
        boolean enabled = flyManager.toggle(target);
        messages.send(target, enabled ? "player-fly-on" : "player-fly-off");
        if (!(sender instanceof Player player) || !player.getUniqueId().equals(target.getUniqueId())) {
            messages.send(sender, enabled ? "player-fly-other-on" : "player-fly-other-off",
                    MessageManager.placeholders("player", target.getName()));
        }
    }

    public void setSpeed(@NotNull Player target, float speed) {
        target.setWalkSpeed(speed);
        target.setFlySpeed(Math.min(1.0f, speed));
        messages.send(target, "player-speed", MessageManager.placeholders("speed", speed));
    }

    public void freeze(@NotNull CommandSender sender, @NotNull Player target) {
        freezeManager.freeze(target);
        messages.send(sender, "player-frozen", MessageManager.placeholders("player", target.getName()));
    }

    public void unfreeze(@NotNull CommandSender sender, @NotNull Player target) {
        freezeManager.unfreeze(target);
        messages.send(sender, "player-unfrozen", MessageManager.placeholders("player", target.getName()));
    }

    public void toggleVanish(@NotNull Player target) {
        if (vanishManager.toggle(target)) {
            messages.send(target, "player-vanish-on");
        } else {
            messages.send(target, "player-vanish-off");
        }
    }

    public void toggleGod(@NotNull CommandSender sender, @NotNull Player target) {
        boolean enabled = godManager.toggle(target);
        messages.send(target, enabled ? "player-god-on" : "player-god-off");
        if (!(sender instanceof Player player) || !player.getUniqueId().equals(target.getUniqueId())) {
            messages.send(sender, enabled ? "player-god-other-on" : "player-god-other-off",
                    MessageManager.placeholders("player", target.getName()));
        }
    }

    public void setGameMode(@NotNull CommandSender sender, @NotNull Player target, @NotNull GameMode mode) {
        target.setGameMode(mode);
        String modeName = PlayerUtil.formatGameMode(mode);
        messages.send(target, "player-gamemode-self", MessageManager.placeholders("mode", modeName));
        if (!(sender instanceof Player player) || !player.getUniqueId().equals(target.getUniqueId())) {
            messages.send(sender, "player-gamemode-other", MessageManager.placeholders(
                    "player", target.getName(),
                    "mode", modeName
            ));
        }
    }

    public void clearEffects(@NotNull CommandSender sender, @NotNull Player target) {
        for (PotionEffect effect : new ArrayList<>(target.getActivePotionEffects())) {
            target.removePotionEffect(effect.getType());
        }
        target.setFireTicks(0);
        messages.send(target, "player-effects-cleared");
        notifyOther(sender, target, "player-effects-cleared-other");
    }

    public void sendPing(@NotNull CommandSender sender, @NotNull Player target) {
        messages.send(sender, "player-ping", MessageManager.placeholders(
                "ping", target.getPing(),
                "player", target.getName()
        ));
    }

    public @NotNull GodManager getGodManager() {
        return godManager;
    }

    public @NotNull FlyManager getFlyManager() {
        return flyManager;
    }

    private void notifyOther(@NotNull CommandSender sender, @NotNull Player target, @NotNull String messageKey) {
        if (!(sender instanceof Player player) || !player.getUniqueId().equals(target.getUniqueId())) {
            messages.send(sender, messageKey, MessageManager.placeholders("player", target.getName()));
        }
    }

    private void sendLine(@NotNull CommandSender sender, @NotNull String key, @NotNull String value) {
        messages.send(sender, "player-info-line", MessageManager.placeholders("key", key, "value", value));
    }
}
