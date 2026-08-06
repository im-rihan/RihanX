package com.rihanx.base;

import com.rihanx.RihanX;
import com.rihanx.managers.MessageManager;
import com.rihanx.protection.ProtectionFlag;
import com.rihanx.protection.ProtectionService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pastes built-in house / base templates at the player's location.
 */
public final class BaseService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull Map<String, BaseTemplates.BaseBlueprint> templates = BaseTemplates.all();

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

    public void paste(@NotNull Player player, @NotNull String id) {
        BaseTemplates.BaseBlueprint blueprint = get(id);
        if (blueprint == null) {
            messages.send(player, "base-missing", MessageManager.placeholders(
                    "name", id,
                    "options", String.join(", ", listIds())
            ));
            return;
        }

        Location origin = player.getLocation().getBlock().getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        BlockFace facing = yawToFace(player.getLocation().getYaw());
        ProtectionService protection = plugin.getProtectionService();
        boolean bypass = protection.hasBypass(player);

        List<BlockChange> changes = new ArrayList<>();
        for (BaseTemplates.RelBlock rel : blueprint.blocks()) {
            int[] xz = rotate(rel.dx(), rel.dz(), facing);
            int x = origin.getBlockX() + xz[0];
            int y = origin.getBlockY() + rel.dy();
            int z = origin.getBlockZ() + xz[1];
            Block block = world.getBlockAt(x, y, z);
            if (!bypass && (!protection.isAllowed(player, block.getLocation(), ProtectionFlag.BUILD)
                    || !protection.isAllowed(player, block.getLocation(), ProtectionFlag.PLACE))) {
                messages.send(player, "base-protected-deny");
                return;
            }
            if (rel.material() == Material.AIR) {
                // only clear if we marked door openings
                changes.add(new BlockChange(block, Material.AIR));
            } else {
                changes.add(new BlockChange(block, rel.material()));
            }
        }

        int perTick = plugin.getConfig().getInt("base.blocks-per-tick", 800);
        messages.send(player, "base-building", MessageManager.placeholders(
                "name", blueprint.id(),
                "blocks", changes.size()
        ));

        final int[] index = {0};
        final BukkitTaskHolder holder = new BukkitTaskHolder();
        holder.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int budget = Math.max(1, perTick);
            while (budget-- > 0 && index[0] < changes.size()) {
                BlockChange change = changes.get(index[0]++);
                change.block().setType(change.material(), false);
            }
            if (index[0] >= changes.size()) {
                if (holder.task != null) {
                    holder.task.cancel();
                }
                messages.send(player, "base-done", MessageManager.placeholders(
                        "name", blueprint.id(),
                        "blocks", changes.size()
                ));
            }
        }, 1L, 1L);
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

    /** Rotate local (dx,dz) so +Z is the player's facing. */
    private static int @NotNull [] rotate(int dx, int dz, @NotNull BlockFace facing) {
        return switch (facing) {
            case SOUTH -> new int[]{dx, dz};
            case NORTH -> new int[]{-dx, -dz};
            case EAST -> new int[]{dz, -dx};
            case WEST -> new int[]{-dz, dx};
            default -> new int[]{dx, dz};
        };
    }

    private record BlockChange(@NotNull Block block, @NotNull Material material) {
    }

    private static final class BukkitTaskHolder {
        org.bukkit.scheduler.BukkitTask task;
    }
}
