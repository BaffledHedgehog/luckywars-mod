#!/usr/bin/env python3
"""Import truefallingstars and Exahoming ordinary/Academy source payloads."""
from __future__ import annotations

from import_lbc_fireblaze import PORTED, STAFF_SOURCE
from import_lbc_loadouts import SOURCE
from import_lbc_wands import import_wand_family

FAMILIES = (
    (STAFF_SOURCE, "falling_stars_wand", "truefallingstars:1", "a"),
    (SOURCE / "loot_table/tuntijaa_air_homing.json", "exahoming_wand", "exahoming_wand:1", "x"),
)


def main() -> None:
    for source, name, marker, tier in FAMILIES:
        import_wand_family(
            source, PORTED / f"{name}.json", PORTED / f"academy_{name}.json",
            name, f"skywars2:{name}", f"skywars2:academy_{name}",
            academy_modifiers=("add_upgrade_desc", f"tiers/{tier}"),
            data_marker=marker,
        )


if __name__ == "__main__":
    main()
