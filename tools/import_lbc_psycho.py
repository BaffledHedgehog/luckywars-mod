#!/usr/bin/env python3
"""Import source Psychowand and Psychoblind payloads without redesigning them."""

from __future__ import annotations

from import_lbc_fireblaze import PORTED, STAFF_SOURCE, import_item
from import_lbc_loadouts import SOURCE


def main() -> None:
    import_item(STAFF_SOURCE, PORTED / "psychowand.json", "psycho_wand",
                "psychowand:1", "skywars2:psychowand")
    import_item(SOURCE / "loot_table/tuntijaa_blind_psycho.json",
                PORTED / "psychoblind_wand.json", "psychoblind_wand",
                "psychoblind_wand:1", "skywars2:psychoblind_wand")


if __name__ == "__main__":
    main()
