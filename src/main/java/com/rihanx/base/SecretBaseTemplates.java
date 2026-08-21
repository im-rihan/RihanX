package com.rihanx.base;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Secret safehouse / bunker — cottage on top, cellar hatch to vaults with stocked chests.
 */
public final class SecretBaseTemplates {

    /** Local Y of stash chests (must match {@link #secret()} vault floor + 1). */
    public static final int CHEST_DY = -7;

    private SecretBaseTemplates() {
    }

    /**
     * Cozy spruce cottage. Bedroom closet door → open floor hatch → ladder → vault
     * with 4 stash rooms (wooden doors) and labeled stocked chests.
     */
    public static @NotNull BaseTemplates.BaseBlueprint secret() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();
        int minX = -5;
        int maxX = 5;
        int minZ = -5;
        int maxZ = 4;
        int wallH = 3;
        int vaultY = -8;

        // ——— Surface cottage ———
        fill(b, minX - 1, maxX + 1, minZ - 1, maxZ + 1, -1, Material.COBBLESTONE);
        fill(b, minX, maxX, minZ, maxZ, 0, Material.SPRUCE_PLANKS);
        clear(b, minX + 1, maxX - 1, minZ + 1, maxZ - 1, 1, wallH);
        shell(b, minX, maxX, minZ, maxZ, 1, wallH, Material.SPRUCE_PLANKS, Material.SPRUCE_LOG);

        LuxuryBaseTemplates.gabledRoof(
                b, minX, maxX, minZ, maxZ, wallH + 1,
                Material.DARK_OAK_STAIRS, Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_SLAB, Material.SPRUCE_PLANKS
        );

        b.doubleDoor(-1, 1, maxZ, Material.SPRUCE_DOOR, BlockFace.SOUTH);
        for (int x = -3; x <= 3; x += 2) {
            if (x == -1 || x == 0) {
                continue;
            }
            b.set(x, 2, minZ, Material.GLASS_PANE);
            b.set(x, 2, maxZ, Material.GLASS_PANE);
        }
        b.set(minX, 2, 0, Material.GLASS_PANE);
        b.set(maxX, 2, 0, Material.GLASS_PANE);

        // Living room
        b.set(-3, 1, 2, Material.CRAFTING_TABLE);
        b.facing(-4, 1, 2, Material.FURNACE, BlockFace.EAST);
        b.set(3, 1, 2, Material.BOOKSHELF);
        b.set(4, 1, 2, Material.BOOKSHELF);
        b.set(0, 1, -3, Material.BARREL);
        b.stairs(-2, 1, 1, Material.SPRUCE_STAIRS, BlockFace.SOUTH);
        b.stairs(2, 1, 1, Material.SPRUCE_STAIRS, BlockFace.SOUTH);

        // Bedroom (west)
        b.bed(-3, 1, -2, Material.RED_BED, BlockFace.EAST);
        b.set(-4, 1, -1, Material.LANTERN);
        b.set(-2, 1, -3, Material.CHEST);

        // ——— Cellar closet (east of bed): door in, open hatch, ladder down ———
        // Closet walls
        for (int y = 1; y <= 3; y++) {
            b.set(1, y, -4, Material.SPRUCE_PLANKS);
            b.set(3, y, -4, Material.SPRUCE_PLANKS);
            b.set(1, y, -1, Material.SPRUCE_PLANKS);
            b.set(3, y, -1, Material.SPRUCE_PLANKS);
            b.set(3, y, -3, Material.SPRUCE_PLANKS);
            b.set(3, y, -2, Material.SPRUCE_PLANKS);
        }
        // Doorway from bedroom into closet (west face of closet)
        b.set(1, 1, -3, Material.AIR);
        b.set(1, 2, -3, Material.AIR);
        b.door(1, 1, -3, Material.SPRUCE_DOOR, BlockFace.WEST);
        // Closet interior clear
        b.set(2, 1, -3, Material.AIR);
        b.set(2, 2, -3, Material.AIR);
        b.set(2, 1, -2, Material.AIR);
        b.set(2, 2, -2, Material.AIR);
        // Open floor hatch (no carpet on top — must be usable)
        b.set(2, 0, -2, Material.SPRUCE_TRAPDOOR);
        b.set(2, 1, -2, Material.AIR);
        b.facing(2, 2, -3, Material.OAK_WALL_SIGN, BlockFace.NORTH);

