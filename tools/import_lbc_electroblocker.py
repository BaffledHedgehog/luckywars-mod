#!/usr/bin/env python3
"""Import the legacy Electroblocker accessory as a native SkyWars item."""

from __future__ import annotations

import json

from import_game_resources import migrate_loot_schema
from import_lbc_electro_gear import build_single_entry_table, write_table
from import_lbc_loadouts import ROOT, SOURCE


SOURCE_TABLE = SOURCE / "loot_table/tuntijaa_electro_shield.json"
DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/electroblocker.json"
LEGACY_PROXY = "minecraft:repeating_command_block"
ITEM_ID = "skywars2:electroblocker"
REQUIRED_FLAGS = ("transformable:1", "accessory:1", "electroblocker:1", "instantitem:1")
MODEL = "lbc:electroblocker"


def select_entry(table: dict) -> tuple[dict, dict]:
    matches = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != LEGACY_PROXY:
                continue
            functions = entry.get("modifier", [])
            functions = functions if isinstance(functions, list) else [functions]
            custom_data = next((function.get("tag", "") for function in functions
                                if function.get("type") == "minecraft:set_custom_data"), "")
            components = next((function.get("components", {}) for function in functions
                               if function.get("type") == "minecraft:set_components"), {})
            if all(flag in custom_data for flag in REQUIRED_FLAGS) and (
                    components.get("minecraft:item_model") == MODEL):
                matches.append((pool, entry))
    if len(matches) != 1:
        raise SystemExit(f"Expected one Electroblocker proxy in {SOURCE_TABLE}, found {len(matches)}")
    return matches[0]


def main() -> None:
    table = json.loads(SOURCE_TABLE.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    source_pool, entry = select_entry(table)
    entry = json.loads(json.dumps(entry))
    entry["name"] = ITEM_ID
    write_table(DESTINATION, build_single_entry_table(table, source_pool, entry))
    print(f"Imported Electroblocker to {DESTINATION.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
