#!/usr/bin/env python3
"""Import ordinary Terra and its source-derived Magic Academy upgrade."""

from __future__ import annotations

import json
import argparse

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE


SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_terra.json"
DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/terra_wand.json"
ITEM_ID = "skywars2:terra_wand"
ACADEMY_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_terra_wand.json"
ACADEMY_ITEM_ID = "skywars2:academy_terra_wand"
ELECTRO_SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_electro.json"
ELECTRO_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/electro_wand.json"
ELECTRO_ITEM_ID = "skywars2:electro_wand"
ACADEMY_ELECTRO_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_electro_wand.json"
ACADEMY_ELECTRO_ITEM_ID = "skywars2:academy_electro_wand"
FIRE_SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_fotja.json"
FIRE_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/fire_wand.json"
FIRE_ITEM_ID = "skywars2:fire_wand"
ACADEMY_FIRE_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_fire_wand.json"
ACADEMY_FIRE_ITEM_ID = "skywars2:academy_fire_wand"
STONE_SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_ananta.json"
STONE_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/stone_wand.json"
STONE_ITEM_ID = "skywars2:stone_wand"
ACADEMY_STONE_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_stone_wand.json"
ACADEMY_STONE_ITEM_ID = "skywars2:academy_stone_wand"
BLIND_SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_blind.json"
BLIND_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/blind_wand.json"
BLIND_ITEM_ID = "skywars2:blind_wand"
ACADEMY_BLIND_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_blind_wand.json"
ACADEMY_BLIND_ITEM_ID = "skywars2:academy_blind_wand"
AIR_SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_air.json"
AIR_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/air_wand.json"
AIR_ITEM_ID = "skywars2:air_wand"
ACADEMY_AIR_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_air_wand.json"
ACADEMY_AIR_ITEM_ID = "skywars2:academy_air_wand"
TELEPORT_SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_teleport.json"
TELEPORT_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/teleport_wand.json"
TELEPORT_ITEM_ID = "skywars2:teleport_wand"
ACADEMY_TELEPORT_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_teleport_wand.json"
ACADEMY_TELEPORT_ITEM_ID = "skywars2:academy_teleport_wand"
RUBY_SOURCE_TABLE = SOURCE / "loot_table/sauvojen_tuntija_swargia.json"
RUBY_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/ruby_wand.json"
RUBY_ITEM_ID = "skywars2:ruby_wand"
ACADEMY_RUBY_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/academy_ruby_wand.json"
ACADEMY_RUBY_ITEM_ID = "skywars2:academy_ruby_wand"


def select_ordinary_terra_entry(table: dict) -> tuple[dict, dict]:
    selected = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != "minecraft:carrot_on_a_stick":
                continue
            functions = entry.get("modifier", [])
            functions = functions if isinstance(functions, list) else [functions]
            name = next((function.get("name") for function in functions
                         if function.get("type") == "minecraft:set_name"), None)
            custom_data = next((function.get("tag", "") for function in functions
                               if function.get("type") == "minecraft:set_custom_data"), "")
            if name and "sauvojen_wand_terra" in json.dumps(name, ensure_ascii=False) and "stterra:1" in custom_data:
                selected.append((pool, entry))
    if len(selected) != 1:
        raise SystemExit(f"Expected one ordinary Terra wand entry in {SOURCE_TABLE}, found {len(selected)}")
    return selected[0]


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


def select_ordinary_wand_entry(table: dict, marker: str, source_table, data_marker=None) -> tuple[dict, dict]:
    selected = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            if entry.get("name") != "minecraft:carrot_on_a_stick":
                continue
            functions = entry.get("modifier", [])
            functions = functions if isinstance(functions, list) else [functions]
            name = next((function.get("name") for function in functions
                         if function.get("type") == "minecraft:set_name"), None)
            custom_data = next((function.get("tag", "") for function in functions
                               if function.get("type") == "minecraft:set_custom_data"), "")
            if name and marker in json.dumps(name, ensure_ascii=False) and (data_marker or marker.replace("sauvojen_wand_", "st")) in custom_data:
                selected.append((pool, entry))
    if len(selected) != 1:
        raise SystemExit(f"Expected one {marker} wand entry in {source_table}, found {len(selected)}")
    return selected[0]


