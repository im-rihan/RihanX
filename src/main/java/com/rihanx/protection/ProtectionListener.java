package com.rihanx.protection;

import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces world and region protection flags.
 */
public final class ProtectionListener implements Listener {

    private final @NotNull ProtectionService protection;
    private final @NotNull Map<UUID, Long> entryThrottle = new ConcurrentHashMap<>();

    public ProtectionListener(@NotNull ProtectionService protection) {
        this.protection = protection;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!protection.isAllowed(player, event.getBlock().getLocation(), ProtectionFlag.BREAK)
                || !protection.isAllowed(player, event.getBlock().getLocation(), ProtectionFlag.BUILD)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(player, "protect-denied-break");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!protection.isAllowed(player, event.getBlock().getLocation(), ProtectionFlag.PLACE)
                || !protection.isAllowed(player, event.getBlock().getLocation(), ProtectionFlag.BUILD)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(player, "protect-denied-place");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
        if (!protection.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ProtectionFlag.PLACE)
                || !protection.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ProtectionFlag.BUILD)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(event.getPlayer(), "protect-denied-place");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(@NotNull PlayerBucketFillEvent event) {
        if (!protection.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ProtectionFlag.BREAK)
                || !protection.isAllowed(event.getPlayer(), event.getBlock().getLocation(), ProtectionFlag.BUILD)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(event.getPlayer(), "protect-denied-break");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgniteInteract(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getItem() == null) {
            return;
        }
        Material type = event.getItem().getType();
        if (type != Material.FLINT_AND_STEEL && type != Material.FIRE_CHARGE) {
            return;
        }
        Player player = event.getPlayer();
        if (!protection.isAllowed(player, event.getClickedBlock().getLocation(), ProtectionFlag.FIRE_DESTROY)
                || !protection.isAllowed(player, event.getClickedBlock().getLocation(), ProtectionFlag.BUILD)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(player, "protect-denied-place");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        ProtectionFlag flag = explosionFlag(event.getEntity());
        Iterator<org.bukkit.block.Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            org.bukkit.block.Block block = it.next();
            if (!protection.isAllowed(null, block.getLocation(), flag)) {
                it.remove();
            }
        }
        if (!protection.isAllowed(null, event.getLocation(), flag)) {
            event.setYield(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        Iterator<org.bukkit.block.Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            org.bukkit.block.Block block = it.next();
            if (!protection.isAllowed(null, block.getLocation(), ProtectionFlag.OTHER_EXPLOSION)) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(@NotNull BlockBurnEvent event) {
        if (!protection.isAllowed(null, event.getBlock().getLocation(), ProtectionFlag.FIRE_DESTROY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(@NotNull BlockIgniteEvent event) {
        ProtectionFlag flag = event.getCause() == BlockIgniteEvent.IgniteCause.LAVA
                ? ProtectionFlag.LAVA_FIRE
                : ProtectionFlag.FIRE_SPREAD;
        Player player = event.getPlayer();
        if (!protection.isAllowed(player, event.getBlock().getLocation(), flag)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpread(@NotNull BlockSpreadEvent event) {
        if (event.getNewState().getType() == Material.FIRE
                && !protection.isAllowed(null, event.getBlock().getLocation(), ProtectionFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFade(@NotNull BlockFadeEvent event) {
        Material type = event.getBlock().getType();
        if ((type == Material.ICE || type == Material.PACKED_ICE || type == Material.SNOW || type == Material.SNOW_BLOCK)
                && !protection.isAllowed(null, event.getBlock().getLocation(), ProtectionFlag.ICE_MELT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeaves(@NotNull LeavesDecayEvent event) {
        if (!protection.isAllowed(null, event.getBlock().getLocation(), ProtectionFlag.LEAF_DECAY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(@NotNull EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        ProtectionFlag flag;
        String type = entity.getType().getKey().getKey();
        if (type.contains("enderman")) {
            flag = ProtectionFlag.ENDERMAN_GRIEF;
        } else if (event.getTo() == Material.AIR || event.getBlock().getType().name().contains("CROP")
                || event.getBlock().getType() == Material.FARMLAND) {
            flag = type.contains("villager") ? ProtectionFlag.MOB_GRIEF : ProtectionFlag.CROP_TRAMPLE;
            if (event.getBlock().getType() != Material.FARMLAND && !type.contains("enderman")) {
                flag = ProtectionFlag.MOB_GRIEF;
            }
            if (event.getBlock().getType() == Material.FARMLAND) {
                flag = ProtectionFlag.CROP_TRAMPLE;
            }
        } else {
            flag = ProtectionFlag.MOB_GRIEF;
        }
        if (!protection.isAllowed(null, event.getBlock().getLocation(), flag)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (!protection.isAllowed(attacker, victim.getLocation(), ProtectionFlag.PVP)
                || !protection.isAllowed(victim, victim.getLocation(), ProtectionFlag.PVP)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(attacker, "protect-denied-pvp");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUseOrChest(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        org.bukkit.block.Block block = event.getClickedBlock();
        if (block.getState() instanceof InventoryHolder) {
            if (!protection.isAllowed(player, block.getLocation(), ProtectionFlag.CHEST_ACCESS)) {
                event.setCancelled(true);
                protection.getPluginMessages().send(player, "protect-denied-chest");
                return;
            }
        }
        Material type = block.getType();
        if (type.isInteractable() && !protection.isAllowed(player, block.getLocation(), ProtectionFlag.USE)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(player, "protect-denied-use");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(@NotNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!protection.isAllowed(player, player.getLocation(), ProtectionFlag.ITEM_DROP)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(player, "protect-denied-drop");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehiclePlace(@NotNull EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (!protection.isAllowed(player, event.getBlock().getLocation(), ProtectionFlag.VEHICLE)) {
            event.setCancelled(true);
            protection.getPluginMessages().send(player, "protect-denied-vehicle");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = entryThrottle.get(player.getUniqueId());
        if (last != null && now - last < 250L) {
            return;
        }
        entryThrottle.put(player.getUniqueId(), now);
        if (!protection.isAllowed(player, event.getTo(), ProtectionFlag.ENTRY)) {
            event.setTo(event.getFrom());
            protection.getPluginMessages().send(player, "protect-denied-entry");
        }
    }

    private @NotNull ProtectionFlag explosionFlag(@NotNull Entity entity) {
        if (entity instanceof TNTPrimed || entity.getType().getKey().getKey().contains("tnt")) {
            return ProtectionFlag.TNT;
        }
        if (entity instanceof Creeper) {
            return ProtectionFlag.CREEPER_EXPLOSION;
        }
        return ProtectionFlag.OTHER_EXPLOSION;
    }

    private @Nullable Player resolvePlayer(@NotNull Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
