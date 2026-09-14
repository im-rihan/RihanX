package com.rihanx.kingdom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

/**
 * Enforces the six-directional citadel wards: entry, fluids, sky, foundation, entities.
 */
public final class KingdomListener implements Listener {

    private final @NotNull KingdomService kingdoms;

    public KingdomListener(@NotNull KingdomService kingdoms) {
        this.kingdoms = kingdoms;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(@NotNull PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (kingdoms.hasBypass(player)) {
            return;
        }
        denyIfSealed(player, from, to, () -> event.setTo(from));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null) {
            return;
        }
        Player player = event.getPlayer();
        if (kingdoms.hasBypass(player)) {
            return;
        }
        denyIfSealed(player, from, to, () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluid(@NotNull BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();
        Material type = from.getType();
        boolean water = type == Material.WATER || type == Material.BUBBLE_COLUMN;
        boolean lava = type == Material.LAVA;
        if (!water && !lava) {
            return;
        }
        Kingdom origin = kingdoms.at(from.getLocation());
        Kingdom dest = kingdoms.at(to.getLocation());
        if (origin == dest) {
            return;
        }
        Kingdom sealed = origin != null ? origin : dest;
        if (sealed == null || !sealed.isSealed(WardLayer.ELEMENTAL)) {
            return;
        }
        if ((water && kingdoms.settings().sealWater()) || (lava && kingdoms.settings().sealLava())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(@NotNull PlayerBucketEmptyEvent event) {
        Material bucket = event.getBucket();
        boolean water = bucket == Material.WATER_BUCKET;
        boolean lava = bucket == Material.LAVA_BUCKET;
        if (!water && !lava) {
            return;
        }
        Block block = event.getBlock();
        Kingdom kingdom = kingdoms.at(block.getLocation());
        Player player = event.getPlayer();
        if (kingdom == null) {
            Kingdom nearby = kingdoms.at(block.getRelative(event.getBlockFace()).getLocation());
            if (nearby != null && nearby.isSealed(WardLayer.ELEMENTAL) && !nearby.isMember(player.getUniqueId())) {
                event.setCancelled(true);
                kingdoms.messages().send(player, "kingdom-denied-fluid");
                kingdoms.handleBreach(nearby, block.getLocation(), player);
            }
            return;
        }
        if (!kingdom.isSealed(WardLayer.ELEMENTAL)) {
            return;
        }
        if (!kingdom.isMember(player.getUniqueId()) && !kingdoms.hasBypass(player)) {
            event.setCancelled(true);
            kingdoms.messages().send(player, "kingdom-denied-fluid");
            kingdoms.handleBreach(kingdom, block.getLocation(), player);
            return;
        }
        kingdoms.recordFluid(block, block.getType());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        Material type = event.getBlockPlaced().getType();
        if (type != Material.WATER && type != Material.LAVA) {
            return;
        }
        Kingdom kingdom = kingdoms.at(event.getBlockPlaced().getLocation());
        if (kingdom != null && kingdom.isSealed(WardLayer.ELEMENTAL)) {
            kingdoms.recordFluid(event.getBlockPlaced(), event.getBlockReplacedState().getType());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(@NotNull CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        Kingdom kingdom = kingdoms.at(event.getLocation());
        if (kingdom == null) {
            return;
        }
        if (entity instanceof Phantom && kingdom.isSealed(WardLayer.SKY)) {
            event.setCancelled(true);
            return;
        }
        if (entity instanceof Monster && kingdom.isSealed(WardLayer.INNER) && !kingdom.rules().mobSpawn()) {
            CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
            if (reason != CreatureSpawnEvent.SpawnReason.CUSTOM
                    && reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                    && reason != CreatureSpawnEvent.SpawnReason.COMMAND) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityMove(@NotNull EntityMoveEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        boolean hostile = entity instanceof Monster || entity instanceof Projectile || entity instanceof FallingBlock;
        if (!hostile) {
            return;
        }
        Kingdom dest = kingdoms.at(to);
        Kingdom origin = kingdoms.at(from);
        if (dest == origin) {
            if (dest != null && dest.isSealed(WardLayer.SKY) && entity instanceof FallingBlock && to.getBlockY() >= dest.bounds().maxY()) {
                event.setCancelled(true);
            }
            return;
        }
        Kingdom sealed = dest != null ? dest : origin;
        if (sealed == null) {
            return;
        }
        if (entity instanceof Projectile && sealed.isSealed(WardLayer.OUTER)) {
            event.setCancelled(true);
            entity.remove();
            return;
        }
        if (entity instanceof FallingBlock && sealed.isSealed(WardLayer.SKY)) {
            event.setCancelled(true);
            entity.remove();
            return;
        }
        if (entity instanceof Monster && sealed.isSealed(WardLayer.OUTER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(@NotNull ProjectileHitEvent event) {
        if (event.getHitBlock() == null) {
            return;
        }
        Location hit = event.getHitBlock().getLocation();
        Kingdom kingdom = kingdoms.at(hit);
        if (kingdom == null || !kingdom.isSealed(WardLayer.OUTER)) {
            return;
        }
        if (event.getEntity().getShooter() instanceof Player player && kingdom.isMember(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        event.getEntity().remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallingBlock(@NotNull EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) {
            return;
        }
        Kingdom kingdom = kingdoms.at(event.getBlock().getLocation());
        if (kingdom != null && kingdom.isSealed(WardLayer.SKY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLightning(@NotNull LightningStrikeEvent event) {
        Kingdom kingdom = kingdoms.at(event.getLightning().getLocation());
        if (kingdom != null && kingdom.isSealed(WardLayer.SKY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreasury(@NotNull InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof org.bukkit.block.BlockState state)) {
            return;
        }
        Kingdom kingdom = kingdoms.at(state.getLocation());
        if (kingdom == null || !kingdom.isTreasuryBlock(
                state.getX(), state.getY(), state.getZ(), state.getWorld().getName())) {
            return;
        }
        KingdomRole role = kingdom.roleOf(player.getUniqueId());
        if (role == null || !role.canBuild()) {
            event.setCancelled(true);
            kingdoms.messages().send(player, "kingdom-treasury-deny");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!kingdoms.isKingdomChat(player)) {
            return;
        }
        event.setCancelled(true);
        var message = event.message();
        kingdoms.plugin().getSchedulerUtil().runSync(() -> kingdoms.broadcastChat(player, message));
    }

    private void denyIfSealed(
            @NotNull Player player,
            @NotNull Location from,
            @NotNull Location to,
            @NotNull Runnable deny
    ) {
        Kingdom dest = kingdoms.at(to);
        Kingdom origin = kingdoms.at(from);
        if (dest == origin) {
            if (dest != null && dest.isSealed(WardLayer.SKY) && to.getBlockY() > dest.bounds().maxY()) {
                deny.run();
                kingdoms.messages().send(player, "kingdom-denied-sky");
            }
            if (dest != null && dest.isSealed(WardLayer.DEEP) && to.getBlockY() < dest.bounds().minY()) {
                deny.run();
                kingdoms.messages().send(player, "kingdom-denied-deep");
            }
            return;
        }
        if (dest != null && dest.isSealed(WardLayer.OUTER) && !dest.isMember(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            if (dest.isOpenGate(to.getBlockX(), to.getBlockY(), to.getBlockZ(), now)
                    || dest.isOpenGate(from.getBlockX(), from.getBlockY(), from.getBlockZ(), now)) {
                return;
            }
            deny.run();
            kingdoms.messages().send(player, "kingdom-denied-entry");
            kingdoms.handleBreach(dest, to, player);
            return;
        }
        if (origin != null && dest == null && origin.isSealed(WardLayer.OUTER) && !origin.isMember(player.getUniqueId())) {
            long now = System.currentTimeMillis();
            if (origin.isOpenGate(from.getBlockX(), from.getBlockY(), from.getBlockZ(), now)) {
                return;
            }
            deny.run();
            kingdoms.messages().send(player, "kingdom-denied-exit");
        }
    }
}
