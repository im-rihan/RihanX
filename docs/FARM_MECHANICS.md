# RihanX AutoFarm — Farm Mechanics Research Log

**Target environment (verified from project):**

| Item | Value | Source |
|------|-------|--------|
| Paper API | `26.2.build.84-stable` | `pom.xml` |
| Minecraft version | **26.2** (date-based Mojang version; Paper artifact name **is** MC version) | [Paper project setup](https://docs.papermc.io/paper/dev/project-setup/), DiscordLogger #115 notes `1.21.11 → 26.1.x → 26.2` |
| Java | **25** | `pom.xml` `<java.version>` |
| plugin.yml `api-version` | `1.21` (kept for broader load compatibility) | `plugin.yml` |
| Plugin package | `com.rihanx` | source tree |
| Farm paste | `FarmService` → `BaseService.pasteBlueprint` | existing |
| Commands today | `/farm`, aliases `/autofarm` `/farms`, `/rx farm …` | `plugin.yml`, `FarmModule` |
| Permissions | `rihanx.farm`, `.build`, `.list`, `.undo` | `plugin.yml` |
| Config paste rate | `base.blocks-per-tick` (shared with bases) | `config.yml` |

**Research date:** 2026-08-25  
**Edition:** Java Edition (Paper)  
**Authority:** Minecraft Wiki + Paper Javadocs. YouTube designs are not treated as primary sources.

---

## Shared collection rule (all farms)

| Mechanic | Verified rule | Decision |
|----------|---------------|----------|
| Hopper → chest | Hopper pushes into the block its spout faces. Reliable UI pattern: hopper **on top of** chest facing **DOWN**. Sideways also works but is easy to overwrite / hard to audit. | `FarmTemplates.LOOT_CHEST_Y` + drain at `chestY+1` DOWN |
| Hopper cooldown | ~8 game ticks between transfers | Design for throughput via parallel columns, not faster hoppers |
| Rotation | Local blueprints use front = **+Z / SOUTH**; `BaseService` rotates by player yaw | Keep one blueprint; rotate facings via existing `mapFacing` |

Sources: [Hopper – Minecraft Wiki](https://minecraft.wiki/w/Hopper)

---

## wheat / potato

| Field | Value |
|-------|-------|
| Version tested | MC 26.2 (mechanics stable since 1.18+ farmland rules) |
| Mechanics | Hoe farmland; crops need light ≥9; hydration speeds growth |
| Hydration | Water within **4 blocks horizontally (incl. diagonal)**, same Y or 1 above → **9×9** field hydrates from one center source |
| Harvest | Villager farmer + bed + **composter** workstation; farmer harvests mature crops and can deposit into composters |
| Collection | Composter → hopper → loot bay |
| Failure | Dry farmland reverts if empty; no farmer = decorative only |
| Sources | [Farmland](https://minecraft.wiki/w/Farmland), villager farmer behavior |

**Implementation decision:** Keep water-cross hydrated plot + farmer pod (not pure decorative). Size uses cross hydration so all farmland within 4 of water.

---

## cane

| Field | Value |
|-------|-------|
| Growth | Max **3** blocks; grows on sand/dirt adjacent to water; light independent |
| Break | Piston **push** destroys cane as item |
| Observer | Face the **second** cane height; when third grows, observer pulses piston breaking #2+#3, base remains |
| Collection | Items fall to carpet/water over hoppers → loot bay |
| Sources | [Sugar Cane](https://minecraft.wiki/w/Sugar_Cane) |

**Decision:** Tip-break observer/piston row; water canal for placement + hopper collect.

---

## bamboo

| Field | Value |
|-------|-------|
| Differs from cane | No water adjacency required; **water breaks bamboo**; plant on podzol/dirt |
| Growth | Tall; observer tip-break same pattern as cane |
| Collection | Carpets over hoppers on **both** sides of stalk |
| Sources | Minecraft Wiki Bamboo |

**Decision:** Dual-side carpet collect → merge to center → loot bay. Never place water next to bamboo.

---

## kelp

| Field | Value |
|-------|-------|
| Growth | Needs water column; bone meal / random ticks |
| Harvest | Observer/piston tip break in aquarium |
| Collection | Flowing water stream over hoppers (items float) |
| Sources | Minecraft Wiki Kelp |

**Decision:** Glass aquarium; stream last so glass never overwrites hopper columns.

---

## nether

| Field | Value |
|-------|-------|
| Growth | Nether wart **only** on **soul sand** (not soul soil) |
| Light | Any |
| Collection | Hopper trenches between columns |
| Sources | Minecraft Wiki Nether Wart |

**Decision:** Soul sand pads only; blackstone shell.

---

## animal

| Field | Value |
|-------|-------|
| Containment | Fence + gate; water trough; hay |
| Collection | Animals do **not** auto-deposit; chests are feed/loot storage |
| Decision | 4 sealed pens; hoppers optional for dropped items only — do not claim “auto cook” |

---

## cactus

| Field | Value |
|-------|-------|
| Break | Adjacent solid (fence) breaks growing cactus |
| Restriction | Cannot place next to horizontal solids when planting |
| Collection | Side hoppers under break path |
| Sources | Minecraft Wiki Cactus |

---

## iron

| Field | Value |
|-------|-------|
| Panic design | Villagers see zombie → golems spawn on **open-sky** platform |
| Beds | ≥1 bed per villager for village recognition |
| LOS | Iron bars OK; solid glass blocks LOS — use bars on viewing faces |
| Kill | Lava blade + magma + hoppers → calculated loot bay |
| Sources | Minecraft Wiki Iron Golem, village panic mechanics |

**Adaptation:** Plugin auto-spawns villagers + nametag zombie in minecart. Loot uses shared hopper-on-chest bay (not sideways-only).

---

## xp (natural dark room)

| Field | Value |
|-------|-------|
| Spawn light | Overworld hostiles need **block light 0** (post-1.18) |
| Distance | Spawns **not** within **24** of player; within **128** sphere |
| Drop | ~22 blocks → ~1 HP for most hostiles (punch finish); exact HP varies by mob |
| Enderman | **No water** (teleport); 3-high ceiling |
| Sources | [Mob spawning](https://minecraft.wiki/w/Mob_spawning), Craftdex 26.2 notes |

**Decision:** Sealed blackstone; 15×15 square centered on kill; **center hole XZ = kill pad** (GitHub straight drop); pads only where 3D AFK distance ≥24 (closer cells are non-spawn slabs); fall = 22 blocks onto legs-only slabs; punch through foot-level bars.

---

## xp-zombie / xp-skeleton / xp-spider

| Field | Value |
|-------|-------|
| Activation | Player within **required player range** (default **16**) |
| Spawn range | Default **4** (square around spawner) |
| Config | Paper `CreatureSpawner`: entity type, delays, spawn count, max nearby, required player range, spawn range |
| Sources | [Monster Spawner](https://minecraft.wiki/w/Monster_Spawner), Paper CreatureSpawner API |

**Decision:** 4 spawners; AFK at window within 16; milk warning for cave spiders.

---

## chicken / cow / pig

| Farm | Mechanic | Output |
|------|----------|--------|
| chicken | Hens on trapdoors; eggs→hoppers; water→lava | Cooked chicken + feathers |
| cow | Wheat dispensers breed at dawn; extras→lava | Cooked beef + leather |
| pig | Carrot dispensers breed at dawn; extras→lava | Cooked porkchops |

Lava is walled so it cannot spread. Animals / feed / eggs stocked on paste.

---

## cook

| Field | Value |
|-------|-------|
| Fuel | Coal in **back chests** → hopper into smoker fuel face |
| Input | Raw food in **top chests** → hopper down into smoker |
| Output | Hopper under smoker → loot bay south |
| Stocked on paste | Raw beef (top) + coal (back) |

---

## slime / redstone

| Field | Value |
|-------|-------|
| Slime | 4 spawners; water on deck → drop into **legs-only** kill pad (same math as XP) |
| Witch | 4 spawners; same kill pad → redstone, glowstone, sugar, sticks |
| Kill math | `KILL_MIN/MAX` = (−1..0)×(0..1); hopper y=0, slab y=1, ceiling y=2; punch bars at z=3 |
| Decision | Real configured spawners; AFK at window within 16 |

---

## diamond

| Field | Value |
|-------|-------|
| Not | Vanilla AFK diamond-ore generator (does not exist) |
| Is | **4 master toolsmiths** + **2 farmers** — emeralds in, diamond gear out |
| Containment | Iron-bar booths; dual iron-door airlock |
| Stocked on paste | Starter emeralds + wheat for farmers |

---

## Existing architecture (adapt, do not destroy)

Current generators live in:

- `com.rihanx.base.FarmTemplates`
- `com.rihanx.base.AdvancedFarmTemplates`
- `com.rihanx.base.XpFarmTemplates`
- Paste/rotation/undo: `BaseService`
- Pre-live checks: `BlueprintValidator`

New layer (`com.rihanx.farm`) provides:

- `FarmBuildContext`, `FarmPlan`, `BlockPlacement`
- `FarmRegistry`, `MaterialCalculator`, `FarmPlanValidator`
- Preview / validate / info without world mutation
- Adapters from existing `BaseBlueprint` → `FarmPlan`

Farms remain registered under the same IDs. Commands gain `preview` / `validate` / `info` while keeping `/farm <name>` build path.
