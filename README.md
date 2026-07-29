# RihanX

Production-ready Paper utility suite for **Minehut / Paper 26.2** (`paper-api 26.2.build.84-stable`).

Root commands: `/rihanx`, `/rx`, `/rihan`

Permission prefix: `rihanx.*`

---

## Requirements

| Item | Version |
|------|---------|
| Server | Paper **26.2** |
| Java | **25+** (required by Paper 26.2) |
| Build | Maven 3.9+ |

> Note: The prompt listed Java 21, but Paper API `26.2.build.84-stable` requires Java 25. This project targets Java 25 so `mvn clean package` succeeds.

---

## Installation

1. Build: `mvn clean package`
2. Copy `target/RihanX-1.0.0.jar` into `plugins/`
3. Restart the server
4. Configure `plugins/RihanX/config.yml` and `messages.yml`
5. `/rx admin reload` to reload configs

---

## Build

```bash
mvn clean package
```

Upload **`RihanX-1.0.0.jar`** (not any `original-*` jar).

---

## Command reference

All modules are under `/rx` (or `/rihanx` / `/rihan`).

### Help

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx help` | `rihanx.use` | List modules |

### Slime

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx slime` | `rihanx.slime` | Check current chunk |
| `/rx slime nearest` | `rihanx.slime.nearest` | Nearest slime chunk |
| `/rx slime search <radius>` | `rihanx.slime.search` | Async radius search |
| `/rx slime density <radius>` | `rihanx.slime.density` | Density + best 3×3 farm |
| `/rx slime map [radius]` | `rihanx.slime.map` | ASCII slime map |
| `/rx slime tp` | `rihanx.slime.tp` | Teleport to nearest slime |

### World

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx world info` | `rihanx.world.info` | World details |
| `/rx world seed` | `rihanx.world.seed` | World seed |
| `/rx world weather <clear\|rain\|thunder>` | `rihanx.world.weather` | Set weather |
| `/rx world difficulty <diff>` | `rihanx.world.difficulty` | Set difficulty |
| `/rx world time <day\|night\|…\|ticks>` | `rihanx.world.time` | Set time |
| `/rx world border` | `rihanx.world.border` | Border info |
| `/rx world spawn` | `rihanx.world.spawn` | Show spawn |
| `/rx world setspawn` | `rihanx.world.setspawn` | Set spawn to you |

### Find

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx find biome <biome>` | `rihanx.find.biome` | Async biome locate |
| `/rx find structure <structure>` | `rihanx.find.structure` | Async structure locate |

Supports every vanilla biome and structure (tab-complete).

### Chunk

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx chunk info` | `rihanx.chunk.info` | Chunk details |
| `/rx chunk load` | `rihanx.chunk.load` | Force-load chunk |
| `/rx chunk unload` | `rihanx.chunk.unload` | Unload chunk |
| `/rx chunk regenerate` | `rihanx.chunk.regenerate` | Regenerate chunk |
| `/rx chunk border` | `rihanx.chunk.border` | Block border coords |
| `/rx chunk entities` | `rihanx.chunk.entities` | Entity count |
| `/rx chunk tileentities` | `rihanx.chunk.tileentities` | Tile entity count |

### Teleport

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx tp pos <x> <y> <z> [world]` | `rihanx.tp.pos` | Coordinate teleport |
| `/rx tp player <name>` | `rihanx.tp.player` | Teleport to player |
| `/rx tp world <name>` | `rihanx.tp.world` | Teleport to world spawn |
| `/rx tp biome <biome>` | `rihanx.tp.biome` | Find + teleport to biome |
| `/rx tp structure <name>` | `rihanx.tp.structure` | Find + teleport to structure |
| `/rx tp chunk <x> <z>` | `rihanx.tp.chunk` | Chunk center |
| `/rx tp random` | `rihanx.tp.random` | Random safe teleport |
| `/rx tp safe` | `rihanx.tp.safe` | Safe spot near you |
| `/rx tp back` / `/rx back` | `rihanx.tp.back` | Previous location |