def import_terra() -> None:
    table = json.loads(SOURCE_TABLE.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    source_pool, ordinary_entry = select_ordinary_terra_entry(table)

    ordinary_entry = json.loads(json.dumps(ordinary_entry))
    ordinary_entry["name"] = ITEM_ID
    write_table(DESTINATION, build_single_entry_table(table, source_pool, ordinary_entry))

    academy_entry = json.loads(json.dumps(ordinary_entry))
    academy_entry["name"] = ACADEMY_ITEM_ID
    functions = academy_entry.get("modifier", [])
    functions = functions if isinstance(functions, list) else [functions]
    custom_data_function = next((function for function in functions
                                 if function.get("type") == "minecraft:set_custom_data"), None)
    if custom_data_function is None:
        raise SystemExit("Ordinary Terra entry has no set_custom_data function")
    tag = custom_data_function.get("tag", "")
    for field in ("upgradable:0", "upgrade_available:1", "upgraded:1", "mode:1"):
        if field not in tag:
            tag = tag.rstrip("}") + f",{field}}}"
    custom_data_function["tag"] = tag
    # Apply the original modifier payloads; design changes must not be mirrored by hand.
    for source_name in ("add_upgrade_desc", "tiers/a"):
        modifier = json.loads((SOURCE / f"item_modifier/{source_name}.json").read_text(encoding="utf-8-sig"))
        migrate_loot_schema(modifier)
        functions.extend(modifier if isinstance(modifier, list) else [modifier])
    academy_entry["modifier"] = functions
    write_table(ACADEMY_DESTINATION, build_single_entry_table(table, source_pool, academy_entry))
    print(f"Imported ordinary Terra wand payload to {DESTINATION.relative_to(ROOT)}")
    print(f"Imported Academy Terra wand payload to {ACADEMY_DESTINATION.relative_to(ROOT)}")


def import_wand_family(source_table, base_destination, academy_destination,
                       marker: str, base_id: str, academy_id: str,
                       academy_modifiers=("add_upgrade_desc", "tiers/a"),
                       academy_tag_fields=("upgradable:0", "upgrade_available:1",
                                            "upgraded:1", "mode:1"), data_marker=None) -> None:
    table = json.loads(source_table.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    source_pool, ordinary_entry = select_ordinary_wand_entry(table, marker, source_table, data_marker)

    ordinary_entry = json.loads(json.dumps(ordinary_entry))
    ordinary_entry["name"] = base_id
    write_table(base_destination,
                build_single_entry_table(table, source_pool, ordinary_entry))

    academy_entry = json.loads(json.dumps(ordinary_entry))
    academy_entry["name"] = academy_id
    functions = academy_entry.get("modifier", [])
    functions = functions if isinstance(functions, list) else [functions]
    custom_data_function = next((function for function in functions
                                 if function.get("type") == "minecraft:set_custom_data"), None)
    if custom_data_function is None:
        raise SystemExit(f"Ordinary wand entry in {source_table} has no set_custom_data function")
    tag = custom_data_function.get("tag", "")
    for field in academy_tag_fields:
        if field not in tag:
            tag = tag.rstrip("}") + f",{field}}}"
    custom_data_function["tag"] = tag
    # Apply the source Academy modifiers so its added lore and tier remain source-derived.
    for source_name in academy_modifiers:
        modifier = json.loads((SOURCE / f"item_modifier/{source_name}.json").read_text(encoding="utf-8-sig"))
        migrate_loot_schema(modifier)
        functions.extend(modifier if isinstance(modifier, list) else [modifier])
    academy_entry["modifier"] = functions
    write_table(academy_destination,
                build_single_entry_table(table, source_pool, academy_entry))
    print(f"Imported ordinary wand payload to {base_destination.relative_to(ROOT)}")
    print(f"Imported Academy wand payload to {academy_destination.relative_to(ROOT)}")


def import_electro_family() -> None:
    import_wand_family(ELECTRO_SOURCE_TABLE, ELECTRO_DESTINATION,
                       ACADEMY_ELECTRO_DESTINATION, "sauvojen_wand_electro",
                       ELECTRO_ITEM_ID, ACADEMY_ELECTRO_ITEM_ID)


def import_fire_family() -> None:
    import_wand_family(FIRE_SOURCE_TABLE, FIRE_DESTINATION,
                       ACADEMY_FIRE_DESTINATION, "sauvojen_wand_fire",
                       FIRE_ITEM_ID, ACADEMY_FIRE_ITEM_ID)


def import_stone_family() -> None:
    import_wand_family(STONE_SOURCE_TABLE, STONE_DESTINATION,
                       ACADEMY_STONE_DESTINATION, "sauvojen_wand_stone",
                       STONE_ITEM_ID, ACADEMY_STONE_ITEM_ID)


def import_blind_family() -> None:
    import_wand_family(BLIND_SOURCE_TABLE, BLIND_DESTINATION,
                       ACADEMY_BLIND_DESTINATION, "sauvojen_wand_blind",
                       BLIND_ITEM_ID, ACADEMY_BLIND_ITEM_ID,
                       academy_modifiers=("add_upgrade_desc", "tiers/b"))


def import_air_family() -> None:
    import_wand_family(AIR_SOURCE_TABLE, AIR_DESTINATION,
                       ACADEMY_AIR_DESTINATION, "sauvojen_wand_air",
                       AIR_ITEM_ID, ACADEMY_AIR_ITEM_ID,
                       academy_modifiers=("add_upgrade_desc", "tiers/s"))


def import_teleport_family() -> None:
    import_wand_family(TELEPORT_SOURCE_TABLE, TELEPORT_DESTINATION,
                       ACADEMY_TELEPORT_DESTINATION, "sauvojen_wand_teleport",
                       TELEPORT_ITEM_ID, ACADEMY_TELEPORT_ITEM_ID,
                       academy_modifiers=("add_upgrade_desc", "tiers/a"))


def import_ruby_family() -> None:
    import_wand_family(RUBY_SOURCE_TABLE, RUBY_DESTINATION,
                       ACADEMY_RUBY_DESTINATION, "sauvojen_wand_ruby",
                       RUBY_ITEM_ID, ACADEMY_RUBY_ITEM_ID,
                       academy_modifiers=("add_upgrade_desc", "tiers/b"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--electro-family", action="store_true",
                        help="import only ordinary Electro and its Academy upgrade")
    parser.add_argument("--fire", action="store_true",
                        help="import only ordinary Fire and its Academy upgrade")
    parser.add_argument("--stone", action="store_true",
                        help="import only ordinary stone and its Academy upgrade")
    parser.add_argument("--blind", action="store_true",
                        help="import only ordinary Blind and its Academy upgrade")
    parser.add_argument("--air", action="store_true",
                        help="import only ordinary Air and its Academy upgrade")
    parser.add_argument("--teleport", action="store_true",
                        help="import only ordinary Teleport and its Academy upgrade")
    parser.add_argument("--ruby", action="store_true",
                        help="import only ordinary Ruby and its Academy upgrade")
    arguments = parser.parse_args()
    if arguments.ruby:
        import_ruby_family()
    elif arguments.teleport:
        import_teleport_family()
    elif arguments.air:
        import_air_family()
    elif arguments.blind:
        import_blind_family()
    elif arguments.stone:
        import_stone_family()
    elif arguments.fire:
        import_fire_family()
    elif arguments.electro_family:
        import_electro_family()
    else:
        import_terra()
