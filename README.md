# RihanX

Production-ready Paper utility suite for **Minehut / Paper 26.2** (`paper-api 26.2.build.84-stable`).

## Dual commands (with or without `/rx`)

Every module works **both** ways:

| Style | Example |
|-------|---------|
| Prefixed | `/rx slime nearest` Â· `/rihanx player fly` Â· `/rihan protect wand` |
| Standalone | `/slime nearest` Â· `/player fly` Â· `/protect wand` |

Aliases: `/rx` = `/rihanx` = `/rihan` Â· `/inv` = `/inventory` Â· `/perf` = `/performance` Â· `/guard` = `/protect` Â· `/we` = `/edit`

**Teleport exception:** standalone is `/rxtp â€¦` (not `/tp`) so vanilla `/tp` is not overridden. `/rx tp â€¦`, `/rx back`, and `/back` still work.

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

Below, `/rx <module> â€¦` and `/<module> â€¦` are equivalent unless noted.

### Help

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx help` | `rihanx.use` | List modules |

### Slime â€” `/rx slime` Â· `/slime`

| Command | Permission | Description |
|---------|------------|-------------|
| `/slime` | `rihanx.slime` | Check current chunk |
| `/slime nearest` | `rihanx.slime.nearest` | Nearest slime chunk |
| `/slime search <radius>` | `rihanx.slime.search` | Async radius search |
| `/slime density <radius>` | `rihanx.slime.density` | Density + best 3Ã—3 farm |
| `/slime map [radius]` | `rihanx.slime.map` | ASCII slime map |
| `/slime tp` | `rihanx.slime.tp` | Teleport to nearest slime |

### World â€” `/rx world` Â· `/world`

| Command | Permission | Description |
|---------|------------|-------------|
| `/world info` | `rihanx.world.info` | World details |
| `/world seed` | `rihanx.world.seed` | World seed |
| `/world weather <clear\|rain\|thunder>` | `rihanx.world.weather` | Set weather |
| `/world difficulty <diff>` | `rihanx.world.difficulty` | Set difficulty |
| `/world time <day\|night\|â€¦\|ticks>` | `rihanx.world.time` | Set time |
| `/world border` | `rihanx.world.border` | Border info |
| `/world spawn` | `rihanx.world.spawn` | Show spawn |
| `/world setspawn` | `rihanx.world.setspawn` | Set spawn to you |

### Find â€” `/rx find` Â· `/find`

| Command | Permission | Description |
|---------|------------|-------------|
| `/find biome <biome>` | `rihanx.find.biome` | Async biome locate |
| `/find structure <structure>` | `rihanx.find.structure` | Async structure locate |

Supports every vanilla biome and structure (tab-complete).

### Chunk â€” `/rx chunk` Â· `/chunk`

| Command | Permission | Description |
|---------|------------|-------------|
| `/chunk info` | `rihanx.chunk.info` | Chunk details |
| `/chunk load` | `rihanx.chunk.load` | Force-load chunk |
| `/chunk unload` | `rihanx.chunk.unload` | Unload chunk |
| `/chunk regenerate` | `rihanx.chunk.regenerate` | Regenerate chunk |
| `/chunk border` | `rihanx.chunk.border` | Block border coords |
| `/chunk entities` | `rihanx.chunk.entities` | Entity count |
| `/chunk tileentities` | `rihanx.chunk.tileentities` | Tile entity count |

### Teleport â€” `/rx tp` Â· `/rxtp` Â· `/back`

| Command | Permission | Description |
|---------|------------|-------------|
| `/rxtp pos <x> <y> <z> [world]` | `rihanx.tp.pos` | Coordinate teleport |
| `/rxtp player <name>` | `rihanx.tp.player` | Teleport **you** to that player (tab-completes names) |
| `/rxtp player <who> <to>` | `rihanx.tp.player` | Teleport player **who** to player **to** |
| `/rxtp here <player>` | `rihanx.tp.here` | Pull a player to you |
| `/rxtp home [player]` | `rihanx.tp.home` | Bed/respawn home (or world spawn if none) |
| `/rxtp world <name>` | `rihanx.tp.world` | Teleport to world spawn |
| `/rxtp biome <biome>` | `rihanx.tp.biome` | Find + teleport to biome |
| `/rxtp structure <name>` | `rihanx.tp.structure` | Find + teleport to structure |
| `/rxtp chunk <x> <z>` | `rihanx.tp.chunk` | Chunk center |
| `/rxtp random` | `rihanx.tp.random` | Random safe teleport |
| `/rxtp safe` | `rihanx.tp.safe` | Safe spot near you |
| `/rxtp back` Â· `/rx tp back` Â· `/rx back` Â· `/back` | `rihanx.tp.back` | Previous location |

Player names support partial match + tab completion (e.g. `/rxtp player Ste` â†’ Steve). Tab also suggests online names for `/fly`, `/god`, `/heal`, `/feed`, `/gm`, `/player â€¦`, `/inv see`, `/rxtp here`, etc.

**Self vs other:** `/fly`, `/god`, `/heal`, `/feed`, `/gm` with no name always affect **only you**. Naming a player affects **only that one player** â€” never everyone online.

### Player â€” `/rx player` Â· `/player`

| Command | Permission | Description |
|---------|------------|-------------|
| `/player info [player]` | `rihanx.player.info` | Player details |
| `/player heal [player]` | `rihanx.player.heal` | Heal |
| `/player feed [player]` | `rihanx.player.feed` | Feed |
| `/player fly [player]` | `rihanx.player.fly` | Toggle flight (**you** by default; only that player if named) |
| `/player god [player]` | `rihanx.player.god` | Toggle god mode (**you** by default; only that player if named) |
| `/fly [player]` | `rihanx.player.fly` | Shortcut â€” same as `/player fly` (self unless you name someone) |
| `/god [player]` | `rihanx.player.god` | Shortcut â€” same as `/player god` |
| `/heal [player]` Â· `/feed [player]` | heal/feed perms | Shortcuts â€” self by default |
| `/vanish` | `rihanx.player.vanish` | Shortcut â€” **always you only** |
| `/gm <mode> [player]` | `rihanx.player.gamemode` | Shortcut â€” self by default |

| `/player gamemode <mode> [player]` | `rihanx.player.gamemode` | Set gamemode (`gm` alias) |
| `/player speed <value> [player]` | `rihanx.player.speed` | Walk/fly speed |
| `/player freeze <player>` | `rihanx.player.freeze` | Freeze |
| `/player unfreeze <player>` | `rihanx.player.unfreeze` | Unfreeze |
| `/player vanish` | `rihanx.player.vanish` | Toggle vanish |
| `/player cleareffects [player]` | `rihanx.player.cleareffects` | Clear potion effects |
| `/player ping [player]` | `rihanx.player.ping` | Ping |

### Inventory â€” `/rx inventory` Â· `/inventory` Â· `/inv`

| Command | Permission | Description |
|---------|------------|-------------|
| `/inv see <player>` | `rihanx.inventory.see` | View inventory |
| `/inv ender <player>` | `rihanx.inventory.ender` | View ender chest |
| `/inv clear [player]` | `rihanx.inventory.clear` | Clear inventory |
| `/inv repair [player]` | `rihanx.inventory.repair` | Repair all items |

### Items â€” `/rx item` Â· `/item`

| Command | Permission | Description |
|---------|------------|-------------|
| `/item info` | `rihanx.item.info` | Held item info |
| `/item give <material> [amount] [player]` | `rihanx.item.give` | Give items (`rihanx.item.give.others` for other players) |

Tipped arrows / potions (not enchants):

```text
/item give arrow_of_harming_2 64
/item give tipped_arrow:strong_harming 64
/item give tipped_arrow:harming 16
/item give potion:strong_healing 1
```

**Note:** Arrow of Harming II is a **tipped arrow** (`STRONG_HARMING`), not an enchantment â€” `/item enchant` will not create it.
| `/item rename <nameâ€¦>` | `rihanx.item.rename` | Rename (MiniMessage) |
| `/item lore <textâ€¦>` | `rihanx.item.lore` | Add lore line |
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
| Raw iron / gold / copper | `raw_iron` Â· `raw_gold` Â· `raw_copper` |

**Blocks**

| Item | Material |
|------|----------|
| Stone / cobble / deepslate | `stone` Â· `cobblestone` Â· `deepslate` |
| Dirt / grass | `dirt` Â· `grass_block` |
| Sand / gravel | `sand` Â· `gravel` |
| Glass | `glass` |
| Obsidian | `obsidian` |
| Glowstone | `glowstone` |
| Oak / spruce / birch log | `oak_log` Â· `spruce_log` Â· `birch_log` |
| Oak / spruce / birch plank | `oak_planks` Â· `spruce_planks` Â· `birch_planks` |
| Chest / ender chest / barrel | `chest` Â· `ender_chest` Â· `barrel` |
| Crafting table / furnace | `crafting_table` Â· `furnace` Â· `blast_furnace` Â· `smoker` |
| Anvil | `anvil` |
| Enchanting table | `enchanting_table` |
| Beacon | `beacon` |
| Scaffolding | `scaffolding` |
| Torches | `torch` Â· `soul_torch` |

**Tools / weapons / armor** (swap material prefix: `wooden_`, `stone_`, `iron_`, `golden_`, `diamond_`, `netherite_`)

| Type | Example |
|------|---------|
| Sword | `diamond_sword` |
| Pickaxe | `diamond_pickaxe` |
| Axe | `diamond_axe` |
| Shovel | `diamond_shovel` |
| Hoe | `diamond_hoe` |
| Bow / crossbow / trident | `bow` Â· `crossbow` Â· `trident` |
| Shield / elytra | `shield` Â· `elytra` |
| Helmet / chest / legs / boots | `netherite_helmet` Â· `netherite_chestplate` Â· `netherite_leggings` Â· `netherite_boots` |

**Food / farming**

| Item | Material |
|------|----------|
| Bread / steak / golden carrot | `bread` Â· `cooked_beef` Â· `golden_carrot` |
| Golden apple / enchanted | `golden_apple` Â· `enchanted_golden_apple` |
| Wheat / carrot / potato | `wheat` Â· `carrot` Â· `potato` |
| Bone meal / bone | `bone_meal` Â· `bone` |
| Water / lava bucket | `water_bucket` Â· `lava_bucket` |
| Milk bucket | `milk_bucket` |

**Mob drops / misc**

| Item | Material |
|------|----------|
| Gunpowder / string / feather | `gunpowder` Â· `string` Â· `feather` |
| Ender pearl / eye | `ender_pearl` Â· `ender_eye` |
| Blaze rod / powder | `blaze_rod` Â· `blaze_powder` |
| Totem of undying | `totem_of_undying` |
| Name tag / saddle | `name_tag` Â· `saddle` |
| Firework rocket | `firework_rocket` |
| Arrow / tipped | `arrow` Â· `spectral_arrow` |
| Spyglass / brush | `spyglass` Â· `brush` |
| Bundle | `bundle` |

Any other vanilla item id works the same way (for example `cyan_concrete`, `oak_sign`, `music_disc_cat`). If unsure, start typing and use tab-complete.

#### Enchant

```text
/item enchant <enchant> [level]
```

Examples: `/item enchant sharpness 5` Â· `/item enchant unbreaking 3` Â· `/item enchant mending 1`

Common enchant ids: `sharpness`, `efficiency`, `fortune`, `silk_touch`, `unbreaking`, `mending`, `protection`, `fire_aspect`, `knockback`, `looting`, `power`, `punch`, `flame`, `infinity`, `fortune`, `respiration`, `aqua_affinity`, `depth_strider`, `soul_speed`, `swift_sneak`, `feather_falling`, `thorns`, `loyalty`, `channeling`, `riptide`, `impaling`, `multishot`, `piercing`, `quick_charge`, `density`, `breach`, `wind_burst`.

Enchant XP cost (config `item.enchant`): `cost = base-cost + level Ã— cost-per-enchant-level`, capped by `max-cost` (default: 1 XP level per enchant level, max 30). Example: `/item enchant sharpness 5` costs **5** levels. Set `require-xp: false` to disable, or grant `rihanx.item.enchant.free`.

### Search â€” `/rx search` Â· `/search`

| Command | Permission | Description |
|---------|------------|-------------|
| `/search slime [radius]` | `rihanx.search.slime` | Slime chunk search |
| `/search cave [radius]` | `rihanx.search.cave` | Cave air pockets |
| `/search lava [radius]` | `rihanx.search.lava` | Lava |
| `/search water [radius]` | `rihanx.search.water` | Water |
| `/search spawner [radius]` | `rihanx.search.spawner` | Spawners |
| `/search village [radius]` | `rihanx.search.village` | Villages |

### Server / performance â€” `/server` Â· `/performance` Â· `/perf`

| Command | Permission | Description |
|---------|------------|-------------|
| `/server info` | `rihanx.server.info` | Server overview |
| `/server tps` | `rihanx.server.tps` | TPS |
| `/server mspt` | `rihanx.server.mspt` | MSPT |
| `/server memory` | `rihanx.server.memory` | Memory |
| `/server uptime` | `rihanx.server.uptime` | Uptime |
| `/perf chunks` | `rihanx.performance.chunks` | Nearby loaded chunks |
| `/perf entities` | `rihanx.performance.entities` | Entity breakdown |

### Homes â€” `/home` Â· `/sethome` Â· `/delhome` Â· `/homes`

| Command | Permission | Description |
|---------|------------|-------------|
| `/home [name]` | `rihanx.home` | Teleport to a home (default name `home`) |
| `/sethome [name]` | `rihanx.home.set` | Set home at your location |
| `/delhome [name]` | `rihanx.home.delete` | Delete a home |
| `/homes` | `rihanx.home.list` | List homes |

Limits: `homes.max-default` (3), `homes.max-op` (20), or `rihanx.home.limit.<n>`. Also `/rx home â€¦`.

### Warps â€” `/warp` Â· `/setwarp` Â· `/delwarp` Â· `/warps`

| Command | Permission | Description |
|---------|------------|-------------|
| `/warp <name>` | `rihanx.warp` | Teleport to a server warp |
| `/setwarp <name>` | `rihanx.warp.set` | Create/update warp |
| `/delwarp <name>` | `rihanx.warp.delete` | Delete warp |
| `/warps` | `rihanx.warp.list` | List warps |

### TPA â€” `/tpa` Â· `/tpahere` Â· `/tpaccept` Â· `/tpdeny` Â· `/tpcancel`

| Command | Permission | Description |
|---------|------------|-------------|
| `/tpa <player>` | `rihanx.tpa` | Request to teleport to them |
| `/tpahere <player>` | `rihanx.tpa.here` | Request they teleport to you |
| `/tpaccept` | `rihanx.tpa.accept` | Accept pending request |
| `/tpdeny` | `rihanx.tpa.deny` | Deny pending request |
| `/tpcancel` | `rihanx.tpa.cancel` | Cancel your outgoing request |

Config: `tpa.timeout-seconds` (60), `tpa.cooldown-seconds` (5). Vanished targets are hidden unless `rihanx.see.vanished`.

### Private messages â€” `/msg` Â· `/tell` Â· `/w` Â· `/reply` Â· `/r`

| Command | Permission | Description |
|---------|------------|-------------|
| `/msg <player> <message>` | `rihanx.msg` | Send a private message (`/tell` and `/w` are aliases) |
| `/reply <message>` | `rihanx.reply` | Reply to the last player who messaged you, or who you last messaged (`/r` alias) |

Vanished players cannot be messaged unless the sender has `rihanx.see.vanished`. Also works as `/rx msg â€¦` / `/rx reply â€¦`.

### AFK â€” `/afk`

| Command | Permission | Description |
|---------|------------|-------------|
| `/afk` | `rihanx.afk` | Toggle your AFK status |

AFK is broadcast to the server and shows an `[AFK]` suffix next to your name in the tab list. Moving, interacting, or chatting automatically clears AFK. Also `/rx afk`.

### Spawn â€” `/spawn` Â· `/setspawn`

| Command | Permission | Description |
|---------|------------|-------------|
| `/spawn` | `rihanx.spawn` | Teleport to the `spawn` warp (falls back to world spawn if unset) |
| `/setspawn` | `rihanx.setspawn` | Set the world spawn **and** create/update the `spawn` warp to your location |

`/setspawn` updates both the vanilla world spawn and a warp named `spawn` (visible via `/warps`), so `/spawn`, `/warp spawn`, and respawns all point to the same place. Also `/rx spawn` / `/rx setspawn`.

### Bases â€” `/base`

Stand where the **front door / porch** should be, look toward where the house should go, then paste (or open `/base` GUI). You stay at the entrance while it builds, then face inside when done.

| Command | Permission | Description |
|---------|------------|-------------|
| `/base` | `rihanx.base` | Open the base selection GUI |
| `/base <name>` | `rihanx.base.build` | Paste a house template facing you |
| `/base list` | `rihanx.base` | List templates in chat |
| `/base undo` | `rihanx.base.undo` | Restore terrain from your last paste (stack up to `base.max-undo`) |

Built-in templates (front door faces you). Open the GUI with `/base`:

| Template | Tier | Features |
|----------|------|----------|
| `hut` | Beginner | Oak cabin, gabled roof |
| `cottage` | Intermediate | Spruce + chimney + porch |
| `village` | Classic | Plains-village oak/cobble |
| `bungalow` | Luxury | **5 bedrooms**, calcite/quartz, **swimming pool**, kitchen, baths |
| `villa` | Luxury | 2-storey, **bubble lift**, 5 beds, pool, balcony |
| `modern` | Luxury | Glass/concrete, lift, 5 beds, **rooftop pool** |
| `mansion` | Mega | 3 floors, **dual lifts**, 6 suites, indoor + outdoor pools |
| `resort` | Mega | Mega pool club, **5 cabanas**, bar lounge |

Also `/house`, `/buildbase`, or `/rx base â€¦`. Large builds ask for confirm. Speed: `base.blocks-per-tick` (default 1200).

**Lift tip:** stand in the glass water tube â€” soul sand = up, magma = down.

Examples: `/base` Â· `/base bungalow` Â· `/base villa` Â· `/base list`

### Farms â€” `/farm`

Stand at the **front / collection** side, face into the farm, then open the GUI:

| Command | Permission | Description |
|---------|------------|-------------|
| `/farm` | `rihanx.farm` | Open farm selection GUI |
| `/farm <name>` | `rihanx.farm.build` | Paste a farm |
| `/farm list` | `rihanx.farm` | List farms |
| `/farm undo` | `rihanx.farm.undo` | Undo your last farm/base paste |

| Farm | Gadgets |
|------|---------|
| `wheat` / `potato` | Water cross, hoppers, chests, composters |
| `cane` / `bamboo` | Observers, pistons, hoppers |
| `melon` / `cocoa` / `kelp` / `mushroom` | Crop-specific + hoppers |
| `nether` | Soul sand wart, hoppers |
| `animal` | 4 pens, water, hay, chests, hoppers |
| `cactus` | Break fences, hoppers |
| `iron` | Water streams, lava kill, hoppers (add villagers + zombie) |
| `xp` | Dark spawn pads, drop chute, magma + hoppers |

Lanterns hang from **chains** under roofs or post caps. Also `/autofarm`, `/farms`, `/rx farm â€¦`.

### Kits â€” `/kit` Â· `/kits`

| Command | Permission | Description |
|---------|------------|-------------|
| `/kit <name>` | `rihanx.kit` | Claim a kit |
| `/kits` | `rihanx.kit.list` | List kits |

Definitions in `kits.yml`. Bundled kits (synced on reload when `kits.sync-bundled: true`):

| Kit | Contents (summary) | Cooldown |
|-----|--------------------|----------|
| `starter` | Chainmail + stone tools (light enchants), food, bow, basics | 1 hour |
| `survival` | **Elytra**, enchanted diamond gear/tools (Protection II, Sharpness III, Efficiency III), totem, shulkers, building | 24 hours |
| `pro` | **Netherite** + elytra, Protection IV / Sharpness V / Efficiency V / Unbreaking III / Mending gear, mace/trident, beacon, totemsÃ—5, blocks of ore | **48 hours** |

**Cooldowns explained:** `cooldown-seconds` in `kits.yml` is the wait time between claims of that kit, per player. `pro` is set to `172800` seconds = **48 hours** â€” a player who claims `/kit pro` must wait 2 full days before claiming it again (unless they have `rihanx.bypass.cooldown` or `rihanx.admin`). `starter` = 3600s (1h), `survival` = 86400s (24h).

Examples: `/kit survival` Â· `/kit pro`

**Delivery:** `/kit` auto-places **colored shulker boxes** (better chests) filled with items â€” white / lime / purple. No crafting needed. Config `kits.delivery`: `auto` Â· `shulker` Â· `inventory`.

#### Enchanted kit gear

`kits.yml` items support an extended format: `MATERIAL:amount:enchant1:level1:enchant2:level2...`

```text
DIAMOND_SWORD:1:sharpness:5:unbreaking:3
NETHERITE_HELMET:1:protection:4:unbreaking:3:mending:1
```

Enchant ids and levels use the same names as `/item enchant`. Applied with `meta.addEnchant(enchant, level, true)`, so unsafe/high levels are allowed on kit items even though `/item enchant` itself is capped by normal rules.

#### First-join kit

Config `kits.first-join-kit` (default: `starter`). The first time a brand-new player joins (`hasPlayedBefore() == false`), they're given that kit automatically 1 second (20 ticks) after joining â€” no command needed. Set it to `""` (blank) to disable.

### Fly / god persistence

`/fly` and `/god` stay as you left them after disconnect and server restart (`player-states.yml`). Set `general.unfly-on-quit: true` / `ungod-on-quit: true` only if you want them cleared on quit.

### Ideas for more features

High value next additions if you want them:

- Simple economy (`/balance` `/pay`) or Vault hook  
- `/rtp` world filter UI  
- Claim GUI for protect regions  
- Discord / webhook alerts for staff  
- Nicknames (`/nick`)  
- AFK-based kick timer

### Do you still need EssentialsX?

For most Minehut survival servers using RihanX: **no â€” you can remove EssentialsX** if you only used it for homes, warps, TPA, kits, heal/fly/god, back, spawn, chat (`/msg`/`/r`), and AFK. RihanX covers those.

**Keep EssentialsX** only if you still need features RihanX does not have, for example:

- Economy (`/pay`, `/balance`, shops, worth)
- Nicknames (`/nick`), mail
- Punishment suite (`/mute`, `/ban`, `/jail`, `/kick` as Essentials commands)
- Vanish beyond RihanX vanish (e.g. Essentials' cross-plugin vanish hooks)

If you remove EssentialsX: delete its jar, restart, and grant players `rihanx.home` / `rihanx.warp` / `rihanx.tpa` / `rihanx.kit` (or keep default op). That also removes the â€œout of dateâ€ EssentialsX chat spam.

### Protect â€” `/rx protect` Â· `/protect` Â· `/guard`

| Command | Permission | Description |
|---------|------------|-------------|
| `/protect flag <flag> <allow\|deny\|unset> [world]` | `rihanx.protect.flag` | Set per-world flag |
| `/protect flags [world]` | `rihanx.protect.flag` | List world flags |
| `/protect wand` | `rihanx.protect.wand` | Wooden axe selection wand |
| `/protect pos1` / `pos2` | `rihanx.protect.wand` | Set corners |
| `/protect define <name>` | `rihanx.protect.region` | Create region |
| `/protect redefine <name>` | `rihanx.protect.region` | Resize region |
| `/protect delete <name>` | `rihanx.protect.region` | Delete region (confirm GUI) |
| `/protect info [name]` | `rihanx.protect.region` | Region info |
| `/protect list` | `rihanx.protect.region` | List regions |
| `/protect setflag <name> <flag> <value>` | `rihanx.protect.region` | Per-region flag |
| `/protect addmember \| removemember <name> <player>` | `rihanx.protect.region` | Members |
| `/protect addowner \| removeowner <name> <player>` | `rihanx.protect.region` | Owners |
| `/protect priority <name> <n>` | `rihanx.protect.region` | Region priority |
| `/protect bypass` | `rihanx.protect.bypass` | Toggle personal bypass |

Flags: `tnt`, `creeper-explosion`, `other-explosion`, `fire-spread`, `fire-destroy`, `lava-fire`, `build`, `break`, `place`, `pvp`, `mob-grief`, `enderman-grief`, `leaf-decay`, `ice-melt`, `crop-trample`, `entry`, `chest-access`, `use`, `vehicle`, `item-drop`.

Data: `plugins/RihanX/protection.yml` + `regions.yml`.

### Edit â€” `/rx edit` Â· `/edit` Â· `/we`

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
| `/edit expand \| contract <n> [dir]` | `rihanx.edit` | Grow/shrink selection |
| `/edit copy` / `paste` / `rotate <deg>` | `rihanx.edit.clipboard` | Clipboard |
| `/edit undo` / `redo` | `rihanx.edit.history` | History |

Limits: `edit.max-blocks` (200000), `edit.max-undo` (10), `edit.confirm-above` (50000 â€” GUI confirm for large set/clear/replace). Protected regions need membership or `/protect bypass`.

### Admin â€” `/rx admin` Â· `/admin`

| Command | Permission | Description |
|---------|------------|-------------|
| `/admin reload` | `rihanx.admin` | Reload config + messages |
| `/admin debug` | `rihanx.admin` | Toggle debug |
| `/admin cache` | `rihanx.admin` | Clear caches |
| `/admin config` | `rihanx.admin` | Dump key config values |
| `/admin cancel` | `rihanx.admin` | Cancel your active search |

---

## Sleep / night skip

By default Minecraft can skip night when only some players sleep. RihanX forces sunrise **only when every required online player is in a bed**:

- Sets `playersSleepingPercentage` to **100** on overworlds
- Cancels `TimeSkipEvent` (NIGHT_SKIP) if anyone required is still awake
- Shows who is still awake in chat

Config (`config.yml` â†’ `sleep`):

| Option | Default | Meaning |
|--------|---------|---------|
| `enabled` | `true` | Master switch |
| `players-sleeping-percentage` | `100` | Vanilla gamerule |
| `same-world-only` | `true` | Only players in that overworld must sleep |
| `ignore-other-dimensions` | `true` | Nether/End players do not block sunrise |
| `ignore-spectators` / `ignore-vanished` | `true` | Those players do not need to sleep |

After changing config: `/admin reload`. Missing keys are merged from the jar defaults via `ConfigMerger`.

## Timezone

`/server info` clock is always **IST (`Asia/Kolkata`)**. RihanX forces this on reload.

### EssentialsX â€œout of dateâ€ chat message

That red line (`You're N EssentialsX dev build(s) out of date!`) comes from **EssentialsX**, not RihanX. To hide it, in `plugins/Essentials/config.yml` set:

