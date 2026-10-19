#!/usr/bin/env python3
"""Import the Exasperato wand and its source-defined Academy upgrade."""

from __future__ import annotations

import json

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_electro_gear import build_single_entry_table, write_table


SOURCE_TABLE = SOURCE / "loot_table/tuntijaa_air_exasperoso.json"
ACADEMY_MODIFIERS = ("add_upgrade_desc", "tiers/x")
DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/exasperato_wand.json"
ACADEMY_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_exasperato_wand.json"
ITEM_ID = "skywars2:exasperato_wand"
ACADEMY_ITEM_ID = "skywars2:academy_exasperato_wand"


def select_entry(table: dict) -> tuple[dict, dict]:
    selected = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != "minecraft:carrot_on_a_stick":
                continue
            functions = entry.get("modifier", [])
            functions = functions if isinstance(functions, list) else [functions]
            components = next((function.get("components", {}) for function in functions
                               if function.get("type") == "minecraft:set_components"), {})
            name = next((function.get("name") for function in functions
                         if function.get("type") == "minecraft:set_name"), None)
            custom_data = next((function.get("tag", "") for function in functions
                                if function.get("type") == "minecraft:set_custom_data"), "")
            if (name and "exasperato_wand" in json.dumps(name, ensure_ascii=False)
                    and "exasperato_wand:1" in custom_data
                    and components.get("minecraft:item_model") == "lbc:exasperato"
                    and components.get("minecraft:tooltip_style") == "lbc:tier_s"):
                selected.append((pool, entry))
    if len(selected) != 1:
        raise SystemExit(f"Expected one ordinary Exasperato entry in {SOURCE_TABLE}, found {len(selected)}")
    return selected[0]


def main() -> None:
    ordinary_table = json.loads(SOURCE_TABLE.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(ordinary_table)
    source_pool, ordinary_entry = select_entry(ordinary_table)
    ordinary_entry = json.loads(json.dumps(ordinary_entry))
    ordinary_entry["name"] = ITEM_ID
    write_table(DESTINATION, build_single_entry_table(ordinary_table, source_pool, ordinary_entry))

    academy_entry = json.loads(json.dumps(ordinary_entry))
    academy_entry["name"] = ACADEMY_ITEM_ID
    functions = academy_entry.get("modifier", [])
    functions = functions if isinstance(functions, list) else [functions]
    custom_data = next((function for function in functions
                        if function.get("type") == "minecraft:set_custom_data"), None)
    if custom_data is None:
        raise SystemExit("Ordinary Exasperato entry has no set_custom_data function")
    tag = custom_data.get("tag", "")
    for field in ("upgradable:0", "upgrade_available:1", "upgraded:1", "mode:1"):
        if field not in tag:
            tag = tag.rstrip("}") + f",{field}}}"
    custom_data["tag"] = tag
    for modifier_name in ACADEMY_MODIFIERS:
        modifier = json.loads((SOURCE / f"item_modifier/{modifier_name}.json")
                              .read_text(encoding="utf-8-sig"))
        migrate_loot_schema(modifier)
        functions.extend(modifier if isinstance(modifier, list) else [modifier])
    academy_entry["modifier"] = functions
    write_table(ACADEMY_DESTINATION,
                build_single_entry_table(ordinary_table, source_pool, academy_entry))
    print(f"Imported Exasperato wand payload to {DESTINATION.relative_to(ROOT)}")
    print(f"Imported Academy Exasperato wand payload to {ACADEMY_DESTINATION.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
