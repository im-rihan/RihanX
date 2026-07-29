# RihanX

Production-ready Paper utility suite for **Minehut / Paper 26.2** (`paper-api 26.2.build.84-stable`).

## Dual commands (with or without `/rx`)

Every module works **both** ways:

| Style | Example |
|-------|---------|
| Prefixed | `/rx slime nearest` · `/rihanx player fly` · `/rihan protect wand` |
| Standalone | `/slime nearest` · `/player fly` · `/protect wand` |

Aliases: `/rx` = `/rihanx` = `/rihan` · `/inv` = `/inventory` · `/perf` = `/performance` · `/guard` = `/protect` · `/we` = `/edit`

**Teleport exception:** standalone is `/rxtp …` (not `/tp`) so vanilla `/tp` is not overridden. `/rx tp …`, `/rx back`, and `/back` still work.

Permission prefix: `rihanx.*`

**OP-only by default.** Every permission node defaults to `op`. Powerful commands (give, god, fly, gamemode, protect, edit, etc.) also require `isOp()`.

---

## Requirements

| Item | Version |
|------|---------|
| Server | Paper **26.2** |
| Java | **25+** (required by Paper 26.2) |
| Build | Maven 3.9+ |

> Note: Paper API `26.2.build.84-stable` requires Java 25. This project targets Java 25 so `mvn clean package` succeeds.

---

## Installation

1. Build: `mvn clean package`
2. Copy `target/RihanX-1.0.0.jar` into `plugins/`
3. Restart the server
4. Configure `plugins/RihanX/config.yml` and `messages.yml`
5. `/rx admin reload` or `/admin reload`

---

## Build

```bash
mvn clean package
```

Upload **`RihanX-1.0.0.jar`** (not any `original-*` jar).

---

## Command reference

Below, `/rx <module> …` and `/<module> …` are equivalent unless noted.

