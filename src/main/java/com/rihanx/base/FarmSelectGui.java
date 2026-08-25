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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI picker for automatic farms (54 slots).
 */
public final class FarmSelectGui implements InventoryHolder, Listener {

    private static final Map<String, MenuEntry> ENTRIES = new LinkedHashMap<>();

    static {
        ENTRIES.put("wheat", new MenuEntry(Material.WHEAT, 10, "<gold><bold>Wheat</bold></gold>",
                List.of("<gray>Water · hoppers · composters</gray>")));
        ENTRIES.put("potato", new MenuEntry(Material.POTATO, 11, "<yellow><bold>Potato</bold></yellow>",
                List.of("<gray>Water · hoppers · composters</gray>")));
        ENTRIES.put("cane", new MenuEntry(Material.SUGAR_CANE, 12, "<green><bold>Sugar Cane</bold></green>",
                List.of("<gray>Observers · pistons · hoppers</gray>")));
        ENTRIES.put("bamboo", new MenuEntry(Material.BAMBOO, 13, "<dark_green><bold>Bamboo</bold></dark_green>",
                List.of("<gray>Observers · pistons · hoppers</gray>")));
        ENTRIES.put("melon", new MenuEntry(Material.MELON, 14, "<green><bold>Melon</bold></green>",
                List.of("<gray>Stems · hoppers</gray>")));
        ENTRIES.put("cocoa", new MenuEntry(Material.COCOA_BEANS, 15, "<gold><bold>Cocoa</bold></gold>",
                List.of("<gray>Jungle logs · hoppers</gray>")));
        ENTRIES.put("kelp", new MenuEntry(Material.KELP, 16, "<aqua><bold>Kelp</bold></aqua>",
                List.of("<gray>Glass aquarium holds water</gray>",
                        "<gray>Pistons break tips · items float → hoppers</gray>")));
        ENTRIES.put("mushroom", new MenuEntry(Material.RED_MUSHROOM, 19, "<red><bold>Mushroom</bold></red>",
                List.of("<gray>Mycelium hut · hoppers</gray>")));
        ENTRIES.put("nether", new MenuEntry(Material.NETHER_WART, 20, "<dark_red><bold>Nether Wart</bold></dark_red>",
                List.of("<gray>Soul sand · hoppers</gray>")));
        ENTRIES.put("animal", new MenuEntry(Material.HAY_BLOCK, 21, "<gold><bold>Animal Pens</bold></gold>",
                List.of("<gray>4 pens · chests · hoppers</gray>")));
        ENTRIES.put("cactus", new MenuEntry(Material.CACTUS, 22, "<green><bold>Cactus</bold></green>",
                List.of("<gray>Break fences · hoppers</gray>")));
        ENTRIES.put("iron", new MenuEntry(Material.IRON_INGOT, 23, "<white><bold>Iron Farm</bold></white>",
                List.of("<gray>Open deck · lava blade · magma alcove</gray>",
                        "<green>Advanced: villagers + minecart zombie</green>")));
        ENTRIES.put("xp", new MenuEntry(Material.EXPERIENCE_BOTTLE, 24, "<light_purple><bold>XP Dark Room</bold></light_purple>",
                List.of("<gray>Sealed blackstone pads · 24+ spawn range</gray>",
                        "<gray>22-block drop · punch 1HP at the window</gray>")));
        ENTRIES.put("xp-zombie", new MenuEntry(Material.ROTTEN_FLESH, 25, "<green><bold>XP Zombie</bold></green>",
                List.of("<gray>4 zombie spawners · stay at the window</gray>",
                        "<yellow>Best for testing — spawners always work</yellow>")));
        ENTRIES.put("xp-skeleton", new MenuEntry(Material.BONE, 28, "<white><bold>XP Skeleton</bold></white>",
                List.of("<gray>4 skeleton spawners · stay at the window</gray>")));
        ENTRIES.put("xp-spider", new MenuEntry(Material.SPIDER_EYE, 29, "<dark_red><bold>XP Cave Spider</bold></dark_red>",
                List.of("<gray>4 cave-spider spawners · milk for poison</gray>")));
        ENTRIES.put("xp-enderman", new MenuEntry(Material.ENDER_PEARL, 30, "<dark_purple><bold>XP Enderman</bold></dark_purple>",
                List.of("<gray>3-high sealed pads · no water (they teleport)</gray>",
                        "<gray>Walk-off drop · 24+ spawn range</gray>")));
        ENTRIES.put("chicken", new MenuEntry(Material.COOKED_CHICKEN, 31, "<gold><bold>Chicken Cooker</bold></gold>",
                List.of("<gray>Eggs fall through trapdoors</gray>",
                        "<yellow>Lava auto-cooks chicken → chests</yellow>")));
        ENTRIES.put("cow", new MenuEntry(Material.COOKED_BEEF, 32, "<red><bold>Cow Cooker</bold></red>",
                List.of("<gray>Wheat dispensers breed at dawn</gray>",
                        "<yellow>Lava auto-cooks beef + leather</yellow>")));
        ENTRIES.put("pig", new MenuEntry(Material.COOKED_PORKCHOP, 33, "<light_purple><bold>Pig Cooker</bold></light_purple>",
                List.of("<gray>Carrot dispensers breed at dawn</gray>",
                        "<yellow>Lava auto-cooks porkchops</yellow>")));
        ENTRIES.put("cook", new MenuEntry(Material.SMOKER, 34, "<yellow><bold>Auto Smoker</bold></yellow>",
                List.of("<gray>Raw food in the top chests</gray>",
                        "<gray>Coal in the back chests → loot south</gray>")));
        ENTRIES.put("slime", new MenuEntry(Material.SLIME_BALL, 37, "<green><bold>Slime Farm</bold></green>",
                List.of("<gray>4 slime spawners · stand at the window</gray>",
                        "<yellow>Slimeballs → loot chests</yellow>")));
        ENTRIES.put("redstone", new MenuEntry(Material.REDSTONE, 38, "<red><bold>Redstone (Witch)</bold></red>",
                List.of("<gray>4 witch spawners · stand at the window</gray>",
                        "<gray>Redstone, glowstone, sugar, sticks</gray>")));
        ENTRIES.put("diamond", new MenuEntry(Material.DIAMOND, 39, "<aqua><bold>Diamond Hall</bold></aqua>",
                List.of("<gray>Not an ore gen — 4 master toolsmiths + 2 farmers</gray>",
                        "<yellow>Trade emeralds for diamond gear</yellow>")));
    }

