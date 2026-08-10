package com.rihanx.build;

import com.rihanx.RihanX;
import com.rihanx.build.BuildShapes.PlannedBlock;
import com.rihanx.edit.BlockApplier;
import com.rihanx.edit.BlockSnapshot;
import com.rihanx.edit.EditHistory;
import com.rihanx.managers.MessageManager;
import com.rihanx.protection.ProtectionFlag;
import com.rihanx.protection.ProtectionService;
import com.rihanx.utils.MaterialUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared aim-and-build toolkit. Every apply always records undo.
 */
public final class BuildToolService {

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull Map<UUID, BukkitTask> active = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, Deque<BuildSession>> undoStacks = new ConcurrentHashMap<>();

    public BuildToolService(@NotNull RihanX plugin, @NotNull MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void platform(@NotNull Player player, @Nullable Integer sizeArg, @Nullable String materialArg) {
        int max = cfgInt("build.platform.max-size", "bridge.max-width", 96);
        int size = sizeArg != null ? sizeArg : cfgInt("build.platform.default-size", null, 16);
        if (!inRange(player, size, 1, max, "build-size-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.platform.default-material", "grass_block");
        if (material == null) {
            return;
        }
        int clearHeight = Math.max(0, cfgInt("build.platform.clear-height", "build.clear-height", 48));
        Location feet = player.getLocation().getBlock().getLocation();
        Map<Long, PlannedBlock> planned = BuildShapes.clearedPlatform(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY() - 1,
                feet.getBlockZ(),
                size,
                clearHeight,
                material.createBlockData()
        );
        apply(player, "platform", planned, MessageManager.placeholders(
                "size", size,
                "material", MaterialUtil.key(material),
                "clear", clearHeight,
                "tool", "platform"
        ));
    }

    /**
     * Large cleared plain / build pad — same as platform with optional larger defaults.
     */
    public void plain(@NotNull Player player, @Nullable Integer sizeArg, @Nullable String materialArg) {
        // Prefer plain.* then fall back to platform.*
        int max = cfgInt("build.plain.max-size", "build.platform.max-size", 96);
        int size = sizeArg != null
                ? sizeArg
                : cfgInt("build.plain.default-size", "build.platform.default-size", 32);
        if (!inRange(player, size, 1, max, "build-size-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.plain.default-material", "grass_block");
        if (material == null) {
            return;
        }
        int clearHeight = Math.max(0, cfgInt("build.plain.clear-height", "build.platform.clear-height", 64));
        Location feet = player.getLocation().getBlock().getLocation();
        Map<Long, PlannedBlock> planned = BuildShapes.clearedPlatform(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY() - 1,
                feet.getBlockZ(),
                size,
                clearHeight,
                material.createBlockData()
        );
        apply(player, "plain", planned, MessageManager.placeholders(
                "size", size,
                "material", MaterialUtil.key(material),
                "clear", clearHeight,
                "tool", "plain"
        ));
    }

    public void wall(
            @NotNull Player player,
            @Nullable Integer lengthArg,
            @Nullable Integer heightArg,
            @Nullable String materialArg
    ) {
        int maxL = cfgInt("build.wall.max-length", "bridge.max-length", 128);
        int maxH = cfgInt("build.wall.max-height", null, 64);
        int length = lengthArg != null ? lengthArg : cfgInt("build.wall.default-length", null, 16);
        int height = heightArg != null ? heightArg : cfgInt("build.wall.default-height", null, 4);
        if (!inRange(player, length, 1, maxL, "build-length-invalid")
                || !inRange(player, height, 1, maxH, "build-height-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.wall.default-material", "stone_bricks");
        if (material == null) {
            return;
        }
        Location feet = player.getLocation().getBlock().getLocation();
        BlockFace facing = BuildShapes.yawToFace(player.getLocation().getYaw());
        Map<Long, PlannedBlock> planned = BuildShapes.wall(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY(),
                feet.getBlockZ(),
                facing,
                length,
                height,
                material.createBlockData()
        );
        apply(player, "wall", planned, MessageManager.placeholders(
                "length", length,
                "height", height,
                "material", MaterialUtil.key(material),
                "tool", "wall"
        ));
    }

    public void pillar(@NotNull Player player, @Nullable Integer heightArg, @Nullable String materialArg) {
        int maxH = cfgInt("build.pillar.max-height", null, 64);
        int height = heightArg != null ? heightArg : cfgInt("build.pillar.default-height", null, 16);
        if (height == 0 || Math.abs(height) > maxH) {
            messages.send(player, "build-height-invalid", MessageManager.placeholders("min", 1, "max", maxH));
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.pillar.default-material", "stone");
        if (material == null) {
            return;
        }
        Location feet = player.getLocation().getBlock().getLocation();
        int startY = height > 0 ? feet.getBlockY() : feet.getBlockY() - 1;
        Map<Long, PlannedBlock> planned = BuildShapes.pillar(
                player.getWorld(),
                feet.getBlockX(),
                startY,
                feet.getBlockZ(),
                height,
                material.createBlockData()
        );
        apply(player, "pillar", planned, MessageManager.placeholders(
                "height", height,
                "material", MaterialUtil.key(material),
                "tool", "pillar"
        ));
    }

    public void cylinder(
            @NotNull Player player,
            boolean hollow,
            @Nullable Integer radiusArg,
            @Nullable Integer heightArg,
            @Nullable String materialArg
    ) {
        int maxR = cfgInt("build.cyl.max-radius", null, 32);
        int maxH = cfgInt("build.cyl.max-height", null, 64);
        int radius = radiusArg != null ? radiusArg : cfgInt("build.cyl.default-radius", null, 5);
        int height = heightArg != null ? heightArg : cfgInt("build.cyl.default-height", null, 1);
        if (!inRange(player, radius, 0, maxR, "build-radius-invalid")
                || !inRange(player, height, 1, maxH, "build-height-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.cyl.default-material", "stone");
        if (material == null) {
            return;
        }
        Location feet = player.getLocation().getBlock().getLocation();
        Map<Long, PlannedBlock> planned = BuildShapes.cylinder(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY() - 1,
                feet.getBlockZ(),
                radius,
                height,
                hollow,
                material.createBlockData()
        );
        String tool = hollow ? "hcyl" : "cyl";
        apply(player, tool, planned, MessageManager.placeholders(
                "radius", radius,
                "height", height,
                "material", MaterialUtil.key(material),
                "tool", tool
        ));
    }

    public void sphere(
            @NotNull Player player,
            boolean hollow,
            @Nullable Integer radiusArg,
            @Nullable String materialArg
    ) {
        int maxR = cfgInt("build.sphere.max-radius", null, 32);
        int radius = radiusArg != null ? radiusArg : cfgInt("build.sphere.default-radius", null, 5);
        if (!inRange(player, radius, 1, maxR, "build-radius-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.sphere.default-material", "stone");
        if (material == null) {
            return;
        }
        Location feet = player.getLocation().getBlock().getLocation();
        // Center at feet so sphere sits around the player
        Map<Long, PlannedBlock> planned = BuildShapes.sphere(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY(),
                feet.getBlockZ(),
                radius,
                hollow,
                material.createBlockData()
        );
        String tool = hollow ? "hsphere" : "sphere";
        apply(player, tool, planned, MessageManager.placeholders(
                "radius", radius,
                "material", MaterialUtil.key(material),
                "tool", tool
        ));
    }

    public void tunnel(
            @NotNull Player player,
            @Nullable Integer lengthArg,
            @Nullable Integer widthArg,
            @Nullable Integer heightArg,
            @Nullable String materialArg
    ) {
        int maxL = cfgInt("build.tunnel.max-length", "bridge.max-length", 128);
        int maxW = cfgInt("build.tunnel.max-width", null, 15);
        int maxH = cfgInt("build.tunnel.max-height", null, 15);
        int length = lengthArg != null ? lengthArg : cfgInt("build.tunnel.default-length", null, 16);
        int width = widthArg != null ? widthArg : cfgInt("build.tunnel.default-width", null, 3);
        int height = heightArg != null ? heightArg : cfgInt("build.tunnel.default-height", null, 3);
        if (!inRange(player, length, 1, maxL, "build-length-invalid")
                || !inRange(player, width, 1, maxW, "build-width-invalid")
                || !inRange(player, height, 1, maxH, "build-height-invalid")) {
            return;
        }
        BlockData lining = null;
        String liningLabel = "air";
        if (materialArg != null && !materialArg.equalsIgnoreCase("none") && !materialArg.equalsIgnoreCase("air")) {
            Material material = resolveMaterial(player, materialArg, null, "stone");
            if (material == null) {
                return;
            }
            lining = material.createBlockData();
            liningLabel = MaterialUtil.key(material);
        } else if (materialArg == null && plugin.getConfig().getBoolean("build.tunnel.default-lining", false)) {
            Material material = resolveMaterial(player, null, "build.tunnel.default-material", "stone");
            if (material != null) {
                lining = material.createBlockData();
                liningLabel = MaterialUtil.key(material);
            }
        }
        Location feet = player.getLocation().getBlock().getLocation();
        BlockFace facing = BuildShapes.yawToFace(player.getLocation().getYaw());
        Map<Long, PlannedBlock> planned = BuildShapes.tunnel(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY(),
                feet.getBlockZ(),
                facing,
                length,
                width,
                height,
                lining
        );
        apply(player, "tunnel", planned, MessageManager.placeholders(
                "length", length,
                "width", width,
                "height", height,
                "material", liningLabel,
                "tool", "tunnel"
        ));
    }

    public void flatten(@NotNull Player player, @Nullable Integer radiusArg) {
        int maxR = cfgInt("build.flatten.max-radius", null, 96);
        int radius = radiusArg != null ? radiusArg : cfgInt("build.flatten.default-radius", null, 16);
        if (!inRange(player, radius, 1, maxR, "build-radius-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, null, "build.flatten.default-material", "grass_block");
        if (material == null) {
            return;
        }
        int clearHeight = Math.max(0, cfgInt("build.flatten.clear-height", "build.clear-height", 48));
        Location feet = player.getLocation().getBlock().getLocation();
        Map<Long, PlannedBlock> planned = BuildShapes.flatten(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY() - 1,
                feet.getBlockZ(),
                radius,
                clearHeight,
                material.createBlockData()
        );
        apply(player, "flatten", planned, MessageManager.placeholders(
                "radius", radius,
                "material", MaterialUtil.key(material),
                "clear", clearHeight,
                "tool", "flatten"
        ));
    }

    public void drain(@NotNull Player player, @Nullable Integer radiusArg) {
        int maxR = cfgInt("build.drain.max-radius", null, 64);
        int radius = radiusArg != null ? radiusArg : cfgInt("build.drain.default-radius", null, 8);
        if (!inRange(player, radius, 1, maxR, "build-radius-invalid")) {
            return;
        }
        Location feet = player.getLocation().getBlock().getLocation();
        Map<Long, PlannedBlock> planned = BuildShapes.drain(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY(),
                feet.getBlockZ(),
                radius
        );
        apply(player, "drain", planned, MessageManager.placeholders(
                "radius", radius,
                "tool", "drain"
        ));
    }

    public void pyramid(
            @NotNull Player player,
            boolean hollow,
            @Nullable Integer sizeArg,
            @Nullable String materialArg
    ) {
        int max = cfgInt("build.pyramid.max-size", null, 32);
        int size = sizeArg != null ? sizeArg : cfgInt("build.pyramid.default-size", null, 5);
        if (!inRange(player, size, 1, max, "build-size-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.pyramid.default-material", "sandstone");
        if (material == null) {
            return;
        }
        Location feet = player.getLocation().getBlock().getLocation();
        Map<Long, PlannedBlock> planned = BuildShapes.pyramid(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY() - 1,
                feet.getBlockZ(),
                size,
                hollow,
                material.createBlockData()
        );
        String tool = hollow ? "hpyramid" : "pyramid";
        apply(player, tool, planned, MessageManager.placeholders(
                "size", size,
                "material", MaterialUtil.key(material),
                "tool", tool
        ));
    }

    public void stairs(
            @NotNull Player player,
            @Nullable Integer lengthArg,
            @Nullable Integer widthArg,
            @Nullable String materialArg
    ) {
        int maxL = cfgInt("build.stairs.max-length", "bridge.max-length", 64);
        int maxW = cfgInt("build.stairs.max-width", null, 15);
        int length = lengthArg != null ? lengthArg : cfgInt("build.stairs.default-length", null, 8);
        int width = widthArg != null ? widthArg : cfgInt("build.stairs.default-width", null, 3);
        if (!inRange(player, length, 1, maxL, "build-length-invalid")
                || !inRange(player, width, 1, maxW, "build-width-invalid")) {
            return;
        }
        Material material = resolveMaterial(player, materialArg, "build.stairs.default-material", "oak_stairs");
        // Stair block materials are fine; also allow full blocks
        if (material == null) {
            return;
        }
        Location feet = player.getLocation().getBlock().getLocation();
        BlockFace facing = BuildShapes.yawToFace(player.getLocation().getYaw());
        Map<Long, PlannedBlock> planned = BuildShapes.stairs(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY() - 1,
                feet.getBlockZ(),
                facing,
                length,
                width,
                material.createBlockData()
        );
        apply(player, "stairs", planned, MessageManager.placeholders(
                "length", length,
                "width", width,
                "material", MaterialUtil.key(material),
                "tool", "stairs"
        ));
    }

    public void bridge(
            @NotNull Player player,
            @Nullable Integer lengthArg,
            @Nullable Integer widthArg,
            @Nullable String materialArg
    ) {
        int maxLength = cfgInt("build.bridge.max-length", "bridge.max-length", 128);
        int maxWidth = cfgInt("build.bridge.max-width", "bridge.max-width", 15);
        int length = lengthArg != null ? lengthArg : cfgInt("build.bridge.default-length", "bridge.default-length", 16);
        int width = widthArg != null ? widthArg : cfgInt("build.bridge.default-width", "bridge.default-width", 3);
        if (!inRange(player, length, 1, maxLength, "bridge-length-invalid")
                || !inRange(player, width, 1, maxWidth, "bridge-width-invalid")) {
            return;
        }
        String materialName = materialArg != null
                ? materialArg
                : firstString("build.bridge.default-material", "bridge.default-material", "oak_planks");
        Material deckMaterial = MaterialUtil.match(materialName);
        if (deckMaterial == null || !deckMaterial.isBlock() || deckMaterial.isAir()) {
            messages.send(player, "bridge-material-invalid", MessageManager.placeholders(
                    "input", materialName == null ? "" : materialName
            ));
            return;
        }
        boolean railings = plugin.getConfig().contains("build.bridge.railings")
                ? plugin.getConfig().getBoolean("build.bridge.railings")
                : plugin.getConfig().getBoolean("bridge.railings", true);
        Material railing = BuildShapes.resolveRailing(deckMaterial);
        Location feet = player.getLocation().getBlock().getLocation();
        BlockFace facing = BuildShapes.yawToFace(player.getLocation().getYaw());
        Map<Long, PlannedBlock> planned = BuildShapes.bridge(
                player.getWorld(),
                feet.getBlockX(),
                feet.getBlockY() - 1,
                feet.getBlockZ(),
                facing,
                length,
                width,
                deckMaterial.createBlockData(),
                railings,
                railing
        );
        apply(player, "bridge", planned, MessageManager.placeholders(
                "length", length,
                "width", width,
                "material", MaterialUtil.key(deckMaterial),
                "blocks", planned.size(),
                "tool", "bridge"
        ), true);
    }

    /**
     * Records an external edit (e.g. {@code /edit stack}) on the builder undo stack only
     * (caller already pushed Edit history) so {@code /build undo} can reverse it too.
     */
    public void recordExternalUndo(
            @NotNull UUID playerId,
            @NotNull UUID worldId,
            @NotNull String tool,
            @NotNull List<BlockSnapshot> before,
            @NotNull List<BlockSnapshot> after
    ) {
        int max = Math.max(1, cfgInt("build.max-undo", "bridge.max-undo", 10));
        Deque<BuildSession> stack = undoStacks.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        stack.push(new BuildSession(worldId, tool, before, after));
        while (stack.size() > max) {
            stack.removeLast();
        }
    }

    public void undo(@NotNull Player player) {
        if (active.containsKey(player.getUniqueId())) {
            messages.send(player, "build-busy");
            return;
        }
        Deque<BuildSession> stack = undoStacks.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            messages.send(player, "build-undo-none");
            return;
        }
        BuildSession session = stack.pop();
        World world = Bukkit.getWorld(session.worldId());
        if (world == null) {
            messages.send(player, "build-undo-world-missing");
            return;
        }
        if (session.before().isEmpty()) {
            messages.send(player, "build-undo-none");
            return;
        }
        messages.send(player, "build-undo-running", MessageManager.placeholders(
                "tool", session.tool(),
                "blocks", session.before().size()
        ));
        int perTick = Math.max(50, cfgInt("build.blocks-per-tick", "bridge.blocks-per-tick", 2000));
        BukkitTask task = BlockApplier.applySnapshotsChunked(
                plugin,
                world,
                session.before(),
                false,
                perTick,
                () -> {
                    active.remove(player.getUniqueId());
                    messages.send(player, "build-undo-done", MessageManager.placeholders(
                            "tool", session.tool(),
                            "blocks", session.before().size()
                    ));
                }
        );
        if (task != null) {
            active.put(player.getUniqueId(), task);
        }
    }

    private void apply(
            @NotNull Player player,
            @NotNull String tool,
            @NotNull Map<Long, PlannedBlock> planned,
            @NotNull Map<String, String> placeholders
    ) {
        apply(player, tool, planned, placeholders, false);
    }

    private void apply(
            @NotNull Player player,
            @NotNull String tool,
            @NotNull Map<Long, PlannedBlock> planned,
            @NotNull Map<String, String> placeholders,
            boolean bridgeMessages
    ) {
        if (active.containsKey(player.getUniqueId())) {
            messages.send(player, bridgeMessages ? "bridge-busy" : "build-busy");
            return;
        }
        if (planned.isEmpty()) {
            messages.send(player, bridgeMessages ? "bridge-nothing" : "build-nothing");
            return;
        }

        World world = player.getWorld();
        ProtectionService protection = plugin.getProtectionService();
        boolean bypass = protection.hasBypass(player);

        List<BlockSnapshot> before = new ArrayList<>(planned.size());
        List<BlockSnapshot> after = new ArrayList<>(planned.size());
        List<PlannedBlock> changes = new ArrayList<>(planned.size());

        for (PlannedBlock plan : planned.values()) {
            Block block = world.getBlockAt(plan.x(), plan.y(), plan.z());
            if (!bypass && (!protection.isAllowed(player, block.getLocation(), ProtectionFlag.BUILD)
                    || !protection.isAllowed(player, block.getLocation(), ProtectionFlag.PLACE))) {
                messages.send(player, bridgeMessages ? "bridge-protected-deny" : "build-protected-deny");
                return;
            }
            if (block.getBlockData().matches(plan.data())) {
                continue;
            }
            before.add(BlockSnapshot.from(block));
            after.add(new BlockSnapshot(plan.x(), plan.y(), plan.z(), plan.data()));
            changes.add(plan);
        }

        if (changes.isEmpty()) {
            messages.send(player, bridgeMessages ? "bridge-nothing" : "build-nothing");
            return;
        }

        Map<String, String> withBlocks = new java.util.HashMap<>(placeholders);
        withBlocks.put("blocks", String.valueOf(changes.size()));

        messages.send(player, bridgeMessages ? "bridge-building" : "build-working", withBlocks);

        int perTick = Math.max(50, cfgInt("build.blocks-per-tick", "bridge.blocks-per-tick", 2000));
        final int[] index = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                active.remove(player.getUniqueId());
                pushUndo(player.getUniqueId(), new BuildSession(world.getUID(), tool, before, after));
                return;
            }
            int budget = perTick;
            while (budget-- > 0 && index[0] < changes.size()) {
                PlannedBlock plan = changes.get(index[0]++);
                world.getBlockAt(plan.x(), plan.y(), plan.z()).setBlockData(plan.data(), false);
            }
            if (index[0] >= changes.size()) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                active.remove(player.getUniqueId());
                pushUndo(player.getUniqueId(), new BuildSession(world.getUID(), tool, before, after));
                messages.send(player, bridgeMessages ? "bridge-done" : "build-done", withBlocks);
                messages.send(player, bridgeMessages ? "bridge-undo-hint" : "build-undo-hint");
            }
        }, 1L, 1L);
        active.put(player.getUniqueId(), holder[0]);
    }

    private void pushUndo(@NotNull UUID playerId, @NotNull BuildSession session) {
        int max = Math.max(1, cfgInt("build.max-undo", "bridge.max-undo", 10));
        Deque<BuildSession> stack = undoStacks.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        stack.push(session);
        while (stack.size() > max) {
            stack.removeLast();
        }
        plugin.getEditService().recordHistory(
                playerId,
                new EditHistory.EditSession(session.before(), session.after())
        );
    }

    private boolean inRange(@NotNull Player player, int value, int min, int max, @NotNull String key) {
        if (value < min || value > max) {
            messages.send(player, key, MessageManager.placeholders("min", min, "max", max));
            return false;
        }
        return true;
    }

    private @Nullable Material resolveMaterial(
            @NotNull Player player,
            @Nullable String materialArg,
            @Nullable String configKey,
            @NotNull String fallback
    ) {
        String name = materialArg != null
                ? materialArg
                : (configKey != null ? plugin.getConfig().getString(configKey, fallback) : fallback);
        if (name == null || name.isBlank()) {
            name = fallback;
        }
        Material material = MaterialUtil.match(name);
        if (material == null || !material.isBlock() || material.isAir()) {
            messages.send(player, "build-material-invalid", MessageManager.placeholders("input", name));
            return null;
        }
        return material;
    }

    private int cfgInt(@NotNull String primary, @Nullable String fallback, int def) {
        if (plugin.getConfig().contains(primary)) {
            return plugin.getConfig().getInt(primary, def);
        }
        if (fallback != null && plugin.getConfig().contains(fallback)) {
            return plugin.getConfig().getInt(fallback, def);
        }
        return def;
    }

    private @Nullable String firstString(@NotNull String primary, @NotNull String fallback, @NotNull String def) {
        if (plugin.getConfig().contains(primary)) {
            return plugin.getConfig().getString(primary, def);
        }
        if (plugin.getConfig().contains(fallback)) {
            return plugin.getConfig().getString(fallback, def);
        }
        return def;
    }

    private record BuildSession(
            @NotNull UUID worldId,
            @NotNull String tool,
            @NotNull List<BlockSnapshot> before,
            @NotNull List<BlockSnapshot> after
    ) {
        private BuildSession {
            before = List.copyOf(before);
            after = List.copyOf(after);
        }
    }
}
