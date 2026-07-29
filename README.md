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
| `/item give <material> [amount] [player]` | `rihanx.item.give` | Give items |
| `/item rename <name…>` | `rihanx.item.rename` | Rename (MiniMessage) |
| `/item lore <text…>` | `rihanx.item.lore` | Set lore |
| `/item enchant <enchant> [level]` | `rihanx.item.enchant` | Add enchant |
| `/item repair` | `rihanx.item.repair` | Repair held item |

Examples: `/item give diamond 64` · `/rx item give netherite_sword 1 Steve`

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

- `config.yml` — search, teleport, cooldowns, slime, particles, database, cache, performance, protection, edit
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