    private final @NotNull RihanX plugin;
    private final @NotNull Inventory inventory;
    private boolean handled;

    public FarmSelectGui(@NotNull RihanX plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                MessageUtil.parse("<gradient:#86efac:#22c55e><bold>✦ RihanX Farms ✦</bold></gradient>")
        );
        draw();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void draw() {
        ItemStack border = named(Material.LIME_STAINED_GLASS_PANE, "<green> </green>", List.of());
        for (int i = 0; i < 54; i++) {
            boolean edge = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;
            if (edge) {
                inventory.setItem(i, border);
            }
        }
        inventory.setItem(4, named(
                Material.DIAMOND_HOE,
                "<gradient:#bbf7d0:#16a34a><bold>Choose a farm</bold></gradient>",
                List.of(
                        "<gray>Stand at the <yellow>front / collection</yellow> side.</gray>",
                        "<gray>Lanterns hang from <aqua>chains</aqua> under roofs/posts.</gray>",
                        "<aqua>All farms include hoppers & storage.</aqua>"
                )
        ));
        for (Map.Entry<String, MenuEntry> entry : ENTRIES.entrySet()) {
            MenuEntry menu = entry.getValue();
            BaseTemplates.BaseBlueprint bp = plugin.getFarmService().get(entry.getKey());
            List<String> lore = new ArrayList<>(menu.lore());
            lore.add("");
            lore.add("<yellow>Click to build</yellow>");
            if (bp != null) {
                lore.add("<dark_gray>Blocks: <white>" + bp.blocks().size() + "</white></dark_gray>");
            }
            inventory.setItem(menu.slot(), named(menu.icon(), menu.title(), lore));
        }
        inventory.setItem(49, named(Material.BARRIER, "<red><bold>Close</bold></red>", List.of()));
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
        if (!(event.getInventory().getHolder() instanceof FarmSelectGui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || handled) {
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
            if (!player.hasPermission(PermissionNodes.FARM_BUILD) && !player.isOp()) {
                plugin.getMessageManager().send(player, "no-permission");
                return;
            }
            plugin.getFarmService().paste(player, entry.getKey());
            return;
        }
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
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
