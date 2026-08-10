#!/usr/bin/env python3
"""Rewrite kelp, iron, and xp farm blueprints in FarmTemplates.java."""
from pathlib import Path

p = Path(r"D:\Work\minecraft\RihanX\src\main\java\com\rihanx\base\FarmTemplates.java")
text = p.read_text(encoding="utf-8")

KELP = r'''
    /**
     * Kelp aquarium farm.
     * Glass walls hold the water — kelp only grows underwater (that is why it looks "inside glass").
     * Observer+piston breaks grown tips; broken items float to the surface and flush into hoppers→chest.
     */
    public static @NotNull BaseTemplates.BaseBlueprint kelp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Sand + water + kelp columns
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 2; z++) {
                b.set(x, -1, z, Material.SAND);
                b.set(x, -2, z, Material.STONE);
                for (int y = 0; y <= 4; y++) {
                    b.set(x, y, z, Material.WATER);
                }
                b.set(x, 0, z, Material.KELP);
                b.set(x, 1, z, Material.KELP_PLANT);
                b.set(x, 2, z, Material.KELP_PLANT);
                b.set(x, 3, z, Material.WATER); // tip grows here and gets broken
            }
        }

        // Glass aquarium shell (required water container)
        for (int y = -1; y <= 5; y++) {
            for (int x = -4; x <= 4; x++) {
                b.set(x, y, -4, Material.GLASS);
                b.set(x, y, 3, Material.GLASS);
            }
            for (int z = -4; z <= 3; z++) {
                b.set(-4, y, z, Material.GLASS);
                b.set(4, y, z, Material.GLASS);
            }
        }

        // Harvester on +Z: hole in glass, piston faces kelp, observer faces kelp, dust powers piston
        for (int x = -3; x <= 3; x++) {
            b.set(x, 3, 3, Material.AIR);
            b.facing(x, 3, 4, Material.PISTON, BlockFace.NORTH);
            b.facing(x, 4, 4, Material.OBSERVER, BlockFace.NORTH);
            b.set(x, 4, 5, Material.SMOOTH_STONE);
            b.set(x, 3, 5, Material.REDSTONE_WIRE);
            b.set(x, 2, 4, Material.SMOOTH_STONE);
            b.set(x, 2, 5, Material.SMOOTH_STONE);
        }

        // Surface flush: water at top pushes floating kelp items into hoppers above the pistons
        for (int x = -3; x <= 3; x++) {
            b.set(x, 5, 2, Material.WATER);
            b.set(x, 5, 3, Material.WATER);
            b.facing(x, 5, 4, Material.HOPPER,
                    x < 0 ? BlockFace.EAST : (x > 0 ? BlockFace.WEST : BlockFace.SOUTH));
        }
        b.facing(0, 5, 5, Material.HOPPER, BlockFace.DOWN);
        b.set(0, 4, 5, Material.CHEST);
        b.facing(-1, 4, 5, Material.BARREL, BlockFace.SOUTH);
        // Keep redstone column beside chest
        b.set(0, 4, 5, Material.CHEST);
        b.set(1, 4, 5, Material.SMOOTH_STONE);
        b.set(1, 3, 5, Material.REDSTONE_WIRE);

        b.set(-5, 5, 0, Material.STONE);
        b.set(5, 5, 0, Material.STONE);
        b.hangingLantern(-5, 4, 0, Material.LANTERN, 5);
        b.hangingLantern(5, 4, 0, Material.LANTERN, 5);

        spawnPad(b, 0, 7);
        spawnPad(b, 0, 8);
        b.set(-1, 0, 8, Material.CRAFTING_TABLE);
        b.set(1, 0, 8, Material.BARREL);
        return b.build(
                "kelp",
                "Kelp aquarium - glass holds water; pistons break tips; items float into hoppers",
                0, 0, 8
        );
    }

'''

