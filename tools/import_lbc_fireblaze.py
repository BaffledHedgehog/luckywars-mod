#!/usr/bin/env python3
"""Import the ordinary Blaze Wand and Fireblaze Wand loot payloads."""

from __future__ import annotations

import json

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, write_table


STAFF_SOURCE = SOURCE / "loot_table/transformables_staff.json"
FIREBLAZE_SOURCE = SOURCE / "loot_table/tuntijaa_fire_blaze.json"
PORTED = ROOT / "src/main/resources/data/lbc/loot_table/ported"


def select_entry(table: dict, name_key: str, custom_data_marker: str) -> tuple[dict, dict]:
    matches = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != "minecraft:carrot_on_a_stick":
                continue
            modifiers = entry.get("modifier", entry.get("functions", []))
            modifiers = modifiers if isinstance(modifiers, list) else [modifiers]
            serialized = json.dumps(modifiers, ensure_ascii=False)
            if name_key not in serialized or custom_data_marker not in serialized:
                continue
            matches.append((pool, entry))
    if len(matches) != 1:
        raise SystemExit(
            f"Expected one {name_key} item marked {custom_data_marker} in source; found {len(matches)}"
        )
    return matches[0]


def import_item(source, destination, name_key: str, marker: str, native_id: str) -> None:
    table = json.loads(source.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    pool, entry = select_entry(table, name_key, marker)
    entry = json.loads(json.dumps(entry))
    entry["name"] = native_id
    write_table(destination, build_single_entry_table(table, pool, entry))
    print(f"Imported {name_key} as {native_id} to {destination.relative_to(ROOT)}")


def main() -> None:
    import_item(STAFF_SOURCE, PORTED / "blaze_wand.json", "blaze_wand",
                "fallingstars:1", "skywars2:blaze_wand")
    import_item(FIREBLAZE_SOURCE, PORTED / "fireblaze_wand.json", "fireblaze_wand",
                "fireblaze_wand:1", "skywars2:fireblaze_wand")


if __name__ == "__main__":
    main()
