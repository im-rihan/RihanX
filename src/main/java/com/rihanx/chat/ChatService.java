package com.rihanx.chat;

import com.rihanx.managers.MessageManager;
import com.rihanx.managers.VanishManager;
import com.rihanx.utils.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Private messaging ({@code /msg}, {@code /r}) with vanish-aware visibility and reply tracking.
 */
public final class ChatService {

    private final @NotNull MessageManager messages;
    private final @NotNull VanishManager vanishManager;

    /** player → last conversation partner (updated for both sides on every message, used by /r). */
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();

    public ChatService(@NotNull MessageManager messages, @NotNull VanishManager vanishManager) {
        this.messages = messages;
        this.vanishManager = vanishManager;
    }

    public void msg(@NotNull Player sender, @NotNull Player target, @NotNull String message) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            messages.send(sender, "msg-self");
            return;
        }
        if (!canMessage(sender, target)) {
            messages.send(sender, "player-not-found", MessageManager.placeholders("player", target.getName()));
            return;
        }

        lastPartner.put(sender.getUniqueId(), target.getUniqueId());
        lastPartner.put(target.getUniqueId(), sender.getUniqueId());

        messages.send(sender, "msg-sent", MessageManager.placeholders(
                "player", target.getName(),
                "message", message
        ));
        messages.send(target, "msg-received", MessageManager.placeholders(
                "player", sender.getName(),
                "message", message
        ));
    }

    public void reply(@NotNull Player sender, @NotNull String message) {
        UUID partnerId = lastPartner.get(sender.getUniqueId());
        Player target = partnerId == null ? null : Bukkit.getPlayer(partnerId);
        if (target == null || !target.isOnline()) {
            messages.send(sender, "msg-no-reply-target");
            return;
        }
        msg(sender, target, message);
    }

    /** Vanished targets can't be messaged unless the sender can see vanished players. */
    private boolean canMessage(@NotNull Player sender, @NotNull Player target) {
        if (!target.isOnline()) {
            return false;
        }
        return !vanishManager.isVanished(target) || PermissionUtil.canSeeVanished(sender);
    }

    public @Nullable UUID getLastPartner(@NotNull UUID playerId) {
        return lastPartner.get(playerId);
    }

    public void clear(@NotNull UUID playerId) {
        lastPartner.remove(playerId);
    }
}