        // ——— Underground vault shell ———
        int vMinX = -8;
        int vMaxX = 8;
        int vMinZ = -8;
        int vMaxZ = 4;

        fill(b, vMinX, vMaxX, vMinZ, vMaxZ, vaultY - 1, Material.DEEPSLATE_BRICKS);
        fill(b, vMinX, vMaxX, vMinZ, vMaxZ, vaultY, Material.POLISHED_DEEPSLATE);
        clear(b, vMinX + 1, vMaxX - 1, vMinZ + 1, vMaxZ - 1, vaultY + 1, vaultY + 4);
        shell(b, vMinX, vMaxX, vMinZ, vMaxZ, vaultY + 1, vaultY + 4,
                Material.DEEPSLATE_BRICKS, Material.DEEPSLATE_TILES);
        fill(b, vMinX, vMaxX, vMinZ, vMaxZ, vaultY + 5, Material.DEEPSLATE_BRICKS);

        // Partition walls → 4 stash rooms around a center hall
        for (int y = vaultY + 1; y <= vaultY + 4; y++) {
            for (int z = vMinZ + 1; z <= vMaxZ - 1; z++) {
                if (z >= -2 && z <= 0) {
                    continue;
                }
                b.set(-3, y, z, Material.DEEPSLATE_BRICKS);
                b.set(3, y, z, Material.DEEPSLATE_BRICKS);
            }
            for (int x = vMinX + 1; x <= vMaxX - 1; x++) {
                if (x >= -2 && x <= 2) {
                    continue;
                }
                b.set(x, y, -3, Material.DEEPSLATE_BRICKS);
                b.set(x, y, 0, Material.DEEPSLATE_BRICKS);
            }
        }

        // Room doorways with wooden doors (no buttons — always openable)
        // NW room (tools/weapons/armor): door in south wall at z=-3
        placeRoomDoor(b, -5, vaultY + 1, -3, BlockFace.SOUTH);
        // NE room (food/farm/potions)
        placeRoomDoor(b, 5, vaultY + 1, -3, BlockFace.SOUTH);
        // SW room (blocks/redstone): door in north wall at z=0
        placeRoomDoor(b, -5, vaultY + 1, 0, BlockFace.NORTH);
        // SE room (ores/nether)
        placeRoomDoor(b, 5, vaultY + 1, 0, BlockFace.NORTH);

        // Center hall workstations
        b.set(0, vaultY + 1, 0, Material.CRAFTING_TABLE);
        b.set(1, vaultY + 1, 0, Material.SMITHING_TABLE);
        b.set(-1, vaultY + 1, 0, Material.ANVIL);
        b.set(0, vaultY + 1, 1, Material.ENCHANTING_TABLE);
        b.set(-1, vaultY + 1, 1, Material.BOOKSHELF);
        b.set(1, vaultY + 1, 1, Material.BOOKSHELF);
        b.set(0, vaultY + 1, -1, Material.BREWING_STAND);
        b.set(2, vaultY + 1, -1, Material.ENDER_CHEST);
        b.set(-2, vaultY + 1, -1, Material.CARTOGRAPHY_TABLE);

        for (int x = -6; x <= 6; x += 3) {
            for (int z = -6; z <= 2; z += 3) {
                b.set(x, vaultY + 4, z, Material.SEA_LANTERN);
            }
        }
        b.set(-5, vaultY + 4, -5, Material.SEA_LANTERN);
        b.set(5, vaultY + 4, -5, Material.SEA_LANTERN);
        b.set(-5, vaultY + 4, 2, Material.SEA_LANTERN);
        b.set(5, vaultY + 4, 2, Material.SEA_LANTERN);

        // Chests INSIDE rooms (not on door tiles)
        placeStashChests(b, CHEST_DY);

        // ——— Ladder shaft LAST so vault roof / floors never seal it ———
        // Hatch column local XZ = (2, -2)
        for (int y = -1; y >= vaultY; y--) {
            b.set(2, y, -2, Material.AIR);
            b.set(2, y, -3, Material.STONE_BRICKS);
            b.set(2, y, -1, Material.STONE_BRICKS);
            b.set(1, y, -2, Material.STONE_BRICKS);
            b.set(3, y, -2, Material.STONE_BRICKS);
            // Ladder on west wall of shaft, facing EAST into the climb space
            b.facing(1, y, -2, Material.LADDER, BlockFace.EAST);
        }
        b.set(2, vaultY, -2, Material.POLISHED_DEEPSLATE); // land on vault floor
        b.set(2, vaultY + 1, -2, Material.AIR);
        b.set(2, vaultY + 2, -2, Material.AIR);
        b.set(2, vaultY + 3, -2, Material.AIR);
        b.facing(1, vaultY + 1, -2, Material.LADDER, BlockFace.EAST);
        b.facing(1, vaultY + 2, -2, Material.LADDER, BlockFace.EAST);
        b.set(2, vaultY + 3, -2, Material.SEA_LANTERN);
        // Re-assert surface hatch
        b.set(2, 0, -2, Material.SPRUCE_TRAPDOOR);
        b.set(2, 1, -2, Material.AIR);