IRON = r'''
    /**
     * Iron golem farm (Java panic design):
     * open-sky dry spawn deck → corner water → center 2×2 drop tunnel → lava on trapdoors → hoppers→chest.
     * Covered glass villager pods + covered zombie cage (line of sight). Add 3 villagers/pod + nametag zombie.
     */
    public static @NotNull BaseTemplates.BaseBlueprint iron() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // Open-sky spawn deck (NO roof — golems need this solid platform)
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                b.set(x, 8, z, Material.STONE_BRICKS);
                b.set(x, 9, z, Material.AIR);
                b.set(x, 10, z, Material.AIR);
                b.set(x, 11, z, Material.AIR);
            }
        }

        // Center 2×2 drop hole through the deck
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                b.set(x, 8, z, Material.AIR);
            }
        }

        // Corner water sources — flow across dry deck into the center hole
        b.set(-6, 9, -6, Material.WATER);
        b.set(-6, 9, 6, Material.WATER);
        b.set(6, 9, -6, Material.WATER);
        b.set(6, 9, 6, Material.WATER);

        // Continuous 2×2 drop tunnel from deck down to kill chamber
        for (int y = 1; y <= 8; y++) {
            for (int x = -2; x <= 1; x++) {
                for (int z = -2; z <= 1; z++) {
                    boolean wall = x == -2 || x == 1 || z == -2 || z == 1;
                    boolean shaft = x >= -1 && x <= 0 && z >= -1 && z <= 0;
                    if (wall) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    } else if (shaft) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }

        // Kill chamber: open trapdoors hold lava; drops fall through into hoppers→chest
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                b.facing(x, 1, z, Material.IRON_TRAPDOOR, BlockFace.SOUTH);
                b.set(x, 2, z, Material.LAVA);
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
            }
        }
        b.facing(-1, 0, 1, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 1, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 1, Material.CHEST);
        b.facing(-1, -1, 1, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -1, 1, Material.BARREL);

        // Collection / AFK alcove on +Z of kill chamber
        for (int x = -2; x <= 2; x++) {
            for (int z = 2; z <= 5; z++) {
                b.set(x, -1, z, Material.STONE_BRICKS);
                b.set(x, 0, z, Material.STONE_BRICKS);
                b.set(x, 1, z, Material.AIR);
                b.set(x, 2, z, Material.AIR);
                b.set(x, 3, z, Material.STONE_BRICK_SLAB);
            }
        }
        b.set(0, 1, 2, Material.AIR);
        b.set(0, 2, 2, Material.AIR);
        b.hangingLantern(0, 2, 4, Material.LANTERN, 3);

        // Covered villager pods (glass + roof) left/right — NOT open to sky
        for (int side : new int[]{-9, 9}) {
            int outer = side < 0 ? side - 1 : side + 1;
            for (int dz = -4; dz <= 3; dz++) {
                b.set(side, 8, dz, Material.STONE_BRICKS);
                b.set(outer, 8, dz, Material.STONE_BRICKS);
            }
            for (int i = 0; i < 3; i++) {
                int z = -3 + i * 2;
                BlockFace bedFace = side < 0 ? BlockFace.EAST : BlockFace.WEST;
                b.bed(side, 9, z, Material.RED_BED, bedFace);
                b.set(side, 9, z + 1, Material.COMPOSTER);
            }
            for (int y = 9; y <= 11; y++) {
                for (int dz = -4; dz <= 3; dz++) {
                    b.set(outer, y, dz, Material.GLASS);
                    if (dz == -4 || dz == 3) {
                        b.set(side, y, dz, Material.GLASS);
                    }
                }
                // roof
                for (int dz = -4; dz <= 3; dz++) {
                    b.set(side, 12, dz, Material.STONE_BRICK_SLAB);
                    b.set(outer, 12, dz, Material.STONE_BRICK_SLAB);
                }
            }
            // Iron bars window toward platform / zombie (line of sight)
            b.set(side, 9, 0, Material.IRON_BARS);
            b.set(side, 10, 0, Material.IRON_BARS);
        }

        // Covered zombie cage (roof so it never burns) with LOS to both pods
        for (int x = -1; x <= 1; x++) {
            for (int z = -10; z <= -8; z++) {
                b.set(x, 8, z, Material.STONE_BRICKS);
            }
        }
        b.set(0, 9, -9, Material.AIR);
        b.set(0, 10, -9, Material.AIR);
        for (int x = -1; x <= 1; x++) {
            b.set(x, 9, -8, Material.IRON_BARS);
            b.set(x, 10, -8, Material.IRON_BARS);
            b.set(x, 9, -10, Material.IRON_BARS);
            b.set(x, 10, -10, Material.IRON_BARS);
            b.set(x, 11, -9, Material.STONE_BRICKS); // roof
        }
        b.set(-1, 9, -9, Material.IRON_BARS);
        b.set(1, 9, -9, Material.IRON_BARS);
        b.set(-1, 10, -9, Material.IRON_BARS);
        b.set(1, 10, -9, Material.IRON_BARS);

        spawnPad(b, 0, 6);
        spawnPad(b, 0, 7);
        b.set(-1, 0, 7, Material.CRAFTING_TABLE);
        b.set(1, 0, 7, Material.BARREL);
        return b.build(
                "iron",
                "Iron farm - open deck, center drop tunnel, lava→hoppers→chest (add villagers + zombie)",
                0, 0, 7
        );
    }

'''

