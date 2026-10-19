#!/usr/bin/env python3
"""Import selected legacy LBC item payloads as standalone native loot tables."""

from __future__ import annotations

import json
import re
from pathlib import Path

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import SOURCE


ROOT = Path(__file__).resolve().parents[1]
SOURCE_TABLE = SOURCE / "loot_table/transformables_sword.json"
DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported/ice_sword.json"
MAGIC_SOURCE = SOURCE / "loot_table"
MAGIC_DESTINATION = ROOT / "src/main/resources/data/lbc/loot_table/ported"
COLOR_TAG_SOURCE = SOURCE.parent / "minecraft/tags/block"
COLOR_TAG_DESTINATION = ROOT / "src/main/resources/data/minecraft/tags/block"
COLOR_TAGS = ("colored", "in_colored", "black", "blue", "brown", "cyan", "gray", "green",
              "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red",
              "white", "yellow")
POTION_SOURCE = SOURCE / "loot_table/imba/potions.json"
REGULAR_POTION_SOURCE = SOURCE / "loot_table/transformables_potion.json"
MANA_FAMILIES = {
    "add_manamax": 17,
    "add_manaregen": 18,
    "add_mana": 19,
    "remove_mana": 20,
    "temporary_mana": 16,
}
POTION_DELIVERIES = {
    "drink": ("minecraft:potion", "skywars2:mana_potion"),
    "splash_potion": ("minecraft:splash_potion", "skywars2:mana_splash_potion"),
    "lingering_potion": ("minecraft:lingering_potion", "skywars2:mana_lingering_potion"),
}
REGULAR_NAME_KEYS = {
    "add_manamax": "max_mana_potion",
    "add_manaregen": "regen_mana_potion",
    "add_mana": "add_mana_potion",
    "remove_mana": "remove_mana_potion",
    "temporary_mana": "mana_potion",
}


def migrate_legacy_score_value_checks(value: object) -> None:
    """Map legacy score value checks to the Minecraft 26.3 int codec without changing their test."""
    if isinstance(value, list):
        for child in value:
            migrate_legacy_score_value_checks(child)
        return
    if not isinstance(value, dict):
        return
    for child in list(value.values()):
        migrate_legacy_score_value_checks(child)
    if value.get("type") != "minecraft:value_check":
        return
    provider = value.get("value")
    if not isinstance(provider, dict) or provider.get("type") != "minecraft:score":
        raise SystemExit(f"Unsupported legacy value_check provider: {provider!r}")
    scale = provider.pop("scale", 1)
    if scale != 1:
        raise SystemExit(f"Cannot preserve non-integral legacy score scale in 26.3: {scale!r}")
    range_value = value.pop("range", None)
    if not isinstance(range_value, int):
        raise SystemExit(f"Expected exact integer legacy score range, found {range_value!r}")
    # 26.3 has a dedicated int_value_check and ScoreboardValue yields integer scoreboard values.
    # The source scale is exactly 1, so converting this primitive point keeps exact equality.
    value["type"] = "minecraft:int_value_check"
    value["test"] = range_value


