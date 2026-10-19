#!/usr/bin/env python3
"""Import ordinary Wall Wand and its source-derived Academy upgrade."""

from __future__ import annotations

from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family


SOURCE_TABLE = SOURCE / "loot_table/transformables_staff.json"
UPGRADE_FUNCTION = SOURCE / "function/other/magic_academy/upgrade/wall.mcfunction"
PORTED = ROOT / "src/main/resources/data/lbc/loot_table/ported"
DESTINATION = PORTED / "wall_wand.json"
ACADEMY_DESTINATION = PORTED / "academy_wall_wand.json"


def academy_modifiers_from_source() -> tuple[str, ...]:
    import re

    source = UPGRADE_FUNCTION.read_text(encoding="utf-8-sig")
    modifiers = re.findall(
        r"^\s*item modify entity @s weapon\.mainhand lbc:([a-z0-9_./-]+)\s*$",
        source,
        flags=re.MULTILINE,
    )
    if len(modifiers) != 1:
        raise SystemExit(
            f"Expected one Wall Academy tier modifier in {UPGRADE_FUNCTION}; found {modifiers!r}"
        )
    return ("add_upgrade_desc", *modifiers)


def import_wall_family() -> None:
    import_wand_family(
        SOURCE_TABLE,
        DESTINATION,
        ACADEMY_DESTINATION,
        marker="wall_wand",
        base_id="skywars2:wall_wand",
        academy_id="skywars2:academy_wall_wand",
        academy_modifiers=academy_modifiers_from_source(),
        academy_tag_fields=("upgradable:0", "upgrade_available:1", "upgraded:1", "mode:1"),
        data_marker="wall:1",
    )


if __name__ == "__main__":
    import_wall_family()
