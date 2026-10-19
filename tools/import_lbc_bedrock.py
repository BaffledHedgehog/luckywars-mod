#!/usr/bin/env python3
"""Import the legacy Bedrock wand and prepare the legacy Wall Wand donor loot."""

from __future__ import annotations

import re

from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family
from import_lbc_wall import import_wall_family


BEDROCK_SOURCE = SOURCE / "loot_table/tuntijaa_stone_wall.json"
BEDROCK_UPGRADE = SOURCE / "function/other/magic_academy/upgrade/bedrock_wand.mcfunction"

PORTED = ROOT / "src/main/resources/data/lbc/loot_table/ported"
BEDROCK_DESTINATION = PORTED / "bedrock_wand.json"
ACADEMY_BEDROCK_DESTINATION = PORTED / "academy_bedrock_wand.json"


def academy_modifiers_from_source() -> tuple[str, ...]:
    source = BEDROCK_UPGRADE.read_text(encoding="utf-8-sig")
    modifiers = re.findall(
        r"^\s*item modify entity @s weapon\.mainhand lbc:([a-z0-9_./-]+)\s*$",
        source,
        flags=re.MULTILINE,
    )
    if len(modifiers) != 1:
        raise SystemExit(
            f"Expected the Bedrock Academy upgrade to apply one source item modifier in {BEDROCK_UPGRADE}; "
            f"found {modifiers!r}"
        )
    return tuple(modifiers)


def main() -> None:
    # The family upgrade adds tier SS; the shared Academy flow contributes
    # the upgrade lore and state flags before handing back the result.
    import_wand_family(
        BEDROCK_SOURCE,
        BEDROCK_DESTINATION,
        ACADEMY_BEDROCK_DESTINATION,
        marker="bedrock_wand",
        base_id="skywars2:bedrock_wand",
        academy_id="skywars2:academy_bedrock_wand",
        academy_modifiers=("add_upgrade_desc",) + academy_modifiers_from_source(),
        academy_tag_fields=("upgradable:0", "upgrade_available:1", "upgraded:1", "mode:1"),
        data_marker="bedrock_wand:1",
    )

    import_wall_family()


if __name__ == "__main__":
    main()
