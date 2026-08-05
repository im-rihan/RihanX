package com.rihanx.edit;

import com.rihanx.RihanX;
import com.rihanx.api.PermissionNodes;
import com.rihanx.managers.MessageManager;
import com.rihanx.protection.ProtectionFlag;
import com.rihanx.protection.ProtectionService;
import com.rihanx.utils.MaterialUtil;
import com.rihanx.utils.MessageUtil;
import com.rihanx.utils.PermissionUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * WorldEdit-lite cuboid editing with full protect checks and chunked applies.
 */
public final class EditService {

    public static final String WAND_NAME = "RihanX Edit Wand";

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull SelectionManager selections;
    private final @NotNull ProtectionService protection;
    private final @NotNull EditHistory history;
    private final @NotNull Map<UUID, Clipboard> clipboards = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, BukkitTask> activeEdits = new ConcurrentHashMap<>();

    public EditService(
            @NotNull RihanX plugin,
            @NotNull MessageManager messages,
            @NotNull SelectionManager selections,
            @NotNull ProtectionService protection
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.selections = selections;
        this.protection = protection;
        this.history = new EditHistory(plugin.getConfig().getInt("edit.max-undo", 10));
    }

    public int getMaxBlocks() {
        return plugin.getConfig().getInt("edit.max-blocks", 200_000);
    }

    public int getBlocksPerTick() {
        return plugin.getConfig().getInt("edit.blocks-per-tick", 4000);
    }

    public long getConfirmAbove() {
        return plugin.getConfig().getLong("edit.confirm-above", 50_000L);
    }

    public boolean usePhysics() {
        return plugin.getConfig().getBoolean("edit.physics", false);
    }

