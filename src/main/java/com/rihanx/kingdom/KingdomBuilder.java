package com.rihanx.kingdom;

import com.rihanx.RihanX;
import com.rihanx.edit.BlockApplier;
import com.rihanx.edit.BlockSnapshot;
import com.rihanx.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Places Old Kingdom masonry and always records undo.
 */
public final class KingdomBuilder {

    private record WorldSession(
            @NotNull UUID worldId,
            @NotNull String tool,
            @NotNull String kingdomId,
            @NotNull List<BlockSnapshot> before
    ) {
    }

    private final @NotNull RihanX plugin;
    private final @NotNull KingdomService kingdoms;
    private final @NotNull Map<UUID, BukkitTask> busy = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, Deque<WorldSession>> undoWorld = new ConcurrentHashMap<>();
    private final @NotNull KingdomUndoStack undoMeta;

    public KingdomBuilder(@NotNull RihanX plugin, @NotNull KingdomService kingdoms) {
        this.plugin = plugin;
        this.kingdoms = kingdoms;
        this.undoMeta = new KingdomUndoStack(Math.max(1, plugin.getConfig().getInt("kingdom.towers.max-undo", 5)));
    }

    public @NotNull KingdomUndoStack undoStack() {
        return undoMeta;
    }

    public void buildTowers(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (busy.containsKey(player.getUniqueId())) {
            kingdoms.messages().send(player, "kingdom-busy");
            return;
        }
        World world = player.getWorld();
        kingdoms.ensureTowers(kingdom, world);
        List<KingdomStructurePlanner.Planned> plan = KingdomStructurePlanner.planKingdom(
                kingdom,
                kingdoms.settings().towerInset(),
                kingdoms.settings().tierSpacing(),
                kingdoms.settings().autoLightTiers(),
                world.getMaxHeight()
        );
        for (KingdomTower tower : kingdom.towers().values()) {
            tower.setBuilt(true);
        }
        kingdom.setTowersBuilt(true);
        KingdomTower central = kingdom.tower(TowerType.CENTRAL);
        if (central != null && !kingdom.hasTreasury()) {
            kingdom.setTreasury(central.x(), central.groundY() + 1, central.z() + 2, world.getName());
        }
        kingdoms.store().put(kingdom);
        apply(player, "towers", kingdom.id(), plan, () -> {
            kingdoms.messages().send(
                    player,
                    "kingdom-towers-built",
                    MessageManager.placeholders("name", kingdom.displayName(), "count", kingdom.towers().size())
            );
            kingdoms.messages().send(player, "kingdom-undo-hint");
        });
    }

    public void placeGateLanterns(@NotNull Player player, @NotNull World world, @NotNull KingdomGate gate, @NotNull Kingdom kingdom) {
        if (!gate.lanterns()) {
            return;
        }
        String banner = kingdom.bannerMaterial() == null ? "ORANGE_BANNER" : kingdom.bannerMaterial();
        List<KingdomStructurePlanner.Planned> plan = KingdomStructurePlanner.planGateLanterns(gate, banner);
        apply(player, "gate", kingdom.id(), plan, () -> {
        });
    }

    public void placeGateLanterns(@NotNull World world, @NotNull KingdomGate gate, @NotNull Kingdom kingdom) {
        String banner = kingdom.bannerMaterial() == null ? "ORANGE_BANNER" : kingdom.bannerMaterial();
        for (KingdomStructurePlanner.Planned planned : KingdomStructurePlanner.planGateLanterns(gate, banner)) {
            BlockData data = toBlockData(planned.material(), gate.side());
            safeSet(world, planned.x(), planned.y(), planned.z(), data);
        }
    }