XP = r'''
    /**
     * Dark-room XP mob farm:
     * enclosed spawn pads → water channels → continuous 2×2 drop shaft (~22 blocks) →
     * hopper+slab kill floor (one-hit XP) → chest. AFK room beside the kill chamber.
     */
    public static @NotNull BaseTemplates.BaseBlueprint xp() {
        BaseTemplates.Builder b = new BaseTemplates.Builder();

        // ——— AFK + kill house at ground ———
        for (int x = -4; x <= 4; x++) {
            for (int z = 0; z <= 10; z++) {
                b.set(x, -2, z, Material.STONE_BRICKS);
                b.set(x, -1, z, Material.STONE_BRICKS);
                boolean wall = x == -4 || x == 4 || z == 0 || z == 10;
                if (wall) {
                    for (int y = 0; y <= 3; y++) {
                        b.set(x, y, z, Material.STONE_BRICKS);
                    }
                } else {
                    b.set(x, 0, z, Material.STONE_BRICKS);
                    b.set(x, 1, z, Material.AIR);
                    b.set(x, 2, z, Material.AIR);
                    b.set(x, 3, z, Material.STONE_BRICKS);
                }
            }
        }

        // Front door + wall buttons
        b.set(0, 1, 10, Material.AIR);
        b.set(0, 2, 10, Material.AIR);
        b.door(0, 1, 10, Material.IRON_DOOR, BlockFace.SOUTH);
        b.facing(-1, 2, 11, Material.STONE_BUTTON, BlockFace.SOUTH);
        b.facing(-1, 2, 9, Material.STONE_BUTTON, BlockFace.NORTH);

        // Kill floor under the shaft: hoppers with bottom slabs (fall ~22 → one-hit XP)
        for (int x = -1; x <= 0; x++) {
            for (int z = 1; z <= 2; z++) {
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.slab(x, 1, z, Material.STONE_SLAB, Slab.Type.BOTTOM);
            }
        }
        b.facing(-1, 0, 3, Material.HOPPER, BlockFace.EAST);
        b.facing(0, 0, 3, Material.HOPPER, BlockFace.DOWN);
        b.set(0, -1, 3, Material.CHEST);
        b.facing(-1, -1, 3, Material.CHEST, BlockFace.SOUTH);
        b.set(1, -1, 3, Material.BARREL);

        // Safety bars between AFK (z=5+) and kill (z=1..2)
        for (int x = -3; x <= 3; x++) {
            b.set(x, 1, 4, Material.IRON_BARS);
            b.set(x, 2, 4, Material.IRON_BARS);
        }
        b.set(0, 1, 4, Material.AIR);
        b.set(0, 2, 4, Material.AIR);

        b.set(-3, 1, 7, Material.CRAFTING_TABLE);
        b.set(-3, 1, 8, Material.ANVIL);
        b.set(3, 1, 7, Material.BARREL);
        b.set(3, 1, 8, Material.CHEST);
        b.hangingLantern(0, 2, 7, Material.LANTERN, 3);

        // ——— Continuous 2×2 drop shaft from y=1..24 (aligned under spawn holes) ———
        for (int y = 1; y <= 24; y++) {
            for (int x = -2; x <= 1; x++) {
                for (int z = 0; z <= 3; z++) {
                    boolean wall = x == -2 || x == 1 || z == 0 || z == 3;
                    boolean shaft = (x == -1 || x == 0) && (z == 1 || z == 2);
                    if (wall) {
                        b.set(x, y, z, Material.COBBLESTONE);
                    } else if (shaft) {
                        b.set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        // Re-clear kill slabs/air above hoppers inside shaft footprint
        for (int x = -1; x <= 0; x++) {
            for (int z = 1; z <= 2; z++) {
                b.facing(x, 0, z, Material.HOPPER, BlockFace.SOUTH);
                b.slab(x, 1, z, Material.STONE_SLAB, Slab.Type.BOTTOM);
                for (int y = 2; y <= 24; y++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
        }

        // Outside ladder for maintenance
        for (int y = 1; y <= 23; y++) {
            b.facing(-3, y, 1, Material.LADDER, BlockFace.WEST);
        }

        // ——— Two dark enclosed spawn floors (roofed — not open sky) ———
        // Floor A at y=24, Floor B at y=28. Drop hole = shaft top.
        for (int floor = 0; floor < 2; floor++) {
            int y = 24 + floor * 4;
            // Platform + dark room shell
            for (int x = -8; x <= 7; x++) {
                for (int z = -6; z <= 5; z++) {
                    b.set(x, y, z, Material.COBBLESTONE);
                    b.set(x, y + 1, z, Material.AIR);
                    b.set(x, y + 2, z, Material.AIR);
                    b.set(x, y + 3, z, Material.COBBLESTONE); // solid dark roof
                }
            }
            // Outer walls
            for (int yy = y + 1; yy <= y + 2; yy++) {
                for (int x = -8; x <= 7; x++) {
                    b.set(x, yy, -6, Material.COBBLESTONE);
                    b.set(x, yy, 5, Material.COBBLESTONE);
                }
                for (int z = -6; z <= 5; z++) {
                    b.set(-8, yy, z, Material.COBBLESTONE);
                    b.set(7, yy, z, Material.COBBLESTONE);
                }
            }
            // 2×2 hole into shaft (must match shaft x=-1..0, z=1..2)
            for (int x = -1; x <= 0; x++) {
                for (int z = 1; z <= 2; z++) {
                    b.set(x, y, z, Material.AIR);
                }
            }
            // Water channels (not full flood): ring of water pushing toward the hole
            // North channel
            for (int x = -1; x <= 0; x++) {
                for (int z = -5; z <= 0; z++) {
                    b.set(x, y + 1, z, Material.WATER);
                }
            }
            // South channel
            for (int x = -1; x <= 0; x++) {
                for (int z = 3; z <= 4; z++) {
                    b.set(x, y + 1, z, Material.WATER);
                }
            }
            // West / east channels into hole
            for (int z = 1; z <= 2; z++) {
                for (int x = -7; x <= -2; x++) {
                    b.set(x, y + 1, z, Material.WATER);
                }
                for (int x = 1; x <= 6; x++) {
                    b.set(x, y + 1, z, Material.WATER);
                }
            }
            // Dry spawn pads in the four corners (hostile mobs spawn here)
            for (int x = -7; x <= -3; x++) {
                for (int z = -5; z <= -1; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
                for (int z = 3; z <= 4; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
            }
            for (int x = 2; x <= 6; x++) {
                for (int z = -5; z <= -1; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
                for (int z = 3; z <= 4; z++) {
                    b.set(x, y + 1, z, Material.AIR);
                }
            }
        }

        spawnPad(b, 0, 11);
        spawnPad(b, 0, 12);
        b.set(-1, 0, 12, Material.CRAFTING_TABLE);
        b.set(1, 0, 12, Material.BARREL);
        return b.build(
                "xp",
                "XP mob farm - dark roofed pads, continuous 2x2 drop shaft, hopper+slab kill, door buttons",
                0, 0, 12
        );
    }

'''

