#!/usr/bin/env python3
"""Import the lightning egg donor and ordinary/Academy Infbolt payloads."""
from __future__ import annotations

import json

from import_game_resources import migrate_loot_schema
from import_lbc_fireblaze import PORTED
from import_lbc_loadouts import SOURCE
from import_lbc_wands import import_wand_family, write_table


def main() -> None:
    # The source egg table is already a single entry. Preserve its count (8),
    # lightning entity_data and all presentation functions without reconstruction.
    donor = json.loads((SOURCE / "loot_table/lightning_bolt_egg.json").read_text(encoding="utf-8-sig"))
    migrate_loot_schema(donor)
    entry = donor["pools"][0]["entries"][0]
    assert entry["name"] == "minecraft:panda_spawn_egg"
    entry["name"] = "skywars2:bolt_material"
    write_table(PORTED / "bolt_material.json", donor)
    import_wand_family(
        SOURCE / "loot_table/tuntijaa_bolts_reforging.json",
        PORTED / "infbolt_wand.json", PORTED / "academy_infbolt_wand.json",
        "infbolt_wand", "skywars2:infbolt_wand", "skywars2:academy_infbolt_wand",
        academy_modifiers=("add_upgrade_desc", "tiers/x"),
        academy_tag_fields=("upgraded:1", "mode:1"), data_marker="infbolt_wand:1",
    )


if __name__ == "__main__":
    main()