    public void drain(@NotNull Player player, @NotNull Kingdom kingdom) {
        if (busy.containsKey(player.getUniqueId())) {
            kingdoms.messages().send(player, "kingdom-busy");
            return;
        }
        World world = player.getWorld();
        if (!world.getName().equals(kingdom.bounds().worldName())) {
            kingdoms.messages().send(player, "kingdom-wrong-world");
            return;
        }
        KingdomBounds bounds = kingdom.bounds();
        List<KingdomStructurePlanner.Planned> plan = new ArrayList<>();
        int minY = Math.max(world.getMinHeight(), bounds.minY());
        int maxY = Math.min(world.getMaxHeight() - 1, bounds.maxY());
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.WATER || type == Material.LAVA
                            || type == Material.BUBBLE_COLUMN
                            || type.name().contains("CAULDRON")) {
                        kingdoms.recordFluid(block, type);
                        plan.add(new KingdomStructurePlanner.Planned(x, y, z, "AIR"));
                    }
                }
            }
        }
        if (plan.isEmpty()) {
            kingdoms.messages().send(player, "kingdom-drain-none");
            return;
        }
        apply(player, "drain", kingdom.id(), plan, () -> {
            kingdoms.messages().send(
                    player,
                    "kingdom-drain-done",
                    MessageManager.placeholders("count", plan.size())
            );
            kingdoms.messages().send(player, "kingdom-undo-hint");
        });
    }

    public void undo(@NotNull Player player) {
        if (busy.containsKey(player.getUniqueId())) {
            kingdoms.messages().send(player, "kingdom-busy");
            return;
        }
        Deque<WorldSession> stack = undoWorld.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            undoMeta.pop(player.getUniqueId());
            kingdoms.messages().send(player, "kingdom-undo-none");
            return;
        }
        WorldSession session = stack.pop();
        undoMeta.pop(player.getUniqueId());
        World world = Bukkit.getWorld(session.worldId());
        if (world == null) {
            kingdoms.messages().send(player, "kingdom-undo-world-missing");
            return;
        }
        if (session.before().isEmpty()) {
            kingdoms.messages().send(player, "kingdom-undo-none");
            return;
        }
        kingdoms.messages().send(player, "kingdom-undo-running", MessageManager.placeholders(
                "tool", session.tool(),
                "blocks", session.before().size()
        ));
        int perTick = kingdoms.settings().buildBlocksPerTick();
        BukkitTask task = BlockApplier.applySnapshotsChunked(
                plugin,
                world,
                session.before(),
                false,
                perTick,
                () -> {
                    busy.remove(player.getUniqueId());
                    if (session.tool().equals("towers")) {
                        Kingdom kingdom = kingdoms.byId(session.kingdomId());
                        if (kingdom != null) {
                            kingdom.setTowersBuilt(false);
                            for (KingdomTower tower : kingdom.towers().values()) {
                                tower.setBuilt(false);
                            }
                            kingdoms.store().put(kingdom);
                        }
                    }
                    kingdoms.messages().send(player, "kingdom-undo-done", MessageManager.placeholders(
                            "tool", session.tool(),
                            "blocks", session.before().size()
                    ));
                }
        );
        if (task != null) {
            busy.put(player.getUniqueId(), task);
        }
    }

    public boolean hasUndo(@NotNull Player player) {
        Deque<WorldSession> stack = undoWorld.get(player.getUniqueId());
        return stack != null && !stack.isEmpty();
    }

    private void apply(
            @NotNull Player player,
            @NotNull String tool,
            @NotNull String kingdomId,
            @NotNull List<KingdomStructurePlanner.Planned> plan,
            @NotNull Runnable onDone
    ) {
        if (plan.isEmpty()) {
            onDone.run();
            return;
        }
        kingdoms.messages().send(player, "kingdom-building", MessageManager.placeholders(
                "tool", tool,
                "blocks", plan.size()
        ));
        World world = player.getWorld();
        List<BlockSnapshot> before = new ArrayList<>(plan.size());
        List<KingdomUndoStack.Snapshot> meta = new ArrayList<>(plan.size());
        List<KingdomStructurePlanner.Planned> changes = new ArrayList<>(plan.size());
        for (KingdomStructurePlanner.Planned next : plan) {
            if (next.y() < world.getMinHeight() || next.y() >= world.getMaxHeight()) {
                continue;
            }
            Block block = world.getBlockAt(next.x(), next.y(), next.z());
            BlockData want = toBlockData(next.material(), GateSide.NORTH);
            if (block.getBlockData().matches(want)) {
                continue;
            }
            before.add(BlockSnapshot.from(block));
            meta.add(new KingdomUndoStack.Snapshot(next.x(), next.y(), next.z(), block.getType().name()));
            changes.add(next);
        }
        if (changes.isEmpty()) {
            onDone.run();
            return;
        }

        int perTick = kingdoms.settings().buildBlocksPerTick();
        final int[] index = {0};
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int budget = perTick;
            while (budget-- > 0 && index[0] < changes.size()) {
                KingdomStructurePlanner.Planned next = changes.get(index[0]++);
                world.getBlockAt(next.x(), next.y(), next.z()).setBlockData(toBlockData(next.material(), GateSide.NORTH), false);
            }
            if (index[0] >= changes.size()) {
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                busy.remove(player.getUniqueId());
                pushUndo(player.getUniqueId(), new WorldSession(world.getUID(), tool, kingdomId, before));
                undoMeta.push(player.getUniqueId(), new KingdomUndoStack.Session(
                        tool,
                        kingdomId,
                        world.getUID(),
                        meta
                ));
                onDone.run();
            }
        }, 1L, 1L);
        busy.put(player.getUniqueId(), holder[0]);
    }

    private void pushUndo(@NotNull UUID playerId, @NotNull WorldSession session) {
        int max = Math.max(1, plugin.getConfig().getInt("kingdom.towers.max-undo", 5));
        Deque<WorldSession> stack = undoWorld.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        stack.push(session);
        while (stack.size() > max) {
            stack.removeLast();
        }
    }

    private @NotNull BlockData toBlockData(@NotNull String materialName, @NotNull GateSide side) {
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException ex) {
            material = Material.STONE_BRICKS;
        }
        BlockData data = material.createBlockData();
        if (material == Material.LADDER && data instanceof Directional directional) {
            directional.setFacing(BlockFace.SOUTH);
            return directional;
        }
        if (material.name().endsWith("BANNER") && data instanceof Directional directional) {
            directional.setFacing(toFace(side));
            return directional;
        }
        return data;
    }

    private void safeSet(@NotNull World world, int x, int y, int z, @NotNull BlockData data) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return;
        }
        world.getBlockAt(x, y, z).setBlockData(data, false);
    }

    private @NotNull BlockFace toFace(@NotNull GateSide side) {
        return switch (side) {
            case NORTH -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.NORTH;
            case WEST -> BlockFace.EAST;
            case EAST -> BlockFace.WEST;
            case ABOVE, BELOW -> BlockFace.NORTH;
        };
    }
}