### Help

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx help` | `rihanx.use` | List modules |

### Slime — `/rx slime` · `/slime`

| Command | Permission | Description |
|---------|------------|-------------|
| `/slime` | `rihanx.slime` | Check current chunk |
| `/slime nearest` | `rihanx.slime.nearest` | Nearest slime chunk |
| `/slime search <radius>` | `rihanx.slime.search` | Async radius search |
| `/slime density <radius>` | `rihanx.slime.density` | Density + best 3×3 farm |
| `/slime map [radius]` | `rihanx.slime.map` | ASCII slime map |
| `/slime tp` | `rihanx.slime.tp` | Teleport to nearest slime |

### World — `/rx world` · `/world`

| Command | Permission | Description |
|---------|------------|-------------|
| `/world info` | `rihanx.world.info` | World details |
| `/world seed` | `rihanx.world.seed` | World seed |
| `/world weather <clear\|rain\|thunder>` | `rihanx.world.weather` | Set weather |
| `/world difficulty <diff>` | `rihanx.world.difficulty` | Set difficulty |
| `/world time <day\|night\|…\|ticks>` | `rihanx.world.time` | Set time |
| `/world border` | `rihanx.world.border` | Border info |
| `/world spawn` | `rihanx.world.spawn` | Show spawn |
| `/world setspawn` | `rihanx.world.setspawn` | Set spawn to you |

### Find — `/rx find` · `/find`

| Command | Permission | Description |
|---------|------------|-------------|
| `/find biome <biome>` | `rihanx.find.biome` | Async biome locate |
| `/find structure <structure>` | `rihanx.find.structure` | Async structure locate |

Supports every vanilla biome and structure (tab-complete).

### Chunk — `/rx chunk` · `/chunk`

| Command | Permission | Description |
|---------|------------|-------------|
| `/chunk info` | `rihanx.chunk.info` | Chunk details |
| `/chunk load` | `rihanx.chunk.load` | Force-load chunk |
| `/chunk unload` | `rihanx.chunk.unload` | Unload chunk |
| `/chunk regenerate` | `rihanx.chunk.regenerate` | Regenerate chunk |
| `/chunk border` | `rihanx.chunk.border` | Block border coords |
| `/chunk entities` | `rihanx.chunk.entities` | Entity count |
| `/chunk tileentities` | `rihanx.chunk.tileentities` | Tile entity count |

### Teleport — `/rx tp` · `/rxtp` · `/back`

| Command | Permission | Description |
|---------|------------|-------------|
| `/rxtp pos <x> <y> <z> [world]` | `rihanx.tp.pos` | Coordinate teleport |
| `/rxtp player <name>` | `rihanx.tp.player` | Teleport to player |
| `/rxtp world <name>` | `rihanx.tp.world` | Teleport to world spawn |
| `/rxtp biome <biome>` | `rihanx.tp.biome` | Find + teleport to biome |
| `/rxtp structure <name>` | `rihanx.tp.structure` | Find + teleport to structure |
| `/rxtp chunk <x> <z>` | `rihanx.tp.chunk` | Chunk center |
| `/rxtp random` | `rihanx.tp.random` | Random safe teleport |
| `/rxtp safe` | `rihanx.tp.safe` | Safe spot near you |
| `/rxtp back` · `/rx tp back` · `/rx back` · `/back` | `rihanx.tp.back` | Previous location |

### Player — `/rx player` · `/player`

| Command | Permission | Description |
|---------|------------|-------------|
| `/player info [player]` | `rihanx.player.info` | Player details |
| `/player heal [player]` | `rihanx.player.heal` | Heal |
| `/player feed [player]` | `rihanx.player.feed` | Feed |
| `/player fly [player]` | `rihanx.player.fly` | Toggle flight |
| `/player god [player]` | `rihanx.player.god` | Toggle god mode |
| `/player gamemode <mode> [player]` | `rihanx.player.gamemode` | Set gamemode (`gm` alias) |
| `/player speed <value> [player]` | `rihanx.player.speed` | Walk/fly speed |
| `/player freeze <player>` | `rihanx.player.freeze` | Freeze |
| `/player unfreeze <player>` | `rihanx.player.unfreeze` | Unfreeze |
| `/player vanish` | `rihanx.player.vanish` | Toggle vanish |
| `/player cleareffects [player]` | `rihanx.player.cleareffects` | Clear potion effects |
| `/player ping [player]` | `rihanx.player.ping` | Ping |

### Inventory — `/rx inventory` · `/inventory` · `/inv`

| Command | Permission | Description |
|---------|------------|-------------|
| `/inv see <player>` | `rihanx.inventory.see` | View inventory |
| `/inv ender <player>` | `rihanx.inventory.ender` | View ender chest |
| `/inv clear [player]` | `rihanx.inventory.clear` | Clear inventory |
| `/inv repair [player]` | `rihanx.inventory.repair` | Repair all items |

### Items — `/rx item` · `/item`

| Command | Permission | Description |
|---------|------------|-------------|
| `/item info` | `rihanx.item.info` | Held item info |
| `/item give <material> [amount] [player]` | `rihanx.item.give` | Give items (`rihanx.item.give.others` for other players) |
| `/item rename <name…>` | `rihanx.item.rename` | Rename (MiniMessage) |
| `/item lore <text…>` | `rihanx.item.lore` | Add lore line |
| `/item enchant <enchant> [level]` | `rihanx.item.enchant` | Add enchant (**costs XP**; free with `rihanx.item.enchant.free`) |
| `/item repair` | `rihanx.item.repair` | Repair held item |

#### Give usage

```text
/item give <material> [amount] [player]
/rx item give <material> [amount] [player]
```

Examples:

```text
/item give diamond 64
/item give redstone 64
/item give repeater 16
/item give netherite_sword 1 Steve
```

**Material names** use vanilla item ids: lowercase, underscores (`redstone`, `oak_log`, `diamond_pickaxe`). Tab-complete suggests materials. Almost every Minecraft item/block that is an item works.

#### Common materials reference

**Redstone**

| Item | Material |
|------|----------|
| Redstone dust | `redstone` |
| Redstone block | `redstone_block` |
| Redstone torch | `redstone_torch` |
| Redstone lamp | `redstone_lamp` |
| Repeater | `repeater` |
| Comparator | `comparator` |
| Observer | `observer` |
| Piston | `piston` |
| Sticky piston | `sticky_piston` |
| Lever | `lever` |
| Target | `target` |
| Daylight detector | `daylight_detector` |
| Tripwire hook | `tripwire_hook` |
| Hopper | `hopper` |
| Dropper | `dropper` |
| Dispenser | `dispenser` |
| Copper bulb | `copper_bulb` |
| Calibrated sculk sensor | `calibrated_sculk_sensor` |
| Sculk sensor | `sculk_sensor` |
| Slime block | `slime_block` |
| Honey block | `honey_block` |

**Ores / minerals**

| Item | Material |
|------|----------|
| Coal | `coal` |
| Copper ingot | `copper_ingot` |
| Iron ingot | `iron_ingot` |
| Gold ingot | `gold_ingot` |
| Diamond | `diamond` |
| Emerald | `emerald` |
| Lapis lazuli | `lapis_lazuli` |
| Amethyst shard | `amethyst_shard` |
| Netherite ingot | `netherite_ingot` |
| Ancient debris | `ancient_debris` |
| Quartz | `quartz` |
| Raw iron / gold / copper | `raw_iron` · `raw_gold` · `raw_copper` |

**Blocks**

| Item | Material |
|------|----------|
| Stone / cobble / deepslate | `stone` · `cobblestone` · `deepslate` |
| Dirt / grass | `dirt` · `grass_block` |
| Sand / gravel | `sand` · `gravel` |
| Glass | `glass` |
| Obsidian | `obsidian` |
| Glowstone | `glowstone` |
| Oak / spruce / birch log | `oak_log` · `spruce_log` · `birch_log` |
| Oak / spruce / birch plank | `oak_planks` · `spruce_planks` · `birch_planks` |
| Chest / ender chest / barrel | `chest` · `ender_chest` · `barrel` |
| Crafting table / furnace | `crafting_table` · `furnace` · `blast_furnace` · `smoker` |
| Anvil | `anvil` |
| Enchanting table | `enchanting_table` |
| Beacon | `beacon` |
| Scaffolding | `scaffolding` |
| Torches | `torch` · `soul_torch` |

**Tools / weapons / armor** (swap material prefix: `wooden_`, `stone_`, `iron_`, `golden_`, `diamond_`, `netherite_`)

| Type | Example |
|------|---------|
| Sword | `diamond_sword` |
| Pickaxe | `diamond_pickaxe` |
| Axe | `diamond_axe` |
| Shovel | `diamond_shovel` |
| Hoe | `diamond_hoe` |
| Bow / crossbow / trident | `bow` · `crossbow` · `trident` |
| Shield / elytra | `shield` · `elytra` |
| Helmet / chest / legs / boots | `netherite_helmet` · `netherite_chestplate` · `netherite_leggings` · `netherite_boots` |

**Food / farming**

| Item | Material |
|------|----------|
| Bread / steak / golden carrot | `bread` · `cooked_beef` · `golden_carrot` |
| Golden apple / enchanted | `golden_apple` · `enchanted_golden_apple` |
| Wheat / carrot / potato | `wheat` · `carrot` · `potato` |
| Bone meal / bone | `bone_meal` · `bone` |
| Water / lava bucket | `water_bucket` · `lava_bucket` |
| Milk bucket | `milk_bucket` |

**Mob drops / misc**

| Item | Material |
|------|----------|
| Gunpowder / string / feather | `gunpowder` · `string` · `feather` |
| Ender pearl / eye | `ender_pearl` · `ender_eye` |
| Blaze rod / powder | `blaze_rod` · `blaze_powder` |
| Totem of undying | `totem_of_undying` |
| Name tag / saddle | `name_tag` · `saddle` |
| Firework rocket | `firework_rocket` |
| Arrow / tipped | `arrow` · `spectral_arrow` |
| Spyglass / brush | `spyglass` · `brush` |
| Bundle | `bundle` |

Any other vanilla item id works the same way (for example `cyan_concrete`, `oak_sign`, `music_disc_cat`). If unsure, start typing and use tab-complete.

#### Enchant

```text
/item enchant <enchant> [level]
```

Examples: `/item enchant sharpness 5` · `/item enchant unbreaking 3` · `/item enchant mending 1`

Common enchant ids: `sharpness`, `efficiency`, `fortune`, `silk_touch`, `unbreaking`, `mending`, `protection`, `fire_aspect`, `knockback`, `looting`, `power`, `punch`, `flame`, `infinity`, `fortune`, `respiration`, `aqua_affinity`, `depth_strider`, `soul_speed`, `swift_sneak`, `feather_falling`, `thorns`, `loyalty`, `channeling`, `riptide`, `impaling`, `multishot`, `piercing`, `quick_charge`, `density`, `breach`, `wind_burst`.

Enchant XP cost (config `item.enchant`): `cost = base-cost + level × cost-per-enchant-level`, capped by `max-cost` (default: 1 XP level per enchant level, max 30). Example: `/item enchant sharpness 5` costs **5** levels. Set `require-xp: false` to disable, or grant `rihanx.item.enchant.free`.

### Search — `/rx search` · `/search`

| Command | Permission | Description |
|---------|------------|-------------|
| `/search slime [radius]` | `rihanx.search.slime` | Slime chunk search |
| `/search cave [radius]` | `rihanx.search.cave` | Cave air pockets |
| `/search lava [radius]` | `rihanx.search.lava` | Lava |
| `/search water [radius]` | `rihanx.search.water` | Water |
| `/search spawner [radius]` | `rihanx.search.spawner` | Spawners |
| `/search village [radius]` | `rihanx.search.village` | Villages |

### Server / performance — `/server` · `/performance` · `/perf`

| Command | Permission | Description |
|---------|------------|-------------|
| `/server info` | `rihanx.server.info` | Server overview |
| `/server tps` | `rihanx.server.tps` | TPS |
| `/server mspt` | `rihanx.server.mspt` | MSPT |
| `/server memory` | `rihanx.server.memory` | Memory |
| `/server uptime` | `rihanx.server.uptime` | Uptime |
| `/perf chunks` | `rihanx.performance.chunks` | Nearby loaded chunks |
| `/perf entities` | `rihanx.performance.entities` | Entity breakdown |

### Protect — `/rx protect` · `/protect` · `/guard`

| Command | Permission | Description |
|---------|------------|-------------|
| `/protect flag <flag> <allow\|deny\|unset> [world]` | `rihanx.protect.flag` | Set per-world flag |
| `/protect flags [world]` | `rihanx.protect.flag` | List world flags |
| `/protect wand` | `rihanx.protect.wand` | Wooden axe selection wand |
| `/protect pos1` / `pos2` | `rihanx.protect.wand` | Set corners |
| `/protect define <name>` | `rihanx.protect.region` | Create region |
| `/protect redefine <name>` | `rihanx.protect.region` | Resize region |
| `/protect delete <name>` | `rihanx.protect.region` | Delete region |
| `/protect info [name]` | `rihanx.protect.region` | Region info |
| `/protect list` | `rihanx.protect.region` | List regions |
| `/protect setflag <name> <flag> <value>` | `rihanx.protect.region` | Per-region flag |
| `/protect addmember \| removemember <name> <player>` | `rihanx.protect.region` | Members |
| `/protect bypass` | `rihanx.protect.bypass` | Toggle personal bypass |

Flags: `tnt`, `creeper-explosion`, `other-explosion`, `fire-spread`, `fire-destroy`, `lava-fire`, `build`, `break`, `place`, `pvp`, `mob-grief`, `enderman-grief`, `leaf-decay`, `ice-melt`, `crop-trample`, `entry`.

Data: `plugins/RihanX/protection.yml` + `regions.yml`.

### Edit — `/rx edit` · `/edit` · `/we`

| Command | Permission | Description |
|---------|------------|-------------|
| `/edit wand` | `rihanx.edit.wand` | Golden axe selection wand |
| `/edit pos1` / `pos2` | `rihanx.edit.wand` | Set corners |
| `/edit size` | `rihanx.edit` | Selection size |
| `/edit count [material]` | `rihanx.edit` | Count blocks |
| `/edit set <material>` | `rihanx.edit` | Fill selection |
| `/edit replace <from> <to>` | `rihanx.edit` | Replace blocks |
| `/edit walls \| outline \| hollow <material>` | `rihanx.edit` | Shell fills |
| `/edit clear` | `rihanx.edit` | Set to air |
| `/edit copy` / `paste` / `rotate <deg>` | `rihanx.edit.clipboard` | Clipboard |
| `/edit undo` / `redo` | `rihanx.edit.history` | History |

Limits: `edit.max-blocks` (200000), `edit.max-undo` (10). Protected regions need membership or `/protect bypass`.

### Admin — `/rx admin` · `/admin`

| Command | Permission | Description |
|---------|------------|-------------|
| `/admin reload` | `rihanx.admin` | Reload config + messages |
| `/admin debug` | `rihanx.admin` | Toggle debug |
| `/admin cache` | `rihanx.admin` | Clear caches |
| `/admin config` | `rihanx.admin` | Dump key config values |
| `/admin cancel` | `rihanx.admin` | Cancel your active search |

---

## Configuration

- `config.yml` — search, teleport, cooldowns, slime, particles, database, cache, performance, protection, edit, **item.enchant XP**
- `messages.yml` — every user-facing string (MiniMessage + legacy)
- `permissions.yml` — permission node list (plugin.yml is authoritative for Bukkit)
- `protection.yml` / `regions.yml` — created at runtime for world flags and regions

Optional SQLite back-location storage: set `database.enabled: true`.

---

## Architecture

```text
com.rihanx
├── api            Public facade + permission constants
├── commands       /rx + standalone module router
├── tabcomplete    Tab completion (both styles)
├── managers       Config, messages, cooldowns, freeze, vanish, god, back, commands
├── protection     WorldGuard-lite (flags, regions, listeners)
├── edit           WorldEdit-lite (selection, clipboard, history)
├── slime / world / chunk / teleport / player / inventory / items
├── search         Biome/structure + block searches
├── performance    TPS / memory / entity reports
├── gui            Simple GUI helpers
├── cache / scheduler / database / models / listeners / utils
└── RihanX.java    Main plugin
```

Searches run off-thread via `CompletableFuture` + Bukkit scheduler and support cancellation.

---

## Repository

Public source: https://github.com/im-rihan/RihanX

---

## License

MIT — see [LICENSE](LICENSE).
