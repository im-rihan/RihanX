package com.rihanx.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Simple confirm/info inventory GUI.
 */
public final class SimpleConfirmGui implements InventoryHolder, Listener {

    private final @NotNull Inventory inventory;
    private final @NotNull Consumer<Player> onConfirm;
    private final @NotNull Consumer<Player> onCancel;
    private boolean handled;

    public SimpleConfirmGui(
            @NotNull Plugin plugin,
            @NotNull Component title,
            @NotNull Consumer<Player> onConfirm,
            @NotNull Consumer<Player> onCancel
    ) {
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.inventory = Bukkit.createInventory(this, 27, title);
        ItemStack confirm = pane(Material.LIME_STAINED_GLASS_PANE, "<green>Confirm</green>");
        ItemStack cancel = pane(Material.RED_STAINED_GLASS_PANE, "<red>Cancel</red>");
        inventory.setItem(11, confirm);
        inventory.setItem(15, cancel);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private @NotNull ItemStack pane(@NotNull Material material, @NotNull String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(com.rihanx.utils.MessageUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void open(@NotNull Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SimpleConfirmGui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (handled) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 11) {
            handled = true;
            player.closeInventory();
            onConfirm.accept(player);
        } else if (slot == 15) {
            handled = true;
            player.closeInventory();
            onCancel.accept(player);
        }
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        HandlerList.unregisterAll(this);
        if (!handled && event.getPlayer() instanceof Player player) {
            onCancel.accept(player);
        }
    }
}