# Replace kelp block (messy) through end of kelpClean, keep mushroom
start = text.index("    /**\n     * Kelp aquarium farm.")
end = text.index("    /** Mooshroom / mushroom farm hut with hoppers.")
text = text[:start] + KELP + text[end:]

# Replace iron
start = text.index("    /**\n     * Iron golem farm")
# find xp after iron
xp_marker = "    /**\n     * Mob XP farm"
if xp_marker not in text:
    xp_marker = "    /**\n     * Dark-room XP mob farm"
    # might still be old
    for cand in (
        "    /**\n     * Mob XP farm",
        "    /**\n     * Dark-room XP mob farm",
    ):
        if cand in text:
            xp_marker = cand
            break
end = text.index(xp_marker)
text = text[:start] + IRON + text[end:]

# Replace xp through private fillRectRoof
start = text.index(xp_marker) if xp_marker in text else text.index("    /**\n     * Mob XP farm")
# After previous replace, xp might still be old header
for cand in (
    "    /**\n     * Mob XP farm",
    "    /**\n     * Dark-room XP mob farm",
):
    if cand in text:
        start = text.index(cand)
        break
end = text.index("    private static void fillRectRoof(")
text = text[:start] + XP + text[end:]

p.write_text(text, encoding="utf-8", newline="\n")
print("rewrote kelp, iron, xp")
# sanity
assert "kelpClean" not in text
assert "continuous 2×2 drop" in text or "continuous 2x2 drop" in text or "Center 2" in text
print("ok")
