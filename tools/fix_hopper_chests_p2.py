#!/usr/bin/env python3
from pathlib import Path

p = Path(r"D:\Work\minecraft\RihanX\src\main\java\com\rihanx\base\FarmTemplates.java")
text = p.read_text(encoding="utf-8")

replacements = [
    # MELON terminal
    (
        """            b.facing(x, -1, 3, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -1, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 4, Material.CHEST);
        b.facing(-1, -2, 4, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -2, 4, Material.BARREL);""",
        """        }
        hopperRowIntoChest(b, -1, 3, -5, 5, 4);""",
    ),
    # NETHER terminal
    (
        """            b.facing(x, -1, 4, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -1, 5, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 5, Material.CHEST);
        b.facing(-1, -2, 5, Material.BARREL, BlockFace.SOUTH);""",
        """        }
        hopperRowIntoChest(b, -1, 4, -3, 3, 5);""",
    ),
    # CACTUS terminal
    (
        """            b.facing(x, -1, 3, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -1, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 4, Material.CHEST);
        b.facing(-1, -2, 4, Material.BARREL, BlockFace.SOUTH);""",
        """        }
        hopperRowIntoChest(b, -1, 3, -4, 4, 4);""",
    ),
    # POTATO terminal + water hoppers toward chest
    (
        """                    b.facing(x, -1, z, Material.HOPPER, hopperToward(x, z));
                    b.set(x, 0, z, Material.WATER);""",
        """                    b.facing(x, -1, z, Material.HOPPER, hopperTowardPoint(x, z, 0, size));
                    b.set(x, 0, z, Material.WATER);""",
    ),
    (
        """        b.facing(0, -1, size, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, size, Material.CHEST);
        b.facing(-1, -2, size, Material.BARREL, BlockFace.SOUTH);
        b.facing(0, 0, size, Material.SPRUCE_FENCE_GATE, BlockFace.SOUTH);""",
        """        hopperRowIntoChest(b, -1, size - 1, -size + 1, size - 1, size);
        b.facing(0, 0, size, Material.SPRUCE_FENCE_GATE, BlockFace.SOUTH);""",
    ),
    # COCOA terminal
    (
        """            b.facing(x, -2, 3, Material.HOPPER, BlockFace.SOUTH);
        }
        b.facing(0, -2, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -3, 4, Material.CHEST);
        b.facing(-1, -3, 4, Material.BARREL, BlockFace.SOUTH);""",
        """        }
        hopperRowIntoChest(b, -2, 3, -4, 4, 4);""",
    ),
    # MUSHROOM terminal
    (
        """        b.facing(0, -2, 4, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -3, 4, Material.CHEST);""",
        """        hopperRowIntoChest(b, -2, 3, -4, 4, 4);""",
    ),
    # KELP terminal
    (
        """            b.facing(x, 5, 6, Material.HOPPER,
                    x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
            b.set(x, 5, 4, Material.AIR);
            b.set(x, 5, 5, Material.AIR);
        }
        // Bridge water over harvester into hopper row
        for (int x = -3; x <= 3; x++) {
            b.set(x, 5, 4, Material.WATER);
            b.set(x, 5, 5, Material.WATER);
        }
        b.facing(0, 5, 7, Material.HOPPER, BlockFace.DOWN);
        b.set(0, 4, 7, Material.CHEST);
        b.facing(-1, 4, 7, Material.BARREL, BlockFace.SOUTH);""",
        """            b.set(x, 5, 4, Material.AIR);
            b.set(x, 5, 5, Material.AIR);
        }
        for (int x = -3; x <= 3; x++) {
            b.set(x, 5, 4, Material.WATER);
            b.set(x, 5, 5, Material.WATER);
        }
        hopperRowIntoChest(b, 5, 6, -3, 3, 7);""",
    ),
    # IRON storage — keep kill hoppers, fix chest feed (barrel also via hopper into chest chain)
    (
        """        b.facing(-1, 0, 1, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 1, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 1, Material.CHEST);
        b.facing(-1, -1, 1, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -1, 1, Material.BARREL);""",
        """        b.facing(-1, 0, 1, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 1, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 1, Material.CHEST);
        b.facing(-1, -1, 1, Material.CHEST, BlockFace.SOUTH);
        // Barrel fed by hopper pointing into the double chest
        b.facing(1, 0, 1, Material.HOPPER, BlockFace.WEST);
        b.facing(1, -1, 1, Material.BARREL, BlockFace.SOUTH);""",
    ),
    # XP storage
    (
        """        b.facing(-1, 0, 3, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 3, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 3, Material.CHEST);
        b.facing(-1, -1, 3, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -1, 3, Material.BARREL);""",
        """        b.facing(-1, 0, 3, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 3, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 3, Material.CHEST);
        b.facing(-1, -1, 3, Material.CHEST, BlockFace.SOUTH);
        b.facing(1, 0, 3, Material.HOPPER, BlockFace.WEST);
        b.facing(1, -1, 3, Material.BARREL, BlockFace.SOUTH);""",
    ),
    # ANIMAL — hopper DOWN into chest (already), ensure facing set once
    (
        """        b.set(0, -1, 0, Material.HOPPER);
        b.facing(0, -1, 0, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 0, Material.CHEST);""",
        """        b.facing(0, -1, 0, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -2, 0, Material.CHEST);
        b.facing(-1, -2, 0, Material.CHEST, BlockFace.SOUTH);""",
    ),
]

