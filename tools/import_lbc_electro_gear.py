#!/usr/bin/env python3
"""Import the legacy Electro sword (Cyberkatana) and Electro shield items."""

from __future__ import annotations

import json

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE


DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported"
ITEMS = {
    "cyberkatana": {
        "source": SOURCE / "loot_table/cyberkatana.json",
        "item_id": "skywars2:cyberkatana",
        "base_item": "minecraft:carrot_on_a_stick",
        "required_flags": ("cyberkatana:1", "sword:1", "swordwithability:1"),
        "model": "lbc:cyberkatana",
    },
    "electro_shield": {
        "source": SOURCE / "loot_table/electro_shield.json",
        "item_id": "skywars2:electro_shield",
        "base_item": "minecraft:shield",
        "required_flags": ("energy_shield:1", "energyshield:1", "shield:1"),
        "model": None,
    },
}


def select_item_entry(table: dict, key: str, spec: dict) -> tuple[dict, dict]:
    matches = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != spec["base_item"]:
                continue
            functions = entry.get("modifier", [])
            functions = functions if isinstance(functions, list) else [functions]
            custom_data = next((function.get("tag", "") for function in functions
                                if function.get("type") == "minecraft:set_custom_data"), "")
            components = next((function.get("components", {}) for function in functions
                               if function.get("type") == "minecraft:set_components"), {})
            name = next((function.get("name") for function in functions
                         if function.get("type") == "minecraft:set_name"), None)
            if (all(flag in custom_data for flag in spec["required_flags"])
                    and name and (spec["model"] is None
                                  or components.get("minecraft:item_model") == spec["model"])):
                matches.append((pool, entry))
    if len(matches) != 1:
        raise SystemExit(
            f"Expected one {key} entry ({spec['base_item']}, flags "
            f"{spec['required_flags']}) in {spec['source']}, found {len(matches)}"
        )
    return matches[0]


def build_single_entry_table(table: dict, source_pool: dict, entry: dict) -> dict:
    payload = {key: value for key, value in table.items() if key != "pools"}
    pool = {key: value for key, value in source_pool.items() if key != "entries"}
    pool["entries"] = [entry]
    payload["pools"] = [pool]
    return payload


def write_table(destination, payload: dict) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8", newline="\n")


def main() -> None:
    for key, spec in ITEMS.items():
        table = json.loads(spec["source"].read_text(encoding="utf-8-sig"))
        migrate_loot_schema(table)
        source_pool, entry = select_item_entry(table, key, spec)
        entry = json.loads(json.dumps(entry))
        entry["name"] = spec["item_id"]
        destination = DESTINATION / f"{key}.json"
        write_table(destination, build_single_entry_table(table, source_pool, entry))
        print(f"Imported {key} payload to {destination.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
