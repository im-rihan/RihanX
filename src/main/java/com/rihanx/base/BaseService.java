package com.rihanx.base;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import com.rihanx.protection.ProtectionFlag;
import com.rihanx.protection.ProtectionService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pastes house templates: aligns entrance to the player, builds smoothly, then stands you at the door.
 */
public final class BaseService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull Map<String, BaseTemplates.BaseBlueprint> templates = BaseTemplates.all();
    private final @NotNull Set<UUID> building = ConcurrentHashMap.newKeySet();

    public BaseService(@NotNull RihanX plugin, @NotNull MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public @NotNull List<String> listIds() {
        return List.copyOf(templates.keySet());
    }

    public @Nullable BaseTemplates.BaseBlueprint get(@NotNull String id) {
        return templates.get(id.toLowerCase(Locale.ROOT).trim());
    }

    public void sendList(@NotNull Player player) {
        messages.send(player, "base-list-header", MessageManager.placeholders("count", templates.size()));
        for (BaseTemplates.BaseBlueprint blueprint : templates.values()) {
            messages.send(player, "base-list-line", MessageManager.placeholders(
                    "name", blueprint.id(),
                    "description", blueprint.description(),
                    "blocks", blueprint.blocks().size()
            ));
        }
    }

    public void openMenu(@NotNull Player player) {
        new BaseSelectGui(plugin).open(player);
    }

    public void paste(@NotNull Player player, @NotNull String id) {
        BaseTemplates.BaseBlueprint blueprint = get(id);
        if (blueprint == null) {
            messages.send(player, "base-missing", MessageManager.placeholders(
                    "name", id,
                    "options", String.join(", ", listIds())
            ));
            return;
        }
        pasteBlueprint(player, blueprint, "base");
    }

    /**
     * Paste any blueprint (homes or farms) with entrance alignment and safe spawn.
     */
    public void pasteBlueprint(
            @NotNull Player player,
            @NotNull BaseTemplates.BaseBlueprint blueprint,
            @NotNull String kind
    ) {
        if (!building.add(player.getUniqueId())) {
            messages.send(player, "base-busy");
            return;
        }
        try {
            startPaste(player, blueprint, kind);
        } catch (RuntimeException ex) {
            building.remove(player.getUniqueId());
            throw ex;
        }
    }

    private void startPaste(
            @NotNull Player player,
            @NotNull BaseTemplates.BaseBlueprint blueprint,
            @NotNull String kind
    ) {
        World world = player.getWorld();
        BlockFace facing = yawToFace(player.getLocation().getYaw());

        // Align so the blueprint entrance lands on the player's feet (not the house center).
        Location playerBlock = player.getLocation().getBlock().getLocation();
        int[] spawnOff = rotate(blueprint.spawnDx(), blueprint.spawnDz(), facing);
        Location origin = new Location(
                world,
                playerBlock.getBlockX() - spawnOff[0],
                playerBlock.getBlockY() - blueprint.spawnDy(),
                playerBlock.getBlockZ() - spawnOff[1]
        );

        ProtectionService protection = plugin.getProtectionService();
        boolean bypass = protection.hasBypass(player);

        List<BlockChange> changes = new ArrayList<>(blueprint.blocks().size());
        for (BaseTemplates.RelBlock rel : blueprint.blocks()) {
            int[] xz = rotate(rel.dx(), rel.dz(), facing);
            int x = origin.getBlockX() + xz[0];
            int y = origin.getBlockY() + rel.dy();
            int z = origin.getBlockZ() + xz[1];
            Block block = world.getBlockAt(x, y, z);
            if (!bypass && (!protection.isAllowed(player, block.getLocation(), ProtectionFlag.BUILD)
                    || !protection.isAllowed(player, block.getLocation(), ProtectionFlag.PLACE))) {
                building.remove(player.getUniqueId());
                messages.send(player, "base-protected-deny");
                return;
            }
            changes.add(new BlockChange(block, createData(rel, facing), pastePriority(rel.material())));
        }

        // Solids first, then specials, water last (lifts/pools need floor under water).
        changes.sort(Comparator
                .comparingInt(BlockChange::priority)
                .thenComparingInt(c -> c.block().getY())
                .thenComparingInt(c -> c.block().getZ())
                .thenComparingInt(c -> c.block().getX()));

        // Keep the builder safe at the entrance while the house appears behind them.
        Location entrance = entranceLocation(origin, blueprint, facing, player.getLocation().getYaw());
        player.teleport(entrance);
        player.setVelocity(new Vector(0, 0, 0));
        boolean wasFlying = player.isFlying();
        boolean allowFlight = player.getAllowFlight();
        GameMode mode = player.getGameMode();
        if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        int perTick = Math.max(50, plugin.getConfig().getInt("base.blocks-per-tick", 1200));
        String buildingKey = kind.equals("farm") ? "farm-building" : "base-building";
        messages.send(player, buildingKey, MessageManager.placeholders(
                "name", blueprint.id(),
                "blocks", changes.size()
        ));

        final int[] index = {0};
        final int[] lastPct = {-1};
        final BukkitTaskHolder holder = new BukkitTaskHolder();
        holder.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                if (holder.task != null) {
                    holder.task.cancel();
                }
                building.remove(player.getUniqueId());
                return;
            }

            // Keep entrance clear so the player is never sealed in a wall mid-build.
            clearEntranceColumn(world, entrance);

            int budget = perTick;
            while (budget-- > 0 && index[0] < changes.size()) {
                BlockChange change = changes.get(index[0]++);
                // Never overwrite the two blocks the player occupies at the entrance.
                if (isPlayerOccupied(change.block(), entrance)) {
                    continue;
                }
                change.block().setBlockData(change.data(), false);
            }

            int pct = changes.isEmpty() ? 100 : (index[0] * 100) / changes.size();
            if (pct >= lastPct[0] + 25 && pct < 100) {
                lastPct[0] = pct;
                messages.send(player, "base-progress", MessageManager.placeholders(
                        "name", blueprint.id(),
                        "percent", pct
                ));
            }

            if (index[0] >= changes.size()) {
                if (holder.task != null) {
                    holder.task.cancel();
                }
                // Final entrance clear + stand the player at the door looking inside.
                clearEntranceColumn(world, entrance);
                Location done = entranceLocation(origin, blueprint, facing, faceToYaw(opposite(facing)));
                player.teleport(done);
                if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
                    player.setFlying(wasFlying);
                    player.setAllowFlight(allowFlight);
                }
                building.remove(player.getUniqueId());
                messages.send(player, kind.equals("farm") ? "farm-done" : "base-done", MessageManager.placeholders(
                        "name", blueprint.id(),
                        "blocks", changes.size()
                ));
                messages.send(player, kind.equals("farm") ? "farm-ready-hint" : "base-ready-hint");
            }
        }, 1L, 1L);
    }

    private static boolean isPlayerOccupied(@NotNull Block block, @NotNull Location entrance) {
        int x = entrance.getBlockX();
        int y = entrance.getBlockY();
        int z = entrance.getBlockZ();
        return block.getX() == x && block.getZ() == z
                && (block.getY() == y || block.getY() == y + 1);
    }

    private static void clearEntranceColumn(@NotNull World world, @NotNull Location entrance) {
        int x = entrance.getBlockX();
        int y = entrance.getBlockY();
        int z = entrance.getBlockZ();
        world.getBlockAt(x, y, z).setType(Material.AIR, false);
        world.getBlockAt(x, y + 1, z).setType(Material.AIR, false);
        // Solid standable floor under feet
        Block floor = world.getBlockAt(x, y - 1, z);
        if (!floor.getType().isSolid()) {
            floor.setType(Material.SMOOTH_STONE, false);
        }
    }

    private static @NotNull Location entranceLocation(
            @NotNull Location origin,
            @NotNull BaseTemplates.BaseBlueprint blueprint,
            @NotNull BlockFace structureFacing,
            float yaw
    ) {
        int[] xz = rotate(blueprint.spawnDx(), blueprint.spawnDz(), structureFacing);
        Location loc = new Location(
                origin.getWorld(),
                origin.getBlockX() + xz[0] + 0.5,
                origin.getBlockY() + blueprint.spawnDy(),
                origin.getBlockZ() + xz[1] + 0.5,
                yaw,
                0f
        );
        return loc;
    }

    private static int pastePriority(@NotNull Material material) {
        if (material == Material.AIR) {
            return 0;
        }
        if (material == Material.WATER || material == Material.LAVA) {
            return 3;
        }
        if (material.name().endsWith("_CARPET")) {
            return 2;
        }
        return 1;
    }

    private static @NotNull BlockData createData(
            @NotNull BaseTemplates.RelBlock rel,
            @NotNull BlockFace structureFacing
    ) {
        Material material = rel.material();
        if (material == Material.AIR || material == Material.WATER) {
            return material.createBlockData();
        }

        BlockData data = material.createBlockData();
        BlockFace worldFacing = rel.facing() == null ? null : mapFacing(rel.facing(), structureFacing);

        if (data instanceof Door door && worldFacing != null) {
            door.setFacing(worldFacing);
            door.setHalf(rel.upperHalf() ? Bisected.Half.TOP : Bisected.Half.BOTTOM);
            door.setOpen(false);
            if (rel.hinge() != null) {
                door.setHinge(rel.hinge());
            }
            return door;
        }
        if (data instanceof Bed bed && worldFacing != null) {
            bed.setFacing(worldFacing);
            if (rel.bedPart() != null) {
                bed.setPart(rel.bedPart());
            }
            return bed;
        }
        if (data instanceof Stairs stairs && worldFacing != null) {
            stairs.setFacing(worldFacing);
            stairs.setHalf(Bisected.Half.BOTTOM);
            stairs.setShape(Stairs.Shape.STRAIGHT);
            return stairs;
        }
        if (data instanceof Slab slab) {
            if (rel.slabType() != null) {
                slab.setType(rel.slabType());
            }
            return slab;
        }
        if (data instanceof Directional directional && worldFacing != null) {
            if (directional.getFaces().contains(worldFacing)) {
                directional.setFacing(worldFacing);
            }
            return directional;
        }
        if (data instanceof Lantern lantern) {
            lantern.setHanging(rel.hanging());
            return lantern;
        }
        if (data instanceof Lightable lightable
                && (material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE)) {
            lightable.setLit(true);
            return lightable;
        }
        return data;
    }

    private static @NotNull BlockFace yawToFace(float yaw) {
        float rot = (yaw % 360 + 360) % 360;
        if (rot >= 45 && rot < 135) {
            return BlockFace.WEST;
        }
        if (rot >= 135 && rot < 225) {
            return BlockFace.NORTH;
        }
        if (rot >= 225 && rot < 315) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }

    private static float faceToYaw(@NotNull BlockFace face) {
        return switch (face) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> 0f;
        };
    }

    private static @NotNull BlockFace opposite(@NotNull BlockFace face) {
        return face.getOppositeFace();
    }

    /** Rotate local (dx,dz) so +Z is the structure front / player facing. */
    static int @NotNull [] rotate(int dx, int dz, @NotNull BlockFace facing) {
        return switch (facing) {
            case SOUTH -> new int[]{dx, dz};
            case NORTH -> new int[]{-dx, -dz};
            case EAST -> new int[]{dz, -dx};
            case WEST -> new int[]{-dz, dx};
            default -> new int[]{dx, dz};
        };
    }

    static @NotNull BlockFace mapFacing(@NotNull BlockFace local, @NotNull BlockFace structureFront) {
        if (local == BlockFace.UP || local == BlockFace.DOWN) {
            return local;
        }
        int turns = switch (structureFront) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        BlockFace face = local;
        for (int i = 0; i < turns; i++) {
            face = rotateClockwise(face);
        }
        return face;
    }

    private static @NotNull BlockFace rotateClockwise(@NotNull BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> face;
        };
    }

    private record BlockChange(@NotNull Block block, @NotNull BlockData data, int priority) {
    }

    private static final class BukkitTaskHolder {
        org.bukkit.scheduler.BukkitTask task;
    }
}