        // Front path
        b.set(0, 0, maxZ + 1, Material.COBBLESTONE_SLAB);
        b.set(0, 0, maxZ + 2, Material.DIRT_PATH);
        b.set(-1, 0, maxZ + 2, Material.DIRT_PATH);
        b.set(1, 0, maxZ + 2, Material.DIRT_PATH);

        return b.build(
                "secret",
                "Secret safehouse - closet hatch to vault, stocked category chests",
                0, 0, maxZ + 2
        );
    }

    /** Wooden door in a partition opening (2-high air + door). */
    private static void placeRoomDoor(
            @NotNull BaseTemplates.Builder b,
            int x,
            int y,
            int z,
            @NotNull BlockFace facing
    ) {
        b.set(x, y, z, Material.AIR);
        b.set(x, y + 1, z, Material.AIR);
        b.door(x, y, z, Material.SPRUCE_DOOR, facing);
    }

    private static void placeStashChests(@NotNull BaseTemplates.Builder b, int y) {
        for (StashSpec spec : stashChestSpecsLazy()) {
            int dx = spec.dx();
            int dz = spec.dz();
            b.facing(dx, y, dz, Material.CHEST, BlockFace.SOUTH);
            b.facing(dx - 1, y, dz, Material.CHEST, BlockFace.SOUTH);
        }
    }

    /**
     * Positions only (no ItemStacks) — safe during unit tests.
     */
    private static @NotNull List<StashSpec> stashChestSpecsLazy() {
        List<StashSpec> list = new ArrayList<>();
        list.add(new StashSpec(-6, -6, "tools", List.of()));
        list.add(new StashSpec(-6, -5, "weapons", List.of()));
        list.add(new StashSpec(-6, -4, "armor", List.of()));
        list.add(new StashSpec(6, -6, "food", List.of()));
        list.add(new StashSpec(6, -5, "farm", List.of()));
        list.add(new StashSpec(6, -4, "potions", List.of()));
        list.add(new StashSpec(-6, 2, "blocks", List.of()));
        list.add(new StashSpec(-6, 3, "redstone", List.of()));
        list.add(new StashSpec(6, 2, "ores", List.of()));
        list.add(new StashSpec(6, 3, "nether", List.of()));
        return list;
    }

    /**
     * Full kits — call only on a live server (creates ItemStacks).
     */
    public static @NotNull List<StashSpec> stashChestSpecs() {
        List<StashSpec> list = new ArrayList<>();
        list.add(new StashSpec(-6, -6, "tools", kitsTools()));
        list.add(new StashSpec(-6, -5, "weapons", kitsWeapons()));
        list.add(new StashSpec(-6, -4, "armor", kitsArmor()));
        list.add(new StashSpec(6, -6, "food", kitsFood()));
        list.add(new StashSpec(6, -5, "farm", kitsFarm()));
        list.add(new StashSpec(6, -4, "potions", kitsPotions()));
        list.add(new StashSpec(-6, 2, "blocks", kitsBlocks()));
        list.add(new StashSpec(-6, 3, "redstone", kitsRedstone()));
        list.add(new StashSpec(6, 2, "ores", kitsOres()));
        list.add(new StashSpec(6, 3, "nether", kitsNether()));
        return list;
    }

    public record StashSpec(int dx, int dz, @NotNull String category, @NotNull List<ItemStack> items) {
    }

    private static @NotNull ItemStack stack(@NotNull Material material, int amount) {
        return new ItemStack(material, amount);
    }

    private static @NotNull List<ItemStack> kitsTools() {
        return List.of(
                stack(Material.NETHERITE_PICKAXE, 1),
                stack(Material.NETHERITE_AXE, 1),
                stack(Material.NETHERITE_SHOVEL, 1),
                stack(Material.NETHERITE_HOE, 1),
                stack(Material.SHEARS, 1),
                stack(Material.FLINT_AND_STEEL, 1),
                stack(Material.BUCKET, 3),
                stack(Material.WATER_BUCKET, 2),
                stack(Material.LAVA_BUCKET, 1),
                stack(Material.CLOCK, 1),
                stack(Material.COMPASS, 1),
                stack(Material.RECOVERY_COMPASS, 1),
                stack(Material.SPYGLASS, 1),
                stack(Material.TORCH, 64),
                stack(Material.TORCH, 64),
                stack(Material.LANTERN, 16)
        );
    }

    private static @NotNull List<ItemStack> kitsWeapons() {
        return List.of(
                stack(Material.NETHERITE_SWORD, 1),
                stack(Material.NETHERITE_AXE, 1),
                stack(Material.BOW, 1),
                stack(Material.CROSSBOW, 1),
                stack(Material.TRIDENT, 1),
                stack(Material.SHIELD, 1),
                stack(Material.ARROW, 64),
                stack(Material.ARROW, 64),
                stack(Material.SPECTRAL_ARROW, 32),
                stack(Material.FIRE_CHARGE, 16),
                stack(Material.SNOWBALL, 16),
                stack(Material.ENDER_PEARL, 16)
        );
    }

    private static @NotNull List<ItemStack> kitsArmor() {
        return List.of(
                stack(Material.NETHERITE_HELMET, 1),
                stack(Material.NETHERITE_CHESTPLATE, 1),
                stack(Material.NETHERITE_LEGGINGS, 1),
                stack(Material.NETHERITE_BOOTS, 1),
                stack(Material.ELYTRA, 1),
                stack(Material.TURTLE_HELMET, 1),
                stack(Material.GOLDEN_APPLE, 16),
                stack(Material.ENCHANTED_GOLDEN_APPLE, 2),
                stack(Material.TOTEM_OF_UNDYING, 2),
                stack(Material.SHIELD, 1)
        );
    }

    private static @NotNull List<ItemStack> kitsFood() {
        return List.of(
                stack(Material.COOKED_BEEF, 64),
                stack(Material.COOKED_PORKCHOP, 64),
                stack(Material.BREAD, 64),
                stack(Material.GOLDEN_CARROT, 64),
                stack(Material.BAKED_POTATO, 64),
                stack(Material.COOKED_SALMON, 32),
                stack(Material.COOKED_CHICKEN, 32),
                stack(Material.PUMPKIN_PIE, 16),
                stack(Material.HONEY_BOTTLE, 16),
                stack(Material.SWEET_BERRIES, 32),
                stack(Material.MILK_BUCKET, 4),
                stack(Material.CAKE, 2)
        );
    }

    private static @NotNull List<ItemStack> kitsFarm() {
        return List.of(
                stack(Material.WHEAT_SEEDS, 64),
                stack(Material.POTATO, 64),
                stack(Material.CARROT, 64),
                stack(Material.BEETROOT_SEEDS, 32),
                stack(Material.PUMPKIN_SEEDS, 16),
                stack(Material.MELON_SEEDS, 16),
                stack(Material.SUGAR_CANE, 64),
                stack(Material.BAMBOO, 64),
                stack(Material.KELP, 64),
                stack(Material.COCOA_BEANS, 32),
                stack(Material.NETHER_WART, 64),
                stack(Material.BONE_MEAL, 64),
                stack(Material.BONE_MEAL, 64),
                stack(Material.OAK_SAPLING, 16),
                stack(Material.SPRUCE_SAPLING, 16),
                stack(Material.DARK_OAK_SAPLING, 16)
        );
    }

    private static @NotNull List<ItemStack> kitsPotions() {
        return List.of(
                stack(Material.GLASS_BOTTLE, 64),
                stack(Material.BLAZE_POWDER, 32),
                stack(Material.BLAZE_ROD, 16),
                stack(Material.NETHER_WART, 64),
                stack(Material.GLISTERING_MELON_SLICE, 16),
                stack(Material.SPIDER_EYE, 16),
                stack(Material.FERMENTED_SPIDER_EYE, 8),
                stack(Material.GHAST_TEAR, 8),
                stack(Material.MAGMA_CREAM, 16),
                stack(Material.PHANTOM_MEMBRANE, 8),
                stack(Material.GOLDEN_CARROT, 16),
                stack(Material.RABBIT_FOOT, 4),
                stack(Material.BREWING_STAND, 1),
                stack(Material.CAULDRON, 2)
        );
    }

    private static @NotNull List<ItemStack> kitsBlocks() {
        return List.of(
                stack(Material.STONE, 64),
                stack(Material.STONE, 64),
                stack(Material.COBBLESTONE, 64),
                stack(Material.DEEPSLATE, 64),
                stack(Material.OAK_LOG, 64),
                stack(Material.SPRUCE_LOG, 64),
                stack(Material.GLASS, 64),
                stack(Material.SAND, 64),
                stack(Material.GRAVEL, 64),
                stack(Material.DIRT, 64),
                stack(Material.GRASS_BLOCK, 32),
                stack(Material.OBSIDIAN, 32),
                stack(Material.CRYING_OBSIDIAN, 16),
                stack(Material.WHITE_WOOL, 64),
                stack(Material.SCAFFOLDING, 64)
        );
    }

    private static @NotNull List<ItemStack> kitsRedstone() {
        return List.of(
                stack(Material.REDSTONE, 64),
                stack(Material.REDSTONE, 64),
                stack(Material.REDSTONE_TORCH, 32),
                stack(Material.REDSTONE_BLOCK, 16),
                stack(Material.REPEATER, 16),
                stack(Material.COMPARATOR, 8),
                stack(Material.PISTON, 16),
                stack(Material.STICKY_PISTON, 8),
                stack(Material.OBSERVER, 8),
                stack(Material.HOPPER, 16),
                stack(Material.DROPPER, 8),
                stack(Material.DISPENSER, 8),
                stack(Material.TARGET, 4),
                stack(Material.SLIME_BLOCK, 16),
                stack(Material.HONEY_BLOCK, 8),
                stack(Material.LEVER, 16),
                stack(Material.STONE_BUTTON, 16),
                stack(Material.TRIPWIRE_HOOK, 8),
                stack(Material.STRING, 32)
        );
    }

    private static @NotNull List<ItemStack> kitsOres() {
        return List.of(
                stack(Material.DIAMOND, 32),
                stack(Material.EMERALD, 32),
                stack(Material.GOLD_INGOT, 64),
                stack(Material.IRON_INGOT, 64),
                stack(Material.IRON_INGOT, 64),
                stack(Material.COPPER_INGOT, 64),
                stack(Material.COAL, 64),
                stack(Material.LAPIS_LAZULI, 64),
                stack(Material.REDSTONE, 64),
                stack(Material.QUARTZ, 64),
                stack(Material.AMETHYST_SHARD, 32),
                stack(Material.NETHERITE_INGOT, 4),
                stack(Material.ANCIENT_DEBRIS, 8),
                stack(Material.EXPERIENCE_BOTTLE, 64)
        );
    }

    private static @NotNull List<ItemStack> kitsNether() {
        return List.of(
                stack(Material.NETHERRACK, 64),
                stack(Material.BLACKSTONE, 64),
                stack(Material.BASALT, 64),
                stack(Material.SOUL_SAND, 32),
                stack(Material.SOUL_SOIL, 32),
                stack(Material.MAGMA_BLOCK, 32),
                stack(Material.GLOWSTONE, 32),
                stack(Material.SHROOMLIGHT, 16),
                stack(Material.CRIMSON_STEM, 32),
                stack(Material.WARPED_STEM, 32),
                stack(Material.ENDER_EYE, 16),
                stack(Material.ENDER_PEARL, 16),
                stack(Material.FIRE_CHARGE, 16),
                stack(Material.RESPAWN_ANCHOR, 1),
                stack(Material.LODESTONE, 1)
        );
    }

    private static void fill(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ, int y,
            @NotNull Material material
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                b.set(x, y, z, material);
            }
        }
    }

    private static void clear(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ, int y1, int y2
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = y1; y <= y2; y++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
        }
    }

    private static void shell(
            @NotNull BaseTemplates.Builder b,
            int minX, int maxX, int minZ, int maxZ,
            int y1, int y2,
            @NotNull Material wall,
            @NotNull Material corner
    ) {
        for (int y = y1; y <= y2; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean c = x == minX || x == maxX;
                b.set(x, y, minZ, c ? corner : wall);
                b.set(x, y, maxZ, c ? corner : wall);
            }
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                b.set(minX, y, z, wall);
                b.set(maxX, y, z, wall);
            }
        }
    }
}
