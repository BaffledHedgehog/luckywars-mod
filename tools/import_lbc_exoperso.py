#!/usr/bin/env python3
"""Import source-exact Exoperso payloads through the shared wand importer."""
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family

SOURCE_TABLE = SOURCE / "loot_table/transformables_staff.json"
DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/exoperso_wand.json"
ACADEMY_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_exoperso_wand.json"
ITEM_ID = "skywars2:exoperso_wand"
ACADEMY_ITEM_ID = "skywars2:academy_exoperso_wand"


def import_exoperso_family() -> None:
    # Its name key and custom-data flag differ in the source pack.
    import_wand_family(
        SOURCE_TABLE, DESTINATION, ACADEMY_DESTINATION,
        "megumin_wand", ITEM_ID, ACADEMY_ITEM_ID,
        academy_modifiers=("add_upgrade_desc", "tiers/s"), data_marker="exoperso",
    )


if __name__ == "__main__":
    import_exoperso_family()
