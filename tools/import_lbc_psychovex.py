#!/usr/bin/env python3
"""Import source SummonerVexes and Psychovex payloads without redesigning them."""

from __future__ import annotations

import json
import re

from import_lbc_fireblaze import PORTED, import_item
from import_lbc_loadouts import ROOT, SOURCE


def import_summon_presets() -> None:
    """Keep typed SNBT verbatim for the native server entity DFU codec."""
    directory = SOURCE / "function/workingitems/summoner"
    cultists = (directory / "psychovex_spawn.mcfunction").read_text(encoding="utf-8-sig")
    vexes = (directory / "vexes.mcfunction").read_text(encoding="utf-8-sig")
    kinds = ("strangeness", "chaos", "luck", "omnipresence", "obsession", "hypocrisy")
    presets = {}
    vex_matches = re.findall(r"^summon (minecraft:vex) \S+ \S+ \S+ (\{.*\})$", vexes, re.MULTILINE)
    if len(vex_matches) != 4 or len(set(vex_matches)) != 1:
        raise SystemExit("Expected four identical source Vex entity payloads")
    entity_id, nbt = vex_matches[0]
    presets["vex"] = '{id:"' + entity_id + '",' + nbt[1:]
    for index, kind in enumerate(kinds, 1):
        matches = re.findall(
            rf"^execute if score #random6 lbc.math matches {index} run summon (minecraft:[a-z_]+) ~ ~ ~ (\{{.*\}})$",
            cultists, re.MULTILINE)
        if len(matches) != 1:
            raise SystemExit(f"Expected one source cultist preset for {kind}")
        entity_id, nbt = matches[0]
        presets[kind] = '{id:"' + entity_id + '",' + nbt[1:]
    # Source already uses 1.21.5+ equipment/drop_chances, unlike the unrelated
    # Academy structure's 4189. 4440 is Mojang 1.21.8 version.json world_version;
    # preserve raw SNBT and let native ENTITY DFU migrate nested item components.
    payload = {"originalDataVersion": 4440, "presets": presets}
    destination = ROOT / "src/main/resources/skywars2/summons_presets.json"
    destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8", newline="\n")
    print(f"Imported seven raw entity presets to {destination.relative_to(ROOT)}")


def main() -> None:
    import_item(SOURCE / "loot_table/transformables_summons.json",
                PORTED / "summoner_vexes_wand.json", "vex_soul",
                "summonervexes:1", "skywars2:summoner_vexes_wand")
    import_item(SOURCE / "loot_table/tuntijaa_blind_vex.json",
                PORTED / "psychovex_wand.json", "psychovex_wand",
                "psychovex_wand:1", "skywars2:psychovex_wand")
    import_summon_presets()


if __name__ == "__main__":
    main()