```yaml
update-check: false
```

Then restart or `/ess reload`. Or update EssentialsX from https://essentialsx.net/downloads.html.

## PlaceholderAPI (optional)

Soft-depend. When PlaceholderAPI is installed:

| Placeholder | Value |
|-------------|--------|
| `%rihanx_homes_count%` | Player home count |
| `%rihanx_homes%` | Comma-separated home names |
| `%rihanx_warps_count%` | Server warp count |

## Configuration

- `config.yml` â€” search, teleport, cooldowns, slime, particles, database, cache, performance, protection, edit, sleep, homes, tpa, kits (incl. `kits.first-join-kit`), **timezone (`general.timezone`)**, **item.enchant XP**
- `messages.yml` â€” every user-facing string (MiniMessage + legacy)
- `kits.yml` â€” kit definitions
- `permissions.yml` â€” permission node list (plugin.yml is authoritative for Bukkit)
- `protection.yml` / `regions.yml` / `homes.yml` / `warps.yml` â€” created at runtime

Optional SQLite back-location storage: set `database.enabled: true`.

---

## Architecture

```text
com.rihanx
â”œâ”€â”€ api            Public facade + permission constants
â”œâ”€â”€ commands       /rx + standalone module router
â”‚   â””â”€â”€ modules    Home, Warp, Tpa, Kit, Chat, Afk, Spawn, Protect, Edit handlers
â”œâ”€â”€ tabcomplete    Tab completion (both styles)
â”œâ”€â”€ managers       Config, messages, cooldowns, freeze, vanish, god, back, afk, commands
â”œâ”€â”€ chat           Private messages (/msg, /r) with vanish + reply tracking
â”œâ”€â”€ spawn          /spawn + /setspawn (world spawn + "spawn" warp)
â”œâ”€â”€ base           /base house templates (hut, cottage, bungalow, villa, village)
â”œâ”€â”€ home / warp / kits / teleport (incl. TPA)
â”œâ”€â”€ protection     WorldGuard-lite (flags, regions, owners, priority)
â”œâ”€â”€ edit           WorldEdit-lite (selection, clipboard, history, expand)
â”œâ”€â”€ placeholders   Soft-depend PlaceholderAPI
â”œâ”€â”€ slime / world / chunk / player / inventory / items
â”œâ”€â”€ search         Biome/structure + block searches
â”œâ”€â”€ performance    TPS / memory / entity reports
â”œâ”€â”€ gui            Confirm + info GUIs
â”œâ”€â”€ cache / scheduler / database / models / listeners / utils
â””â”€â”€ RihanX.java    Main plugin
```

Searches run off-thread via `CompletableFuture` + Bukkit scheduler and support cancellation.

---

## Repository

Public source: https://github.com/im-rihan/RihanX

---

## License

MIT â€” see [LICENSE](LICENSE).