### Player

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx player info [player]` | `rihanx.player.info` | Player details |
| `/rx player heal [player]` | `rihanx.player.heal` | Heal |
| `/rx player feed [player]` | `rihanx.player.feed` | Feed |
| `/rx player fly [player]` | `rihanx.player.fly` | Toggle flight |
| `/rx player god [player]` | `rihanx.player.god` | Toggle god mode (no damage/hunger/fire) |
| `/rx player gamemode <mode> [player]` | `rihanx.player.gamemode` | Set gamemode (`gm` alias) |
| `/rx player speed <value> [player]` | `rihanx.player.speed` | Walk/fly speed |
| `/rx player freeze <player>` | `rihanx.player.freeze` | Freeze |
| `/rx player unfreeze <player>` | `rihanx.player.unfreeze` | Unfreeze |
| `/rx player vanish` | `rihanx.player.vanish` | Toggle vanish |
| `/rx player cleareffects [player]` | `rihanx.player.cleareffects` | Clear potion effects |
| `/rx player ping [player]` | `rihanx.player.ping` | Ping |

### Inventory

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx inventory see <player>` | `rihanx.inventory.see` | View inventory |
| `/rx inventory ender <player>` | `rihanx.inventory.ender` | View ender chest |
| `/rx inventory clear [player]` | `rihanx.inventory.clear` | Clear inventory |
| `/rx inventory repair [player]` | `rihanx.inventory.repair` | Repair all items |

### Items

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx item info` | `rihanx.item.info` | Held item info |
| `/rx item give <material> [amount] [player]` | `rihanx.item.give` | Give items (`rihanx.item.give.others` for others) |
| `/rx item rename <name…>` | `rihanx.item.rename` | Rename (MiniMessage) |
| `/rx item lore <text…>` | `rihanx.item.lore` | Set lore |
| `/rx item enchant <enchant> [level]` | `rihanx.item.enchant` | Add enchant |
| `/rx item repair` | `rihanx.item.repair` | Repair held item |

Examples: `/rx item give diamond 64` · `/rx item give netherite_sword 1 Steve`

### Search

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx search slime [radius]` | `rihanx.search.slime` | Slime chunk search |
| `/rx search cave [radius]` | `rihanx.search.cave` | Cave air pockets |
| `/rx search lava [radius]` | `rihanx.search.lava` | Lava |
| `/rx search water [radius]` | `rihanx.search.water` | Water |
| `/rx search spawner [radius]` | `rihanx.search.spawner` | Spawners |
| `/rx search village [radius]` | `rihanx.search.village` | Villages |

### Performance / server

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx server info` | `rihanx.server.info` | Server overview |
| `/rx server tps` | `rihanx.server.tps` | TPS |
| `/rx server mspt` | `rihanx.server.mspt` | MSPT |
| `/rx server memory` | `rihanx.server.memory` | Memory |
| `/rx server uptime` | `rihanx.server.uptime` | Uptime |
| `/rx performance chunks` | `rihanx.performance.chunks` | Nearby loaded chunks |
| `/rx performance entities` | `rihanx.performance.entities` | Entity breakdown |

### Admin

| Command | Permission | Description |
|---------|------------|-------------|
| `/rx admin reload` | `rihanx.admin` | Reload config + messages |
| `/rx admin debug` | `rihanx.admin` | Toggle debug |
| `/rx admin cache` | `rihanx.admin` | Clear caches |
| `/rx admin config` | `rihanx.admin` | Dump key config values |
| `/rx admin cancel` | `rihanx.admin` | Cancel your active search |

---

## Configuration

- `config.yml` — search, teleport, cooldowns, slime, particles, database, cache, performance
- `messages.yml` — every user-facing string (MiniMessage + legacy)
- `permissions.yml` — permission node list (plugin.yml is authoritative for Bukkit)

Optional SQLite back-location storage: set `database.enabled: true`.

---

## Architecture

```text
com.rihanx
├── api            Public facade + permission constants
├── commands       /rx router
├── tabcomplete    Tab completion
├── managers       Config, messages, cooldowns, freeze, vanish, god, back, commands
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