def import_mana_potions() -> None:
    table = json.loads(POTION_SOURCE.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    entries = [entry for pool in table.get("pools", []) for entry in pool.get("entries", [])]
    regular_table = json.loads(REGULAR_POTION_SOURCE.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(regular_table)
    regular_entries = [entry for pool in regular_table.get("pools", []) for entry in pool.get("entries", [])]
    regular_counts = {family: 0 for family in MANA_FAMILIES}
    for family, amplifier in MANA_FAMILIES.items():
        for delivery, (source_item, polymer_item) in POTION_DELIVERIES.items():
            selected = []
            for entry in entries:
                if entry.get("name") != source_item:
                    continue
                modifier = entry.get("modifier", [])
                functions = modifier if isinstance(modifier, list) else [modifier]
                components = next((function.get("components", {}) for function in functions
                                   if function.get("type") == "minecraft:set_components"), {})
                effects = components.get("minecraft:potion_contents", {}).get("custom_effects", [])
                if (len(effects) == 1 and effects[0].get("id") == "minecraft:raid_omen"
                        and effects[0].get("amplifier") == amplifier
                        and any(function.get("type") == "minecraft:set_name"
                                and family in json.dumps(function, ensure_ascii=False)
                                for function in functions)):
                    selected_entry = json.loads(json.dumps(entry))
                    selected_entry["name"] = polymer_item
                    selected.append(selected_entry)
            durations = sorted(
                next(function["components"]["minecraft:potion_contents"]["custom_effects"][0]["duration"]
                     for function in entry.get("modifier", [])
                     if function.get("type") == "minecraft:set_components")
                for entry in selected
            )
            if len(selected) != 2 or durations != [3600, 9600]:
                raise SystemExit(f"Unexpected {family}/{delivery} source variants: {durations}")
            regular_selected = []
            for entry in regular_entries:
                if entry.get("name") != source_item:
                    continue
                functions = entry.get("modifier", [])
                functions = functions if isinstance(functions, list) else [functions]
                components = next((function.get("components", {}) for function in functions
                                   if function.get("type") == "minecraft:set_components"), {})
                effects = components.get("minecraft:potion_contents", {}).get("custom_effects", [])
                name_key_matches = any(
                    function.get("type") == "minecraft:set_name"
                    and REGULAR_NAME_KEYS[family] in json.dumps(function, ensure_ascii=False)
                    for function in functions
                )
                if (len(effects) == 1 and effects[0].get("id") == "minecraft:raid_omen"
                        and effects[0].get("amplifier") == amplifier and name_key_matches):
                    selected_entry = json.loads(json.dumps(entry))
                    selected_entry["name"] = polymer_item
                    migrate_legacy_score_value_checks(selected_entry)
                    regular_selected.append(selected_entry)
            expected_regular = 1 if family == "remove_mana" and delivery == "lingering_potion" else (
                2 if family != "remove_mana" and delivery == "drink" else 0)
            if len(regular_selected) != expected_regular:
                raise SystemExit(
                    f"Unexpected {family}/{delivery} regular source variants: {len(regular_selected)}")
            regular_counts[family] += len(regular_selected)
            selected.extend(regular_selected)
            payload = {"pools": [{"rolls": 1, "entries": selected}]}
            destination = MAGIC_DESTINATION / f"mana_{family}_{delivery}.json"
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                                   encoding="utf-8", newline="\n")
            print(f"Imported {family}/{delivery} ({len(selected)} source entries) to {destination.relative_to(ROOT)}")
    if regular_counts != {"add_manamax": 2, "add_manaregen": 2, "add_mana": 2,
                          "remove_mana": 1, "temporary_mana": 2}:
        raise SystemExit(f"Unexpected regular potion source entry counts: {regular_counts}")


def import_selected_entry(source_name: str, destination_name: str, predicate, item_id: str) -> None:
    table_path = MAGIC_SOURCE / source_name
    table = json.loads(table_path.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)
    matches = [
        (pool, entry)
        for pool in table.get("pools", [])
        for entry in pool.get("entries", [])
        if predicate(entry)
    ]
    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one selected entry in {table_path}, found {len(matches)}")
    source_pool, entry = matches[0]
    entry["name"] = item_id
    pool = {key: value for key, value in source_pool.items() if key != "entries"}
    pool["entries"] = [entry]
    payload = {key: value for key, value in table.items() if key != "pools"}
    payload["pools"] = [pool]
    target = MAGIC_DESTINATION / destination_name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"Imported {source_name} payload to {target.relative_to(ROOT)}")


def import_overload_items() -> None:
    """Import the Crux and four exact armor replacement payloads from OLD LBC."""
    selections = [
        ("overload_crux.json", "overload_crux.json", "overload_crux", "minecraft:carrot_on_a_stick"),
        ("neko_set_overload/helmet.json", "overload_helmet.json", "helmet", "minecraft:leather_helmet"),
        ("neko_set_overload/chestplate.json", "overload_chestplate.json", "chestplate", "minecraft:leather_chestplate"),
        ("neko_set_overload/leggings.json", "overload_leggings.json", "leggings", "minecraft:leather_leggings"),
        ("neko_set_overload/boots.json", "overload_boots.json", "boots", "minecraft:leather_boots"),
    ]
    for source_name, destination_name, marker, proxy in selections:
        def selected(entry: dict, marker: str = marker, proxy: str = proxy) -> bool:
            if entry.get("name") != proxy:
                return False
            data_key = "overload_crux" if marker == "overload_crux" else "overload"
            return any(
                function.get("type") == "minecraft:set_custom_data"
                and re.search(rf"(?:^|[,{{}}]){data_key}:1(?:[,{{}}]|$)", function.get("tag", ""))
                and (marker == "overload_crux" or re.search(
                    rf"(?:^|[,{{}}]){re.escape(marker)}:1(?:[,{{}}]|$)", function.get("tag", "")))
                for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                 else [entry.get("modifier", {})])
            )

        item_id = "skywars2:overload_crux" if marker == "overload_crux" else f"skywars2:overload_{marker}"
        import_selected_entry(source_name, destination_name, selected, item_id)


