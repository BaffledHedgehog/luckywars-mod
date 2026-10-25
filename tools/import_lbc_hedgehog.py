#!/usr/bin/env python3
"""Import only Hedgehog native leaves and its two exact reward aliases."""
from __future__ import annotations
import copy
import json
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family, build_single_entry_table, write_table

PORTED = ROOT / "src/main/resources/data/lbc/loot_table/ported"
SPIKES = ("ender_spike", "blind_spike", "explosive_spike", "spike",
          "wither_spike", "poison_spike", "impulse_spike")

def import_hedgehog_family():
    import_wand_family(
        SOURCE / "loot_table/transformables_staff.json",
        PORTED / "hedgehog_wand.json", PORTED / "academy_hedgehog_wand.json",
        marker="hedgehog_wand", data_marker="baffledhedgehog:1",
        base_id="skywars2:hedgehog_wand", academy_id="skywars2:academy_hedgehog_wand",
        academy_modifiers=("add_upgrade_desc", "tiers/s"))
    wall = json.loads((SOURCE / "loot_table/hedgehog_wall.json").read_text(encoding="utf-8-sig"))
    migrate_loot_schema(wall)
    assert len(wall["pools"]) == 1 and len(wall["pools"][0]["entries"]) == 7
    for entry, name in zip(wall["pools"][0]["entries"], SPIKES, strict=True):
        assert entry["name"] == "minecraft:snowball"
        entry["name"] = "skywars2:" + name
        write_table(PORTED / (name + ".json"), build_single_entry_table(wall, wall["pools"][0], copy.deepcopy(entry)))
    write_table(PORTED.parent / "hedgehog_wall.json", wall)
    god = json.loads((SOURCE / "loot_table/god.json").read_text(encoding="utf-8-sig"))
    migrate_loot_schema(god)
    assert god["pools"][0]["entries"][0]["name"] == "minecraft:carrot_on_a_stick"
    god["pools"][0]["entries"][0]["name"] = "skywars2:god"
    write_table(PORTED / "god.json", god)
    write_table(PORTED.parent / "god.json", god)

if __name__ == "__main__":
    import_hedgehog_family()
