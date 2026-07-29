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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * WorldEdit-lite cuboid editing.
 */
public final class EditService {

    public static final String WAND_NAME = "RihanX Edit Wand";

    private final @NotNull RihanX plugin;
    private final @NotNull MessageManager messages;
    private final @NotNull SelectionManager selections;
    private final @NotNull ProtectionService protection;
    private final @NotNull EditHistory history;
    private final @NotNull Map<UUID, Clipboard> clipboards = new ConcurrentHashMap<>();

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
        BlockData data = material.createBlockData();
        EditHistory.EditSession session = BlockApplier.fill(cuboid, data, block -> true, usePhysics());
        history.push(player.getUniqueId(), session);
        messages.send(player, "edit-set-done", MessageManager.placeholders(
                "count", session.after().size(),
                "material", MaterialUtil.key(material)
        ));
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
        EditHistory.EditSession session = BlockApplier.replace(
                cuboid,
                block -> block.getType() == from,
                to.createBlockData(),
                usePhysics()
        );
        history.push(player.getUniqueId(), session);
        messages.send(player, "edit-replace-done", MessageManager.placeholders(
                "count", session.after().size(),
                "from", MaterialUtil.key(from),
                "to", MaterialUtil.key(to)
        ));
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
        BlockData data = material.createBlockData();
        EditHistory.EditSession session = BlockApplier.fill(
                cuboid,
                data,
                block -> predicate.test(cuboid, block.getX(), block.getY(), block.getZ()),
                usePhysics()
        );
        history.push(player.getUniqueId(), session);
        messages.send(player, "edit-shape-done", MessageManager.placeholders(
                "kind", kind,
                "count", session.after().size(),
                "material", MaterialUtil.key(material)
        ));
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
        int baseX = loc.getBlockX();
        int baseY = loc.getBlockY();
        int baseZ = loc.getBlockZ();
        Cuboid bounds = new Cuboid(
                world,
                baseX,
                baseY,
                baseZ,
                baseX + clipboard.getWidth() - 1,
                baseY + clipboard.getHeight() - 1,
                baseZ + clipboard.getLength() - 1
        );
        if (!canEditRegion(player, bounds)) {
            return;
        }
        EditHistory.EditSession session = BlockApplier.paste(
                world,
                baseX,
                baseY,
                baseZ,
                clipboard,
                usePhysics(),
                (block, data) -> canEditBlock(player, block)
        );
        history.push(player.getUniqueId(), session);
        messages.send(player, "edit-paste-done", MessageManager.placeholders("count", session.after().size()));
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
        World world = player.getWorld();
        BlockApplier.applySnapshots(world, session.before(), usePhysics());
        messages.send(player, "edit-undo-done", MessageManager.placeholders("count", session.before().size()));
    }

    public void redo(@NotNull Player player) {
        EditHistory.EditSession session = history.popRedo(player.getUniqueId());
        if (session == null) {
            messages.send(player, "edit-redo-none");
            return;
        }
        World world = player.getWorld();
        BlockApplier.applySnapshots(world, session.after(), usePhysics());
        messages.send(player, "edit-redo-done", MessageManager.placeholders("count", session.after().size()));
    }

    public void clearPlayer(@NotNull Player player) {
        selections.clear(player, SelectionManager.Channel.EDIT);
        clipboards.remove(player.getUniqueId());
        history.clear(player.getUniqueId());
    }

    private boolean canEditRegion(@NotNull Player player, @NotNull Cuboid cuboid) {
        if (protection.hasBypass(player) || PermissionUtil.hasOpOnly(player, PermissionNodes.PROTECT_BYPASS)) {
            return true;
        }
        // Sample corners + center; deny if any protected without membership
        Location[] samples = {
                new Location(cuboid.getWorld(), cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ()),
                new Location(cuboid.getWorld(), cuboid.getMaxX(), cuboid.getMaxY(), cuboid.getMaxZ()),
                cuboid.getCenter()
        };
        for (Location sample : samples) {
            if (!protection.isAllowed(player, sample, ProtectionFlag.BUILD)
                    || !protection.isAllowed(player, sample, ProtectionFlag.PLACE)) {
                messages.send(player, "edit-protected-deny");
                return false;
            }
        }
        return true;
    }

    private boolean canEditBlock(@NotNull Player player, @NotNull Block block) {
        if (protection.hasBypass(player)) {
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
