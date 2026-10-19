#!/usr/bin/env python3
"""Import Lightning Bolts and Boltbolt ordinary/Academy source payloads."""
from __future__ import annotations

from import_lbc_fireblaze import PORTED, STAFF_SOURCE
from import_lbc_loadouts import SOURCE
from import_lbc_wands import import_wand_family

FAMILIES = (
    (STAFF_SOURCE, "lightning_bolts_wand", "lightning_bolt_wand", "lightning_bolts:1", "s"),
    (SOURCE / "loot_table/tuntijaa_bolts_bolts.json", "boltbolt_wand", "boltbolt_wand", "boltbolt_wand:1", "ss"),
)


def main() -> None:
    for source, name, name_key, marker, tier in FAMILIES:
        import_wand_family(
            source, PORTED / f"{name}.json", PORTED / f"academy_{name}.json",
            name_key, f"skywars2:{name}", f"skywars2:academy_{name}",
            academy_modifiers=("add_upgrade_desc", f"tiers/{tier}"),
            data_marker=marker,
        )


if __name__ == "__main__":
    main()