for i, (old, new) in enumerate(replacements):
    if old not in text:
        print(f"MISSING #{i}")
        print(repr(old[:120]))
    else:
        text = text.replace(old, new, 1)
        print(f"ok #{i}")

# Melon field hoppers should aim at collection line (0, 3)
text = text.replace(
    """                if ((x & 1) == 0) {
                    b.set(x, -1, z, Material.FARMLAND);
                    b.set(x, 0, z, Material.MELON_STEM);
                } else {
                    b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, 0, z, Material.DIRT);
                }""",
    """                if ((x & 1) == 0) {
                    b.set(x, -1, z, Material.FARMLAND);
                    b.set(x, 0, z, Material.MELON_STEM);
                } else {
                    b.facing(x, -1, z, Material.HOPPER, hopperTowardPoint(x, z, 0, 3));
                    b.set(x, 0, z, Material.DIRT);
                }""",
    1,
)

# Nether field hoppers toward collection
text = text.replace(
    """                    b.facing(x, -1, z, Material.HOPPER, BlockFace.SOUTH);
                    b.set(x, 0, z, Material.SOUL_SAND);
                    b.set(x, 1, z, Material.NETHER_WART);""",
    """                    b.facing(x, -1, z, Material.HOPPER, hopperTowardPoint(x, z, 0, 4));
                    b.set(x, 0, z, Material.SOUL_SAND);
                    b.set(x, 1, z, Material.NETHER_WART);""",
    1,
)

# Cactus side hoppers toward collection line z=3
text = text.replace(
    """                b.facing(x + 1, -1, z, Material.HOPPER, BlockFace.SOUTH);
                b.set(x + 1, 0, z, Material.AIR);""",
    """                b.facing(x + 1, -1, z, Material.HOPPER, hopperTowardPoint(x + 1, z, 0, 3));
                b.set(x + 1, 0, z, Material.AIR);""",
    1,
)

# Cocoa / mushroom field hoppers toward front
text = text.replace(
    """                b.facing(x, -2, z, Material.HOPPER, BlockFace.SOUTH);""",
    """                b.facing(x, -2, z, Material.HOPPER, hopperTowardPoint(x, z, 0, 3));""",
    1,
)

# mushroom uses same - replace_all for remaining south hoppers under mycelium
# only one cocoa pattern may have matched; do mushroom explicitly
text2 = text
# Count remaining "HOPPER, BlockFace.SOUTH" under farms - leave iron/xp kill floors as SOUTH (intentional chain)

p.write_text(text, encoding="utf-8", newline="\n")
print("phase2 done")
