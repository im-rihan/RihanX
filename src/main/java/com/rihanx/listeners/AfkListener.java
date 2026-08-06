package com.rihanx.listeners;

import com.rihanx.managers.AfkManager;
import com.rihanx.managers.MessageManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Clears AFK status when a player moves, interacts, or chats.
 */
public final class AfkListener implements Listener {

    private final @NotNull AfkManager afkManager;
    private final @NotNull MessageManager messages;

    public AfkListener(@NotNull AfkManager afkManager, @NotNull MessageManager messages) {
        this.afkManager = afkManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        // Ignore pure head-rotation to avoid clearing AFK from idle look drift
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        clear(event.getPlayer());
    }

    private void clear(@NotNull Player player) {
        if (!afkManager.clearOnActivity(player)) {
            return;
        }
        messages.broadcast("afk-off", MessageManager.placeholders("player", player.getName()));
    }
}
