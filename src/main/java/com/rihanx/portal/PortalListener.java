package com.rihanx.portal;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers linked portals when stepping on the pad plate or pressing the pad button.
 */
public final class PortalListener implements Listener {

    private final @NotNull PortalService portals;

    public PortalListener(@NotNull PortalService portals) {
        this.portals = portals;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Block block = event.getTo().getBlock();
        if (!isPlate(block.getType())) {
            Block below = block.getRelative(0, -1, 0);
            if (!isPlate(below.getType())) {
                return;
            }
            block = below;
        }
        portals.tryUsePad(event.getPlayer(), block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Material type = block.getType();
        if (!isPlate(type) && !type.name().endsWith("_BUTTON")) {
            return;
        }
        Player player = event.getPlayer();
        portals.tryUsePad(player, block.getLocation());
    }

    private static boolean isPlate(@NotNull Material material) {
        return material.name().endsWith("_PRESSURE_PLATE");
    }
}
