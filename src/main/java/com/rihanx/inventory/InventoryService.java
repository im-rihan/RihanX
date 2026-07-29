package com.rihanx.inventory;

import com.rihanx.managers.MessageManager;
import com.rihanx.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Inventory management operations.
 */
public final class InventoryService {

    private final @NotNull MessageManager messages;

    public InventoryService(@NotNull MessageManager messages) {
        this.messages = messages;
    }

    public void openInventory(@NotNull CommandSender sender, @NotNull Player viewer, @NotNull Player target) {
        messages.send(sender, "inventory-see", MessageManager.placeholders("player", target.getName()));
        viewer.openInventory(target.getInventory());
    }

    public void openEnderChest(@NotNull CommandSender sender, @NotNull Player viewer, @NotNull Player target) {
        messages.send(sender, "inventory-ender", MessageManager.placeholders("player", target.getName()));
        viewer.openInventory(target.getEnderChest());
    }

    public void clear(@NotNull Player target) {
        PlayerUtil.clearInventory(target);
        messages.send(target, "inventory-clear");
    }

    public void repairAll(@NotNull Player target) {
        int count = PlayerUtil.repairAll(target);
        if (count <= 0) {
            messages.send(target, "inventory-repair-none");
            return;
        }
        messages.send(target, "inventory-repair", MessageManager.placeholders("count", count));
    }

    public @NotNull Player resolveTarget(@NotNull CommandSender sender, @NotNull String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            throw new IllegalArgumentException("Player not found: " + name);
        }
        return target;
    }
}
