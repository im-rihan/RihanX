package com.rihanx.base;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.utils.MessageUtil;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Polished 54-slot GUI for choosing a base template.
 */
public final class BaseSelectGui implements InventoryHolder, Listener {

    private static final Map<String, MenuEntry> ENTRIES = new LinkedHashMap<>();

    static {
        ENTRIES.put("hut", new MenuEntry(
                Material.OAK_DOOR, 10,
                "<gradient:#c4a574:#8b6914><bold>Hut</bold></gradient>",
                List.of(
                        "<gray>Beginner starter cabin</gray>",
                        "<dark_gray>Oak · door · gabled roof</dark_gray>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
        ENTRIES.put("cottage", new MenuEntry(
                Material.SPRUCE_DOOR, 11,
                "<gradient:#5c4033:#a67c52><bold>Cottage</bold></gradient>",
                List.of(
                        "<gray>Cozy spruce home</gray>",
                        "<dark_gray>Chimney · porch · kitchen</dark_gray>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
        ENTRIES.put("village", new MenuEntry(
                Material.COBBLESTONE, 12,
                "<gradient:#7a7a7a:#c0c0c0><bold>Village</bold></gradient>",
                List.of(
                        "<gray>Classic plains house</gray>",
                        "<dark_gray>Oak + cobble villager style</dark_gray>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
        ENTRIES.put("bungalow", new MenuEntry(
                Material.SMOOTH_QUARTZ, 14,
                "<gradient:#f5f5f5:#b0e0e6><bold>Bungalow</bold></gradient>",
                List.of(
                        "<aqua>Luxury 5-bedroom</aqua>",
                        "<gray>Pool · lounge · kitchen · baths</gray>",
                        "<dark_gray>Calcite / quartz real-world design</dark_gray>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
        ENTRIES.put("villa", new MenuEntry(
                Material.QUARTZ_PILLAR, 15,
                "<gradient:#e8d5b7:#8b7355><bold>Villa</bold></gradient>",
                List.of(
                        "<gold>2-storey luxury villa</gold>",
                        "<gray>Bubble lift · 5 beds · pool · balcony</gray>",
                        "<dark_gray>Quartz + deepslate</dark_gray>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
        ENTRIES.put("modern", new MenuEntry(
                Material.WHITE_CONCRETE, 16,
                "<gradient:#ffffff:#333333><bold>Modern</bold></gradient>",
                List.of(
                        "<white>Glass & concrete cube</white>",
                        "<gray>Lift · 5 beds · rooftop pool</gray>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
        ENTRIES.put("mansion", new MenuEntry(
                Material.GOLD_BLOCK, 22,
                "<gradient:#ffd700:#b8860b><bold>Mansion</bold></gradient>",
                List.of(
                        "<gold><bold>Mega mansion</bold></gold>",
                        "<gray>3 floors · dual lifts · 6 suites</gray>",
                        "<aqua>Indoor + outdoor pools</aqua>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
        ENTRIES.put("resort", new MenuEntry(
                Material.HEART_OF_THE_SEA, 24,
                "<gradient:#00ced1:#20b2aa><bold>Resort</bold></gradient>",
                List.of(
                        "<aqua>Pool club resort</aqua>",
                        "<gray>Mega pool · 5 cabanas · bar</gray>",
                        "",
                        "<yellow>Click to build</yellow>"
                )
        ));
    }

    private final @NotNull RihanX plugin;
    private final @NotNull Inventory inventory;
    private boolean handled;

    public BaseSelectGui(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                MessageUtil.parse("<gradient:#6ee7ff:#a78bfa><bold>✦ RihanX Bases ✦</bold></gradient>")
        );
        draw();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void draw() {
        ItemStack border = named(Material.CYAN_STAINED_GLASS_PANE, "<aqua> </aqua>", List.of());
        ItemStack accent = named(Material.PURPLE_STAINED_GLASS_PANE, "<light_purple> </light_purple>", List.of());
        for (int i = 0; i < 54; i++) {
            boolean edge = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;
            inventory.setItem(i, edge ? border : accent);
        }

        inventory.setItem(4, named(
                Material.NETHER_STAR,
                "<gradient:#ffd700:#fff8dc><bold>Choose your base</bold></gradient>",
                List.of(
                        "<gray>Stand where the <yellow>front door</yellow> should be.</gray>",
                        "<gray>Face into the house direction, then click a design.</gray>",
                        "<gray>You stay at the entrance while it builds.</gray>",
                        "",
                        "<aqua>Tip:</aqua> <gray>Need a flat open area (large for villa/mansion).</gray>"
                )
        ));

        for (Map.Entry<String, MenuEntry> entry : ENTRIES.entrySet()) {
            MenuEntry menu = entry.getValue();
            BaseTemplates.BaseBlueprint blueprint = plugin.getBaseService().get(entry.getKey());
            List<String> lore = new ArrayList<>(menu.lore());
            if (blueprint != null) {
                lore.add("");
                lore.add("<dark_gray>Blocks: <white>" + blueprint.blocks().size() + "</white></dark_gray>");
            }
            ItemStack icon = named(menu.icon(), menu.title(), lore);
            inventory.setItem(menu.slot(), icon);
        }

        inventory.setItem(49, named(
                Material.BARRIER,
                "<red><bold>Close</bold></red>",
                List.of("<gray>Close this menu</gray>")
        ));
    }

    public void open(@NotNull Player player) {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BaseSelectGui)) {
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
        if (slot == 49) {
            handled = true;
            player.closeInventory();
            return;
        }
        for (Map.Entry<String, MenuEntry> entry : ENTRIES.entrySet()) {
            if (entry.getValue().slot() != slot) {
                continue;
            }
            handled = true;
            player.closeInventory();
            String id = entry.getKey();
            if (!player.hasPermission(PermissionNodes.BASE_BUILD) && !player.isOp()) {
                plugin.getMessageManager().send(player, "no-permission");
                return;
            }
            // Confirm for large builds
            BaseTemplates.BaseBlueprint blueprint = plugin.getBaseService().get(id);
            int blocks = blueprint == null ? 0 : blueprint.blocks().size();
            if (blocks >= 2500) {
                openConfirm(player, id, blocks);
            } else {
                plugin.getBaseService().paste(player, id);
            }
            return;
        }
    }

    private void openConfirm(@NotNull Player player, @NotNull String id, int blocks) {
        plugin.getGuiManager().confirm(
                MessageUtil.parse("<gold>Build <yellow>" + id + "</yellow>?</gold>"),
                p -> plugin.getBaseService().paste(p, id),
                p -> plugin.getMessageManager().send(p, "base-cancelled")
        ).open(player);
        // Enrich confirm inventory lore via a small delay message
        plugin.getMessageManager().send(player, "base-confirm-hint",
                com.rihanx.managers.MessageManager.placeholders(
                        "name", id,
                        "blocks", blocks
                ));
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        HandlerList.unregisterAll(this);
    }

    private static @NotNull ItemStack named(
            @NotNull Material material,
            @NotNull String name,
            @NotNull List<String> loreLines
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private record MenuEntry(
            @NotNull Material icon,
            int slot,
            @NotNull String title,
            @NotNull List<String> lore
    ) {
    }
}
