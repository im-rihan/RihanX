#!/usr/bin/env python3
"""Normalize README + messages.yml encoding and refresh portal/station docs."""
from pathlib import Path
import re

ROOT = Path(r"D:\Work\minecraft\RihanX")


def fix_mojibake(text: str) -> str:
    fixes = {
        "\u00c2\u00b7": " · ",
        "Â·": " · ",
        "â€”": "—",
        "â€“": "–",
        "â€¦": "…",
        "â†’": "→",
        "Ã—": "×",
        "â€™": "'",
        "â€˜": "'",
        "â€œ": '"',
        "â€": '"',
        "âž": "→",
        "�": "",
    }
    for a, b in fixes.items():
        text = text.replace(a, b)
    text = re.sub(r" â€. ", " — ", text)
    text = re.sub(r"â€.", "—", text)
    text = re.sub(r" +· +", " · ", text)
    text = re.sub(r"\uFFFD", "", text)
    return text


def patch_readme(text: str) -> str:
    text = fix_mojibake(text)

    text = re.sub(
        r"\| `iron` \|[^|\n]+\|",
        "| `iron` | Dry deck, water push, lava on trapdoors, hoppers (add villagers + zombie) |",
        text,
    )
    text = re.sub(
        r"\| `xp` \|[^|\n]+\|",
        "| `xp` | AFK house, hopper kill floor, magma edges, working door buttons |",
        text,
    )
    text = re.sub(
        r"\| `cane` / `bamboo` \|[^|\n]+\|",
        "| `cane` / `bamboo` | Observers + pistons facing crops, hopper trench |",
        text,
    )

    portal_station = """
### Portals - `/portal`

Create two pads and link them to teleport between locations (step on the plate or press the button).

| Command | Permission | Description |
|---------|------------|-------------|
| `/portal create <name>` | `rihanx.portal.create` | Place a portal pad at your feet |
| `/portal link <a> <b>` | `rihanx.portal.link` | Link two portals both ways |
| `/portal delete <name>` | `rihanx.portal.delete` | Remove a portal |
| `/portal list` | `rihanx.portal.list` | List portals and links |
| `/portal tp <name>` | `rihanx.portal.tp` | Teleport to a portal by command |

```text
/portal create home
# walk elsewhere
/portal create shop
/portal link home shop
# step on either pressure plate
```

Also `/rx portal …`, `/portals`.

### Stations / railways - `/station`

Paste train stations and track segments. Place minecarts after paste; link stops with `/portal`.

| Command | Permission | Description |
|---------|------------|-------------|
| `/station` / `/station list` | `rihanx.station` | List templates |
| `/station <name>` | `rihanx.station.build` | Paste a station / rail blueprint |
| `/station undo` | `rihanx.station.undo` | Undo last paste |

| Template | What you get |
|----------|----------------|
| `station` | Platform, powered rails, waiting pavilion |
| `depot` | Storage shed, track, minecart bay |
| `crossing` | 4-way rail crossing with signal lamps |
| `rail` | ~25 block powered railway segment |
| `terminal` | End-of-line buffers + ticket booth |

```text
/station station
/station rail
/station depot
/station undo
```

Aliases: `/train`, `/railway`, `/rx station …`.

"""

    if "### Portals" not in text and "### Stations" not in text:
        for marker in ("### Kits", "### Bridge", "### Builder"):
            if marker in text:
                text = text.replace(marker, portal_station + marker, 1)
                break

    return text


def main() -> None:
    readme = ROOT / "README.md"
    messages = ROOT / "src" / "main" / "resources" / "messages.yml"
    readme.write_text(patch_readme(readme.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
    messages.write_text(fix_mojibake(messages.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
    print("updated README.md and messages.yml")


if __name__ == "__main__":
    main()
