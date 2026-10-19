#!/usr/bin/env python3
"""Import the supported datapack GUI source map and missing storage captions.

Reads the legacy server tree and the user resource pack, both as read-only inputs.
Writes converted LBC GUI loot tables, src/main/resources/skywars2/gui_design.json and
build/verification/gui-design-new-translations.json in this project.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from import_game_resources import migrate_loot_schema


PROJECT = Path(__file__).resolve().parents[1]
OLD_SERVER = Path(r"E:\servers_mew\cringe1_21_4_skywars")
RESOURCE_PACK = Path(
    r"C:\Users\yarik_temp\AppData\Roaming\FreesmLauncher\instances\26.3(1)\minecraft\resourcepacks\all_in_one_1.21.8"
)
GUI_PAGES = {
    "swrg": {
        "0": "data/swrg/function/gui/page/0.mcfunction",
        "1000": "data/swrg/function/gui/page/1000.mcfunction",
        "1100": "data/swrg/function/gui/page/1100.mcfunction",
        "2000": "data/swrg/function/gui/page/2000.mcfunction",
        "2000b": "data/swrg/function/gui/page/2000b.mcfunction",
        "2100": "data/swrg/function/gui/page/2100.mcfunction",
        "2200": "data/swrg/function/gui/page/2200.mcfunction",
        "3000": "data/swrg/function/gui/page/3000.mcfunction",
        "4000": "data/swrg/function/gui/page/4000.mcfunction",
        "5000": "data/swrg/function/gui/page/5000.mcfunction",
        "6000": "data/swrg/function/gui/page/6000.mcfunction",
        "7000": "data/swrg/function/gui/page/7000.mcfunction",
    },
    "lbc": {
        "8000": "data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction",
        "8100": "data/lbc/function/swrg_kit_integration/gui/page/8100.mcfunction",
        "9000": "data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction",
        "9100": "data/lbc/function/swrg_kit_integration/gui/page/9100.mcfunction",
        "10000": "data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction",
        "10100": "data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction",
        "11000": "data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction",
        "11100": "data/lbc/function/swrg_kit_integration/gui/page/11100.mcfunction",
        "12000": "data/lbc/function/swrg_kit_integration/gui/page/12000.mcfunction",
    },
}
LOOT_REF = re.compile(
    r"enderchest\.(\d+)\s+loot\s+((?:swrg|lbc):gui/page/([0-9]+)/([a-z0-9_]+))(?=\s|$)"
)
ITEM_REF = re.compile(r"enderchest\.(\d+)\s+with\s+(minecraft:[a-z0-9_]+)(\[[^\]]*\])?")
MODIFIER_REF = re.compile(r"enderchest\.(\d+)\s+([a-z0-9_]+:[a-z0-9_./-]+)\s*$")
MAP_SLOT_REF = re.compile(
    r"#map\s+swrg\.math\s+matches\s+(-?\d+)\s+run\s+item modify entity @s enderchest\.(\d+) swrg:select"
)
MAP_SELECT_REF = re.compile(
    r"map_selected\s+swrg\.math\s+matches\s+(\d+)\s+run\s+(?:schedule\s+)?function swrg:maploader/map/([0-9]+b?)"
)
STORAGE_LINE = re.compile(r"data modify storage swrg:lang\s+(\S+)\s+set value\s+(.+)$")
TEXT_VALUE = re.compile(r'"text"\s*:\s*("(?:\\.|[^"\\])*")')
STORAGE_ENTRY = re.compile(r"data modify storage swrg:lang\s+(\S+)\s+set value\s+(.+)$")
SELECTOR_REF = re.compile(r"@s(?:\[([^\]]*)\])?")
ENTITY_SLOT_REF = re.compile(r"enderchest\.(\d+)")
LOCKED_MODEL_REF = re.compile(r'minecraft:item_model="([^"]*locked[^"]*)"')
ITEM_REPLACE_REF = re.compile(
    r"item replace entity (@s(?:\[[^\]]*\])?)\s+enderchest\.(\d+)\s+with\s+"
    r"(minecraft:[a-z0-9_]+)(\[[^\]]*\])?"
)
ITEM_MODIFY_REF = re.compile(
    r"item modify entity (@s(?:\[[^\]]*\])?)\s+enderchest\.(\d+)\s+"
    r"([a-z0-9_]+:[a-z0-9_./-]+)\s*$"
)
LOOT_REPLACE_REF = re.compile(
    r"loot replace entity (@s(?:\[[^\]]*\])?)\s+enderchest\.(\d+)\s+loot\s+"
)
ADVANCEMENT_TERM = re.compile(r"([a-z0-9_.-]+:[a-z0-9_./-]+)=(true|false)")


def resource_pack_language(locale: str) -> dict[str, str]:
    values = {}
    for lang_file in sorted((RESOURCE_PACK / "assets").glob(f"*/lang/{locale}.json")):
        values.update(json.loads(lang_file.read_text(encoding="utf-8-sig")))
    return values


def project_language(locale: str) -> dict[str, str]:
    values = {}
    for root in (PROJECT / "src/main/resources/assets", PROJECT / "resourcepack_template/assets"):
        for lang_file in sorted(root.glob(f"*/lang/{locale}.json")):
            values.update(json.loads(lang_file.read_text(encoding="utf-8-sig")))
    return values


def map_translation_key(page_id: str, slot: int, source_text: str) -> str:
    key = f"swrg.gui.map.{page_id}.{slot}"
    en = {**resource_pack_language("en_us"), **project_language("en_us")}
    ru = {**resource_pack_language("ru_ru"), **project_language("ru_ru")}
    if en.get(key) == source_text and key in ru:
        return key
    candidates = [candidate for candidate, value in en.items() if value == source_text and candidate in ru]
    candidates.sort(key=lambda candidate: (0 if candidate.startswith("swrg.gui.map.") else 1, candidate))
    return candidates[0] if candidates else ""


def parse_snbt_component(raw: str):
    # Source GUI text uses JSON-compatible component values inside SNBT compounds.
    strings: list[str] = []

    def protect(match: re.Match[str]) -> str:
        strings.append(match.group(0))
        return f"§SOURCE_STRING_{len(strings) - 1}§"

    normalized = re.sub(r'"(?:\\.|[^"\\])*"', protect, raw)
    normalized = re.sub(r'([,{]\s*)([A-Za-z_][A-Za-z0-9_.:-]*)\s*:', r'\1"\2":', normalized)
    for index, value in enumerate(strings):
        normalized = normalized.replace(f"§SOURCE_STRING_{index}§", value)
    return json.loads(normalized)


def source_storage_components(old_root: Path) -> dict:
    result = {"en": {}, "ru": {}}
    for locale, filename in (("en", "en.mcfunction"), ("ru", "ru.mcfunction")):
        source = old_root / "world/datapacksdisabled/swarogskywars/data/swrg/function/core/lang" / filename
        for line_no, line in enumerate(source.read_text(encoding="utf-8-sig").splitlines(), 1):
            match = STORAGE_ENTRY.search(line)
            if not match:
                continue
            try:
                result[locale][match.group(1)] = parse_snbt_component(match.group(2))
            except (json.JSONDecodeError, ValueError):
                # Retain the exact source line reference for unusual non-JSON SNBT values.
                result[locale][match.group(1)] = {"sourceLine": line_no, "raw": match.group(2)}
    return result


def page_manifest(old_root: Path, local_resources: Path) -> dict:
    pages = []
    for namespace, page_sources in GUI_PAGES.items():
        for page_id, relative in page_sources.items():
            source_file = old_root / "world/datapacksdisabled" / ("swarogskywars" if namespace == "swrg" else "LBC") / relative
            if not source_file.is_file():
                source_file = old_root / "world/datapacksdisabled" / relative
            content = source_file.read_text(encoding="utf-8-sig")
            slots: dict[int, dict] = {}
            map_id_by_slot: dict[int, str] = {}
            map_slot_order: list[int] = []
            for line_no, line in enumerate(content.splitlines(), 1):
                stripped = line.strip()
                if not stripped or stripped.startswith("#"):
                    continue
                loot = LOOT_REF.search(stripped)
                if loot:
                    slot = int(loot.group(1))
                    entry = slots.setdefault(slot, {"slot": slot, "sources": []})
                    entry["sources"].append({
                        "sourceLootTable": loot.group(2),
                        "sourceLine": line_no,
                        "condition": stripped.split(" run ", 1)[0] if " run " in stripped else None,
                    })
                    continue
                item = ITEM_REF.search(stripped)
                if item:
                    slot = int(item.group(1))
                    entry = slots.setdefault(slot, {"slot": slot, "sources": []})
                    entry["sources"].append({
                        "sourceItem": item.group(2),
                        "sourceComponentText": item.group(3),
                        "sourceLine": line_no,
                        "condition": stripped.split(" run ", 1)[0] if " run " in stripped else None,
                    })
                    continue
                modifier = MODIFIER_REF.search(stripped)
                if modifier and "item modify" in stripped:
                    slot = int(modifier.group(1))
                    entry = slots.setdefault(slot, {"slot": slot, "sources": []})
                    entry["sources"].append({
                        "itemModifier": modifier.group(2),
                        "sourceLine": line_no,
                        "condition": stripped.split(" run ", 1)[0] if " run " in stripped else None,
                    })
                map_match = MAP_SLOT_REF.search(stripped)
                if map_match and page_id in {"2000", "2000b", "2100", "2200"}:
                    score, map_slot = map(int, map_match.groups())
                    map_id_by_slot[map_slot] = str(score)
                    map_slot_order.append(map_slot)
                map_selection_ids = []
            if page_id in {"2000", "2000b", "2100", "2200"}:
                select_function = old_root / "world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select" / f"{page_id}_mapselect.mcfunction"
                if not select_function.is_file():
                    raise FileNotFoundError(f"Missing source map selection function: {select_function}")
                for select_line in select_function.read_text(encoding="utf-8-sig").splitlines():
                    selected = MAP_SELECT_REF.search(select_line)
                    if selected:
                        selected_index, source_map_id = selected.groups()
                        map_selection_ids.append((int(selected_index), source_map_id))
                if len(map_slot_order) != len(map_selection_ids):
                    raise ValueError(
                        f"Source map page/selection cardinality differs for {page_id}: "
                        f"{len(map_slot_order)} page entries vs {len(map_selection_ids)} selection entries"
                    )
                map_ids_by_slot = dict(zip(map_slot_order, (map_id for _, map_id in map_selection_ids)))
            for entry in slots.values():
                for source in entry["sources"]:
                    table_id = source.get("sourceLootTable")
                    if table_id:
                        relative_table = Path("data") / table_id.split(":", 1)[0] / "loot_table" / Path(*table_id.split(":", 1)[1].split("/"))
                        relative_table = relative_table.with_suffix(".json")
                        if not (local_resources / relative_table).is_file():
                            source["localTableMissing"] = True
                        else:
                            source["sourceComponentBindings"] = loot_component_bindings(
                                local_resources / relative_table
                            )
                    if page_id in {"2000", "2000b", "2100", "2200"} and entry["slot"] in map_id_by_slot:
                        map_score = int(map_id_by_slot[entry["slot"]])
                        source_map_id = map_ids_by_slot.get(entry["slot"])
                        if source_map_id is None:
                            raise ValueError(f"No source map selection for score {map_score} on page {page_id}, slot {entry['slot']}")
                        source["sourceMapId"] = source_map_id
            if page_id in {"2000", "2000b", "2100", "2200"}:
                # The source mapselect functions establish stable IDs independently
                # of display-slot ordering; retain the source reference for audits.
                select_page = page_id
                select_function = old_root / "world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select" / f"{select_page}_mapselect.mcfunction"
                pages.append({
                    "pageId": page_id,
                    "namespace": namespace,
                    "sourceFunction": f"{namespace}:gui/page/{page_id}",
                    "sourceMapSelectionFunction": f"swrg:gui/select/{select_page}_mapselect",
                    "sourceMapSelectionExists": select_function.is_file(),
                    "completeSourcePageImport": True,
                    "slots": [slots[s] for s in sorted(slots)],
                })
                continue
            pages.append({
                "pageId": page_id,
                "namespace": namespace,
                "sourceFunction": f"{namespace}:gui/page/{page_id}",
                "completeSourcePageImport": True,
                "slots": [slots[s] for s in sorted(slots)],
            })
    shared = []
    for namespace, path in (("swrg", "gui/back"), ("swrg", "gui/next"), ("swrg", "gui/reset")):
        table_id = f"{namespace}:{path}"
        table_path = local_resources / "data" / namespace / "loot_table" / f"{path}.json"
        button = {"sourceLootTable": table_id, "localTableMissing": not table_path.is_file()}
        if table_path.is_file():
            button["sourceComponentBindings"] = loot_component_bindings(table_path)
        shared.append(button)
    shared_modifiers = []
    for modifier_id in ("swrg:balance", "swrg:balance_mem"):
        modifier_name = modifier_id.split(":", 1)[1]
        modifier_path = old_root / "world/datapacksdisabled/swarogskywars/data/swrg/item_modifier" / f"{modifier_name}.json"
        if not modifier_path.is_file():
            raise FileNotFoundError(f"Missing source item modifier {modifier_path}")
        shared_modifiers.append({
            "modifierId": modifier_id,
            "sourceComponentBindings": loot_component_bindings(modifier_path),
        })
    for page in pages:
        for slot in page["slots"]:
            for source in slot["sources"]:
                component = source.get("sourceComponentText")
                if not component or "custom_name=" not in component:
                    continue
                raw_name = component.split("custom_name=", 1)[1].rsplit("]", 1)[0]
                try:
                    parsed_name = json.loads(raw_name.replace(r"\s", " "))
                except json.JSONDecodeError:
                    continue
                source["sourceName"] = parsed_name
                if page["pageId"] in {"2000", "2000b", "2100", "2200"}:
                    source["translationKey"] = map_translation_key(
                        page["pageId"], slot["slot"], parsed_name.get("text", "")
                    ) or f"skywars2.gui.map.{source['sourceMapId']}"

    raw_components = source_storage_components(old_root)
    localized_components, _ = localize_storage_components(raw_components)
    component_gaps = add_source_component_gaps(localized_components, raw_components, pages, shared)
    return {
        "schemaVersion": 1,
        "container": {"size": 27, "title": {"translate": "container.enderchest"}, "slots": list(range(27))},
        "requiredSourcePages": [f"{n}:{p}" for n, ps in GUI_PAGES.items() for p in ps],
        "pages": pages,
        "sharedButtons": shared,
        "sharedModifiers": shared_modifiers,
        "storageBindings": [{"storagePath": "swrg:lang", "sourceFunction": "swrg:gui/page/1000-lang"}],
        "sourceStorageComponents": raw_components,
        "localizedStorageComponents": localized_components,
        "sourceComponentGaps": component_gaps,
        "sourceNotes": [
            "Map page items in pages 2000/2000b/2100/2200 are literal item-replace commands, not loot tables.",
            "Page 1100 live player-head profiles are direct item-replace commands; commented loot references are ignored.",
            "Scoreboard-backed selected/glint, unlock, price, and live-stat variants are not runtime imported by this manifest.",
        ],
    }


def _split_selector_terms(raw: str) -> list[str]:
    """Split selector fields on top-level commas, preserving nested compounds."""
    terms, start, depth = [], 0, 0
    for index, char in enumerate(raw):
        if char in "{[":
            depth += 1
        elif char in "}]":
            depth -= 1
            if depth < 0:
                raise ValueError(f"Unbalanced selector field: {raw}")
        elif char == "," and depth == 0:
            terms.append(raw[start:index].strip())
            start = index + 1
    if depth != 0:
        raise ValueError(f"Unbalanced selector field: {raw}")
    if raw[start:].strip():
        terms.append(raw[start:].strip())
    return terms


def parse_gui_selector(raw_selector: str, *, allow_scores: bool) -> dict:
    """Return the source tag/advancement predicates, failing on unknown fields."""
    match = SELECTOR_REF.fullmatch(raw_selector.strip())
    if not match:
        raise ValueError(f"Unsupported GUI selector: {raw_selector}")
    conditions = {"tags": [], "advancements": {}}
    body = match.group(1)
    if body is None:
        return conditions
    for term in _split_selector_terms(body):
        if term.startswith("tag="):
            value = term[len("tag="):]
            if not value or any(char in value for char in "{}[],"):
                raise ValueError(f"Unsupported tag predicate: {term}")
            conditions["tags"].append(value)
        elif term.startswith("advancements={") and term.endswith("}"):
            inner = term[len("advancements={"):-1]
            if not inner:
                raise ValueError(f"Empty advancement selector: {term}")
            for advancement in _split_selector_terms(inner):
                advancement_match = ADVANCEMENT_TERM.fullmatch(advancement)
                if not advancement_match:
                    raise ValueError(f"Unsupported advancement selector: {advancement}")
                advancement_id, completed = advancement_match.groups()
                if advancement_id in conditions["advancements"]:
                    raise ValueError(f"Repeated advancement selector: {advancement_id}")
                conditions["advancements"][advancement_id] = completed == "true"
        elif term.startswith("scores={") and term.endswith("}") and allow_scores:
            # Selection/glint scores are UI state, not unlock requirements.
            continue
        else:
            raise ValueError(f"Unsupported GUI selector field: {term} in {raw_selector}")
    conditions["tags"].sort()
    return conditions


def _gui_source_root(old_root: Path, namespace: str) -> Path:
    datapack = "swarogskywars" if namespace == "swrg" else "LBC"
    return old_root / "world/datapacksdisabled" / datapack / "data" / namespace


def _component_modifier_binding(old_root: Path, modifier_id: str) -> dict:
    namespace, path = modifier_id.split(":", 1)
    source_root = _gui_source_root(old_root, namespace)
    modifier_path = source_root / "item_modifier" / Path(*path.split("/"))
    modifier_path = modifier_path.with_suffix(".json")
    if not modifier_path.is_file():
        raise FileNotFoundError(f"Missing source item modifier for locked GUI variant: {modifier_path}")
    return {
        "modifierId": modifier_id,
        "sourceComponentBindings": loot_component_bindings(modifier_path),
    }


def inverse_gui_conditions(conditions: dict) -> dict:
    """Return the exact logical complement used by the paired open/locked branch."""
    return {
        "tags": sorted(value[1:] if value.startswith("!") else "!" + value
                       for value in conditions["tags"]),
        "advancements": {
            advancement_id: not completed
            for advancement_id, completed in conditions["advancements"].items()
        },
    }


def refresh_locked_design(design: dict, old_root: Path) -> dict:
    """Refresh locked variants and advancement/tag gates without rebuilding the manifest."""
    page_index = {(page["namespace"], page["pageId"]): page for page in design.get("pages", [])}
    slot_index = {
        (page["namespace"], page["pageId"], slot["slot"]): slot
        for page in design.get("pages", []) for slot in page.get("slots", [])
    }
    replacements: dict[tuple[str, str, int], list[dict]] = {}
    modifies: dict[tuple[str, str, int], list[dict]] = {}
    unlocks: dict[tuple[str, str, int, int], dict] = {}
    locked_modifier_ids: set[str] = set()

    for namespace, page_sources in GUI_PAGES.items():
        for page_id, relative in page_sources.items():
            source_file = old_root / "world/datapacksdisabled" / (
                "swarogskywars" if namespace == "swrg" else "LBC"
            ) / relative
            if not source_file.is_file():
                raise FileNotFoundError(f"Missing source GUI page function: {source_file}")
            page_key = (namespace, page_id)
            if page_key not in page_index:
                raise ValueError(f"No existing GUI design page for locked refresh: {namespace}:{page_id}")
            lines = source_file.read_text(encoding="utf-8-sig").splitlines()
            for line_number, line in enumerate(lines, 1):
                stripped = line.strip()
                loot_match = LOOT_REPLACE_REF.search(stripped)
                if loot_match:
                    slot_number = int(loot_match.group(2))
                    selector_match = SELECTOR_REF.search(stripped)
                    if selector_match:
                        conditions = parse_gui_selector(selector_match.group(0), allow_scores=True)
                        if conditions["tags"] or conditions["advancements"]:
                            unlocks[(namespace, page_id, slot_number, line_number)] = conditions

                replace_match = ITEM_REPLACE_REF.search(stripped)
                if replace_match:
                    selector, slot_text, source_item, component_text = replace_match.groups()
                    component_text = component_text or ""
                    model_match = LOCKED_MODEL_REF.search(component_text)
                    if not model_match:
                        continue
                    conditions = parse_gui_selector(selector, allow_scores=False)
                    slot_number = int(slot_text)
                    key = (namespace, page_id, slot_number)
                    replacements.setdefault(key, []).append({
                        "item": source_item,
                        "components": {"minecraft:item_model": model_match.group(1)},
                        "conditions": conditions,
                        "_line": line_number,
                    })

                modify_match = ITEM_MODIFY_REF.search(stripped)
                if modify_match:
                    selector, slot_text, modifier_id = modify_match.groups()
                    # Only source modifier definitions referenced by a locked variant are imported.
                    if "swrg_unlocks/" not in modifier_id and not modifier_id.startswith("swrg:locked"):
                        continue
                    conditions = parse_gui_selector(selector, allow_scores=False)
                    slot_number = int(slot_text)
                    key = (namespace, page_id, slot_number)
                    modifies.setdefault(key, []).append({
                        "modifier": modifier_id,
                        "conditions": conditions,
                        "_line": line_number,
                    })

    for key, variants in replacements.items():
        namespace, page_id, slot_number = key
        slot = slot_index.get(key)
        if slot is None:
            raise ValueError(f"Locked source slot is missing from existing design: {namespace}:{page_id}/{slot_number}")
        # Modifier association is by predicate. The old LBC Ruby command targets slot 0
        # while Ruby's locked replacement uses slot 1; the confirmed correction below
        # puts its modifier on the matching Ruby-locked slot 1 in generated design.
        for variant in variants:
            variant_conditions = variant["conditions"]
            modifier_rows = []
            for (mod_namespace, mod_page, mod_slot), rows in modifies.items():
                if (mod_namespace, mod_page) != (namespace, page_id):
                    continue
                for row in rows:
                    same_gate = row["conditions"] == variant_conditions
                    ruby_correction = (
                        namespace == "lbc" and page_id == "9100"
                        and row["modifier"] == "lbc:swrg_unlocks/ruby_upgraded"
                        and variant["components"].get("minecraft:item_model") == "lbc:locked"
                        and variant_conditions.get("advancements") == {
                            "lbc:true_advancements/legends/academy/ruby": False
                        }
                    )
                    if same_gate and (mod_slot == slot_number or ruby_correction):
                        modifier_rows.append(row)
            modifier_rows.sort(key=lambda row: row["_line"])
            variant["modifiers"] = [row["modifier"] for row in modifier_rows]
            for modifier_id in variant["modifiers"]:
                locked_modifier_ids.add(modifier_id)
            variant.pop("_line", None)

        slot["lockedVariants"] = variants

    slot_unlocks: dict[tuple[str, str, int], list[dict]] = {}
    for (namespace, page_id, slot_number, _line_number), conditions in unlocks.items():
        key = (namespace, page_id, slot_number)
        # Only treat an advancement/tag-gated loot item as an unlock branch when
        # this same slot has the inverse locked fallback. Other page states (for
        # example challenge-progress displays) are not item unlock requirements.
        locked_conditions = [variant["conditions"] for variant in replacements.get(key, [])]
        if inverse_gui_conditions(conditions) in locked_conditions:
            slot_unlocks.setdefault(key, []).append(conditions)
    for key, condition_rows in slot_unlocks.items():
        unique_conditions = {json.dumps(row, sort_keys=True) for row in condition_rows}
        if len(unique_conditions) > 1:
            raise ValueError(f"Multiple distinct unlock conditions for GUI slot {key}: {condition_rows}")
        slot = slot_index.get(key)
        if slot is not None:
            slot["unlockConditions"] = condition_rows[0]

    existing_shared = design.setdefault("sharedModifiers", [])
    existing_ids = {entry.get("modifierId") for entry in existing_shared}
    for modifier_id in sorted(locked_modifier_ids):
        if modifier_id not in existing_ids:
            existing_shared.append(_component_modifier_binding(old_root, modifier_id))
    return design


def localize_storage_components(raw_components: dict) -> tuple[dict, dict]:
    """Replace source literal text leaves with exact paired resource-pack keys."""
    pack_en = resource_pack_language("en_us")
    pack_ru = resource_pack_language("ru_ru")
    known_en = {**pack_en, **project_language("en_us")}
    known_ru = {**pack_ru, **project_language("ru_ru")}
    project_en = project_language("en_us")
    project_ru = project_language("ru_ru")
    reusable: dict[tuple[str, str], str] = {}
    external_keys = set()
    for locale, merged in (("en_us", pack_en), ("ru_ru", pack_ru),
                           ("en_us", project_en), ("ru_ru", project_ru)):
        external_keys.update(merged)
    for source_en, source_ru in ((pack_en, pack_ru), (project_en, project_ru)):
        for key in sorted(set(source_en) & set(source_ru),
                          key=lambda candidate: (0 if candidate.startswith("swrg.gui.map.") else 1, candidate)):
            reusable.setdefault((source_en[key], source_ru[key]), key)
    existing_path = PROJECT / "build/verification/gui-design-new-translations.json"
    previous_en, previous_ru = {}, {}
    if existing_path.is_file():
        previous = json.loads(existing_path.read_text(encoding="utf-8-sig"))
        previous_en, previous_ru = previous.get("en_us", {}), previous.get("ru_ru", {})
        for key in sorted(set(previous_en) & set(previous_ru)):
            reusable.setdefault((previous_en[key], previous_ru[key]), key)
    localized = {}
    additions = {"en_us": {}, "ru_ru": {}}

    def text_nodes(value, path=()):
        found = {}
        if isinstance(value, list):
            for index, child in enumerate(value):
                found.update(text_nodes(child, path + (str(index),)))
        elif isinstance(value, dict):
            if isinstance(value.get("text"), str):
                found[path] = value["text"]
            for key, child in value.items():
                if key != "text":
                    found.update(text_nodes(child, path + (key,)))
        return found

    def replace_text(value, keys, path=()):
        if isinstance(value, list):
            return [replace_text(child, keys, path + (str(index),)) for index, child in enumerate(value)]
        if isinstance(value, dict):
            result = {key: replace_text(child, keys, path + (key,)) for key, child in value.items() if key != "text"}
            if path in keys:
                result["translate"] = keys[path]
            elif "text" in value:
                result["text"] = value["text"]
            return result
        return value

    for storage_path in sorted(set(raw_components["en"]) & set(raw_components["ru"])):
        en_nodes = text_nodes(raw_components["en"][storage_path])
        ru_nodes = text_nodes(raw_components["ru"][storage_path])
        if en_nodes.keys() != ru_nodes.keys():
            continue
        leaf_keys = {}
        for ordinal, node_path in enumerate(en_nodes):
            pair = (en_nodes[node_path], ru_nodes[node_path])
            key = reusable.get(pair)
            # Prefer the original semantic key over an unrelated caption with
            # the same wording (e.g. Spider kit and Spider skill).
            suffix = tuple(part for part in node_path if part not in ("Name", "Lore"))
            preferred = "swrg." + storage_path + ("." + ".".join(suffix) if suffix else "")
            if (pack_en.get(preferred), pack_ru.get(preferred)) == pair:
                key = preferred
            if key is None:
                key = "skywars2.gui.source." + re.sub(r"[^a-zA-Z0-9_.-]", "_", storage_path) + f".part{ordinal}"
                for locale, value in (("en_us", pair[0]), ("ru_ru", pair[1])):
                    previous = additions[locale].get(key)
                    if previous is not None and previous != value:
                        raise ValueError(f"Conflicting generated source translation {key} for {locale}")
                    additions[locale][key] = value
                reusable[pair] = key
            elif key not in external_keys:
                # Reused project additions must remain in the generated file on every run.
                additions["en_us"][key] = pair[0]
                additions["ru_ru"][key] = pair[1]
            leaf_keys[node_path] = key
        localized[storage_path] = replace_text(raw_components["en"][storage_path], leaf_keys)
    # A few legacy keys exist in only one core language file while the paired
    # translation is already present in the resource pack. Reuse that key and
    # the available source tree/style rather than emitting a raw literal.
    for storage_path in sorted(set(raw_components["en"]) ^ set(raw_components["ru"])):
        locale = "en_us" if storage_path in raw_components["en"] else "ru_ru"
        source_locale = "en" if locale == "en_us" else "ru"
        other_locale = "ru_ru" if locale == "en_us" else "en_us"
        pack_here = known_en if locale == "en_us" else known_ru
        pack_other = known_ru if locale == "en_us" else known_en
        project_here = previous_en if locale == "en_us" else previous_ru
        project_other = previous_ru if locale == "en_us" else previous_en
        template = raw_components[source_locale][storage_path]
        nodes = text_nodes(template)
        leaf_keys = {}
        for ordinal, (node_path, value) in enumerate(nodes.items()):
            candidates = [key for key, translated in pack_here.items()
                          if translated == value and key in pack_other
                          and not key.startswith("skywars2.gui.source.")]
            candidates += [key for key, translated in project_here.items()
                           if translated == value and key in project_other
                           and not key.startswith("skywars2.gui.source.")]
            if candidates:
                leaf_keys[node_path] = sorted(set(candidates), key=lambda candidate: (
                    0 if candidate.startswith("swrg.gui.map.") else 1, candidate
                ))[0]
                continue
            en_value, ru_value = (value, None) if locale == "en_us" else (None, value)
            if locale == "ru_ru":
                en_value = value
            else:
                ru_value = value
            if en_value is None or ru_value is None:
                raise ValueError(f"No paired translation for one-sided source component {storage_path} text {value!r}")
            pair = (en_value, ru_value)
            key = reusable.get(pair) or "skywars2.gui.source." + re.sub(r"[^a-zA-Z0-9_.-]", "_", storage_path) + f".part{ordinal}"
            additions["en_us"][key] = en_value
            additions["ru_ru"][key] = ru_value
            reusable[pair] = key
            leaf_keys[node_path] = key
        localized[storage_path] = replace_text(template, leaf_keys)
    return localized, additions


def add_source_component_gaps(localized: dict, raw_components: dict, pages: list[dict], shared: list[dict]) -> list[dict]:
    """Make source paths missing from both languages resolve to empty components explicitly."""
    references: dict[str, set[tuple[str, int | None]]] = {}

    def add_bindings(source):
        for binding in source.get("sourceComponentBindings", {}).values():
            for row in binding:
                terms = row.get("terms", []) if isinstance(row, dict) else row
                for term in terms:
                    path = term.get("storagePath") if isinstance(term, dict) else None
                    if not path:
                        continue
                    if path.endswith(".Name"):
                        root, component = path[:-5], ("Name", None)
                    elif ".Lore[" in path and path.endswith("]"):
                        marker = path.rfind(".Lore[")
                        root = path[:marker]
                        try:
                            component = ("Lore", int(path[marker + 6:-1]))
                        except ValueError:
                            raise ValueError(f"Invalid source lore reference {path}")
                    else:
                        root, component = path, ("direct", None)
                    references.setdefault(root, set()).add(component)

    for page in pages:
        for slot in page["slots"]:
            for source in slot["sources"]:
                add_bindings(source)
    for source in shared:
        add_bindings(source)

    gaps = []
    for root, components in sorted(references.items()):
        if root in raw_components["en"] or root in raw_components["ru"] or root in localized:
            continue
        if any(key.startswith(root + "-") for locale in ("en", "ru") for key in raw_components[locale]):
            continue
        value = {}
        for component, index in sorted(components):
            if component == "Name":
                value["Name"] = {"text": ""}
            elif component == "Lore":
                lore = value.setdefault("Lore", [])
                while len(lore) <= index:
                    lore.append({"text": ""})
            else:
                value["text"] = ""
        localized[root] = value
        gaps.append({
            "storagePath": root,
            "reason": "No English or Russian source assignment; native interpreted lookup resolves to an empty component.",
        })
    return gaps


def loot_component_bindings(table_path: Path) -> dict:
    """Preserve source set_name/set_lore expressions as storage refs and dynamics."""
    try:
        table = json.loads(table_path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"Cannot import component bindings from {table_path}: {exc}") from exc
    bindings = {"name": [], "lore": []}

    def collect(element):
        if isinstance(element, list):
            for child in element:
                collect(child)
            return
        if not isinstance(element, dict):
            return
        function = element.get("function", element.get("type"))
        if isinstance(function, str) and function.endswith(":set_name") and "name" in element:
            bindings["name"].append({
                "terms": source_component_terms(element["name"]),
                **({"condition": element["condition"]} if "condition" in element else {}),
            })
        if isinstance(function, str) and function.endswith(":set_lore") and isinstance(element.get("lore"), list):
            for line in element["lore"]:
                bindings["lore"].append({
                    "terms": source_component_terms(line),
                    "mode": element.get("mode", "replace_all"),
                    **({"condition": element["condition"]} if "condition" in element else {}),
                })
        for value in element.values():
            collect(value)

    collect(table)
    if not bindings["name"]:
        bindings.pop("name")
    if not bindings["lore"]:
        bindings.pop("lore")
    return bindings


def source_component_terms(value) -> list[dict]:
    """Flatten only top-level component concatenation, retaining nested styles."""
    values = value if isinstance(value, list) else [value]
    terms = []
    for component in values:
        if isinstance(component, dict) and component.get("type") == "nbt" \
                and component.get("storage") == "swrg:lang" and isinstance(component.get("nbt"), str):
            term = {"storagePath": component["nbt"]}
            for key in ("color", "bold", "italic", "underlined", "strikethrough", "obfuscated", "font", "extra"):
                if key in component:
                    term[key] = component[key]
            terms.append(term)
        else:
            terms.append({"literalComponent": component})
    return terms


def decode_text_components(raw: str) -> list[str]:
    result = []
    for value in TEXT_VALUE.findall(raw):
        try:
            result.append(json.loads(value))
        except json.JSONDecodeError:
            continue
    return result


def missing_storage_translations(old_root: Path, design: dict | None = None) -> dict:
    wanted: set[str] = set()
    for slot, values in {
        0: range(1, 3), 1: range(1, 4), 3: range(0, 2), 4: range(0, 2),
        5: range(0, 10), 6: range(0, 8),
        24: range(0, 5),
    }.items():
        wanted.update(f"page1000.s{slot}-{value}" for value in values)

    pack_en = resource_pack_language("en_us")
    pack_ru = resource_pack_language("ru_ru")
    known_en = {**pack_en, **project_language("en_us")}
    known_ru = {**pack_ru, **project_language("ru_ru")}
    locale_values: dict[str, dict[str, str]] = {"en_us": {}, "ru_ru": {}}
    for locale, file_name in (("en_us", "en.mcfunction"), ("ru_ru", "ru.mcfunction")):
        path = old_root / "world/datapacksdisabled/swarogskywars/data/swrg/function/core/lang" / file_name
        assignments = {}
        for line in path.read_text(encoding="utf-8-sig").splitlines():
            match = STORAGE_LINE.search(line)
            if match and match.group(1) in wanted:
                texts = decode_text_components(match.group(2))
                assignments[match.group(1)] = "".join(texts)
        pack = known_en if locale == "en_us" else known_ru
        for storage_key in sorted(wanted):
            translation_key = "swrg." + storage_key
            if translation_key not in pack:
                value = assignments.get(storage_key)
                if value is None:
                    raise ValueError(f"Missing source language value for {storage_key} ({locale})")
                locale_values[locale][translation_key] = value
    design = design or page_manifest(old_root, PROJECT / "src/main/resources")
    _, source_component_additions = localize_storage_components(design["sourceStorageComponents"])
    for locale, values in source_component_additions.items():
        for key, value in values.items():
            existing = locale_values[locale].get(key)
            if existing is not None and existing != value:
                raise ValueError(f"Generated component translation conflicts for {key} ({locale})")
            locale_values[locale][key] = value
    for page in design["pages"]:
        if page["pageId"] not in {"2000", "2000b", "2100", "2200"}:
            continue
        for slot in page["slots"]:
            for source in slot["sources"]:
                if "sourceMapId" not in source:
                    continue
                text = source.get("sourceName", {}).get("text")
                if text is None:
                    continue
                key = source.get("translationKey") or "skywars2.gui.map." + source["sourceMapId"]
                source["translationKey"] = key
                for locale, existing in (("en_us", known_en), ("ru_ru", known_ru)):
                    if key not in existing:
                        locale_values[locale][key] = text
    locale_values["en_us"]["skywars2.gui.original.health"] = "Default Health: %s"
    locale_values["ru_ru"]["skywars2.gui.original.health"] = "Здоровье игроков: %s"
    locale_values["en_us"]["skywars2.gui.original.price_prefix"] = "Price: "
    locale_values["ru_ru"]["skywars2.gui.original.price_prefix"] = "Цена: "
    return locale_values


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--old-root", type=Path, default=OLD_SERVER)
    parser.add_argument("--project", type=Path, default=PROJECT)
    parser.add_argument(
        "--refresh-locks",
        action="store_true",
        help="Update only lockedVariants, slot unlockConditions, and referenced modifier bindings in gui_design.json.",
    )
    args = parser.parse_args()
    resources = args.project / "src/main/resources"
    if args.refresh_locks:
        design_path = resources / "skywars2/gui_design.json"
        design = json.loads(design_path.read_text(encoding="utf-8-sig"))
        design = refresh_locked_design(design, args.old_root)
        design_path.write_text(
            json.dumps(design, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        return
    # Legacy functions/function fields are silently ignored by the 26.3 codecs.
    # Convert model/component modifiers just like the existing swrg loot importer.
    source_loot = args.old_root / "world/datapacksdisabled/LBC/data/lbc/loot_table/gui/page"
    for page in ("8000", "9000", "10000", "10100", "11000", "12000"):
        for source in sorted((source_loot / page).glob("*.json")):
            table = json.loads(source.read_text(encoding="utf-8-sig"))
            migrate_loot_schema(table)
            target = resources / "data/lbc/loot_table/gui/page" / page / source.name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(table, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (resources / "skywars2").mkdir(parents=True, exist_ok=True)
    design_path = resources / "skywars2/gui_design.json"
    design = page_manifest(args.old_root, resources)
    design_path.write_text(json.dumps(design, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

    translations_path = args.project / "build/verification/gui-design-new-translations.json"
    translations_path.parent.mkdir(parents=True, exist_ok=True)
    generated = missing_storage_translations(args.old_root, design)
    if translations_path.is_file():
        existing = json.loads(translations_path.read_text(encoding="utf-8-sig"))
        prepared_component_keys = {
            key
            for values in generated.values()
            for key in values
            if key.startswith("skywars2.gui.source.")
        }
        prepared_map_keys = {
            source["translationKey"]
            for page in design["pages"]
            for slot in page["slots"]
            for source in slot["sources"]
            if source.get("translationKey", "").startswith("skywars2.gui.map.")
        }
        for locale, values in generated.items():
            target = existing.setdefault(locale, {})
            for key in list(target):
                if key.startswith("skywars2.gui.source.") and key not in prepared_component_keys:
                    del target[key]
                elif key.startswith("skywars2.gui.map.") and key not in prepared_map_keys:
                    del target[key]
            for key, value in values.items():
                previous = target.get(key)
                if previous is not None and previous != value:
                    if key.startswith("skywars2.gui.source."):
                        target[key] = value
                        continue
                    raise ValueError(f"Translation additions conflict for {key} ({locale}); preserving existing value")
                target[key] = value
        generated = existing
    translations_path.write_text(json.dumps(generated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
