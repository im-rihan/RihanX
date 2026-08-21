package com.rihanx.base;

import com.rihanx.RihanX;
import com.rihanx.edit.BlockApplier;
import com.rihanx.edit.BlockSnapshot;
import com.rihanx.managers.MessageManager;
import com.rihanx.protection.FlagValue;
import com.rihanx.protection.ProtectionFlag;
import com.rihanx.protection.ProtectionService;
import com.rihanx.protection.Region;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
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
    private final @NotNull Map<UUID, ActivePaste> activePastes = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, Deque<PasteSession>> undoStacks = new ConcurrentHashMap<>();

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

    /**
     * Place absolute world blocks with undo (no teleport). Used for station rail links.
     *
     * @return false if the player is already building or placements were empty / denied
     */
    public boolean pasteAbsolute(
            @NotNull Player player,
            @NotNull String kind,
            @NotNull String name,
            @NotNull List<AbsolutePlacement> placements
    ) {
        if (placements.isEmpty()) {
            return false;
        }
        if (!building.add(player.getUniqueId())) {
            messages.send(player, "base-busy");
            return false;
        }
        try {
            return startAbsolutePaste(player, kind, name, placements);
        } catch (RuntimeException ex) {
            building.remove(player.getUniqueId());
            throw ex;
        }
    }

    /** World-space block for {@link #pasteAbsolute}. */
    public record AbsolutePlacement(int x, int y, int z, @NotNull Material material) {
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
        List<BlockSnapshot> before = snapshotBefore(world, changes, entrance);
        PasteSession session = new PasteSession(
                world.getUID(),
                blueprint.id(),
                kind,
                List.copyOf(before),
                null
        );

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
        messages.send(player, kindMessage(kind, "building"), MessageManager.placeholders(
                "name", blueprint.id(),
                "blocks", changes.size()
        ));

        final int[] index = {0};
        final int[] lastPct = {-1};
        ActivePaste active = new ActivePaste(session, wasFlying, allowFlight, mode);
        activePastes.put(player.getUniqueId(), active);
        active.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (active.cancelled || !player.isOnline()) {
                finishActive(player, active, true);
                return;
            }

            // Keep spawn pad clear so the player is never sealed mid-build.
            protectSpawnPad(world, entrance);

            int budget = perTick;
            while (budget-- > 0 && index[0] < changes.size()) {
                if (active.cancelled) {
                    finishActive(player, active, true);
                    return;
                }
                BlockChange change = changes.get(index[0]++);
                // Never overwrite the spawn pad (3×3 feet/head).
                if (isSpawnPadOccupied(change.block(), entrance)) {
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
                protectSpawnPad(world, entrance);
                activateBubbleLifts(world, changes);
                refreshRails(world, changes);
                activateFarmFluids(world, changes);
                activateFarmFluids(world, changes);
                String regionName = claimBaseRegion(player, world, blueprint, changes, entrance, kind);
                Location done = entranceLocation(origin, blueprint, facing, faceToYaw(opposite(facing)));
                player.teleport(done);
                // Replace session with region name for undo cleanup
                active.session = new PasteSession(
                        session.worldId(), session.name(), session.kind(), session.before(), regionName
                );
                finishActive(player, active, false);
                messages.send(player, kindMessage(kind, "done"), MessageManager.placeholders(
                        "name", blueprint.id(),
                        "blocks", changes.size()
                ));
                messages.send(player, kindMessage(kind, "ready-hint"));
                if (regionName != null && "base".equals(kind)) {
                    messages.send(player, "base-protected", MessageManager.placeholders("name", regionName));
                }
                if ("base".equals(kind) && blueprint.id().equals("secret")) {
                    // Snapshot fill after chests exist; retry once in case chunk TE was late
                    Runnable stock = () -> stockSecretStashChests(world, origin, facing);
                    plugin.getServer().getScheduler().runTaskLater(plugin, stock, 5L);
                    plugin.getServer().getScheduler().runTaskLater(plugin, stock, 40L);
                    messages.send(player, "base-secret-hint");
                }
                if ("farm".equals(kind) && blueprint.id().equals("kelp")) {
                    // North water sources need physics ticks to flow south over hoppers
                    startKelpWaterStream(world, origin, facing);
                }
                if ("farm".equals(kind) && Set.of("iron", "xp", "bamboo", "cane", "kelp", "wheat", "potato", "animal").contains(blueprint.id())) {
                    messages.send(player, "farm-" + blueprint.id() + "-hint");
                }
                if ("farm".equals(kind) && (blueprint.id().equals("wheat") || blueprint.id().equals("potato"))) {
                    spawnFarmerInPod(world, origin, facing);
                }
                if ("farm".equals(kind) && blueprint.id().equals("iron")) {
                    spawnIronFarmMobs(world, origin, facing);
                }
                if ("farm".equals(kind) && blueprint.id().equals("animal")) {
                    spawnAnimalFarmMobs(world, origin, facing);
                }
                messages.send(player, kindMessage(kind, "undo-hint"));
            }
        }, 1L, 1L);
    }

    /**
     * Primes the kelp top stream: north-edge water sources with physics so water
     * flows south over the hopper row (static full-source water never pushes items).
     */
    private void startKelpWaterStream(
            @NotNull World world,
            @NotNull Location origin,
            @NotNull BlockFace structureFacing
    ) {
        Runnable prime = () -> {
            for (int lx = -3; lx <= 3; lx++) {
                // Clear stream path to air first (local y=5, z=-2..5)
                for (int lz = -2; lz <= 5; lz++) {
                    int[] xz = rotate(lx, lz, structureFacing);
                    Block cell = world.getBlockAt(
                            origin.getBlockX() + xz[0],
                            origin.getBlockY() + 5,
                            origin.getBlockZ() + xz[1]
                    );
                    if (cell.getType() == Material.WATER || cell.getType() == Material.AIR
                            || cell.getType() == Material.GLASS) {
                        // don't wipe glass lid — only clear stream layer if not glass
                        if (cell.getType() != Material.GLASS) {
                            cell.setType(Material.AIR, false);
                        }
                    }
                }
                // Place north source with physics so it spreads south
                int[] north = rotate(lx, -3, structureFacing);
                Block src = world.getBlockAt(
                        origin.getBlockX() + north[0],
                        origin.getBlockY() + 5,
                        origin.getBlockZ() + north[1]
                );
                src.setType(Material.WATER, true);
            }
        };
        plugin.getServer().getScheduler().runTaskLater(plugin, prime, 5L);
        plugin.getServer().getScheduler().runTaskLater(plugin, prime, 25L);
    }

    /** Spawns a farmer villager inside the crop pod next to the bed (walk Y=1). */
    private void spawnFarmerInPod(
            @NotNull World world,
            @NotNull Location origin,
            @NotNull BlockFace structureFacing
    ) {
        // cropVillagerFarm size=4: bed foot at (-1,1,7), open cell beside at (0,1,7)
        int[] xz = rotate(0, 7, structureFacing);
        Location at = origin.clone().add(xz[0] + 0.5, 1.0, xz[1] + 0.5);
        Block feet = at.getBlock();
        // Don't destroy bed/composter — only clear air-ish occupancy
        if (!feet.getType().name().endsWith("_BED") && feet.getType() != Material.COMPOSTER) {
            if (feet.getType().isSolid()) {
                feet.setType(Material.AIR, false);
            }
        } else {
            // Shift one block toward door if we landed on furniture
            int[] alt = rotate(0, 6, structureFacing);
            at = origin.clone().add(alt[0] + 0.5, 1.0, alt[1] + 0.5);
            at.getBlock().setType(Material.AIR, false);
        }
        at.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
        Block under = at.getBlock().getRelative(0, -1, 0);
        if (!under.getType().isSolid()) {
            under.setType(Material.SMOOTH_STONE, false);
        }
        Location spawnAt = at;
        world.spawn(spawnAt, Villager.class, villager -> {
            villager.setProfession(Villager.Profession.FARMER);
            villager.setVillagerLevel(2);
            villager.setAdult();
            villager.setCanPickupItems(true);
            villager.setRemoveWhenFarAway(false);
            villager.customName(net.kyori.adventure.text.Component.text("Farm Helper"));
            villager.setCustomNameVisible(true);
        });
    }

    /**
     * Spawns 3 unemployed villagers per pod (claim farmer at composters) + nametag zombie.
     * Pods face the zombie through iron bars so they panic; golems spawn on the TOP deck.
     */
    private void spawnIronFarmMobs(
            @NotNull World world,
            @NotNull Location origin,
            @NotNull BlockFace structureFacing
    ) {
        List<Villager> spawned = new ArrayList<>();
        // Stand on side column at z=1,2,3 looking through iron bars at the center zombie
        for (int side : new int[]{-5, 5}) {
            for (int i = 0; i < 3; i++) {
                int spawnZ = 1 + i;
                int[] xz = rotate(side, spawnZ, structureFacing);
                Location at = origin.clone().add(xz[0] + 0.5, 9.0, xz[1] + 0.5);
                Block floor = at.getBlock().getRelative(0, -1, 0);
                if (!floor.getType().isSolid()) {
                    floor.setType(Material.STONE_BRICKS, false);
                }
                Material here = at.getBlock().getType();
                if (here != Material.AIR && !here.name().endsWith("_BED") && here != Material.COMPOSTER) {
                    at.getBlock().setType(Material.AIR, false);
                } else if (here.name().endsWith("_BED") || here == Material.COMPOSTER) {
                    // Stand one step toward the zombie (bridge column is bars — use side±0 clear)
                    at.getBlock().setType(Material.AIR, false);
                }
                at.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
                Location spawnAt = at;
                Villager villager = world.spawn(spawnAt, Villager.class, v -> {
                    v.setProfession(Villager.Profession.NONE);
                    v.setVillagerLevel(1);
                    v.setAdult();
                    v.setAI(true);
                    v.setAware(true);
                    v.setCanPickupItems(false);
                    v.setRemoveWhenFarAway(false);
                    v.setPersistent(true);
                    v.customName(net.kyori.adventure.text.Component.text("Iron Villager"));
                    v.setCustomNameVisible(false);
                });
                spawned.add(villager);
            }
        }

        // Force farmer after a short delay so composters are claimed even if pathfinding is slow
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Villager v : spawned) {
                if (!v.isValid()) {
                    continue;
                }
                if (v.getProfession() == Villager.Profession.NONE
                        || v.getProfession() == Villager.Profession.NITWIT) {
                    v.setProfession(Villager.Profession.FARMER);
                    v.setVillagerExperience(1);
                }
            }
        }, 40L);

        // Zombie in the center bars cage at local (0, 9, 2) — visible to both pods
        int[] zx = rotate(0, 2, structureFacing);
        Location zombieAt = origin.clone().add(zx[0] + 0.5, 9.0, zx[1] + 0.5);
        Block zUnder = zombieAt.getBlock().getRelative(0, -1, 0);
        if (!zUnder.getType().isSolid()) {
            zUnder.setType(Material.STONE_BRICKS, false);
        }
        zombieAt.getBlock().setType(Material.AIR, false);
        zombieAt.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
        world.spawn(zombieAt, org.bukkit.entity.Zombie.class, zombie -> {
            zombie.setRemoveWhenFarAway(false);
            zombie.setPersistent(true);
            zombie.setShouldBurnInDay(false);
            zombie.setAware(true);
            zombie.setAI(true);
            zombie.customName(net.kyori.adventure.text.Component.text("Iron Panic"));
            zombie.setCustomNameVisible(true);
            zombie.setAdult();
            zombie.setSilent(true);
            zombie.setCanPickupItems(false);
        });
    }

    /**
     * Fills secret vault chests with categorized kits.
     * Uses snapshot inventory + one {@code update()} — the Paper-safe pattern
     * (same as {@code KitService}). Filling live then calling update() wipes items.
     */
    private void stockSecretStashChests(
            @NotNull World world,
            @NotNull Location origin,
            @NotNull BlockFace structureFacing
    ) {
        int chestY = origin.getBlockY() + SecretBaseTemplates.CHEST_DY;
        List<SecretBaseTemplates.StashSpec> specs = SecretBaseTemplates.stashChestSpecs();
        int filled = 0;

        for (SecretBaseTemplates.StashSpec spec : specs) {
            int[] rightXz = rotate(spec.dx(), spec.dz(), structureFacing);
            int[] leftXz = rotate(spec.dx() - 1, spec.dz(), structureFacing);
            Block right = world.getBlockAt(
                    origin.getBlockX() + rightXz[0], chestY, origin.getBlockZ() + rightXz[1]);
            Block left = world.getBlockAt(
                    origin.getBlockX() + leftXz[0], chestY, origin.getBlockZ() + leftXz[1]);

            left = findOrPlaceChest(left, structureFacing);
            right = findOrPlaceChest(right, structureFacing);
            pairDoubleChest(left, right, structureFacing);

            String label = "Stash: " + Character.toUpperCase(spec.category().charAt(0))
                    + spec.category().substring(1);
            List<org.bukkit.inventory.ItemStack> items = spec.items();
            if (items.isEmpty()) {
                continue;
            }

            if (depositIntoChest(left, label, items)) {
                filled++;
            } else if (depositIntoChest(right, label, items)) {
                filled++;
            }
        }

        if (filled < specs.size()) {
            filled += stockOrphanVaultChests(world, origin, chestY, structureFacing, specs, filled);
        }

        if (filled == 0) {
            plugin.getLogger().warning("[RihanX] Secret vault: failed to stock any stash chests at y="
                    + chestY + " origin=" + origin.getBlockX() + "," + origin.getBlockY() + ","
                    + origin.getBlockZ());
        } else {
            plugin.getLogger().info("[RihanX] Secret vault: stocked " + filled + " stash chest groups.");
        }
    }

    private int stockOrphanVaultChests(
            @NotNull World world,
            @NotNull Location origin,
            int chestY,
            @NotNull BlockFace structureFacing,
            @NotNull List<SecretBaseTemplates.StashSpec> specs,
            int alreadyFilled
    ) {
        List<Block> empties = new ArrayList<>();
        int ox = origin.getBlockX();
        int oz = origin.getBlockZ();
        // Scan vault Y and ±1 in case paste origin / floor is slightly off
        for (int y = chestY - 1; y <= chestY + 1; y++) {
            for (int x = ox - 12; x <= ox + 12; x++) {
                for (int z = oz - 12; z <= oz + 12; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.CHEST) {
                        continue;
                    }
                    if (!(b.getState() instanceof org.bukkit.block.Chest chest)) {
                        continue;
                    }
                    boolean empty = true;
                    for (org.bukkit.inventory.ItemStack s : chest.getBlockInventory().getContents()) {
                        if (s != null && !s.getType().isAir()) {
                            empty = false;
                            break;
                        }
                    }
                    if (empty && empties.stream().noneMatch(e -> e.getX() == b.getX()
                            && e.getY() == b.getY() && e.getZ() == b.getZ())) {
                        empties.add(b);
                    }
                }
            }
        }
        empties.sort(Comparator
                .comparingInt(Block::getX)
                .thenComparingInt(Block::getZ));
        int added = 0;
        int specIdx = alreadyFilled;
        for (int i = 0; i < empties.size() && specIdx < specs.size(); i++) {
            SecretBaseTemplates.StashSpec spec = specs.get(specIdx);
            String label = "Stash: " + Character.toUpperCase(spec.category().charAt(0))
                    + spec.category().substring(1);
            if (depositIntoChest(empties.get(i), label, spec.items())) {
                added++;
                specIdx++;
                if (i + 1 < empties.size()) {
                    Block next = empties.get(i + 1);
                    Block cur = empties.get(i);
                    if (Math.abs(next.getX() - cur.getX()) + Math.abs(next.getZ() - cur.getZ()) == 1
                            && next.getY() == cur.getY()) {
                        i++; // skip twin half — already filled via double-chest inventory
                    }
                }
            }
        }
        return added;
    }

    private static @NotNull Block findOrPlaceChest(
            @NotNull Block expected,
            @NotNull BlockFace structureFacing
    ) {
        if (expected.getType() == Material.CHEST) {
            return expected;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block n = expected.getRelative(dx, 0, dz);
                if (n.getType() == Material.CHEST) {
                    return n;
                }
            }
        }
        expected.setType(Material.CHEST, false);
        if (expected.getBlockData() instanceof org.bukkit.block.data.type.Chest data) {
            data.setFacing(mapFacing(BlockFace.SOUTH, structureFacing));
            expected.setBlockData(data, false);
        }
        Block under = expected.getRelative(0, -1, 0);
        if (!under.getType().isSolid()) {
            under.setType(Material.POLISHED_DEEPSLATE, false);
        }
        return expected;
    }

    /** Force two adjacent chests into a double-chest pair (physics-off paste never links them). */
    private static void pairDoubleChest(
            @NotNull Block left,
            @NotNull Block right,
            @NotNull BlockFace structureFacing
    ) {
        BlockFace face = mapFacing(BlockFace.SOUTH, structureFacing);
        // Type is from the front: local dx-1 = LEFT half, dx = RIGHT half
        if (left.getBlockData() instanceof org.bukkit.block.data.type.Chest leftData) {
            leftData.setFacing(face);
            leftData.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
            left.setBlockData(leftData, false);
        }
        if (right.getBlockData() instanceof org.bukkit.block.data.type.Chest rightData) {
            rightData.setFacing(face);
            rightData.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
            right.setBlockData(rightData, false);
        }
    }



    private static boolean depositIntoChest(
            @NotNull Block block,
            @NotNull String label,
            @NotNull List<org.bukkit.inventory.ItemStack> items
    ) {
        if (block.getType() != Material.CHEST || items.isEmpty()) {
            return false;
        }
        if (!(block.getState() instanceof org.bukkit.block.Chest chest)) {
            return false;
        }
        chest.customName(net.kyori.adventure.text.Component.text(label));
        org.bukkit.inventory.Inventory inv = chest.getSnapshotInventory();
        inv.clear();
        int slot = 0;
        int placed = 0;
        for (org.bukkit.inventory.ItemStack stack : items) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            if (slot >= inv.getSize()) {
                break;
            }
            inv.setItem(slot++, stack.clone());
            placed++;
        }
        if (placed == 0) {
            return false;
        }
        chest.update(true, false);
        return true;
    }

    /**
     * Fills the 4 animal pens: cows, sheep, pigs, chickens (2 adults each so they can breed).
     */
    private void spawnAnimalFarmMobs(
            @NotNull World world,
            @NotNull Location origin,
            @NotNull BlockFace structureFacing
    ) {
        // Pen centers match FarmTemplates.animal() layout (local coords before rotation).
        record Pen(int lx, int lz, Class<? extends org.bukkit.entity.Animals> type, String label) {
        }
        List<Pen> pens = List.of(
                new Pen(-4, -4, org.bukkit.entity.Cow.class, "Cow Pen"),
                new Pen(4, -4, org.bukkit.entity.Sheep.class, "Sheep Pen"),
                new Pen(-4, 4, org.bukkit.entity.Pig.class, "Pig Pen"),
                new Pen(4, 4, org.bukkit.entity.Chicken.class, "Chicken Pen")
        );
        for (Pen pen : pens) {
            int[] xz = rotate(pen.lx(), pen.lz(), structureFacing);
            for (int i = 0; i < 2; i++) {
                double ox = (i == 0) ? 0.3 : -0.3;
                double oz = (i == 0) ? 0.3 : -0.3;
                Location at = origin.clone().add(xz[0] + 0.5 + ox, 1.0, xz[1] + 0.5 + oz);
                // Clear fence/hay if paste left something in the way
                Block feet = at.getBlock();
                if (feet.getType().isSolid() || feet.getType().name().contains("FENCE")) {
                    feet.setType(Material.AIR, false);
                }
                at.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
                Block under = feet.getRelative(0, -1, 0);
                if (!under.getType().isSolid()) {
                    under.setType(Material.GRASS_BLOCK, false);
                }
                String label = pen.label();
                world.spawn(at, pen.type(), animal -> {
                    animal.setAdult();
                    animal.setRemoveWhenFarAway(false);
                    animal.setCanPickupItems(false);
                    animal.customName(net.kyori.adventure.text.Component.text(label));
                    animal.setCustomNameVisible(false);
                    if (animal instanceof org.bukkit.entity.Ageable ageable) {
                        ageable.setAdult();
                    }
                });
            }
        }
    }

    /**
     * Re-apply water sources (and nudge observers) after silent paste so streams/redstone wake up.
     */
    private static void activateFarmFluids(@NotNull World world, @NotNull List<BlockChange> changes) {
        for (BlockChange change : changes) {
            Material planned = change.data().getMaterial();
            if (planned != Material.WATER) {
                continue;
            }
            Block block = change.block();
            BlockData water = Material.WATER.createBlockData();
            if (water instanceof Levelled levelled) {
                levelled.setLevel(0);
            }
            block.setBlockData(water, true);
        }
        for (BlockChange change : changes) {
            Material planned = change.data().getMaterial();
            if (planned != Material.OBSERVER && planned != Material.REDSTONE_WIRE
                    && planned != Material.PISTON && planned != Material.STICKY_PISTON) {
                continue;
            }
            Block block = change.block();
            BlockData data = change.data();
            block.setType(Material.AIR, false);
            block.setBlockData(data, true);
        }
    }

    private boolean startAbsolutePaste(
            @NotNull Player player,
            @NotNull String kind,
            @NotNull String name,
            @NotNull List<AbsolutePlacement> placements
    ) {
        World world = player.getWorld();
        ProtectionService protection = plugin.getProtectionService();
        boolean bypass = protection.hasBypass(player);

        // Last write wins per cell; higher layers (rails) overwrite if planned twice.
        Map<Long, AbsolutePlacement> unique = new LinkedHashMap<>(placements.size());
        for (AbsolutePlacement placement : placements) {
            if (placement.y() < world.getMinHeight() || placement.y() >= world.getMaxHeight()) {
                continue;
            }
            unique.put(pack(placement.x(), placement.y(), placement.z()), placement);
        }

        List<BlockChange> changes = new ArrayList<>(unique.size());
        for (AbsolutePlacement placement : unique.values()) {
            Block block = world.getBlockAt(placement.x(), placement.y(), placement.z());
            if (!bypass && (!protection.isAllowed(player, block.getLocation(), ProtectionFlag.BUILD)
                    || !protection.isAllowed(player, block.getLocation(), ProtectionFlag.PLACE))) {
                building.remove(player.getUniqueId());
                messages.send(player, "base-protected-deny");
                return false;
            }
            changes.add(new BlockChange(block, placement.material().createBlockData(), pastePriority(placement.material())));
        }
        if (changes.isEmpty()) {
            building.remove(player.getUniqueId());
            return false;
        }

        changes.sort(Comparator
                .comparingInt(BlockChange::priority)
                .thenComparingInt(c -> c.block().getY())
                .thenComparingInt(c -> c.block().getZ())
                .thenComparingInt(c -> c.block().getX()));

        List<BlockSnapshot> before = new ArrayList<>(changes.size());
        Map<Long, BlockSnapshot> seen = new LinkedHashMap<>(changes.size());
        for (BlockChange change : changes) {
            Block block = change.block();
            seen.putIfAbsent(pack(block.getX(), block.getY(), block.getZ()), BlockSnapshot.from(block));
        }
        before.addAll(seen.values());

        PasteSession session = new PasteSession(world.getUID(), name, kind, List.copyOf(before), null);
        int perTick = Math.max(50, plugin.getConfig().getInt("base.blocks-per-tick", 1200));
        messages.send(player, kindMessage(kind, "building"), MessageManager.placeholders(
                "name", name,
                "blocks", changes.size()
        ));

        final int[] index = {0};
        ActivePaste active = new ActivePaste(session, player.isFlying(), player.getAllowFlight(), player.getGameMode());
        activePastes.put(player.getUniqueId(), active);
        active.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (active.cancelled || !player.isOnline()) {
                finishActive(player, active, true);
                return;
            }
            int budget = perTick;
            while (budget-- > 0 && index[0] < changes.size()) {
                if (active.cancelled) {
                    finishActive(player, active, true);
                    return;
                }
                BlockChange change = changes.get(index[0]++);
                change.block().setBlockData(change.data(), false);
            }
            if (index[0] >= changes.size()) {
                activateBubbleLifts(world, changes);
                refreshRails(world, changes);
                activateFarmFluids(world, changes);
                finishActive(player, active, false);
                messages.send(player, kindMessage(kind, "done"), MessageManager.placeholders(
                        "name", name,
                        "blocks", changes.size()
                ));
                messages.send(player, kindMessage(kind, "undo-hint"));
            }
        }, 1L, 1L);
        return true;
    }

    /**
     * Restores terrain from the last base/farm paste for this player.
     */
    public void undo(@NotNull Player player) {
        UUID id = player.getUniqueId();
        ActivePaste active = activePastes.get(id);
        if (active != null) {
            active.cancelled = true;
            if (active.task != null) {
                active.task.cancel();
            }
            activePastes.remove(id, active);
            restoreFlight(player, active);
            building.remove(id);
            if (!building.add(id)) {
                pushUndo(id, active.session);
                messages.send(player, "base-busy");
                return;
            }
            applyUndo(player, active.session);
            return;
        }

        Deque<PasteSession> stack = undoStacks.get(id);
        if (stack == null || stack.isEmpty()) {
            messages.send(player, "base-undo-none");
            return;
        }
        if (!building.add(id)) {
            messages.send(player, "base-busy");
            return;
        }
        PasteSession session = stack.pop();
        applyUndo(player, session);
    }

    public boolean hasUndo(@NotNull Player player) {
        if (activePastes.containsKey(player.getUniqueId())) {
            return true;
        }
        Deque<PasteSession> stack = undoStacks.get(player.getUniqueId());
        return stack != null && !stack.isEmpty();
    }

    private void applyUndo(@NotNull Player player, @NotNull PasteSession session) {
        World world = Bukkit.getWorld(session.worldId());
        if (world == null) {
            building.remove(player.getUniqueId());
            messages.send(player, "base-undo-world-missing");
            return;
        }

        if (session.regionName() != null) {
            plugin.getProtectionService().getRegions().remove(world.getName(), session.regionName());
        }

        messages.send(player, "base-undo-running", MessageManager.placeholders(
                "name", session.name(),
                "blocks", session.before().size()
        ));

        int perTick = Math.max(50, plugin.getConfig().getInt("base.blocks-per-tick", 1200));
        BlockApplier.applySnapshotsChunked(
                plugin,
                world,
                session.before(),
                false,
                perTick,
                () -> {
                    building.remove(player.getUniqueId());
                    String doneKey = kindMessage(session.kind(), "undo-done");
                    messages.send(player, doneKey, MessageManager.placeholders(
                            "name", session.name(),
                            "blocks", session.before().size()
                    ));
                }
        );
    }

    private static @NotNull String kindMessage(@NotNull String kind, @NotNull String suffix) {
        String prefix = switch (kind) {
            case "farm" -> "farm";
            case "station" -> "station";
            default -> "base";
        };
        return prefix + "-" + suffix;
    }

    private void finishActive(@NotNull Player player, @NotNull ActivePaste active, boolean pushForPartial) {
        if (active.task != null) {
            active.task.cancel();
            active.task = null;
        }
        if (!activePastes.remove(player.getUniqueId(), active)) {
            return;
        }
        restoreFlight(player, active);
        building.remove(player.getUniqueId());
        if (pushForPartial || !active.cancelled) {
            pushUndo(player.getUniqueId(), active.session);
        }
    }

    private static void restoreFlight(@NotNull Player player, @NotNull ActivePaste active) {
        if (!player.isOnline()) {
            return;
        }
        if (active.mode != GameMode.CREATIVE && active.mode != GameMode.SPECTATOR) {
            player.setFlying(active.wasFlying);
            player.setAllowFlight(active.allowFlight);
        }
    }

    private void pushUndo(@NotNull UUID playerId, @NotNull PasteSession session) {
        int max = Math.max(1, plugin.getConfig().getInt("base.max-undo", 5));
        Deque<PasteSession> stack = undoStacks.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        stack.push(session);
        while (stack.size() > max) {
            stack.removeLast();
        }
    }

    private static @NotNull List<BlockSnapshot> snapshotBefore(
            @NotNull World world,
            @NotNull List<BlockChange> changes,
            @NotNull Location entrance
    ) {
        Map<Long, BlockSnapshot> unique = new LinkedHashMap<>(changes.size() + 32);
        for (BlockChange change : changes) {
            Block block = change.block();
            unique.putIfAbsent(pack(block.getX(), block.getY(), block.getZ()), BlockSnapshot.from(block));
        }
        int cx = entrance.getBlockX();
        int cy = entrance.getBlockY();
        int cz = entrance.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    unique.putIfAbsent(pack(block.getX(), block.getY(), block.getZ()), BlockSnapshot.from(block));
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** True when the block is in the protected 3×3 spawn pad (feet or head). */
    private static boolean isSpawnPadOccupied(@NotNull Block block, @NotNull Location entrance) {
        int cx = entrance.getBlockX();
        int cy = entrance.getBlockY();
        int cz = entrance.getBlockZ();
        int dx = Math.abs(block.getX() - cx);
        int dz = Math.abs(block.getZ() - cz);
        if (dx > 1 || dz > 1) {
            return false;
        }
        int y = block.getY();
        return y == cy || y == cy + 1;
    }

    /** Keep a 3×3 spawn pad clear with solid floor under every cell. */
    private static void protectSpawnPad(@NotNull World world, @NotNull Location entrance) {
        int cx = entrance.getBlockX();
        int cy = entrance.getBlockY();
        int cz = entrance.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                world.getBlockAt(x, cy, z).setType(Material.AIR, false);
                world.getBlockAt(x, cy + 1, z).setType(Material.AIR, false);
                Block floor = world.getBlockAt(x, cy - 1, z);
                if (!floor.getType().isSolid()) {
                    floor.setType(dx == 0 && dz == 0 ? Material.DIRT_PATH : Material.SMOOTH_STONE, false);
                }
            }
        }
    }

    /**
     * Re-place rails with physics so vanilla updates {@code RailShape} at curves / joins.
     * Silent paste leaves default north-south shapes, which look connected then fail after turns.
     */
    private static void refreshRails(@NotNull World world, @NotNull List<BlockChange> changes) {
        List<BlockChange> rails = new ArrayList<>();
        for (BlockChange change : changes) {
            if (change.data().getMaterial().name().endsWith("RAIL")) {
                rails.add(change);
            }
        }
        for (BlockChange change : rails) {
            change.block().setType(Material.AIR, false);
        }
        for (BlockChange change : rails) {
            change.block().setType(change.data().getMaterial(), true);
        }
        // Second pass after all neighbors exist so curves/joins finalize.
        for (BlockChange change : rails) {
            Block block = change.block();
            Material want = change.data().getMaterial();
            if (block.getType() != want) {
                block.setType(want, true);
            } else {
                block.setBlockData(block.getBlockData(), true);
            }
        }
    }

    /**
     * Re-apply water sources above soul sand / magma so bubble columns form after silent paste.
     * Critical on Bedrock (via Geyser) and when physics were suppressed during build.
     */
    private static void activateBubbleLifts(@NotNull World world, @NotNull List<BlockChange> changes) {
        for (BlockChange change : changes) {
            Material planned = change.data().getMaterial();
            if (planned != Material.SOUL_SAND && planned != Material.MAGMA_BLOCK) {
                continue;
            }
            Block base = change.block();
            if (base.getType() != planned) {
                base.setType(planned, false);
            }
            int x = base.getX();
            int z = base.getZ();
            for (int y = base.getY() + 1; y < world.getMaxHeight(); y++) {
                Block above = world.getBlockAt(x, y, z);
                Material type = above.getType();
                if (type != Material.WATER && type != Material.BUBBLE_COLUMN && type != Material.KELP
                        && type != Material.KELP_PLANT) {
                    break;
                }
                BlockData water = Material.WATER.createBlockData();
                if (water instanceof Levelled levelled) {
                    levelled.setLevel(0);
                }
                // physics=false so water does not spill out tiny seal gaps during refresh
                above.setBlockData(water, false);
            }
            Material keep = base.getType();
            base.setType(Material.STONE, false);
            base.setType(keep, true);
        }
        // Re-place planned glass around lifts so any spilled water is sealed again
        for (BlockChange change : changes) {
            if (change.data().getMaterial() != Material.GLASS) {
                continue;
            }
            Block block = change.block();
            if (block.getType() == Material.WATER || block.getType() == Material.BUBBLE_COLUMN
                    || block.getType() == Material.AIR) {
                // Only restore glass if a neighbor water column exists (lift shell)
                boolean nearWater = false;
                for (BlockFace face : new BlockFace[]{
                        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                        BlockFace.UP, BlockFace.DOWN
                }) {
                    Material n = block.getRelative(face).getType();
                    if (n == Material.WATER || n == Material.BUBBLE_COLUMN
                            || n == Material.SOUL_SAND || n == Material.MAGMA_BLOCK) {
                        nearWater = true;
                        break;
                    }
                }
                if (nearWater) {
                    block.setType(Material.GLASS, false);
                }
            } else if (block.getType() != Material.GLASS) {
                // Keep intentional glass
                block.setBlockData(change.data(), false);
            }
        }
    }

    /**
     * Auto-claim a protected region around a pasted base so strangers cannot break it.
     * Owner (builder) remains able to build via member bypass.
     */
    private @Nullable String claimBaseRegion(
            @NotNull Player player,
            @NotNull World world,
            @NotNull BaseTemplates.BaseBlueprint blueprint,
            @NotNull List<BlockChange> changes,
            @NotNull Location entrance,
            @NotNull String kind
    ) {
        if (!"base".equals(kind)) {
            return null;
        }
        if (!plugin.getConfig().getBoolean("base.auto-protect", true)) {
            return null;
        }
        ProtectionService protection = plugin.getProtectionService();
        if (!protection.isEnabled()) {
            return null;
        }
        if (changes.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockChange change : changes) {
            Block block = change.block();
            minX = Math.min(minX, block.getX());
            minY = Math.min(minY, block.getY());
            minZ = Math.min(minZ, block.getZ());
            maxX = Math.max(maxX, block.getX());
            maxY = Math.max(maxY, block.getY());
            maxZ = Math.max(maxZ, block.getZ());
        }
        int pad = Math.max(0, plugin.getConfig().getInt("base.protect-padding", 2));
        minX -= pad;
        minZ -= pad;
        maxX += pad;
        maxZ += pad;
        minY = Math.max(world.getMinHeight(), minY - 1);
        maxY = Math.min(world.getMaxHeight() - 1, maxY + 2);

        String baseName = ("base-" + blueprint.id() + "-" + entrance.getBlockX() + "-" + entrance.getBlockZ())
                .toLowerCase(Locale.ROOT);
        String name = baseName;
        int suffix = 2;
        while (protection.getRegions().get(world.getName(), name) != null) {
            name = baseName + "-" + suffix++;
        }
        if (protection.getRegions().count(world.getName()) >= protection.getMaxRegionsPerWorld()) {
            return null;
        }

        Region region = new Region(name, world.getName(), minX, minY, minZ, maxX, maxY, maxZ);
        region.addOwner(player.getUniqueId());
        region.setPriority(10);
        region.setFlag(ProtectionFlag.BUILD, FlagValue.DENY);
        region.setFlag(ProtectionFlag.BREAK, FlagValue.DENY);
        region.setFlag(ProtectionFlag.PLACE, FlagValue.DENY);
        // Keep creeper/other grief out; do not blanket-deny TNT so owners can still use it if world allows.
        region.setFlag(ProtectionFlag.CREEPER_EXPLOSION, FlagValue.DENY);
        region.setFlag(ProtectionFlag.OTHER_EXPLOSION, FlagValue.DENY);
        region.setFlag(ProtectionFlag.MOB_GRIEF, FlagValue.DENY);
        protection.getRegions().put(region);
        return name;
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
        if (material == Material.AIR) {
            return material.createBlockData();
        }
        // Still water/lava sources (level 0) — flowing levels get washed away by ticks
        if (material == Material.WATER || material == Material.LAVA) {
            BlockData fluid = material.createBlockData();
            if (fluid instanceof Levelled levelled) {
                levelled.setLevel(0);
            }
            return fluid;
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
        // Open iron trapdoors only (lava blades / XP landings). Wooden floor hatches stay closed.
        if (data instanceof org.bukkit.block.data.type.TrapDoor trapDoor && worldFacing != null) {
            if (trapDoor.getFaces().contains(worldFacing)) {
                trapDoor.setFacing(worldFacing);
            }
            if (material == Material.IRON_TRAPDOOR) {
                trapDoor.setOpen(true);
                trapDoor.setHalf(Bisected.Half.TOP);
            } else {
                trapDoor.setOpen(false);
                trapDoor.setHalf(Bisected.Half.BOTTOM);
            }
            return trapDoor;
        }
        // Buttons / levers need wall attachment + facing or they won't power doors
        if (worldFacing != null
                && (material.name().endsWith("_BUTTON") || material == Material.LEVER)
                && data instanceof org.bukkit.block.data.FaceAttachable attachable) {
            attachable.setAttachedFace(org.bukkit.block.data.FaceAttachable.AttachedFace.WALL);
            if (data instanceof Directional directional && directional.getFaces().contains(worldFacing)) {
                directional.setFacing(worldFacing);
            }
            return data;
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

    private record PasteSession(
            @NotNull UUID worldId,
            @NotNull String name,
            @NotNull String kind,
            @NotNull List<BlockSnapshot> before,
            @Nullable String regionName
    ) {
    }

    private static final class ActivePaste {
        @NotNull PasteSession session;
        final boolean wasFlying;
        final boolean allowFlight;
        final @NotNull GameMode mode;
        volatile boolean cancelled;
        @Nullable BukkitTask task;

        ActivePaste(
                @NotNull PasteSession session,
                boolean wasFlying,
                boolean allowFlight,
                @NotNull GameMode mode
        ) {
            this.session = session;
            this.wasFlying = wasFlying;
            this.allowFlight = allowFlight;
            this.mode = mode;
        }
    }
}