def import_first_wand_resources() -> None:
    """Import the two tiny wand entries and the two trinkets required by their casts."""
    selections = [
        ("random_sauvojen_wand_tiny.json", "tiny_fire_wand.json", "minecraft:carrot_on_a_stick",
         "stfire_tiny:1", "skywars2:tiny_fire_wand"),
        ("random_sauvojen_wand_tiny.json", "tiny_blind_wand.json", "minecraft:carrot_on_a_stick",
         "stblind_tiny:1", "skywars2:tiny_blind_wand"),
        ("trinket/mana_gem.json", "mana_gem.json", "minecraft:repeating_command_block",
         'trinket:"mana_gem"', "skywars2:mana_gem"),
        ("trinket/blood_anal_boom.json", "blood_anal_boom.json", "minecraft:repeating_command_block",
         'trinket:"blood_anal_boom"', "skywars2:blood_anal_boom"),
    ]
    for source_name, destination_name, proxy, marker, item_id in selections:
        def selected(entry: dict, proxy: str = proxy, marker: str = marker) -> bool:
            if entry.get("name") != proxy:
                return False
            return any(function.get("type") == "minecraft:set_custom_data"
                       and marker in function.get("tag", "")
                       for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                        else [entry.get("modifier", {})]))

        import_selected_entry(source_name, destination_name, selected, item_id)


def import_second_wand_resources() -> None:
    """Import only the Air, Stone, and Teleport tiny wand source entries."""
    selections = [
        ("stair_tiny", "tiny_air_wand.json", "skywars2:tiny_air_wand"),
        ("ststone_tiny", "tiny_stone_wand.json", "skywars2:tiny_stone_wand"),
        ("stteleport_tiny", "tiny_teleport_wand.json", "skywars2:tiny_teleport_wand"),
    ]
    for marker, destination_name, item_id in selections:
        def selected(entry: dict, marker: str = marker) -> bool:
            if entry.get("name") != "minecraft:carrot_on_a_stick":
                return False
            return any(function.get("type") == "minecraft:set_custom_data"
                       and re.search(rf"(?:^|[,{{}}]){re.escape(marker)}:1(?:[,{{}}]|$)", function.get("tag", ""))
                       for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                        else [entry.get("modifier", {})]))

        import_selected_entry("random_sauvojen_wand_tiny.json", destination_name, selected, item_id)


def import_third_wand_resources() -> None:
    """Import only the Ruby and Terra tiny wand source entries."""
    selections = [
        ("struby_tiny", "tiny_ruby_wand.json", "skywars2:tiny_ruby_wand"),
        ("stterra_tiny", "tiny_terra_wand.json", "skywars2:tiny_terra_wand"),
    ]
    for marker, destination_name, item_id in selections:
        def selected(entry: dict, marker: str = marker) -> bool:
            if entry.get("name") != "minecraft:carrot_on_a_stick":
                return False
            return any(function.get("type") == "minecraft:set_custom_data"
                       and re.search(rf"(?:^|[,{{}}]){re.escape(marker)}:1(?:[,{{}}]|$)", function.get("tag", ""))
                       for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                        else [entry.get("modifier", {})]))

        import_selected_entry("random_sauvojen_wand_tiny.json", destination_name, selected, item_id)


def import_fourth_wand_resources() -> None:
    """Import only the tiny Acid wand entry; Electro awaits its separate dependency batch."""
    marker = "stacid_tiny"

    def selected(entry: dict) -> bool:
        if entry.get("name") != "minecraft:carrot_on_a_stick":
            return False
        return any(function.get("type") == "minecraft:set_custom_data"
                   and re.search(r"(?:^|[,{}])stacid_tiny:1(?:[,{}]|$)", function.get("tag", ""))
                   for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                    else [entry.get("modifier", {})]))

    import_selected_entry("random_sauvojen_wand_tiny.json", "tiny_acid_wand.json", selected,
                          "skywars2:tiny_acid_wand")


