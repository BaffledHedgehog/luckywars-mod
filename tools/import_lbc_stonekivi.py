#!/usr/bin/env python3
"""Import the Stonekivi wand family and its actual Tannerkivi crafting donor."""

from __future__ import annotations

import argparse
import json

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family, build_single_entry_table, write_table


PORTED = ROOT / "src/main/resources/data/lbc/loot_table/ported"
STONE_SOURCE = SOURCE / "loot_table/tuntijaa_stone_tanner.json"
STONE_DEST = PORTED / "stonekivi_wand.json"
ACADEMY_STONE_DEST = PORTED / "academy_stonekivi_wand.json"
TANNER_SOURCE = SOURCE / "loot_table/transformables_arcanum.json"
TANNER_DEST = PORTED / "tannerkivi.json"


def select_tannerkivi(table: dict) -> tuple[dict, dict]:
    found = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != "minecraft:repeating_command_block":
                continue
            functions = entry.get("functions", entry.get("modifier", []))
            functions = functions if isinstance(functions, list) else [functions]
            data = next((fn.get("tag", "") for fn in functions
                         if fn.get("function", fn.get("type")) == "minecraft:set_custom_data"), "")
            model_fn = next((fn for fn in functions
                             if fn.get("function", fn.get("type")) == "minecraft:set_components"), {})
            if "tannerkivi:1" in data and model_fn.get("components", {}).get("minecraft:item_model") == "lbc:tannerkivi":
                found.append((pool, entry))
    if len(found) != 1:
        raise SystemExit(f"Expected one real Tannerkivi donor in {TANNER_SOURCE}; found {len(found)}")
    return found[0]


def import_tannerkivi() -> None:
    table = json.loads(TANNER_SOURCE.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    pool, donor = select_tannerkivi(table)
    donor = json.loads(json.dumps(donor))
    donor["name"] = "skywars2:tannerkivi"
    write_table(TANNER_DEST, build_single_entry_table(table, pool, donor))
    print(f"Imported Tannerkivi donor payload to {TANNER_DEST.relative_to(ROOT)}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stonekivi", action="store_true", help="import only Stonekivi and Academy Stonekivi")
    parser.add_argument("--tannerkivi", action="store_true", help="import only the actual Tannerkivi donor")
    args = parser.parse_args()
    if args.tannerkivi and not args.stonekivi:
        import_tannerkivi()
        return
    if args.stonekivi and not args.tannerkivi:
        import_stonekivi()
        return
    import_stonekivi()
    import_tannerkivi()


def import_stonekivi() -> None:
    import_wand_family(
        STONE_SOURCE,
        STONE_DEST,
        ACADEMY_STONE_DEST,
        marker="stonekivi_wand",
        base_id="skywars2:stonekivi_wand",
        academy_id="skywars2:academy_stonekivi_wand",
        academy_modifiers=("add_upgrade_desc", "tiers/x"),
        academy_tag_fields=("upgradable:0", "upgrade_available:1", "upgraded:1", "mode:1"),
        data_marker="stonekivi_wand:1",
    )


if __name__ == "__main__":
    main()
