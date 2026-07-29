package com.rihanx.listeners;

import com.rihanx.managers.ConfigManager;
import com.rihanx.managers.MessageManager;
import com.rihanx.teleport.TeleportManager;
import com.rihanx.utils.TeleportUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Cancels pending teleports when the player moves.
 */
public final class TeleportListener implements Listener {

    private final @NotNull TeleportManager teleportManager;
    private final @NotNull MessageManager messages;
    private final boolean cancelOnMove;

    public TeleportListener(
            @NotNull TeleportManager teleportManager,
            @NotNull MessageManager messages,
            @NotNull ConfigManager configManager
    ) {
        this.teleportManager = teleportManager;
        this.messages = messages;
        this.cancelOnMove = configManager.cancelTeleportOnMove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (!cancelOnMove) {
            return;
        }
        Player player = event.getPlayer();
        var pending = teleportManager.getPending(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (TeleportUtil.hasMovedBeyond(pending, player, 0.5)) {
            teleportManager.cancel(player.getUniqueId(), true);
            messages.send(player, "teleport-cancelled-move");
        }
    }
}
