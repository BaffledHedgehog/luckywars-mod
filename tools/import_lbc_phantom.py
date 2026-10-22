#!/usr/bin/env python3
"""Import only Phantom Knives; keep the unfinished shooter random pool disabled."""
from __future__ import annotations
import json
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, select_ordinary_wand_entry, write_table


def main() -> None:
    source = SOURCE / "loot_table/transformables_shooter.json"
    table = json.loads(source.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    pool, entry = select_ordinary_wand_entry(table, "phantom_knifes", source, "phantomknifes:1")
    entry = json.loads(json.dumps(entry))
    entry["name"] = "skywars2:phantom_knives"
    target = ROOT / "src/main/resources/data/lbc/loot_table/ported/phantom_knives.json"
    write_table(target, build_single_entry_table(table, pool, entry))
    print(f"Imported Phantom Knives: {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