def import_electro_wand_resources() -> None:
    """Import only the tiny Electro wand source entry."""
    marker = "stelectro_tiny"

    def selected(entry: dict) -> bool:
        if entry.get("name") != "minecraft:carrot_on_a_stick":
            return False
        return any(function.get("type") == "minecraft:set_custom_data"
                   and re.search(r"(?:^|[,{}])stelectro_tiny:1(?:[,{}]|$)", function.get("tag", ""))
                   for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                    else [entry.get("modifier", {})]))

    import_selected_entry("random_sauvojen_wand_tiny.json", "tiny_electro_wand.json", selected,
                          "skywars2:tiny_electro_wand")


def import_terra_sword_resources() -> None:
    """Import only the ordinary and awakened Terra Sword item payloads."""
    selections = (
        ("transformables_sword.json", "terra_sword.json", "terrasword", "skywars2:terra_sword"),
        ("tuntijaa_terra_awakened.json", "true_terra_sword.json", "true_terra",
         "skywars2:true_terra_sword"),
    )
    for source_name, destination_name, marker, item_id in selections:
        destination = MAGIC_DESTINATION / destination_name
        if destination.exists():
            raise SystemExit(f"Refusing to overwrite existing Terra Sword table: {destination.relative_to(ROOT)}")

        def selected(entry: dict, marker: str = marker) -> bool:
            if entry.get("name") != "minecraft:carrot_on_a_stick":
                return False
            return any(function.get("type") == "minecraft:set_custom_data"
                       and re.search(rf"(?:^|[,{{}}]){re.escape(marker)}:1(?:[,{{}}]|$)",
                                     function.get("tag", ""))
                       for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                        else [entry.get("modifier", {})]))

        import_selected_entry(source_name, destination_name, selected, item_id)


def import_razorpine_resource() -> None:
    """Import only the Razor Pine reference selected by the shooter transformable table."""
    transformables = json.loads((MAGIC_SOURCE / "transformables_shooter.json").read_text(encoding="utf-8-sig"))
    refs = [entry for pool in transformables.get("pools", []) for entry in pool.get("entries", [])
            if entry.get("type") == "minecraft:loot_table"
            and entry.get("value") == "lbc:razorpine_with_leaves"]
    if len(refs) != 1:
        raise SystemExit(f"Expected exactly one Razor Pine reference in transformables_shooter.json, found {len(refs)}")

    source_path = MAGIC_SOURCE / "razorpine_with_leaves.json"
    source = json.loads(source_path.read_text(encoding="utf-8-sig"))
    matches = []
    for pool in source.get("pools", []):
        for entry in pool.get("entries", []):
            functions = entry.get("functions", [])
            components = next((fn.get("components", {}) for fn in functions
                               if fn.get("function") == "minecraft:set_components"), {})
            custom = next((fn.get("tag", "") for fn in functions
                           if fn.get("function") == "minecraft:set_custom_data"), "")
            if (entry.get("name") == "minecraft:carrot_on_a_stick"
                    and components.get("minecraft:item_model") == "lbc:razorpine"
                    and re.search(r"(?:^|[,{}])razorpine:1(?:[,{}]|$)", custom)):
                matches.append((pool, entry))
    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one Razor Pine item in {source_path}, found {len(matches)}")
    source_pool, entry = matches[0]
    entry["name"] = "skywars2:razorpine"
    source_pool["entries"] = [entry]
    migrate_loot_schema(source)
    payload = source
    target = MAGIC_DESTINATION / "razorpine.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"Imported Razor Pine payload to {target.relative_to(ROOT)}")

    # Keep the legacy ammunition memberships, replacing its custom flowers alias
    # with the equivalent native 26.3 item tag.
    ammo_source = SOURCE.parent / "minecraft/tags/item"
    ammo_target = ROOT / "src/main/resources/data/minecraft/tags/item"
    for tag_name in ("razor_ammo.json", "razor_ammo_best.json"):
        raw = (ammo_source / tag_name).read_bytes()
        if tag_name == "razor_ammo.json":
            old = b"#tag_replacements:flowers"
            if raw.count(old) != 1:
                raise SystemExit(f"Expected one tag_replacements flowers alias in {ammo_source / tag_name}")
            raw = raw.replace(old, b"#minecraft:flowers")
        (ammo_target / tag_name).parent.mkdir(parents=True, exist_ok=True)
        (ammo_target / tag_name).write_bytes(raw)
        print(f"Imported ammo tag {tag_name} to {ammo_target.relative_to(ROOT) / tag_name}")


