package com.rihanx.portal;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import com.rihanx.teleport.TeleportManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Named portal pads that teleport players to a linked portal.
 */
public final class PortalService {

    private static final long COOLDOWN_MS = 2500L;

    private final @NotNull MessageManager messages;
    private final @NotNull TeleportManager teleportManager;
    private final @NotNull PortalStore store;
    private final @NotNull Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();

    public PortalService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager
    ) {
        this(messages, teleportManager, new PortalStore(plugin));
    }

    public PortalService(
            @NotNull MessageManager messages,
            @NotNull TeleportManager teleportManager,
            @NotNull PortalStore store
    ) {
        this.messages = messages;
        this.teleportManager = teleportManager;
        this.store = store;
    }

    public @NotNull PortalStore getStore() {
        return store;
    }

    public void reload() {
        store.load();
    }

    public void create(@NotNull Player player, @NotNull String name) {
        CreateResult result = createAt(name, player.getLocation(), true);
        switch (result) {
            case INVALID -> messages.send(player, "invalid-argument", MessageManager.placeholders("input", name));
            case EXISTS -> messages.send(player, "portal-exists",
                    MessageManager.placeholders("portal", PortalStore.normalize(name)));
            case FAILED -> messages.send(player, "internal-error");
            case CREATED -> {
                String id = PortalStore.normalize(name);
                messages.send(player, "portal-created", MessageManager.placeholders("portal", id));
                messages.send(player, "portal-link-hint", MessageManager.placeholders("portal", id));
            }
        }
    }

    /**
     * Create a portal at an exact location (optionally placing the pad blocks).
     */
    public @NotNull CreateResult createAt(@NotNull String name, @NotNull Location location, boolean placePadBlocks) {
        String id = PortalStore.normalize(name);
        if (id.isEmpty()) {
            return CreateResult.INVALID;
        }
        if (store.has(id)) {
            return CreateResult.EXISTS;
        }
        Location feet = location.getBlock().getLocation().add(0.5, 0, 0.5);
        feet.setYaw(location.getYaw());
        feet.setPitch(location.getPitch());
        if (placePadBlocks && feet.getWorld() != null) {
            placePad(feet);
        }
        if (!store.set(id, feet, null)) {
            return CreateResult.FAILED;
        }
        return CreateResult.CREATED;
    }

    public void link(@NotNull Player player, @NotNull String a, @NotNull String b) {
        LinkResult result = linkNamed(a, b);
        switch (result) {
            case SELF -> messages.send(player, "portal-link-self");
            case MISSING -> {
                String missing = store.has(PortalStore.normalize(a))
                        ? PortalStore.normalize(b)
                        : PortalStore.normalize(a);
                messages.send(player, "portal-missing", MessageManager.placeholders("portal", missing));
            }
            case INVALID -> messages.send(player, "invalid-argument",
                    MessageManager.placeholders("input", a + " / " + b));
            case LINKED -> messages.send(player, "portal-linked", MessageManager.placeholders(
                    "portal", PortalStore.normalize(a),
                    "target", PortalStore.normalize(b)
            ));
        }
    }

    /** Bidirectional link without player messaging (stations / tests). */
    public @NotNull LinkResult linkNamed(@NotNull String a, @NotNull String b) {
        String left = PortalStore.normalize(a);
        String right = PortalStore.normalize(b);
        if (left.isEmpty() || right.isEmpty()) {
            return LinkResult.INVALID;
        }
        if (left.equals(right)) {
            return LinkResult.SELF;
        }
        if (!store.has(left) || !store.has(right)) {
            return LinkResult.MISSING;
        }
        store.setLink(left, right);
        store.setLink(right, left);
        return LinkResult.LINKED;
    }

    public enum CreateResult {
        CREATED,
        EXISTS,
        INVALID,
        FAILED
    }

    public enum LinkResult {
        LINKED,
        SELF,
        MISSING,
        INVALID
    }

    public void delete(@NotNull Player player, @NotNull String name) {
        String id = PortalStore.normalize(name);
        if (!store.delete(id)) {
            messages.send(player, "portal-missing", MessageManager.placeholders("portal", id));
            return;
        }
        messages.send(player, "portal-deleted", MessageManager.placeholders("portal", id));
    }

    public void sendList(@NotNull Player player) {
        List<String> names = store.list();
        if (names.isEmpty()) {
            messages.send(player, "portal-list", MessageManager.placeholders("count", 0, "portals", "none"));
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (String name : names) {
            PortalStore.StoredPortal portal = store.get(name);
            if (joined.length() > 0) {
                joined.append(", ");
            }
            joined.append(name);
            if (portal != null && portal.link() != null) {
                joined.append("→").append(portal.link());
            }
        }
        messages.send(player, "portal-list", MessageManager.placeholders(
                "count", names.size(),
                "portals", joined.toString()
        ));
    }

    public void teleportTo(@NotNull Player player, @NotNull String name) {
        PortalStore.StoredPortal portal = store.get(name);
        if (portal == null) {
            messages.send(player, "portal-missing", MessageManager.placeholders("portal", PortalStore.normalize(name)));
            return;
        }
        Location dest = portal.toLocation();
        if (dest == null) {
            messages.send(player, "portal-world-missing", MessageManager.placeholders("portal", PortalStore.normalize(name)));
            return;
        }
        messages.send(player, "portal-teleport", MessageManager.placeholders("portal", PortalStore.normalize(name)));
        teleportManager.teleport(player, dest.clone().add(0, 0.1, 0), "portal");
    }

    /**
     * Called when a player stands on / clicks a portal pressure plate.
     *
     * @return true if a portal teleport was attempted
     */
    public boolean tryUsePad(@NotNull Player player, @NotNull Location plateLoc) {
        long now = System.currentTimeMillis();
        Long until = cooldownUntil.get(player.getUniqueId());
        if (until != null && until > now) {
            return false;
        }

        String matched = findPortalAt(plateLoc);
        if (matched == null) {
            return false;
        }
        PortalStore.StoredPortal portal = store.get(matched);
        if (portal == null || portal.link() == null) {
            messages.send(player, "portal-unlinked", MessageManager.placeholders("portal", matched));
            cooldownUntil.put(player.getUniqueId(), now + 1500L);
            return true;
        }
        PortalStore.StoredPortal destPortal = store.get(portal.link());
        if (destPortal == null) {
            messages.send(player, "portal-missing", MessageManager.placeholders("portal", portal.link()));
            return true;
        }
        Location dest = destPortal.toLocation();
        if (dest == null) {
            messages.send(player, "portal-world-missing", MessageManager.placeholders("portal", portal.link()));
            return true;
        }
        cooldownUntil.put(player.getUniqueId(), now + COOLDOWN_MS);
        messages.send(player, "portal-teleport", MessageManager.placeholders("portal", portal.link()));
        // Offset slightly so the player does not instantly re-trigger the destination plate
        Location safe = dest.clone().add(0, 0.15, 0);
        teleportManager.teleportPreferExact(player, safe, "portal");
        return true;
    }

    public @Nullable String findPortalAt(@NotNull Location loc) {
        if (loc.getWorld() == null) {
            return null;
        }
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        for (Map.Entry<String, PortalStore.StoredPortal> entry : store.all().entrySet()) {
            PortalStore.StoredPortal portal = entry.getValue();
            if (!portal.worldName().equalsIgnoreCase(loc.getWorld().getName())) {
                continue;
            }
            Location p = portal.toLocation();
            if (p == null) {
                continue;
            }
            if (Math.abs(p.getBlockX() - bx) <= 1
                    && Math.abs(p.getBlockY() - by) <= 1
                    && Math.abs(p.getBlockZ() - bz) <= 1) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void placePad(@NotNull Location center) {
        Block base = center.getBlock().getRelative(BlockFace.DOWN);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block floor = base.getRelative(dx, 0, dz);
                floor.setType(Material.POLISHED_DEEPSLATE, false);
                Block above = floor.getRelative(BlockFace.UP);
                if (dx == 0 && dz == 0) {
                    above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
                } else if (Math.abs(dx) + Math.abs(dz) == 1) {
                    above.setType(Material.AMETHYST_BLOCK, false);
                } else {
                    above.setType(Material.AIR, false);
                }
            }
        }
        Block pillar = base.getRelative(0, 1, -2);
        pillar.setType(Material.POLISHED_DEEPSLATE, false);
        Block button = pillar.getRelative(BlockFace.SOUTH);
        button.setType(Material.STONE_BUTTON, false);
        org.bukkit.block.data.BlockData buttonData = button.getBlockData();
        if (buttonData instanceof org.bukkit.block.data.FaceAttachable attachable) {
            attachable.setAttachedFace(org.bukkit.block.data.FaceAttachable.AttachedFace.WALL);
        }
        if (buttonData instanceof org.bukkit.block.data.Directional directional) {
            directional.setFacing(BlockFace.SOUTH);
        }
        button.setBlockData(buttonData, false);
        // Keep player standing on the plate
        center.getBlock().setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
    }
}