    public void giveWand(@NotNull Player player) {
        ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>" + WAND_NAME + "</gold>"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Left-click: pos1</gray>"),
                    MessageUtil.parse("<gray>Right-click: pos2</gray>")
            ));
            meta.getPersistentDataContainer().set(wandKey(), PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
        messages.send(player, "edit-wand-given");
    }

    public boolean isEditWand(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (meta.getPersistentDataContainer().has(wandKey(), PersistentDataType.BYTE)) {
            return true;
        }
        if (meta.hasDisplayName()) {
            String plain = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            return plain.contains(WAND_NAME);
        }
        return false;
    }

    private @NotNull NamespacedKey wandKey() {
        return new NamespacedKey(plugin, "edit_wand");
    }

    public void setPos(@NotNull Player player, int which, @NotNull Location location) {
        if (which == 1) {
            selections.setPos1(player, SelectionManager.Channel.EDIT, location);
            messages.send(player, "edit-pos1", MessageManager.placeholders(
                    "x", location.getBlockX(),
                    "y", location.getBlockY(),
                    "z", location.getBlockZ()
            ));
        } else {
            selections.setPos2(player, SelectionManager.Channel.EDIT, location);
            messages.send(player, "edit-pos2", MessageManager.placeholders(
                    "x", location.getBlockX(),
                    "y", location.getBlockY(),
                    "z", location.getBlockZ()
            ));
        }
        Cuboid cuboid = selections.getCuboid(player, SelectionManager.Channel.EDIT);
        if (cuboid != null) {
            messages.send(player, "edit-selection-size", MessageManager.placeholders("volume", cuboid.volume()));
        }
    }

    public @Nullable Cuboid requireSelection(@NotNull Player player) {
        Cuboid cuboid = selections.getCuboid(player, SelectionManager.Channel.EDIT);
        if (cuboid == null) {
            messages.send(player, "edit-selection-incomplete");
            return null;
        }
        if (cuboid.volume() > getMaxBlocks()) {
            messages.send(player, "edit-too-large", MessageManager.placeholders(
                    "volume", cuboid.volume(),
                    "max", getMaxBlocks()
            ));
            return null;
        }
        return cuboid;
    }

    public void sendSize(@NotNull Player player) {
        Cuboid cuboid = requireSelection(player);
        if (cuboid == null) {
            return;
        }
        messages.send(player, "edit-size", MessageManager.placeholders(
                "sx", cuboid.getMaxX() - cuboid.getMinX() + 1,
                "sy", cuboid.getMaxY() - cuboid.getMinY() + 1,
                "sz", cuboid.getMaxZ() - cuboid.getMinZ() + 1,
                "volume", cuboid.volume()
        ));
    }

    public void count(@NotNull Player player, @Nullable String materialName) {
        Cuboid cuboid = requireSelection(player);
        if (cuboid == null) {
            return;
        }
        Predicate<Block> matcher;
        String label;
        if (materialName == null || materialName.isBlank()) {
            matcher = block -> !block.getType().isAir();
            label = "non-air";
        } else {
            Material material = MaterialUtil.match(materialName);
            if (material == null) {
                messages.send(player, "item-material-invalid", MessageManager.placeholders("input", materialName));
                return;
            }
            matcher = block -> block.getType() == material;
            label = MaterialUtil.key(material);
        }
        long count = 0;
        for (Block block : cuboid) {
            if (matcher.test(block)) {
                count++;
            }
        }
        messages.send(player, "edit-count", MessageManager.placeholders("count", count, "material", label));
    }

    public void set(@NotNull Player player, @NotNull String materialName) {
        Cuboid cuboid = requireSelection(player);
        if (cuboid == null) {
            return;
        }
        Material material = MaterialUtil.match(materialName);
        if (material == null || !material.isBlock()) {
            messages.send(player, "item-material-invalid", MessageManager.placeholders("input", materialName));
            return;
        }
        if (!canEditRegion(player, cuboid)) {
            return;
        }
        runFill(player, cuboid, material.createBlockData(), block -> true, "set", MaterialUtil.key(material), null);
    }

    public void clear(@NotNull Player player) {
        set(player, "air");
    }

    public void replace(@NotNull Player player, @NotNull String fromName, @NotNull String toName) {
        Cuboid cuboid = requireSelection(player);
        if (cuboid == null) {
            return;
        }
        Material from = MaterialUtil.match(fromName);
        Material to = MaterialUtil.match(toName);
        if (from == null || to == null || !to.isBlock()) {
            messages.send(player, "item-material-invalid", MessageManager.placeholders(
                    "input", from == null ? fromName : toName
            ));
            return;
        }
        if (!canEditRegion(player, cuboid)) {
            return;
        }
        runFill(player, cuboid, to.createBlockData(), block -> block.getType() == from, "replace",
                MaterialUtil.key(from) + " → " + MaterialUtil.key(to), null);
    }

    public void walls(@NotNull Player player, @NotNull String materialName) {
        shape(player, materialName, "walls", (cuboid, x, y, z) -> cuboid.isWall(x, y, z));
    }

    public void outline(@NotNull Player player, @NotNull String materialName) {
        shape(player, materialName, "outline", (cuboid, x, y, z) -> {
            boolean onEdge = x == cuboid.getMinX() || x == cuboid.getMaxX()
                    || y == cuboid.getMinY() || y == cuboid.getMaxY()
                    || z == cuboid.getMinZ() || z == cuboid.getMaxZ();
            if (!onEdge) {
                return false;
            }
            int faces = 0;
            if (x == cuboid.getMinX() || x == cuboid.getMaxX()) faces++;
            if (y == cuboid.getMinY() || y == cuboid.getMaxY()) faces++;
            if (z == cuboid.getMinZ() || z == cuboid.getMaxZ()) faces++;
            return faces >= 2;
        });
    }

    public void hollow(@NotNull Player player, @NotNull String materialName) {
        shape(player, materialName, "hollow", (cuboid, x, y, z) -> cuboid.isHollowShell(x, y, z));
    }

    private void shape(
            @NotNull Player player,
            @NotNull String materialName,
            @NotNull String kind,
            @NotNull ShapePredicate predicate
    ) {
        Cuboid cuboid = requireSelection(player);
        if (cuboid == null) {
            return;
        }
        Material material = MaterialUtil.match(materialName);
        if (material == null || !material.isBlock()) {
            messages.send(player, "item-material-invalid", MessageManager.placeholders("input", materialName));
            return;
        }
        if (!canEditRegion(player, cuboid)) {
            return;
        }
        runFill(
                player,
                cuboid,
                material.createBlockData(),
                block -> predicate.test(cuboid, block.getX(), block.getY(), block.getZ()),
                kind,
                MaterialUtil.key(material),
                kind
        );
    }

    private void runFill(
            @NotNull Player player,
            @NotNull Cuboid cuboid,
            @NotNull BlockData data,
            @NotNull Predicate<Block> filter,
            @NotNull String mode,
            @NotNull String materialLabel,
            @Nullable String shapeKind
    ) {
        if (!beginEdit(player)) {
            return;
        }
        Predicate<Block> gated = block -> filter.test(block) && canEditBlock(player, block);
        messages.send(player, "edit-working", MessageManager.placeholders("volume", cuboid.volume()));
        BukkitTask task = BlockApplier.fillChunked(
                plugin,
                cuboid,
                data,
                gated,
                usePhysics(),
                getBlocksPerTick(),
                session -> {
                    activeEdits.remove(player.getUniqueId());
                    history.push(player.getUniqueId(), session);
                    if (shapeKind != null) {
                        messages.send(player, "edit-shape-done", MessageManager.placeholders(
                                "kind", shapeKind,
                                "count", session.after().size(),
                                "material", materialLabel
                        ));
                    } else if ("replace".equals(mode)) {
                        messages.send(player, "edit-replace-done", MessageManager.placeholders(
                                "count", session.after().size(),
                                "from", materialLabel.contains("→") ? materialLabel.split("→")[0].trim() : materialLabel,
                                "to", materialLabel.contains("→") ? materialLabel.split("→")[1].trim() : materialLabel
                        ));
                    } else {
                        messages.send(player, "edit-set-done", MessageManager.placeholders(
                                "count", session.after().size(),
                                "material", materialLabel
                        ));
                    }
                },
                scanned -> { /* progress optional */ }
        );
        if (task != null) {
            activeEdits.put(player.getUniqueId(), task);
        }
    }

    public void copy(@NotNull Player player) {
        Cuboid cuboid = requireSelection(player);
        if (cuboid == null) {
            return;
        }
        List<Clipboard.RelativeBlock> blocks = new ArrayList<>();
        int ox = cuboid.getMinX();
        int oy = cuboid.getMinY();
        int oz = cuboid.getMinZ();
        for (Block block : cuboid) {
            if (block.getType().isAir()) {
                continue;
            }
            blocks.add(new Clipboard.RelativeBlock(
                    block.getX() - ox,
                    block.getY() - oy,
                    block.getZ() - oz,
                    block.getBlockData()
            ));
        }
        Clipboard clipboard = new Clipboard(
                ox, oy, oz,
                cuboid.getMaxX() - cuboid.getMinX() + 1,
                cuboid.getMaxY() - cuboid.getMinY() + 1,
                cuboid.getMaxZ() - cuboid.getMinZ() + 1,
                blocks
        );
        if (clipboard.size() > getMaxBlocks()) {
            messages.send(player, "edit-too-large", MessageManager.placeholders(
                    "volume", clipboard.size(),
                    "max", getMaxBlocks()
            ));
            return;
        }
        clipboards.put(player.getUniqueId(), clipboard);
        messages.send(player, "edit-copy-done", MessageManager.placeholders("count", clipboard.size()));
    }

    public void paste(@NotNull Player player) {
        Clipboard clipboard = clipboards.get(player.getUniqueId());
        if (clipboard == null) {
            messages.send(player, "edit-clipboard-empty");
            return;
        }
        if (clipboard.size() > getMaxBlocks()) {
            messages.send(player, "edit-too-large", MessageManager.placeholders(
                    "volume", clipboard.size(),
                    "max", getMaxBlocks()
            ));
            return;
        }
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        if (!beginEdit(player)) {
            return;
        }
        int baseX = loc.getBlockX();
        int baseY = loc.getBlockY();
        int baseZ = loc.getBlockZ();
        messages.send(player, "edit-working", MessageManager.placeholders("volume", clipboard.size()));
        BukkitTask task = BlockApplier.pasteChunked(
                plugin,
                world,
                baseX,
                baseY,
                baseZ,
                clipboard,
                usePhysics(),
                (block, data) -> canEditBlock(player, block),
                getBlocksPerTick(),
                session -> {
                    activeEdits.remove(player.getUniqueId());
                    history.push(player.getUniqueId(), session);
                    messages.send(player, "edit-paste-done", MessageManager.placeholders("count", session.after().size()));
                }
        );
        if (task != null) {
            activeEdits.put(player.getUniqueId(), task);
        }
    }

    public void rotate(@NotNull Player player, int degrees) {
        Clipboard clipboard = clipboards.get(player.getUniqueId());
        if (clipboard == null) {
            messages.send(player, "edit-clipboard-empty");
            return;
        }
        if (degrees != 90 && degrees != 180 && degrees != 270) {
            messages.send(player, "invalid-argument", MessageManager.placeholders("input", String.valueOf(degrees)));
            return;
        }
        clipboards.put(player.getUniqueId(), clipboard.rotateY(degrees));
        messages.send(player, "edit-rotate-done", MessageManager.placeholders("degrees", degrees));
    }

    public void undo(@NotNull Player player) {
        EditHistory.EditSession session = history.popUndo(player.getUniqueId());
        if (session == null) {
            messages.send(player, "edit-undo-none");
            return;
        }
        if (!beginEdit(player)) {
            history.push(player.getUniqueId(), session);
            return;
        }
        World world = player.getWorld();
        BukkitTask task = BlockApplier.applySnapshotsChunked(
                plugin,
                world,
                session.before(),
                usePhysics(),
                getBlocksPerTick(),
                () -> {
                    activeEdits.remove(player.getUniqueId());
                    messages.send(player, "edit-undo-done", MessageManager.placeholders("count", session.before().size()));
                }
        );
        if (task != null) {
            activeEdits.put(player.getUniqueId(), task);
        }
    }

    public void redo(@NotNull Player player) {
        EditHistory.EditSession session = history.popRedo(player.getUniqueId());
        if (session == null) {
            messages.send(player, "edit-redo-none");
            return;
        }
        if (!beginEdit(player)) {
            return;
        }
        World world = player.getWorld();
        BukkitTask task = BlockApplier.applySnapshotsChunked(
                plugin,
                world,
                session.after(),
                usePhysics(),
                getBlocksPerTick(),
                () -> {
                    activeEdits.remove(player.getUniqueId());
                    messages.send(player, "edit-redo-done", MessageManager.placeholders("count", session.after().size()));
                }
        );
        if (task != null) {
            activeEdits.put(player.getUniqueId(), task);
        }
    }

    public void expand(@NotNull Player player, int amount, @NotNull String direction) {
        Cuboid cuboid = requireSelection(player);
        if (cuboid == null) {
            return;
        }
        int minX = cuboid.getMinX();
        int minY = cuboid.getMinY();
        int minZ = cuboid.getMinZ();
        int maxX = cuboid.getMaxX();
        int maxY = cuboid.getMaxY();
        int maxZ = cuboid.getMaxZ();
        String dir = direction.toLowerCase(java.util.Locale.ROOT);
        switch (dir) {
            case "up", "u" -> maxY += amount;
            case "down", "d" -> minY -= amount;
            case "north", "n" -> minZ -= amount;
            case "south", "s" -> maxZ += amount;
            case "east", "e" -> maxX += amount;
            case "west", "w" -> minX -= amount;
            case "vert", "vertical" -> {
                minY -= amount;
                maxY += amount;
            }
            case "all" -> {
                minX -= amount;
                maxX += amount;
                minY -= amount;
                maxY += amount;
                minZ -= amount;
                maxZ += amount;
            }
            default -> {
                messages.send(player, "invalid-argument", MessageManager.placeholders("input", direction));
                return;
            }
        }
        selections.setPos1(player, SelectionManager.Channel.EDIT,
                new Location(cuboid.getWorld(), minX, minY, minZ));
        selections.setPos2(player, SelectionManager.Channel.EDIT,
                new Location(cuboid.getWorld(), maxX, maxY, maxZ));
        Cuboid next = selections.getCuboid(player, SelectionManager.Channel.EDIT);
        messages.send(player, "edit-expand-done", MessageManager.placeholders(
                "amount", amount,
                "direction", dir,
                "volume", next == null ? 0 : next.volume()
        ));
    }

    public void contract(@NotNull Player player, int amount, @NotNull String direction) {
        expand(player, -Math.abs(amount), direction);
    }

    public void clearPlayer(@NotNull Player player) {
        cancelActive(player.getUniqueId());
        selections.clear(player, SelectionManager.Channel.EDIT);
        clipboards.remove(player.getUniqueId());
        history.clear(player.getUniqueId());
    }

    private boolean beginEdit(@NotNull Player player) {
        if (activeEdits.containsKey(player.getUniqueId())) {
            messages.send(player, "edit-busy");
            return false;
        }
        return true;
    }

    private void cancelActive(@NotNull UUID id) {
        BukkitTask task = activeEdits.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Full-volume protect check — every block in the cuboid must be editable.
     */
    private boolean canEditRegion(@NotNull Player player, @NotNull Cuboid cuboid) {
        if (protection.hasBypass(player) || PermissionUtil.hasOpOnly(player, PermissionNodes.PROTECT_BYPASS)) {
            return true;
        }
        for (Block block : cuboid) {
            if (!canEditBlock(player, block)) {
                messages.send(player, "edit-protected-deny");
                return false;
            }
        }
        return true;
    }

    private boolean canEditBlock(@NotNull Player player, @NotNull Block block) {
        if (protection.hasBypass(player) || PermissionUtil.hasOpOnly(player, PermissionNodes.PROTECT_BYPASS)) {
            return true;
        }
        return protection.isAllowed(player, block.getLocation(), ProtectionFlag.BUILD)
                && protection.isAllowed(player, block.getLocation(), ProtectionFlag.PLACE);
    }

    @FunctionalInterface
    private interface ShapePredicate {
        boolean test(@NotNull Cuboid cuboid, int x, int y, int z);
    }
}
