package com.rihanx.gui;

import com.rihanx.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Minimal GUI helper for info displays.
 */
public final class GuiManager {

    private final @NotNull Plugin plugin;

    public GuiManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void openInfo(@NotNull Player player, @NotNull String title, @NotNull List<String> lines) {
        Inventory inventory = Bukkit.createInventory(null, 27, MessageUtil.parse(title));
        Material[] panes = {
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE
        };
        for (int i = 0; i < inventory.getSize(); i++) {
            Material pane = panes[i % panes.length];
            ItemStack item = new ItemStack(pane);
            ItemMeta meta = item.getItemMeta();
            if (meta != null && i - 10 < lines.size() && i >= 10 && i <= 10 + lines.size()) {
                String line = lines.get(i - 10);
                meta.displayName(MessageUtil.parse(line));
            } else if (meta != null) {
                meta.displayName(Component.empty());
            }
            if (meta != null) {
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    public @NotNull SimpleConfirmGui confirm(
            @NotNull Component title,
            @NotNull java.util.function.Consumer<Player> onConfirm,
            @NotNull java.util.function.Consumer<Player> onCancel
    ) {
        return new SimpleConfirmGui(plugin, title, onConfirm, onCancel);
    }
}
