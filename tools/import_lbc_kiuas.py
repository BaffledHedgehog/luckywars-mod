#!/usr/bin/env python3
"""Import the original Kiuaskivi and Sun Arcanum loot entries."""

from __future__ import annotations

import json

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, write_table


PORTED = ROOT / "src/main/resources/data/lbc/loot_table/ported"
TRANSFORMABLES_SOURCE = SOURCE / "loot_table/transformables_arcanum.json"
SUN_SOURCE = SOURCE / "loot_table/tuntijaa_fire_kiuas.json"
KIUASKIVI_DEST = PORTED / "kiuaskivi.json"
SUN_DEST = PORTED / "sun_arcanum.json"


def select_item(table: dict, *, item_model: str, custom_data_marker: str) -> tuple[dict, dict]:
    found = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != "minecraft:repeating_command_block":
                continue
            functions = entry.get("functions", entry.get("modifier", []))
            functions = functions if isinstance(functions, list) else [functions]
            components = next((fn.get("components", {}) for fn in functions
                               if fn.get("function", fn.get("type")) == "minecraft:set_components"), {})
            data = next((fn.get("tag", "") for fn in functions
                         if fn.get("function", fn.get("type")) == "minecraft:set_custom_data"), "")
            if components.get("minecraft:item_model") == item_model and custom_data_marker in data:
                found.append((pool, entry))
    if len(found) != 1:
        raise SystemExit(f"Expected one {item_model} donor with {custom_data_marker} in source; found {len(found)}")
    return found[0]


def import_item(source, destination, *, item_model: str, custom_data_marker: str,
                native_id: str) -> None:
    table = json.loads(source.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    source_pool, donor = select_item(table, item_model=item_model,
                                     custom_data_marker=custom_data_marker)
    donor = json.loads(json.dumps(donor))
    donor["name"] = native_id
    write_table(destination, build_single_entry_table(table, source_pool, donor))
    print(f"Imported {item_model} donor as {native_id} to {destination.relative_to(ROOT)}")


def main() -> None:
    import_item(TRANSFORMABLES_SOURCE, KIUASKIVI_DEST,
                item_model="lbc:kiuaskivi", custom_data_marker="kiuaskivi:1",
                native_id="skywars2:kiuaskivi")
    import_item(SUN_SOURCE, SUN_DEST,
                item_model="lbc:the_sun", custom_data_marker="sun_arcanum:1",
                native_id="skywars2:sun_arcanum")


if __name__ == "__main__":
    main()
