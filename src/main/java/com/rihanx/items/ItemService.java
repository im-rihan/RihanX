package com.rihanx.items;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.managers.MessageManager;
import com.rihanx.utils.MaterialUtil;
import com.rihanx.utils.MessageUtil;
import com.rihanx.utils.PermissionUtil;
import com.rihanx.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Item inspection, modification, and giving.
 */
public final class ItemService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;

    public ItemService(@NotNull RihanX plugin, @NotNull MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void sendInfo(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            messages.send(player, "item-empty");
            return;
        }
        messages.send(player, "item-info-header");
        sendLine(player, "Material", item.getType().name());
        sendLine(player, "Amount", String.valueOf(item.getAmount()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            sendLine(player, "Name", MessageUtil.plain(meta.displayName()));
        }
        if (meta instanceof Damageable damageable) {
            sendLine(player, "Damage", damageable.getDamage() + " / " + item.getType().getMaxDurability());
        }
    }

    public void give(
            @NotNull CommandSender sender,
            @NotNull Player target,
            @NotNull String materialName,
            int amount
    ) {
        Material material = MaterialUtil.match(materialName);
        if (material == null || material.isAir() || !material.isItem()) {
            messages.send(sender, "item-material-invalid", MessageManager.placeholders("input", materialName));
            return;
        }
        int safeAmount = Math.max(1, Math.min(amount, material.getMaxStackSize() * 36));
        int remaining = safeAmount;
        while (remaining > 0) {
            int stack = Math.min(remaining, material.getMaxStackSize());
            ItemStack stackItem = new ItemStack(material, stack);
            var leftover = target.getInventory().addItem(stackItem);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(drop -> target.getWorld().dropItemNaturally(target.getLocation(), drop));
            }
            remaining -= stack;
        }
        messages.send(sender, "item-give-success", MessageManager.placeholders(
                "amount", safeAmount,
                "material", MaterialUtil.key(material),
                "player", target.getName()
        ));
        if (!(sender instanceof Player player) || !player.equals(target)) {
            messages.send(target, "item-give-received", MessageManager.placeholders(
                    "amount", safeAmount,
                    "material", MaterialUtil.key(material),
                    "sender", sender.getName()
            ));
        }
    }

    public void rename(@NotNull Player player, @NotNull String name) {
        ItemStack item = requireHand(player);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.displayName(MessageUtil.parse(name));
        item.setItemMeta(meta);
        messages.send(player, "item-renamed");
    }

    public void setLore(@NotNull Player player, @NotNull String line) {
        ItemStack item = requireHand(player);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }
        lore.add(MessageUtil.parse(line));
        meta.lore(lore);
        item.setItemMeta(meta);
        messages.send(player, "item-lore-set");
    }

    public void enchant(@NotNull Player player, @NotNull String enchantName, int level) {
        ItemStack item = requireHand(player);
        Enchantment enchantment = resolveEnchantment(enchantName);
        if (enchantment == null) {
            messages.send(player, "item-enchant-invalid", MessageManager.placeholders("input", enchantName));
            return;
        }
        int safeLevel = Math.max(1, level);
        int cost = calculateEnchantCost(safeLevel);
        boolean free = isEnchantFree(player);

        if (!free && cost > 0) {
            int levels = player.getLevel();
            if (levels < cost) {
                messages.send(player, "item-enchant-xp-needed", MessageManager.placeholders(
                        "need", cost,
                        "have", levels
                ));
                return;
            }
        }

        item.addUnsafeEnchantment(enchantment, safeLevel);

        if (!free && cost > 0) {
            player.setLevel(Math.max(0, player.getLevel() - cost));
            messages.send(player, "item-enchanted-xp", MessageManager.placeholders(
                    "enchant", enchantKey(enchantment),
                    "level", safeLevel,
                    "cost", cost
            ));
        } else {
            messages.send(player, "item-enchanted", MessageManager.placeholders(
                    "enchant", enchantKey(enchantment),
                    "level", safeLevel
            ));
        }
    }

    public int calculateEnchantCost(int enchantLevel) {
        if (!plugin.getConfig().getBoolean("item.enchant.require-xp", true)) {
            return 0;
        }
        int base = plugin.getConfig().getInt("item.enchant.base-cost", 0);
        int perLevel = plugin.getConfig().getInt("item.enchant.cost-per-enchant-level", 1);
        int maxCost = plugin.getConfig().getInt("item.enchant.max-cost", 30);
        int cost = base + Math.max(1, enchantLevel) * Math.max(0, perLevel);
        return Math.max(0, Math.min(maxCost, cost));
    }

    private boolean isEnchantFree(@NotNull Player player) {
        if (!plugin.getConfig().getBoolean("item.enchant.require-xp", true)) {
            return true;
        }
        if (!plugin.getConfig().getBoolean("item.enchant.allow-free-permission", true)) {
            return false;
        }
        return PermissionUtil.has(player, PermissionNodes.ITEM_ENCHANT_FREE);
    }

    private @NotNull String enchantKey(@NotNull Enchantment enchantment) {
        NamespacedKey key = Registry.ENCHANTMENT.getKey(enchantment);
        return key != null ? key.getKey() : enchantment.getKey().getKey();
    }

    public void repairHand(@NotNull Player player) {
        if (!PlayerUtil.repairHand(player)) {
            messages.send(player, "inventory-repair-none");
            return;
        }
        messages.send(player, "item-repaired");
    }

    private @NotNull ItemStack requireHand(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            throw new IllegalStateException("empty");
        }
        return item;
    }

    private @Nullable Enchantment resolveEnchantment(@NotNull String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        Registry<Enchantment> registry = Registry.ENCHANTMENT;
        Enchantment enchantment = registry.get(NamespacedKey.minecraft(normalized));
        if (enchantment != null) {
            return enchantment;
        }
        for (Enchantment candidate : registry) {
            NamespacedKey key = registry.getKey(candidate);
            if (key != null && key.getKey().equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        return null;
    }

    private void sendLine(@NotNull Player player, @NotNull String key, @NotNull String value) {
        messages.send(player, "item-info-line", MessageManager.placeholders("key", key, "value", value));
    }
}
