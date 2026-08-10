#!/usr/bin/env python3
from pathlib import Path

p = Path(r"D:\Work\minecraft\RihanX\src\main\java\com\rihanx\base\FarmTemplates.java")
text = p.read_text(encoding="utf-8")

old_start = text.index("    /**\n     * Connected hopper row at")
old_end = text.index("    /** Materials that count as")

new_helper = r'''
    /**
     * Connected hopper row → DOWN hopper → double chest.
     * Row hoppers feed toward x=0; center walks along Z to chestZ, then drops into the chest.
     * Barrel sits beside the double chest (extra storage).
     */
    private static void hopperRowIntoChest(
            @NotNull BaseTemplates.Builder b,
            int hopperY,
            int lineZ,
            int xMin,
            int xMax,
            int chestZ
    ) {
        for (int x = xMin; x <= xMax; x++) {
            BlockFace face;
            if (x < 0) {
                face = BlockFace.EAST;
            } else if (x > 0) {
                face = BlockFace.WEST;
            } else if (lineZ < chestZ) {
                face = BlockFace.SOUTH;
            } else if (lineZ > chestZ) {
                face = BlockFace.NORTH;
            } else {
                face = BlockFace.DOWN;
            }
            b.facing(x, hopperY, lineZ, Material.HOPPER, face);
        }
        if (lineZ != chestZ) {
            int step = lineZ < chestZ ? 1 : -1;
            BlockFace along = step > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
            for (int z = lineZ + step; z != chestZ; z += step) {
                b.facing(0, hopperY, z, Material.HOPPER, along);
            }
            b.facing(0, hopperY, chestZ, Material.HOPPER, BlockFace.DOWN);
        } else {
            b.facing(0, hopperY, chestZ, Material.HOPPER, BlockFace.DOWN);
        }
        b.set(0, hopperY - 1, chestZ, Material.CHEST);
        b.facing(-1, hopperY - 1, chestZ, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, hopperY - 1, chestZ, Material.BARREL, BlockFace.SOUTH);
    }

'''

text = text[:old_start] + new_helper + text[old_end:]

# --- Fix farm collection endings ---

def replace_between(src: str, start_marker: str, end_marker: str, replacement: str) -> str:
    s = src.index(start_marker)
    # find end after start
    e = src.index(end_marker, s)
    return src[:s] + replacement + src[e:]

# WHEAT: replace collection + chest block
wheat_old = """        for (int x = -size + 1; x <= size - 1; x++) {
            b.facing(x, -1, size - 1, Material.HOPPER, BlockFace.SOUTH);
            if (x != 0) {
                b.set(x, 0, size - 1, Material.WATER);
            }
        }
        b.facing(0, -1, size - 1, Material.HOPPER, BlockFace.SOUTH);
        b.facing(0, -1, size, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, size, Material.CHEST);
        b.facing(-1, -2, size, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, -2, size, Material.BARREL, BlockFace.SOUTH);"""

wheat_new = """        // Water-cross hoppers drain toward the south collection line
        for (int x = -size + 1; x <= size - 1; x++) {
            for (int z = -size + 1; z <= size - 1; z++) {
                if (x == 0 || z == 0) {
                    if (x == -size || x == size || z == -size || z == size) {
                        continue;
                    }
                    b.facing(x, -1, z, Material.HOPPER, hopperTowardPoint(x, z, 0, size));
                    b.set(x, 0, z, Material.WATER);
                }
            }
        }
        for (int x = -size + 1; x <= size - 1; x++) {
            if (x != 0) {
                b.set(x, 0, size - 1, Material.WATER);
            }
        }
        hopperRowIntoChest(b, -1, size - 1, -size + 1, size - 1, size);"""

if wheat_old not in text:
    raise SystemExit("wheat_old not found")
text = text.replace(wheat_old, wheat_new, 1)

# Also remove duplicate hopper placement in wheat loop for water cross - the first loop still sets hopperToward
# Change first loop water branch to use hopperTowardPoint
text = text.replace(
    """                } else if (x == 0 || z == 0) {
                    // Water cross — hoppers under water collect floating drops
                    b.facing(x, -1, z, Material.HOPPER, hopperToward(x, z));
                    b.set(x, 0, z, Material.WATER);""",
    """                } else if (x == 0 || z == 0) {
                    // Water cross — hoppers drain toward south chest line
                    b.facing(x, -1, z, Material.HOPPER, hopperTowardPoint(x, z, 0, size));
                    b.set(x, 0, z, Material.WATER);""",
    1,
)

# CANE
cane_old = """            // Open hopper trench where drops land (in front of cane, not under sand)
            c.facing(x, -1, 4, Material.HOPPER, x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
            c.set(x, 0, 4, Material.AIR);
        }
        c.facing(0, -1, 5, Material.HOPPER, BlockFace.DOWN);
        c.set(0, -2, 5, Material.CHEST);
        c.facing(-1, -2, 5, Material.CHEST, BlockFace.SOUTH);
        c.set(1, -2, 5, Material.BARREL);"""

cane_new = """            c.set(x, 0, 4, Material.AIR);
        }
        hopperRowIntoChest(c, -1, 4, -rows, rows, 5);"""

if cane_old not in text:
    raise SystemExit("cane_old not found")
text = text.replace(cane_old, cane_new, 1)

# BAMBOO
bamboo_old = """            b.facing(x, -1, 3, Material.HOPPER, x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
            b.set(x, 0, 3, Material.AIR);
        }
        b.facing(0, -1, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 4, Material.CHEST);
        b.facing(-1, -2, 4, Material.BARREL, BlockFace.SOUTH);"""

bamboo_new = """            b.set(x, 0, 3, Material.AIR);
        }
        hopperRowIntoChest(b, -1, 3, -rows, rows, 4);"""

if bamboo_old not in text:
    raise SystemExit("bamboo_old not found")
text = text.replace(bamboo_old, bamboo_new, 1)

p.write_text(text, encoding="utf-8", newline="\n")
print("phase1 ok")
