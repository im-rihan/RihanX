package com.rihanx.listeners;

import com.rihanx.api.PermissionNodes;
import com.rihanx.edit.EditService;
import com.rihanx.protection.ProtectionService;
import com.rihanx.utils.PermissionUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

/**
 * Handles protect/edit wand clicks.
 */
public final class WandListener implements Listener {

    private final @NotNull ProtectionService protection;
    private final @NotNull EditService edit;

    public WandListener(@NotNull ProtectionService protection, @NotNull EditService edit) {
        this.protection = protection;
        this.edit = edit;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (protection.isProtectWand(event.getItem())
                && PermissionUtil.hasOpOnly(player, PermissionNodes.PROTECT)) {
            event.setCancelled(true);
            protection.setPos(player, action == Action.LEFT_CLICK_BLOCK ? 1 : 2, block.getLocation());
            return;
        }

        if (edit.isEditWand(event.getItem())
                && PermissionUtil.hasOpOnly(player, PermissionNodes.EDIT)) {
            event.setCancelled(true);
            edit.setPos(player, action == Action.LEFT_CLICK_BLOCK ? 1 : 2, block.getLocation());
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        protection.getSelections().clearAll(player);
        edit.clearPlayer(player);
    }
}