def main() -> None:
    table = json.loads(SOURCE_TABLE.read_text(encoding="utf-8-sig"))
    migrate_loot_schema(table)

    matches = []
    for pool in table.get("pools", []):
        for entry in pool.get("entries", []):
            modifier = entry.get("modifier", [])
            functions = modifier if isinstance(modifier, list) else [modifier]
            for function in functions:
                if (function.get("type") == "minecraft:set_custom_data"
                        and re.search(r"(?:^|[,{}])icesword:1(?:[,{}]|$)", function.get("tag", ""))):
                    matches.append((pool, entry))

    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one icesword:1 entry in {SOURCE_TABLE}, found {len(matches)}")
    source_pool, entry = matches[0]
    if entry.get("name") != "minecraft:iron_sword":
        raise SystemExit(f"Unexpected Ice Sword source item: {entry.get('name')!r}")
    entry["name"] = "skywars2:ice_sword"

    ported_pool = {key: value for key, value in source_pool.items() if key != "entries"}
    ported_pool["entries"] = [entry]
    payload = {"pools": [ported_pool]}
    DESTINATION.parent.mkdir(parents=True, exist_ok=True)
    DESTINATION.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8", newline="\n")
    print(f"Imported Ice Sword payload to {DESTINATION.relative_to(ROOT)}")

    def mystical_artifact(entry: dict) -> bool:
        return entry.get("name") == "minecraft:repeating_command_block" and any(
            function.get("type") == "minecraft:set_custom_data"
            and re.search(r"(?:^|[,{}])unresolved_artifact:1(?:[,{}]|$)", function.get("tag", ""))
            and re.search(r"(?:^|[,{}])mystical:1(?:[,{}]|$)", function.get("tag", ""))
            and re.search(r"(?:^|[,{}])type:1(?:[,{}]|$)", function.get("tag", ""))
            for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                             else [entry.get("modifier", {})])
        )

    import_selected_entry("artifact.json", "mystical_artifact.json", mystical_artifact,
                          "skywars2:mystical_artifact")
    def mystical_color_artifact(entry: dict) -> bool:
        return entry.get("name") == "minecraft:repeating_command_block" and any(
            function.get("type") == "minecraft:set_custom_data"
            and re.search(r"(?:^|[,{}])unresolved_artifact:1(?:[,{}]|$)", function.get("tag", ""))
            and re.search(r"(?:^|[,{}])mystical:1(?:[,{}]|$)", function.get("tag", ""))
            and re.search(r"(?:^|[,{}])type:7(?:[,{}]|$)", function.get("tag", ""))
            for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                             else [entry.get("modifier", {})])
        )

    import_selected_entry("artifact.json", "mystical_artifact_colors.json", mystical_color_artifact,
                          "skywars2:mystical_artifact")
    for artifact_type, suffix in ((6, "clear"), (8, "metabolism"), (5, "debuff"), (4, "arrows"),
                                  (3, "liquids"), (2, "explosions")):
        def selected_passive_artifact(entry: dict, artifact_type: int = artifact_type) -> bool:
            return (entry.get("name") == "minecraft:repeating_command_block" and any(
                function.get("type") == "minecraft:set_custom_data"
                and re.search(r"(?:^|[,{}])unresolved_artifact:1(?:[,{}]|$)", function.get("tag", ""))
                and re.search(r"(?:^|[,{}])mystical:1(?:[,{}]|$)", function.get("tag", ""))
                and re.search(rf"(?:^|[,{{}}])type:{artifact_type}(?:[,{{}}]|$)", function.get("tag", ""))
                for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                                 else [entry.get("modifier", {})])
            ))

        import_selected_entry("artifact.json", f"mystical_artifact_{suffix}.json",
                              selected_passive_artifact, "skywars2:mystical_artifact")
    import_selected_entry("stopper_magic.json", "magic_beacon.json",
                          lambda entry: entry.get("name") == "minecraft:panda_spawn_egg",
                          "skywars2:magic_beacon")
    import_mana_potions()
    import_overload_items()
    import_first_wand_resources()

    COLOR_TAG_DESTINATION.mkdir(parents=True, exist_ok=True)
    for tag_name in COLOR_TAGS:
        source = COLOR_TAG_SOURCE / f"{tag_name}.json"
        destination = COLOR_TAG_DESTINATION / f"{tag_name}.json"
        destination.write_bytes(source.read_bytes())
        print(f"Copied {source.relative_to(SOURCE.parent)} to {destination.relative_to(ROOT)}")


if __name__ == "__main__":
    import sys

    if sys.argv[1:] == ["--terra-swords"]:
        import_terra_sword_resources()
    elif sys.argv[1:] == ["--razorpine"]:
        import_razorpine_resource()
    else:
        main()
