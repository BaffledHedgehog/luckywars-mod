#!/usr/bin/env python3
"""Compare imported GUI design metadata/resources with the independent old-source baseline.

This checker intentionally does not load or inspect Java implementation classes. It validates the
design import manifest and the datapack loot JSON referenced by it against
agent_notes/inventory/design_source_manifest.json.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE_MANIFEST = PROJECT_ROOT / "agent_notes/inventory/design_source_manifest.json"
DEFAULT_DESIGN_MANIFEST = PROJECT_ROOT / "src/main/resources/skywars2/gui_design.json"

LOOT_RE = re.compile(
    r"\b(?:loot replace entity|item replace entity)\s+@s(?:\[[^]]*\])?\s+"
    r"enderchest\.(\d+)\s+(?:loot\s+)?([a-z0-9_.-]+:gui/page/[a-z0-9_.-]+/[a-z0-9_.-]+)",
    re.IGNORECASE,
)
ITEM_MODEL_RE = re.compile(r"(?:minecraft:)?item_model\s*=\s*\"?([a-z0-9_.-]+:[a-z0-9_./-]+)", re.IGNORECASE)
DIRECT_ITEM_RE = re.compile(
    r"\bitem replace entity\s+@s(?:\[[^]]*\])?\s+enderchest\.(\d+)\s+with\s+([^\s\[]+)",
    re.IGNORECASE,
)


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8-sig") as handle:
        return json.load(handle)


def normalized(value: str) -> str:
    return value.replace("\\", "/").strip()


def source_slot_map(source: dict[str, Any]) -> dict[str, dict[str, set[str]]]:
    """Extract active page-function slot assignments, preserving conditional alternatives."""
    result: dict[str, dict[str, set[str]]] = defaultdict(lambda: defaultdict(set))
    for record in source.get("guiInteractionFunctions", []):
        path = normalized(record.get("source", ""))
        match = re.search(r"/function/(?:[^/]+/)?(?:swrg_kit_integration/)?gui/page/([^/]+)\.mcfunction$", path)
        if not match:
            continue
        page = match.group(1)
        namespace = record.get("namespace", "swrg")
        if namespace == "lbc":
            page = "lbc:" + page
        else:
            page = "swrg:" + page
        for line in record.get("exactRaw", "").splitlines():
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            found = LOOT_RE.search(stripped)
            if found:
                slot, table = found.groups()
                result[page][slot].add(table)
    return result


def source_page_details(source: dict[str, Any]) -> tuple[set[str], dict[str, dict[str, dict[str, list[dict[str, Any]]]]]]:
    """Record literal item replacements and item modifiers from active old page functions."""
    pages: set[str] = set()
    details: dict[str, dict[str, dict[str, list[dict[str, Any]]]]] = defaultdict(
        lambda: defaultdict(lambda: {"literal": [], "modifiers": [], "tables": []})
    )
    for record in source.get("guiInteractionFunctions", []):
        path = normalized(record.get("source", ""))
        match = re.search(r"/function/(?:[^/]+/)?(?:swrg_kit_integration/)?gui/page/([^/]+)\.mcfunction$", path)
        if not match:
            continue
        page_id = match.group(1)
        namespace = record.get("namespace", "swrg")
        pkey = f"{namespace}:{page_id}"
        pages.add(pkey)
        for line_no, line in enumerate(record.get("exactRaw", "").splitlines(), 1):
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            loot = LOOT_RE.search(stripped)
            if loot:
                slot, table = loot.groups()
                details[pkey][slot]["tables"].append({"source": path, "line": line_no, "id": table, "exactRaw": stripped})
            direct = DIRECT_ITEM_RE.search(stripped)
            if direct:
                slot, item = direct.groups()
                details[pkey][slot]["literal"].append({"source": path, "line": line_no, "item": item, "exactRaw": stripped})
            if "item modify entity @s enderchest." in stripped:
                slot_match = re.search(r"enderchest\.(\d+).*?\s([a-z0-9_.-]+:[a-z0-9_./-]+)\s*$", stripped, re.IGNORECASE)
                if slot_match:
                    slot, modifier = slot_match.groups()
                    details[pkey][slot]["modifiers"].append({"source": path, "line": line_no, "id": modifier, "exactRaw": stripped})
    return pages, details


def page_key(page: Any) -> str:
    text = str(page)
    if ":" not in text:
        return "swrg:" + text
    namespace, path = text.split(":", 1)
    match = re.search(r"(?:swrg_kit_integration/)?gui/page/([^/]+)$", path)
    return f"{namespace}:{match.group(1)}" if match else text


def slot_number(slot: dict[str, Any]) -> str | None:
    for key in ("slot", "slotId", "enderSlot", "index"):
        if key in slot:
            return str(slot[key])
    return None


def table_id(slot: dict[str, Any]) -> str | None:
    for key in ("sourceLootTable", "lootTable", "lootTableId", "source"):
        value = slot.get(key)
        if isinstance(value, str) and ":gui/" in value:
            return value
    return None


def canonical_loot(value: Any) -> Any:
    """Normalize harmless loot schema rewrites while preserving item/components/functions."""
    if isinstance(value, dict):
        result = {key: canonical_loot(child) for key, child in value.items() if key != "functions"}
        functions = value.get("functions")
        if functions is not None:
            result["modifier"] = canonical_loot(functions[0]) if len(functions) == 1 else canonical_loot(functions)
        if "function" in result and "type" not in result:
            # Older source format puts a loot function id in `function`; newer entries use `type`.
            function_id = result.pop("function")
            result["type"] = function_id if ":" in function_id else "minecraft:" + function_id
        if isinstance(result.get("type"), str) and result["type"] in ("set_components", "set_name", "set_lore"):
            result["type"] = "minecraft:" + result["type"]
        if isinstance(result.get("condition"), dict) and "conditions" not in result:
            result["conditions"] = [result.pop("condition")]
        elif isinstance(result.get("condition"), str) and "type" not in result:
            result["type"] = result.pop("condition")
        if isinstance(result.get("conditions"), list):
            normalized_conditions = []
            for condition in result["conditions"]:
                if isinstance(condition, dict) and "condition" in condition and "type" not in condition:
                    condition["type"] = condition.pop("condition")
                normalized_conditions.append(condition)
            result["conditions"] = normalized_conditions
        return result
    if isinstance(value, list):
        return [canonical_loot(child) for child in value]
    return value


def table_resource_path(project_root: Path, loot_table_id: str) -> Path:
    namespace, path = loot_table_id.split(":", 1)
    return project_root / "src/main/resources/data" / namespace / "loot_table" / (path + ".json")


def item_model_ids(value: Any) -> set[str]:
    found: set[str] = set()
    if isinstance(value, dict):
        for key, child in value.items():
            if key in ("item_model", "minecraft:item_model") and isinstance(child, str):
                found.add(child if ":" in child else "minecraft:" + child)
            found.update(item_model_ids(child))
    elif isinstance(value, list):
        for child in value:
            found.update(item_model_ids(child))
    return found


def dynamic_examples(source: dict[str, Any]) -> list[str]:
    """Examples where identical slot identity has score/player-dependent rendered output."""
    examples = [
        "swrg:1000 slots 0-17,19-24: setting scores choose page1000.sN name components via gui/page/1000-lang",
        "swrg:0 slot 8: minecraft:fill_player_head resolves from the viewer/player profile",
        "swrg:3000 slots 0-20: kit availability tags control populated versus empty/locked cells",
        "swrg:3000 slots 21-23: LBC advancements select seasonal item variants or leave cells empty",
        "swrg:7300 and LBC perk pages: advancement/score conditions select available, locked, selected and empty variants",
    ]
    raw_functions = "\n".join(item.get("exactRaw", "") for item in source.get("guiInteractionFunctions", []))
    if "page1000.s" not in raw_functions:
        examples.pop(0)
    if "fill_player_head" not in "\n".join(json.dumps(x) for x in source.get("slotTables", [])):
        examples.pop(0 if not raw_functions else 1)
    if "advancements=" not in raw_functions:
        examples = [item for item in examples if "advancement" not in item.lower()]
    return examples


def check(source_path: Path, design_path: Path, project_root: Path) -> tuple[int, list[str], dict[str, Any]]:
    errors: list[str] = []
    source = read_json(source_path)
    design = read_json(design_path)
    expected_by_page = source_slot_map(source)
    source_pages, source_details = source_page_details(source)
    source_pages_with_slots = set(expected_by_page) | {
        page for page, slots in source_details.items()
        if any(details["literal"] or details["modifiers"] for details in slots.values())
    }
    counters = {
        "sourceGuiPages": len(expected_by_page),
        "importPages": 0,
        "slotReferencesCompared": 0,
        "localLootJsonCompared": 0,
        "localLootJsonExactMatches": 0,
        "sourceLineReferencesCompared": 0,
        "sourceLineReferencesMatched": 0,
        "sourceModelIdsCompared": 0,
        "sourceModelIdsPresent": 0,
        "sourceStorageComponentsCompared": 0,
        "designStorageSourcesDeclared": 0,
        "dynamicExamples": dynamic_examples(source),
        "loaderStateFlags": design.get("loaderStateFlags", design.get("loaderState", [])),
        "documentedExceptions": design.get("exceptions", design.get("documentedExceptions", [])),
    }

    container = design.get("container", {})
    expected_size = source.get("renderers", [{}])[-1].get("size", 27)
    actual_size = container.get("size", design.get("containerSize"))
    if actual_size != expected_size:
        errors.append(f"container size mismatch: imported={actual_size!r}, source={expected_size!r}")

    title = container.get("title", design.get("title"))
    if isinstance(title, dict):
        title_id = title.get("translate", title.get("key", title.get("value")))
        if title_id not in ("container.enderchest", "minecraft.container.enderchest"):
            errors.append(f"container title mismatch: expected translatable container.enderchest, imported={title!r}")
    elif title not in ("container.enderchest", "minecraft:container.enderchest"):
        errors.append(f"container title mismatch: expected container.enderchest translation, imported={title!r}")

    pages = design.get("pages", [])
    if not isinstance(pages, list):
        errors.append("pages must be an array")
        pages = []
    counters["importPages"] = len(pages)
    imported_by_page: dict[str, dict[str, set[str]]] = defaultdict(lambda: defaultdict(set))
    imported_table_lines: dict[str, dict[str, list[tuple[str, int | None]]]] = defaultdict(lambda: defaultdict(list))
    imported_literal_by_page: dict[str, dict[str, list[dict[str, Any]]]] = defaultdict(lambda: defaultdict(list))
    imported_modifiers_by_page: dict[str, dict[str, set[str]]] = defaultdict(lambda: defaultdict(set))
    imported_modifier_lines: dict[str, dict[str, list[tuple[str, int | None]]]] = defaultdict(lambda: defaultdict(list))
    imported_direct_models: set[str] = set()
    source_manifest_tables = {
        item.get("lootTable"): item
        for item in source.get("slotTables", [])
        if isinstance(item.get("lootTable"), str)
    }
    source_manifest_tables.update({
        item.get("lootTable"): item
        for item in source.get("nonSlotGuiLootTables", [])
        if isinstance(item.get("lootTable"), str)
    })
    source_lines = {
        (normalized(item.get("source", "")), line_no): line
        for item in source.get("guiInteractionFunctions", [])
        for line_no, line in enumerate(item.get("exactRaw", "").splitlines(), 1)
    }
    source_function_path_by_page = {}
    referenced_table_ids: set[str] = set()
    for item in source.get("guiInteractionFunctions", []):
        path = normalized(item.get("source", ""))
        match = re.search(r"/function/(?:swrg|swrg_kit_integration/)?gui/page/([^/]+)\.mcfunction$", path)
        if match:
            source_function_path_by_page[f"{item.get('namespace', 'swrg')}:{match.group(1)}"] = path

    for page_entry in pages:
        raw_page = page_entry.get("pageId", page_entry.get("page", ""))
        namespace = page_entry.get("namespace")
        pkey = page_key(f"{namespace}:{raw_page}" if namespace and ":" not in str(raw_page) else raw_page)
        # Retain empty maps so a direct-item-only page (for example language heads) is recognized.
        imported_by_page[pkey]
        declared_function = page_entry.get("sourceFunction")
        if declared_function:
            namespace, page_id = pkey.split(":", 1)
            expected_function = (f"{namespace}:swrg_kit_integration/gui/page/{page_id}"
                                 if namespace == "lbc" else f"{namespace}:gui/page/{page_id}")
            lbc_alias = f"{namespace}:gui/page/{page_id}"
            valid_functions = (expected_function, expected_function + ".mcfunction", lbc_alias, lbc_alias + ".mcfunction")
            if declared_function not in valid_functions:
                errors.append(f"{pkey}: sourceFunction mismatch, imported={declared_function!r}, expected={expected_function!r}")
        slots = page_entry.get("slots", [])
        if not isinstance(slots, list):
            errors.append(f"{pkey}: slots must be an array")
            continue
        for slot_entry in slots:
            if not isinstance(slot_entry, dict):
                errors.append(f"{pkey}: slot record is not an object")
                continue
            number = slot_number(slot_entry)
            if number is not None and not (0 <= int(number) <= 26):
                errors.append(f"{pkey}: slot outside source EnderItems range 0..26: {number}")
            if number is None:
                errors.append(f"{pkey}: slot record must contain a numeric source slot: {slot_entry!r}")
                continue
            sources = slot_entry.get("sources")
            if not isinstance(sources, list):
                sources = [slot_entry]
            if not sources:
                errors.append(f"{pkey} slot {number}: sources array is empty")
                continue
            for source_variant in sources:
                if not isinstance(source_variant, dict):
                    continue
                modifier_values = source_variant.get(
                    "itemModifiers", source_variant.get("modifiers", slot_entry.get("itemModifiers", slot_entry.get("modifiers", [])))
                )
                if not modifier_values and source_variant.get("itemModifier"):
                    modifier_values = [source_variant["itemModifier"]]
                for modifier in modifier_values:
                    modifier_id = modifier if isinstance(modifier, str) else modifier.get("id")
                    modifier_line = source_variant.get("sourceLine") if isinstance(modifier, str) else modifier.get("sourceLine", source_variant.get("sourceLine"))
                    if modifier_id:
                        imported_modifiers_by_page[pkey][number].add(modifier_id)
                        if modifier_line is not None:
                            try:
                                modifier_line = int(modifier_line)
                            except (TypeError, ValueError):
                                modifier_line = None
                        imported_modifier_lines[pkey][number].append((modifier_id, modifier_line))
            for variant in sources:
                if not isinstance(variant, dict):
                    errors.append(f"{pkey} slot {number}: source variant is not an object")
                    continue
                source_id = table_id(variant) or table_id(slot_entry)
                literal = variant.get("sourceItem", variant.get("sourceLiteral", variant.get("literalSource")))
                source_line = variant.get("sourceLine")
                if source_line is not None:
                    counters["sourceLineReferencesCompared"] += 1
                    function_path = source_function_path_by_page.get(pkey, "")
                    try:
                        source_line = int(source_line)
                    except (TypeError, ValueError):
                        errors.append(f"{pkey} slot {number}: sourceLine must be a 1-based integer, got {source_line!r}")
                        source_line = None
                    exact_line = source_lines.get((function_path, source_line)) if source_line is not None else None
                    if exact_line is None:
                        errors.append(f"{pkey} slot {number}: sourceLine {source_line!r} not present in independent page function")
                    else:
                        stripped = exact_line.strip()
                        loot_on_line = re.search(r"\b(?:loot|item replace entity)\b.*?\b([a-z0-9_.-]+:[a-z0-9_./-]+)\s*$", stripped, re.IGNORECASE)
                        modifier_id = variant.get("itemModifier")
                        literal_item = re.search(r"\bwith\s+([^\s\[]+)", stripped, re.IGNORECASE)
                        if ((source_id and (not loot_on_line or loot_on_line.group(1) != source_id))
                                or (literal is not None and (not literal_item or literal_item.group(1) != literal))
                                or (modifier_id and modifier_id not in stripped)):
                            errors.append(f"{pkey} slot {number}: sourceLine {source_line} does not define imported variant {source_id or literal!r}")
                        else:
                            counters["sourceLineReferencesMatched"] += 1
                        imported_condition = variant.get("condition")
                        expected_condition = stripped.split(" run ", 1)[0] if stripped.startswith("execute ") and " run " in stripped else None
                        if imported_condition != expected_condition:
                            errors.append(f"{pkey} slot {number}: sourceLine {source_line} condition mismatch; source={expected_condition!r}, imported={imported_condition!r}")
                        component_text = variant.get("sourceComponentText")
                        if component_text and component_text not in stripped:
                            errors.append(f"{pkey} slot {number}: sourceLine {source_line} omits imported literal component payload")
                        if component_text:
                            imported_direct_models.update(ITEM_MODEL_RE.findall(component_text))
                modifier_only = variant.get("itemModifier")
                if source_id is None and literal is None and not modifier_only:
                    errors.append(f"{pkey} slot {number}: source variant requires sourceLootTable or sourceItem")
                    continue
                counters["slotReferencesCompared"] += 1
                if source_id is None and literal is not None:
                    literal_ref = {"source": function_path, "line": source_line}
                    imported_literal_by_page[pkey][number].append(literal_ref)
                    continue
                if source_id is None:
                    continue
                imported_by_page[pkey][number].add(source_id)
                imported_table_lines[pkey][number].append((source_id, source_line))
                referenced_table_ids.add(source_id)
                if source_id not in source_manifest_tables:
                    errors.append(f"{pkey} slot {number}: source loot table is absent from independent baseline: {source_id}")
                    continue
                source_record = source_manifest_tables[source_id]
                local_path = table_resource_path(project_root, source_id)
                if not local_path.exists():
                    errors.append(f"{pkey} slot {number}: referenced imported loot JSON missing: {local_path}")
                    continue
                counters["localLootJsonCompared"] += 1
                local_json = read_json(local_path)
                baseline_json = source_record.get("parsed")
                if canonical_loot(local_json) == canonical_loot(baseline_json):
                    counters["localLootJsonExactMatches"] += 1
                else:
                    errors.append(f"{pkey} slot {number}: imported loot JSON differs from old source {source_id} ({local_path})")

    for button in design.get("sharedButtons", []):
        if not isinstance(button, dict):
            continue
        source_id = table_id(button)
        if not source_id:
            errors.append(f"shared button lacks sourceLootTable: {button!r}")
            continue
        if source_id not in source_manifest_tables:
            errors.append(f"shared button source missing from independent baseline: {source_id}")
            continue
        referenced_table_ids.add(source_id)
        local_path = table_resource_path(project_root, source_id)
        if not local_path.exists():
            errors.append(f"shared button imported loot JSON missing: {local_path}")
            continue
        counters["localLootJsonCompared"] += 1
        if canonical_loot(read_json(local_path)) == canonical_loot(source_manifest_tables[source_id].get("parsed")):
            counters["localLootJsonExactMatches"] += 1
        else:
            errors.append(f"shared button imported loot JSON differs from source: {source_id}")

    # Check every imported page's slot mapping against active source function commands. Source builders
    # may omit empty slots; variants are a set because the same slot can have seasonal/ranked branches.
    for pkey, slots in imported_by_page.items():
        expected = expected_by_page.get(pkey, {})
        if pkey not in source_pages:
            errors.append(f"imported page has no matching source page function: {pkey}")
            continue
        for slot, tables in expected.items():
            observed = slots.get(slot, set())
            if observed != tables:
                errors.append(f"{pkey} slot {slot}: source variants={sorted(tables)}, imported refs={sorted(observed)}")
        for slot, refs in slots.items():
            expected_refs = expected.get(slot, set())
            if refs != expected_refs:
                errors.append(f"{pkey} slot {slot}: imported refs={sorted(refs)}, source variants={sorted(expected_refs)}")
        expected_details = source_details.get(pkey, {})
        for slot, details in expected_details.items():
            expected_lines = {(row["id"], row["line"]) for row in details["tables"]}
            if expected_lines:
                actual_lines = set(imported_table_lines.get(pkey, {}).get(slot, []))
                if actual_lines != expected_lines:
                    errors.append(f"{pkey} slot {slot}: source variant lines={sorted(expected_lines)}, imported lines={sorted(actual_lines, key=str)}")
        expected_direct = source_details.get(pkey, {})
        for slot, details in expected_direct.items():
            if details["literal"]:
                refs = imported_literal_by_page.get(pkey, {}).get(slot, [])
                expected_refs = [{"source": row["source"], "line": row["line"]} for row in details["literal"]]
                if refs != expected_refs:
                    errors.append(f"{pkey} slot {slot}: direct item source lines={expected_refs}, imported literal refs={refs}")
            if details["modifiers"]:
                expected_refs = {row["id"] for row in details["modifiers"]}
                actual_refs = imported_modifiers_by_page.get(pkey, {}).get(slot, set())
                if actual_refs != expected_refs:
                    errors.append(f"{pkey} slot {slot}: item modifier ids={sorted(expected_refs)}, imported modifiers={sorted(actual_refs)}")
                expected_lines = {(row["id"], row["line"]) for row in details["modifiers"]}
                actual_lines = set(imported_modifier_lines.get(pkey, {}).get(slot, []))
                if actual_lines != expected_lines:
                    errors.append(f"{pkey} slot {slot}: modifier source lines={sorted(expected_lines)}, imported modifier lines={sorted(actual_lines, key=str)}")

    for pkey, slots in imported_literal_by_page.items():
        if pkey not in source_pages:
            errors.append(f"literal slot page has no matching source page function: {pkey}")
        expected_slots = source_details.get(pkey, {})
        for slot in slots:
            if not expected_slots.get(slot, {}).get("literal"):
                errors.append(f"{pkey} slot {slot}: imported literal has no active source item replacement")

    declared_required = design.get("requiredSourcePages")
    if declared_required is None and design.get("completeSourcePageImport", False):
        declared_required = sorted(source_pages_with_slots)
    if isinstance(declared_required, list):
        imported_pages = set(imported_by_page)
        for raw_page in declared_required:
            pkey = page_key(raw_page)
            # A real source page may contain navigation only (e.g. lbc:11100).
            if pkey not in source_pages_with_slots and pkey not in source_pages:
                errors.append(f"required page has no old source function: {pkey}")
            elif pkey not in imported_pages:
                errors.append(f"required source page omitted from import: {pkey}")

    imported_models: set[str] = set()
    expected_storage: set[str] = set()
    for source_id in referenced_table_ids:
        record = source_manifest_tables.get(source_id, {})
        imported_models.update(item_model_ids(record.get("parsed")))
        expected_storage.update(item.get("storage") for item in record.get("storageComponents", []) if item.get("storage"))
    counters["sourceStorageComponentsCompared"] = len(expected_storage)
    expected_models = set(source.get("resourcePackDependencies", {}).get("ids", {}).get("itemModels", []))
    imported_models.update(imported_direct_models)
    relevant_models = expected_models & imported_models
    counters["sourceModelIdsCompared"] = len(relevant_models)
    counters["sourceModelIdsPresent"] = len(relevant_models)
    missing_models = expected_models - imported_models
    if missing_models:
        # Models can be used by source pages omitted from a deliberately partial import. Report only if
        # the import claims all pages; otherwise the omitted source pages are explicit scope boundaries.
        if design.get("completeSourcePageImport", False):
            errors.append(f"source item_model references missing from imported slot JSON: {sorted(missing_models)}")

    declared_models = design.get("itemModelIds")
    if declared_models is None and isinstance(design.get("resourceDependencies"), dict):
        declared_models = design["resourceDependencies"].get("itemModels")
    if isinstance(declared_models, list) and set(declared_models) != imported_models:
        errors.append(f"declared itemModelIds mismatch: expected={sorted(imported_models)}, imported={sorted(set(declared_models))}")

    declared_storage = design.get("storageSources", design.get("storageContainers"))
    if isinstance(declared_storage, list):
        counters["designStorageSourcesDeclared"] = len(declared_storage)
        normalized_storage = {
            value if isinstance(value, str) else value.get("storage", value.get("id"))
            for value in declared_storage
        }
        normalized_storage.discard(None)
        if normalized_storage != expected_storage:
            errors.append(f"declared storageSources mismatch: source refs={sorted(expected_storage)}, import refs={sorted(normalized_storage)}")

    source_function_raw_by_id = {}
    for item in source.get("guiInteractionFunctions", []):
        path = normalized(item.get("source", ""))
        match = re.search(r"/function/(?:(?:swrg|lbc)/)?(?:swrg_kit_integration/)?(.+?)\.mcfunction$", path)
        if match:
            source_function_raw_by_id[f"{item.get('namespace', 'swrg')}:{match.group(1)}"] = item.get("exactRaw", "")
    for binding in design.get("storageBindings", []):
        if not isinstance(binding, dict):
            errors.append(f"storage binding must be an object: {binding!r}")
            continue
        storage_id = binding.get("storagePath", binding.get("storage"))
        source_function = binding.get("sourceFunction")
        source_raw = source_function_raw_by_id.get(source_function, "")
        if not storage_id or not source_function or not source_raw:
            errors.append(f"storage binding lacks a matching old source function/storage id: {binding!r}")
        elif storage_id not in source_raw:
            errors.append(f"storage binding {storage_id} is absent from old function {source_function}")

    # Optional flattened snapshots are checked too; the referenced source JSON remains authoritative.
    for page_entry in pages:
        pkey = page_key(page_entry.get("pageId", page_entry.get("page", "")))
        for slot_entry in page_entry.get("slots", []):
            flattened = slot_entry.get("item")
            if not isinstance(flattened, dict):
                continue
            source_id = table_id(slot_entry)
            baseline = source_manifest_tables.get(source_id, {}).get("parsed")
            expected_material = next((node.get("name") for node in _walk(baseline) if node.get("type") == "minecraft:item" and "name" in node), None) if baseline else None
            if flattened.get("material") and expected_material and flattened["material"] != expected_material:
                errors.append(f"{pkey} slot {slot_number(slot_entry)}: generic/substitute material {flattened['material']!r}; source has {expected_material!r}")

    return len(errors), errors, counters


def _walk(value: Any):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from _walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from _walk(child)


def _component_id(item: dict[str, Any]) -> str:
    return str(item.get("id", item.get("type", item.get("material", ""))))


def _components(item: dict[str, Any]) -> dict[str, Any]:
    value = item.get("components", {})
    return value if isinstance(value, dict) else {}


def check_looper_artifact_source(project: Path, source_manifest: Path) -> int:
    """Compare native Looper's explicit mapping with the old reveal function and RP keys."""
    from import_lbc_loadouts import SOURCE

    source_function = SOURCE / "function/swrg_kit_integration/game/skills/looper_explain_artifact.mcfunction"
    source_map: dict[tuple[int, int], dict[str, Any]] = {}
    source_line = re.compile(
        r'^execute if entity @s\[nbt=\{SelectedItem:\{components:\{"minecraft:custom_data":'
        r'\{mystical:(\d+),type:(\d+)\}\}\}\}\] run data modify block .+?'
        r'components\."minecraft:lore"\[1\] set value (\{.+\})$'
    )
    for line_number, line in enumerate(source_function.read_text(encoding="utf-8-sig").splitlines(), 1):
        match = source_line.fullmatch(line.strip())
        if not match:
            continue
        mystical, artifact_type, raw_component = match.groups()
        pair = (int(mystical), int(artifact_type))
        if pair in source_map:
            raise AssertionError(f"Duplicate Looper source mapping {pair} at {source_function}:{line_number}")
        component = json.loads(raw_component)
        if set(component) != {"translate", "italic", "color"} \
                or component.get("italic") is not False or component.get("color") != "yellow":
            raise AssertionError(f"Unexpected Looper lore payload for {pair}: {component!r}")
        source_map[pair] = component

    if len(source_map) != 23:
        raise AssertionError(f"Expected 23 source Looper lore mappings, found {len(source_map)}")
    source_text = source_function.read_text(encoding="utf-8-sig")
    if not re.search(r'data remove block .*components\."minecraft:custom_data"\.unresolved_artifact\s*$',
                     source_text, re.MULTILINE):
        raise AssertionError("Source Looper function no longer removes only unresolved_artifact")

    implementation = (project / "src/main/java/me/skywars2/game/LbcSkills.java").read_text(encoding="utf-8-sig")
    key_list = re.search(r"ARTIFACT_KEYS\s*=\s*List\.of\((.*?)\);", implementation, re.DOTALL)
    if key_list is None:
        raise AssertionError("LbcSkills does not declare the explicit ARTIFACT_KEYS list")
    keys = re.findall(r'"([^"\\]+)"', key_list.group(1))
    if len(keys) != 23 or len(set(keys)) != 23:
        raise AssertionError(f"LbcSkills ARTIFACT_KEYS must contain 23 distinct literal keys, found {len(keys)}")

    ranges = re.findall(
        r"if\s*\(mystical\s*==\s*(\d+)\s*&&\s*type\s*>=\s*1\s*&&\s*type\s*<=\s*(\d+)\)\s*return\s*([^;]+);",
        implementation,
    )
    implementation_map: dict[tuple[int, int], str] = {}
    for raw_mystical, raw_max_type, raw_index in ranges:
        mystical, max_type = int(raw_mystical), int(raw_max_type)
        index = re.fullmatch(r"\s*(?:(\d+)\s*\+\s*)?type\s*", raw_index)
        if index is None:
            raise AssertionError(f"Unrecognized Looper artifact index expression: {raw_index!r}")
        offset = int(index.group(1) or 0)
        for artifact_type in range(1, max_type + 1):
            pair = (mystical, artifact_type)
            if pair in implementation_map:
                raise AssertionError(f"Overlapping LbcSkills artifact ranges at {pair}")
            implementation_map[pair] = keys[offset + artifact_type - 1]
    if len(implementation_map) != 23:
        raise AssertionError(f"LbcSkills range rules cover {len(implementation_map)} artifact states, expected 23")

    for pair, component in source_map.items():
        implementation_key = implementation_map.get(pair)
        source_key = component["translate"]
        if implementation_key != source_key:
            raise AssertionError(f"Looper {pair}: Java key {implementation_key!r}, source key {source_key!r}")

    manifest = read_json(source_manifest)
    pack_root = Path(manifest.get("sourceRoots", {}).get("resourcePack", ""))
    if not pack_root.is_dir():
        raise AssertionError(f"Read-only resource-pack root is unavailable: {pack_root}")
    languages = {
        locale: read_json(pack_root / "assets/minecraft/lang" / f"{locale}.json")
        for locale in ("en_us", "ru_ru")
    }
    for key in implementation_map.values():
        for locale, table in languages.items():
            if not isinstance(table.get(key), str) or not table[key]:
                raise AssertionError(f"Missing existing {locale} resource-pack translation for {key}")
    return len(source_map)


def _text(value: Any) -> str:
    """Flatten only literal text for semantic checks; preserve full JSON elsewhere."""
    if isinstance(value, str):
        return value
    if isinstance(value, list):
        return "".join(_text(part) for part in value)
    if isinstance(value, dict):
        return str(value.get("text", "")) + _text(value.get("extra", []))
    return ""


def _check_component_shape(expected: Any, actual: Any, where: str, errors: list[str]) -> None:
    """Require all authored source properties while allowing resolved score text values."""
    if isinstance(expected, dict):
        if not isinstance(actual, dict):
            # ItemStack.CODEC writes an unstyled literal text component as a bare string.
            if set(expected) == {"text"} and actual == expected["text"]:
                return
            # Score components resolve against the check player's score before export;
            # the codec serializes their resulting numeric literal as a string.
            if isinstance(actual, str) and isinstance(expected.get("score"), dict) \
                    and re.fullmatch(r"-?\d+(?:\.\d+)?", actual):
                return
            errors.append(f"{where}: source component object became {type(actual).__name__}")
            return
        if actual.get("translate"):
            if "text" in expected and actual.get("fallback") is not None and actual["fallback"] != expected["text"]:
                errors.append(f"{where}: translated component fallback {actual['fallback']!r}, source text {expected['text']!r}")
            for key in ("color", "italic", "bold", "underlined", "strikethrough", "obfuscated", "font"):
                if key in expected and actual.get(key) != expected[key]:
                    errors.append(f"{where}: translated component {key}={actual.get(key)!r}, source value {expected[key]!r}")
            return
        for key, value in expected.items():
            # These fields identify a live storage/score expansion, not a literal text payload.
            if key in ("nbt", "storage", "source", "type", "interpret", "score"):
                continue
            if key not in actual:
                errors.append(f"{where}: missing source component field {key!r}")
            else:
                _check_component_shape(value, actual[key], where + "." + key, errors)
    elif isinstance(expected, list):
        if isinstance(actual, dict) and isinstance(actual.get("extra"), list):
            actual_items = list(actual["extra"])
            if actual.get("text") not in (None, "") or actual.get("translate"):
                actual_items.insert(0, {key: value for key, value in actual.items() if key != "extra"})
        else:
            actual_items = actual if isinstance(actual, list) else []
        if len(actual_items) < len(expected):
            # Component serialization can coalesce adjacent terms under one `extra`
            # wrapper. Compare the ordered authored leaves after dropping empty wrapper
            # nodes, while the recursive shape checks still enforce each term's style.
            flattened: list[Any] = []
            def flatten_terms(value: Any) -> None:
                if isinstance(value, dict):
                    if value.get("translate") or value.get("fallback") or value.get("text") not in (None, ""):
                        flattened.append({key: child for key, child in value.items() if key != "extra"})
                    if isinstance(value.get("extra"), list):
                        for child in value["extra"]:
                            flatten_terms(child)
                elif isinstance(value, list):
                    for child in value:
                        flatten_terms(child)
                elif isinstance(value, str):
                    flattened.append(value)
            flatten_terms(actual)
            if len(flattened) >= len(expected):
                actual_items = flattened
            else:
                errors.append(f"{where}: source component list length {len(actual_items)} < {len(expected)}")
                return
        for index, value in enumerate(expected):
            _check_component_shape(value, actual_items[index], f"{where}[{index}]", errors)
    elif isinstance(expected, (str, int, float, bool)):
        if actual != expected:
            errors.append(f"{where}: source component value {actual!r}, expected {expected!r}")


def _source_language_values(source: dict[str, Any]) -> dict[str, dict[str, Any]]:
    values: dict[str, dict[str, Any]] = {"en": {}, "ru": {}}
    commented_values: dict[str, dict[str, Any]] = {"en": {}, "ru": {}}
    for locale_record in source.get("languageStorageSources", []):
        name = str(locale_record.get("source", "")).lower()
        locale = "ru" if "ru" in name else "en" if "en" in name else ""
        if not locale:
            continue
        for line in locale_record.get("exactRaw", "").splitlines():
            is_comment = line.lstrip().startswith("#")
            candidate_line = line.lstrip()[1:].lstrip() if is_comment else line
            match = re.search(r"data modify storage swrg:lang ([^ ]+) set value (.+)$", candidate_line)
            if not match:
                continue
            path, raw = match.groups()
            # Helpers use SNBT member names and single-quoted JSON text components.
            # Quote conversion must ignore colons inside text values (e.g. "Games:").
            jsonish = _snbt_jsonish(raw)
            try:
                target = commented_values[locale] if is_comment else values[locale]
                target[path] = json.loads(jsonish)
            except json.JSONDecodeError:
                continue
    for locale in values:
        merged = dict(commented_values[locale])
        merged.update(values[locale])
        values[locale] = merged
    return values


def _snbt_jsonish(raw: str) -> str:
    out: list[str] = []
    index = 0
    while index < len(raw):
        char = raw[index]
        if char == '"':
            start = index
            index += 1
            escaped = False
            while index < len(raw):
                current = raw[index]
                index += 1
                if escaped:
                    escaped = False
                elif current == "\\":
                    escaped = True
                elif current == '"':
                    break
            out.append(raw[start:index])
            continue
        if char == "'":
            index += 1
            value: list[str] = []
            while index < len(raw):
                current = raw[index]
                index += 1
                if current == "\\" and index < len(raw):
                    escaped_char = raw[index]
                    index += 1
                    value.append(escaped_char if escaped_char in ("'", "\\") else "\\" + escaped_char)
                elif current == "'":
                    break
                else:
                    value.append(current)
            out.append(json.dumps("".join(value), ensure_ascii=False))
            continue
        if char.isalpha() or char == "_":
            end = index + 1
            while end < len(raw) and (raw[end].isalnum() or raw[end] in "_.-"):
                end += 1
            token = raw[index:end]
            lookahead = end
            while lookahead < len(raw) and raw[lookahead].isspace():
                lookahead += 1
            out.append(json.dumps(token) if lookahead < len(raw) and raw[lookahead] == ":" else token)
            index = end
            continue
        if char.isdigit() or (char == "-" and index + 1 < len(raw) and raw[index + 1].isdigit()):
            match = re.match(r"-?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?[bBsSlLfFdD]", raw[index:])
            if match:
                token = match.group(0)
                out.append(token[:-1])
                index += len(token)
                continue
        out.append(char)
        index += 1
    return "".join(out)


def _resolve_nbt_path(values: dict[str, Any], path: str) -> Any:
    candidates = sorted((key for key in values if path == key or path.startswith(key + ".")), key=len, reverse=True)
    if not candidates:
        return None
    current: Any = values[candidates[0]]
    tail = path[len(candidates[0]):].lstrip(".")
    for segment in re.findall(r"[^.\[\]]+|\[\d+\]", tail):
        if segment.startswith("["):
            if not isinstance(current, list):
                return None
            try:
                current = current[int(segment[1:-1])]
            except (ValueError, IndexError):
                return None
        elif isinstance(current, dict):
            current = current.get(segment)
        else:
            return None
    return current


def _source_path_has_assignment(values: dict[str, Any], path: str) -> bool:
    if _resolve_nbt_path(values, path) is not None:
        return True
    # The settings helper stores score-selected alternatives as `pageN.sM-V`,
    # while the loot table references the logical aggregate `pageN.sM`.
    match = re.match(r"^(page\d+\.s\d+)(\..+)?$", path)
    if not match:
        return False
    stem, tail = match.groups()
    return any(key.startswith(stem + "-")
               and _resolve_nbt_path(values, key + (tail or "")) is not None
               for key in values)


def _paired_translation_errors(source_en: Any, source_ru: Any, localized: Any,
                               en_text: dict[str, str], ru_text: dict[str, str], where: str) -> list[str]:
    errors: list[str] = []
    if isinstance(localized, dict):
        key = localized.get("translate")
        if isinstance(key, str):
            shape_source = source_en if source_en is not None else source_ru
            _check_component_shape(shape_source, localized, where, errors)
            expected_en = source_en.get("text") if isinstance(source_en, dict) else None
            expected_ru = source_ru.get("text") if isinstance(source_ru, dict) else None
            # Exact source helper text is the baseline. A replacement character marks an
            # upstream encoding fault; style/path checks still run, but text comparison waits.
            for locale, expected, table in (("EN", expected_en, en_text), ("RU", expected_ru, ru_text)):
                actual = table.get(key)
                if expected is None:
                    if actual is None:
                        errors.append(f"{where}: one-sided source key {key!r} has no {locale} resource translation")
                elif "\ufffd" not in expected:
                    if actual is None:
                        errors.append(f"{where}: translation key {key!r} missing from {locale} source/resource languages")
                    elif actual != expected:
                        errors.append(f"{where}: {locale} translation {key!r}={actual!r}, source={expected!r}")
        for name, value in localized.items():
            if name == "translate":
                continue
            en_child = source_en.get(name) if isinstance(source_en, dict) else None
            ru_child = source_ru.get(name) if isinstance(source_ru, dict) else None
            errors.extend(_paired_translation_errors(en_child, ru_child, value, en_text, ru_text, where + "." + name))
    elif isinstance(localized, list):
        for index, value in enumerate(localized):
            en_child = source_en[index] if isinstance(source_en, list) and index < len(source_en) else None
            ru_child = source_ru[index] if isinstance(source_ru, list) and index < len(source_ru) else None
            errors.extend(_paired_translation_errors(en_child, ru_child, value, en_text, ru_text, f"{where}[{index}]"))
    return errors


def _binding_name_terms(binding: Any, localized_templates: dict[str, Any], source_en: dict[str, Any]) -> list[dict[str, Any]]:
    terms: list[dict[str, Any]] = []
    if isinstance(binding, dict):
        literal = binding.get("literalComponent")
        if isinstance(literal, dict) and (literal.get("translate") or _text(literal).strip()):
            terms.append(literal)
        storage_path = binding.get("storagePath")
        if isinstance(storage_path, str):
            resolved = _resolve_nbt_path(localized_templates, storage_path)
            if isinstance(resolved, dict) and (resolved.get("translate") or _text(resolved).strip()):
                terms.append(resolved)
            else:
                source = _resolve_nbt_path(source_en, storage_path)
                if isinstance(source, dict) and (source.get("translate") or _text(source).strip()):
                    terms.append(source)
        for key, value in binding.items():
            if key == "literalComponent":
                continue
            if isinstance(value, (dict, list)):
                terms.extend(_binding_name_terms(value, localized_templates, source_en))
    elif isinstance(binding, list):
        for value in binding:
            terms.extend(_binding_name_terms(value, localized_templates, source_en))
    return terms


def _page_number(value: Any) -> str:
    text = str(value).rsplit(":", 1)[-1].upper()
    aliases = {"ROOT": "0", "ROOT_PAGE": "0", "HUB": "0", "SETTINGS": "1000", "KITS": "3000", "KIT": "3000",
               "SKILLS": "4000", "SKILL": "4000", "SHOP": "6000", "MAP": "MAPS", "MAPS": "MAPS"}
    return aliases.get(text, text)


def check_snapshot(snapshot_path: Path, design_path: Path, source_path: Path,
                   project_root: Path, game_mode: int | None = None) -> tuple[list[str], dict[str, int]]:
    """Check a GameChecks ItemStack.CODEC export against old-source fixtures and imported paths.

    This intentionally consumes serialized stacks only. It does not load Java implementation
    classes, and it does not treat the translator's own output as the source baseline.
    """
    errors: list[str] = []
    snapshot = read_json(snapshot_path)
    design = read_json(design_path)
    source = read_json(source_path)
    localized_templates = design.get("localizedStorageComponents", {})
    pages = snapshot.get("pages", []) if isinstance(snapshot, dict) else []
    by_id = {_page_number(p.get("page", p.get("id", ""))): p for p in pages if isinstance(p, dict)}
    counts = {"snapshotPages": len(pages), "snapshotStacks": 0, "fixtureChecks": 0}
    if not isinstance(pages, list):
        return ["snapshot pages must be an array"], counts

    for key, page in by_id.items():
        page_id = page.get("page", page.get("id", key))
        if page.get("size") != 27:
            errors.append(f"snapshot page {page_id}: virtual EnderItems size must be 27, got {page.get('size')!r}")
        slots: dict[int, dict[str, Any]] = {}
        raw_slots = page.get("slots", [])
        if not isinstance(raw_slots, list):
            errors.append(f"snapshot page {page_id}: slots must be an array")
            continue
        for entry in raw_slots:
            if not isinstance(entry, dict) or not isinstance(entry.get("item"), dict):
                continue
            try:
                slot = int(entry.get("slot"))
            except (TypeError, ValueError):
                errors.append(f"snapshot page {page_id}: invalid slot {entry.get('slot')!r}")
                continue
            if slot in slots:
                errors.append(f"snapshot page {page_id}: duplicate slot {slot}")
            slots[slot] = entry["item"]
            counts["snapshotStacks"] += 1
        page["_slots_by_number"] = slots

    def require(page_key: str, slot: int, *, item_id: str | None = None, model: str | None = None,
                text_has: str | None = None, material: str | None = None) -> dict[str, Any] | None:
        page = by_id.get(page_key)
        if page is None:
            errors.append(f"snapshot missing source fixture page {page_key}")
            return None
        item = page.get("_slots_by_number", {}).get(slot)
        counts["fixtureChecks"] += 1
        if item is None:
            errors.append(f"snapshot {page_key} slot {slot}: expected populated source fixture")
            return None
        if item_id and _component_id(item) != item_id:
            errors.append(f"snapshot {page_key} slot {slot}: item id {_component_id(item)!r}, expected {item_id!r}")
        comps = _components(item)
        if model and comps.get("minecraft:item_model", comps.get("item_model")) != model:
            errors.append(f"snapshot {page_key} slot {slot}: item_model mismatch, expected {model!r}")
        if material and _component_id(item) != material:
            errors.append(f"snapshot {page_key} slot {slot}: material {_component_id(item)!r}, expected {material!r}")
        if text_has:
            text = _text(comps.get("minecraft:custom_name", comps.get("custom_name", "")))
            if text_has.casefold() not in text.casefold():
                errors.append(f"snapshot {page_key} slot {slot}: custom name lacks source text {text_has!r}")
        return item

    # Independent datapack fixtures: virtual EnderItems are a 27-slot Ender Chest page.
    root = by_id.get("0")
    if root:
        counts["fixtureChecks"] += 1
        title = root.get("title", {})
        if isinstance(title, dict):
            title_id = title.get("translate", title.get("key", title.get("value")))
            if title_id not in ("container.enderchest", "minecraft.container.enderchest"):
                errors.append(f"snapshot page {root.get('page')}: title must preserve container.enderchest, got {title!r}")
        elif title not in ("container.enderchest", "minecraft:container.enderchest"):
            errors.append(f"snapshot page {root.get('page')}: title must preserve container.enderchest, got {title!r}")
        for slot, item_id, model in ((0, "minecraft:comparator", None),
                                     (6, "minecraft:gold_ingot", "lbc:shop"),
                                     (13, "minecraft:chest", "lbc:cases")):
            require("0", slot, item_id=item_id, model=model)
        # The datapack's profile head is dynamic, but its source slot and material are fixed.
        require("0", 8, material="minecraft:player_head")
        profile = root.get("_slots_by_number", {}).get(8)
        if profile is not None:
            lore = _components(profile).get("minecraft:lore", _components(profile).get("lore", []))
            if not isinstance(lore, list) or len(lore) != 3:
                errors.append(f"snapshot root slot 8: source profile has exactly 3 stat rows, got {len(lore) if isinstance(lore, list) else 'no lore'}")
            else:
                profile_source = next((table for table in source.get("slotTables", [])
                                       if str(table.get("namespace", "swrg")) == "swrg"
                                       and str(table.get("page")) == "0" and str(table.get("slot")) == "8"), {})
                source_lore_rows: list[tuple[str, set[str]]] = []
                for function in _walk(profile_source.get("parsed", {})):
                    if function.get("function", "").removeprefix("minecraft:") != "set_lore":
                        continue
                    lines = function.get("lore", [])
                    path = next((line.get("nbt") for line in lines if isinstance(line, dict) and line.get("nbt")), None)
                    if not path or any(existing_path == path for existing_path, _ in source_lore_rows):
                        continue
                    objectives = {node["score"].get("objective") for node in _walk(lines)
                                  if isinstance(node.get("score"), dict)}
                    source_lore_rows.append((path, objectives))
                if len(source_lore_rows) != 3:
                    errors.append(f"old source profile table exposes {len(source_lore_rows)} unique lore rows, expected 3")
                for row, (source_path_ref, source_objectives) in enumerate(source_lore_rows[:3]):
                    localized = localized_templates.get(source_path_ref, {}) if isinstance(localized_templates, dict) else {}
                    expected_key = localized.get("translate") if isinstance(localized, dict) else None
                    actual_keys = {node.get("translate") for node in _walk(lore[row])
                                   if isinstance(node.get("translate"), str)}
                    if expected_key and expected_key not in actual_keys:
                        errors.append(f"snapshot root slot 8 lore {row}: source translation {expected_key!r} is missing")
                    if row == 0 and "swrg.stat_games" not in source_objectives:
                        errors.append("old source profile lore 0 no longer references swrg.stat_games")
                    if row == 1 and "swrg.wr" not in source_objectives:
                        errors.append("old source profile lore 1 no longer references swrg.wr")
                    if row == 2 and not {"swrg.kd1..", "swrg.kd..1"}.issubset(source_objectives):
                        errors.append("old source profile lore 2 no longer references the two K/D score objectives")
                if not any(value in _text(lore[2]) for value in (".", ".0")):
                    errors.append("snapshot root slot 8 lore 2: source K/D decimal separator is missing")
        frame_ids = {"minecraft:gray_stained_glass_pane", "minecraft:black_stained_glass_pane",
                     "minecraft:light_gray_stained_glass_pane", "minecraft:white_stained_glass_pane"}
        for slot, item in root.get("_slots_by_number", {}).items():
            if _component_id(item) in frame_ids:
                errors.append(f"snapshot root slot {slot}: source-empty cells must not be filled with a frame")
        # Runtime snapshots contain populated stacks only. Hub slot 9 is intentionally absent
        # when source game mode 1 hides the team-choice button; validate when metadata is present.
        snapshot_config = snapshot.get("config", {})
        snapshot_mode = game_mode if game_mode is not None else snapshot.get(
            "gameMode", snapshot_config.get("gameMode") if isinstance(snapshot_config, dict) else None)
        if snapshot_mode in (1, 2) and 9 in root.get("_slots_by_number", {}):
            errors.append(f"snapshot root slot 9: hub team-choice button must be hidden in gameMode {snapshot_mode}")
        elif snapshot_mode in (3, 4) and 9 not in root.get("_slots_by_number", {}):
            errors.append(f"snapshot root slot 9: hub team-choice button must be present in gameMode {snapshot_mode}")

    # Settings' reset control is a stable old-source fixture.
    if "1000" in by_id:
        require("1000", 24, item_id="minecraft:cooked_porkchop")

    # Compare concrete snapshot stacks against the source manifest's exact page tables.
    # Dynamic alternatives are allowed only when the manifest lists that item for the slot.
    tables = {(str(t.get("namespace", "swrg")), str(t.get("page")), str(t.get("slot"))): t
              for t in source.get("slotTables", [])}
    tables_by_id = {t.get("lootTable"): t for t in source.get("slotTables", []) if t.get("lootTable")}
    tables_by_id.update({t.get("lootTable"): t for t in source.get("nonSlotGuiLootTables", []) if t.get("lootTable")})
    design_pages = {str(p.get("pageId", p.get("page", ""))): p for p in design.get("pages", [])}
    source_language_values = _source_language_values(source)
    # page/1000-lang replaces these intermediate roots with named settings components.
    dynamic_setting_names: dict[int, list[str]] = defaultdict(list)
    for function in source.get("guiInteractionFunctions", []):
        source_path = normalized(function.get("source", ""))
        if not re.search(r"/gui/page/1000-lang\.mcfunction$", source_path):
            continue
        for line in function.get("exactRaw", "").splitlines():
            match = re.search(
                r"data modify storage swrg:lang page1000\.s(\d+) set from storage swrg:lang ([a-z0-9_.-]+)",
                line.strip(), re.IGNORECASE)
            if match:
                slot, source_component = match.groups()
                if source_component not in dynamic_setting_names[int(slot)]:
                    dynamic_setting_names[int(slot)].append(source_component)
    # These are the production defaults/currently supported renderings. Lives and borders
    # remain config-backed variants; hand mode and lucky modifier retain their source defaults.
    expected_setting_names = {
        8: ["lefthand_on"],
        9: [f"lives_{count}" for count in range(1, 6)],
        10: ["no_borders_off", "no_borders_on"],
        11: ["cfg_lucky_modifier_0"],
    }
    # The importer records only source-proven gaps. A path missing from the old
    # language storage is not accepted as an implicit empty component.
    declared_gaps: dict[str, dict[str, Any]] = {}
    for gap in design.get("sourceComponentGaps", []):
        if isinstance(gap, dict) and isinstance(gap.get("storagePath"), str):
            declared_gaps[gap["storagePath"]] = gap

    def gap_for_path(raw_path: str) -> tuple[str, dict[str, Any]] | None:
        path = raw_path.split("[", 1)[0]
        matches = [(root, data) for root, data in declared_gaps.items()
                   if path == root or path.startswith(root + ".")]
        return max(matches, key=lambda entry: len(entry[0])) if matches else None

    for gap_root, _gap_data in declared_gaps.items():
        if any(_resolve_nbt_path(source_language_values[locale], gap_root) is not None
               for locale in ("en", "ru")):
            errors.append(f"declared source component gap {gap_root!r} exists in independent EN/RU language storage")
        gap_match = re.fullmatch(r"page(\d+)\.s(\d+)", gap_root)
        if not gap_match:
            errors.append(f"declared source component gap {gap_root!r} is not a supported page-slot root")
            continue
        page_key, gap_slot = gap_match.group(1), int(gap_match.group(2))
        gap_page = by_id.get(page_key)
        gap_item = gap_page.get("_slots_by_number", {}).get(gap_slot) if gap_page else None
        if gap_item is None:
            errors.append(f"declared source component gap {gap_root!r} has no runtime stack to validate")
            continue
        gap_components = _components(gap_item)
        gap_name = gap_components.get("minecraft:custom_name", gap_components.get("custom_name"))
        if page_key == "1000" and gap_slot in (8, 9, 10, 11):
            # Slots 8–11 follow the original lefthand/lives/borders/lucky-modifier choices.
            expected_variants = expected_setting_names[gap_slot]
            variants = [name for name in expected_variants
                        if name in dynamic_setting_names.get(gap_slot, [])]
            matched_variant = False
            for variant in variants:
                if not any(_resolve_nbt_path(source_language_values[locale], variant) is not None
                           for locale in ("en", "ru")):
                    continue
                component_record = localized_templates.get(variant, {})
                expected_name = component_record.get("Name") if isinstance(component_record, dict) else None
                if expected_name is None:
                    continue
                if (isinstance(expected_name, dict) and expected_name.get("translate")
                        and (not isinstance(gap_name, dict)
                             or gap_name.get("translate") != expected_name["translate"])):
                    continue
                candidate_errors: list[str] = []
                _check_component_shape(expected_name, gap_name,
                                       f"snapshot 1000 slot {gap_slot} source variant {variant}", candidate_errors)
                if not candidate_errors:
                    matched_variant = True
                    break
            missing_variants = [name for name in expected_variants if name not in dynamic_setting_names.get(gap_slot, [])]
            if missing_variants:
                errors.append(f"source gui/page/1000-lang slot {gap_slot} is missing expected component assignments {missing_variants}")
            if not variants:
                errors.append(f"snapshot checker has no supported source name variants for slot {gap_slot}")
            elif not matched_variant:
                errors.append(f"snapshot 1000 slot {gap_slot}: name does not match any source gui/page/1000-lang variant {variants}")
        elif gap_name is None or _text(gap_name).strip() or (isinstance(gap_name, dict) and gap_name.get("translate")):
            errors.append(f"snapshot {page_key} slot {gap_slot}: declared source name gap {gap_root!r} must render empty")
    en_translations: dict[str, str] = {}
    ru_translations: dict[str, str] = {}
    resource_pack_root = Path(source.get("sourceRoots", {}).get("resourcePack", ""))
    translation_roots = [resource_pack_root / "assets/minecraft/lang",
                         project_root / "src/main/resources/assets/skywars2/lang"]
    for root_path in translation_roots:
        for locale, table in (("en_us", en_translations), ("ru_ru", ru_translations)):
            language_file = root_path / f"{locale}.json"
            if language_file.is_file():
                table.update(read_json(language_file))
    map_source_slots: dict[str, dict[int, list[dict[str, Any]]]] = {}
    for source_map_id in ("2000", "2000b", "2100", "2200"):
        source_page = design_pages.get(source_map_id, {})
        for slot_entry in source_page.get("slots", []):
            try:
                number = int(slot_number(slot_entry))
            except (TypeError, ValueError):
                continue
            variants = slot_entry.get("sources", [slot_entry])
            map_source_slots.setdefault(source_map_id, {}).setdefault(number, []).extend(
                {**variant, "sourceTranslationKey": slot_entry.get("translationKey")}
                for variant in variants if isinstance(variant, dict) and variant.get("sourceItem"))
    lbc_snapshot_sources = {"LBC_TRINKETS": "11000", "LBC_LEVELUPS": "8000",
                            "LBC_KITS": "9000", "LBC_SKILLS": "10000"}
    for page_key, page in by_id.items():
        source_page_key = lbc_snapshot_sources.get(page_key, page_key)
        source_page_meta = design_pages.get(source_page_key, {})
        raw_page_name = str(page.get("page", "")).lower()
        source_namespace = str(page.get("namespace", source_page_meta.get("namespace", ""))).lower()
        if page_key in lbc_snapshot_sources:
            source_namespace = "lbc"
        if source_namespace not in ("lbc", "swrg"):
            source_namespace = "lbc" if raw_page_name.startswith("lbc:") or raw_page_name.startswith("lbc_") else "swrg"
        slots = page.get("_slots_by_number", {})
        for slot, item in slots.items():
            imported_page = design_pages.get(source_page_key, {})
            imported_slot = next((record for record in imported_page.get("slots", [])
                                  if slot_number(record) == str(slot)), None)
            if page_key in lbc_snapshot_sources and imported_slot is None:
                # LBC page functions map source loot tables to destination EnderChest
                # slots explicitly; source loot-table slot numbers are not destination
                # slot numbers (for example page 9000 source slot 18 lands in slot 19).
                continue
            table = None
            if imported_slot:
                source_variants = imported_slot.get("sources", [imported_slot])
                candidate_tables = [tables_by_id.get(table_id(variant)) for variant in source_variants
                                    if isinstance(variant, dict) and table_id(variant)]
                table = next((candidate for candidate in candidate_tables if candidate and
                              (not candidate.get("itemIds") or _component_id(item) in candidate.get("itemIds", []))), None)
            if table is None:
                table = tables.get((source_namespace, source_page_key, str(slot)))
            if table is None:
                continue
            counts["fixtureChecks"] += 1
            source_ids = set(table.get("itemIds", []))
            if source_ids and _component_id(item) not in source_ids:
                errors.append(f"snapshot {page_key} slot {slot}: item {_component_id(item)!r} not in source alternatives {sorted(source_ids)}")
            source_models = set(table.get("resourceDependencies", {}).get("itemModels", []))
            if source_models:
                actual_model = _components(item).get("minecraft:item_model", _components(item).get("item_model"))
                if actual_model not in source_models:
                    errors.append(f"snapshot {page_key} slot {slot}: model {actual_model!r} not in source alternatives {sorted(source_models)}")
            # Literal text components in old loot JSON are exact source evidence for
            # translation keys and style flags. Storage-backed components are verified by
            # their independently checked sourceComponentBindings below.
            functions = [node for node in _walk(table.get("parsed", {}))
                         if isinstance(node.get("function"), str)]
            components = _components(item)
            runtime_name = components.get("minecraft:custom_name", components.get("custom_name"))
            runtime_lore = components.get("minecraft:lore", components.get("lore", []))
            # LBC selection now uses the same source indicator as vanilla perks.
            # Validate it explicitly, then compare every original lore row unchanged.
            if (page_key in lbc_snapshot_sources and isinstance(runtime_lore, list) and runtime_lore
                    and isinstance(runtime_lore[0], dict) and runtime_lore[0].get("translate") == "swrg.gui.select"):
                expected_indicator = {"translate": "swrg.gui.select", "color": "green", "italic": False}
                if runtime_lore[0] != expected_indicator or components.get("minecraft:enchantment_glint_override") is not True:
                    errors.append(f"snapshot {page_key} slot {slot}: incorrect selected indicator or glint")
                runtime_lore = runtime_lore[1:]
            lore_offset = 0
            for function in functions:
                kind = function.get("function", "").removeprefix("minecraft:")
                if kind == "set_name" and isinstance(function.get("name"), (dict, list, str)):
                    expected_name = function["name"]
                    if isinstance(expected_name, dict) and expected_name.get("nbt"):
                        expected_name = _resolve_nbt_path(source_language_values["en"], str(expected_name["nbt"]))
                    if expected_name is not None:
                        _check_component_shape(expected_name, runtime_name, f"snapshot {page_key} slot {slot} name", errors)
                elif kind == "set_lore" and isinstance(function.get("lore"), list):
                    if function.get("conditions"):
                        # Mutually exclusive score ranges populate the one K/D stat row.
                        continue
                    expected_lines = []
                    for line in function["lore"]:
                        if isinstance(line, dict) and line.get("nbt"):
                            resolved = _resolve_nbt_path(source_language_values["en"], str(line["nbt"]))
                            if resolved is not None:
                                expected_lines.append(resolved)
                        else:
                            expected_lines.append(line)
                    if expected_lines:
                        if function.get("mode", "replace_all") == "replace_all":
                            lore_offset = 0
                        actual_lines = runtime_lore[lore_offset:lore_offset + len(expected_lines)] if isinstance(runtime_lore, list) else runtime_lore
                        _check_component_shape(expected_lines, actual_lines, f"snapshot {page_key} slot {slot} lore", errors)
                        lore_offset += len(expected_lines)
            if imported_slot:
                matching_name_bindings = []
                for variant in imported_slot.get("sources", [imported_slot]):
                    source_id = table_id(variant)
                    source_table = tables_by_id.get(source_id, {})
                    source_ids_for_variant = set(source_table.get("itemIds", []))
                    variant_item = variant.get("sourceItem")
                    if (_component_id(item) in source_ids_for_variant or variant_item == _component_id(item)):
                        binding = variant.get("sourceComponentBindings", {}).get("name")
                        terms = _binding_name_terms(binding, localized_templates, source_language_values["en"])
                        if terms:
                            matching_name_bindings.append((source_id or variant_item, terms))
                if matching_name_bindings and runtime_name is not None:
                    actual_nodes = list(_walk(runtime_name)) if isinstance(runtime_name, (dict, list)) else []
                    actual_literals: list[str] = []
                    def collect_literals(value: Any) -> None:
                        if isinstance(value, str):
                            actual_literals.append(value)
                        elif isinstance(value, dict):
                            for child in value.values():
                                collect_literals(child)
                        elif isinstance(value, list):
                            for child in value:
                                collect_literals(child)
                    collect_literals(runtime_name)
                    passed = False
                    for source_ref, terms in matching_name_bindings:
                        candidate_errors: list[str] = []
                        for term in terms:
                            expected_key = term.get("translate")
                            expected_text = term.get("text")
                            node = next((candidate for candidate in actual_nodes
                                         if expected_key and candidate.get("translate") == expected_key), None)
                            if node is None and expected_text:
                                node = next((candidate for candidate in actual_nodes
                                             if candidate.get("text") == expected_text
                                             or candidate.get("fallback") == expected_text), None)
                                if node is None:
                                    node = next((candidate for candidate in actual_literals
                                                 if candidate == expected_text), None)
                            if node is None:
                                candidate_errors.append(f"source name term key/text {expected_key or expected_text!r} missing")
                            else:
                                _check_component_shape(term, node, "snapshot source name", candidate_errors)
                            if expected_key and expected_key not in en_translations and expected_key not in ru_translations:
                                candidate_errors.append(f"translation key {expected_key!r} missing from source resource-pack tables")
                        if not candidate_errors:
                            passed = True
                            break
                    if not passed:
                        errors.append(f"snapshot {page_key} slot {slot}: default item name does not match any independent source table/component binding")
            expected_counts = set()
            for node in _walk(table.get("parsed", {})):
                if node.get("function") in ("minecraft:set_count", "set_count"):
                    count_value = node.get("count")
                    if isinstance(count_value, int):
                        expected_counts.add(count_value)
            if not expected_counts:
                expected_counts.add(1)
            actual_count = item.get("count", 1)
            if actual_count not in expected_counts:
                errors.append(f"snapshot {page_key} slot {slot}: count={actual_count!r}, source count alternatives={sorted(expected_counts)}")
            if page_key in ("3000", "4000") and slot == 0:
                expected_lore = 3 if page_key == "3000" else 1
                lore = _components(item).get("minecraft:lore", _components(item).get("lore", []))
                selected = isinstance(lore, list) and any("selected" in _text(line).casefold() for line in lore)
                if not isinstance(lore, list) or len(lore) < expected_lore or (not selected and len(lore) != expected_lore):
                    errors.append(f"snapshot {page_key} slot 0: expected {expected_lore} source lore lines (plus selected overlay only), got {len(lore) if isinstance(lore, list) else 'no lore'}")

        if page_key == "MAPS" or page_key in map_source_slots:
            candidate_sources = map_source_slots.values() if page_key == "MAPS" else (map_source_slots.get(page_key, {}),)
            for slot, item in slots.items():
                alternatives = [variant for mapping in candidate_sources for variant in mapping.get(slot, [])]
                if not alternatives:
                    continue
                allowed_items = {variant.get("sourceItem") for variant in alternatives}
                if _component_id(item) not in allowed_items:
                    errors.append(f"snapshot maps slot {slot}: material {_component_id(item)!r} not in source map-slot materials {sorted(allowed_items)}")
                components = _components(item)
                name = components.get("minecraft:custom_name", components.get("custom_name"))
                if not isinstance(name, (dict, list, str)) or not (_text(name).strip() or (isinstance(name, dict) and name.get("translate"))):
                    errors.append(f"snapshot maps slot {slot}: missing semantic source map name")
                expected_names = set()
                for variant in alternatives:
                    raw_component = variant.get("sourceComponentText", "")
                    match = re.search(r'"text"\s*:\s*"((?:\\.|[^"\\])*)"', raw_component)
                    if match:
                        try:
                            expected_names.add(json.loads('"' + match.group(1) + '"'))
                        except json.JSONDecodeError:
                            pass
                rendered_name = _text(name)
                translated_name = ""
                if isinstance(name, dict) and isinstance(name.get("translate"), str):
                    translated_name = en_translations.get(name["translate"], ru_translations.get(name["translate"], ""))
                    expected_keys = {variant.get("sourceTranslationKey") for variant in alternatives
                                     if variant.get("sourceTranslationKey")}
                    if expected_keys and name["translate"] not in expected_keys:
                        errors.append(f"snapshot maps slot {slot}: translation key {name['translate']!r} differs from source map key(s) {sorted(expected_keys)}")
                if expected_names and rendered_name and not any(expected in rendered_name for expected in expected_names):
                    fallback = name.get("fallback", "") if isinstance(name, dict) else ""
                    if not any(expected in fallback for expected in expected_names) and not any(expected in translated_name for expected in expected_names):
                        errors.append(f"snapshot maps slot {slot}: semantic name {rendered_name or translated_name!r} differs from source labels {sorted(expected_names)}")

    # Empty lobby cells remain empty; source-backed menu pages use no pane frame.
    for page_key in ("0", "3000", "4000", "5000", "6000"):
        if page_key in by_id:
            for slot, item in by_id[page_key].get("_slots_by_number", {}).items():
                if _component_id(item).endswith("_stained_glass_pane"):
                    errors.append(f"snapshot page {page_key} slot {slot}: source page has no decorative frame")

    # Source component bindings must point to actual EN/RU source language records.
    source_language_paths: set[str] = set()
    for locale in source.get("languageStorageSources", []):
        for line in locale.get("exactRaw", "").splitlines():
            match = re.search(r"data modify storage swrg:lang ([^ ]+) set value ", line)
            if match:
                source_language_paths.add(match.group(1))

    imported_templates = design.get("sourceStorageComponents", {})
    if isinstance(imported_templates, dict):
        for locale, records in imported_templates.items():
            if locale not in source_language_values or not isinstance(records, dict):
                continue
            for raw_path, imported_component in records.items():
                expected_component = _resolve_nbt_path(source_language_values[locale], str(raw_path))
                if expected_component is not None and imported_component != expected_component:
                    errors.append(f"sourceStorageComponents[{locale}][{raw_path!r}] differs from exact independent language source")
                elif expected_component is None:
                    exists_other_locale = any(_resolve_nbt_path(source_language_values[other], str(raw_path)) is not None
                                              for other in source_language_values if other != locale)
                    declared_gap = gap_for_path(str(raw_path))
                    if not exists_other_locale and declared_gap is None:
                        errors.append(f"sourceStorageComponents[{locale}][{raw_path!r}] is absent from both independent EN/RU sources and is not a declared gap")

    if isinstance(localized_templates, dict):
        for raw_path, localized in localized_templates.items():
            source_en = _resolve_nbt_path(source_language_values["en"], str(raw_path))
            source_ru = _resolve_nbt_path(source_language_values["ru"], str(raw_path))
            if source_en is not None or source_ru is not None:
                errors.extend(_paired_translation_errors(source_en, source_ru, localized,
                                                         en_translations, ru_translations,
                                                         f"localizedStorageComponents[{raw_path!r}]"))
            elif raw_path not in declared_gaps:
                errors.append(f"localizedStorageComponents[{raw_path!r}] has no independent EN/RU source and is not declared as a source gap")
            elif not isinstance(localized, dict) or localized.get("Name") != {"text": ""}:
                errors.append(f"localizedStorageComponents[{raw_path!r}] declared as a source gap must preserve an explicit empty Name")

    # The imported source binding paths are retained so later runtime exports can be audited
    # against exact languageStorageSources data without trusting the newly generated key text.
    source_path_refs = set()
    def bound_paths(value: Any):
        if isinstance(value, dict):
            if isinstance(value.get("storagePath"), str):
                yield value["storagePath"]
            else:
                for child in value.values():
                    yield from bound_paths(child)
        elif isinstance(value, list):
            for child in value:
                yield from bound_paths(child)
        elif isinstance(value, str) and re.match(r"^(?:page\d|gui\.|stat\.|kit\.|skill\.|lore\.|item\.|entity\.|title\.)", value):
            yield value

    for table in design.get("pages", []):
        for slot in table.get("slots", []):
            variants = slot.get("sources", [slot])
            for variant in variants:
                bindings = variant.get("sourceComponentBindings", {})
                if isinstance(bindings, dict):
                    for value in bound_paths(bindings):
                        source_path_refs.add(value)
                        # Lore bindings may point at indexed leaves under a source storage compound.
                        candidates = [value]
                        path = value.split("[", 1)[0]
                        candidates.append(path)
                        candidates.extend(path.rsplit(".", n)[0] for n in range(1, path.count(".") + 1))
                        available = any(_source_path_has_assignment(source_language_values[locale], candidate)
                                        for candidate in candidates for locale in ("en", "ru"))
                        declared_gap = gap_for_path(value)
                        if not available and declared_gap is None:
                            errors.append(f"imported component binding {value!r} is absent from independent EN/RU language source")
                        elif not available and declared_gap is not None:
                            gap_root, _ = declared_gap
                            if any(_resolve_nbt_path(source_language_values[locale], gap_root) is not None
                                   for locale in ("en", "ru")):
                                errors.append(f"imported component binding {value!r} declares gap {gap_root!r}, but source data exists")
    counts["sourceComponentBindings"] = len(source_path_refs)
    return errors, counts


def check_native_lbc_sources(project: Path) -> tuple[int, int, int]:
    """Compare supported kit/trinket payloads and notifications with read-only sources."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    resources = project / "src/main/resources"
    weapon_source = json.loads((SOURCE / "loot_table/transformables_sword.json").read_text(encoding="utf-8-sig"))
    migrate_loot_schema(weapon_source)
    weapon_entries = [
        (pool, entry)
        for pool in weapon_source.get("pools", [])
        for entry in pool.get("entries", [])
        if any(function.get("type") == "minecraft:set_custom_data"
               and re.search(r"(?:^|[,{}])icesword:1(?:[,{}]|$)", function.get("tag", ""))
               for function in (entry.get("modifier", []) if isinstance(entry.get("modifier", []), list)
                               else [entry.get("modifier", {})]))
    ]
    assert len(weapon_entries) == 1, f"Expected one Ice Sword source entry, found {len(weapon_entries)}"
    source_pool, expected_weapon = weapon_entries[0]
    assert expected_weapon["name"] == "minecraft:iron_sword", "Unexpected Ice Sword source item"
    expected_weapon["name"] = "skywars2:ice_sword"
    actual_weapon = json.loads((resources / "data/lbc/loot_table/ported/ice_sword.json").read_text(encoding="utf-8"))
    expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
    expected_pool["entries"] = [expected_weapon]
    assert actual_weapon == {"pools": [expected_pool]}, (
        "Ported Ice Sword payload differs from the independently selected source entry"
    )
    catalog = json.loads((resources / "skywars2/lbc_loadouts.json").read_text(encoding="utf-8"))
    rocket_source = (SOURCE / "function/items/trinket_effects/charge_firework.mcfunction").read_text(encoding="utf-8-sig")
    assert (resources / "skywars2/ghast_rocket.snbt").read_text(encoding="utf-8").strip() == rocket_source.splitlines()[0].partition(" ~ ~ ~ ")[2], "Ghast source entity preset differs"
    for kit in catalog["kits"]:
        relative = "loot_table/" + kit["table"].removeprefix("lbc:") + ".json"
        expected = json.loads((SOURCE / relative).read_text(encoding="utf-8-sig"))
        migrate_loot_schema(expected)
        actual = json.loads((resources / "data/lbc" / relative).read_text(encoding="utf-8"))
        if kit['id'] == 22:
            # Only the server item type changes. Every visual/component modifier must still match the source.
            assert expected['pools'][0]['entries'][0]['name'] == 'minecraft:chainmail_boots'
            expected['pools'][0]['entries'][0]['name'] = 'skywars2:rocket_boots'
        if kit['id'] == 17:
            for pool, item in zip(expected['pools'], ['skywars2:termit', 'skywars2:sulfur', 'skywars2:phosphin', 'skywars2:gremuch'], strict=True):
                assert pool['entries'][0]['name'] == 'minecraft:lingering_potion'
                pool['entries'][0]['name'] = item
        assert actual == expected, f"LBC kit source payload differs: {kit['table']}"

    for trinket in catalog["trinkets"]:
        table = trinket["table"].removeprefix("lbc:")
        relative = f"loot_table/{table}.json"
        source_table = {
            "ported/blood_anal_boom": "trinket/blood_anal_boom",
            "ported/mana_gem": "trinket/mana_gem",
        }.get(table, table)
        expected = json.loads((SOURCE / f"loot_table/{source_table}.json").read_text(encoding="utf-8-sig"))
        migrate_loot_schema(expected)
        entry = expected["pools"][0]["entries"][0]
        expected_item = {
            "blaze": ("minecraft:repeating_command_block", "skywars2:blaze_trinket"),
            "ghast": ("minecraft:repeating_command_block", "skywars2:ghast_trinket"),
            "intercal": ("minecraft:repeating_command_block", "skywars2:intercal"),
            "gneiss_stone": ("minecraft:stone", "skywars2:gneiss_stone"),
            "snow_gem": ("minecraft:repeating_command_block", "skywars2:snow_gem"),
            "ice_gem": ("minecraft:repeating_command_block", "skywars2:ice_gem"),
            "blood_anal_boom": ("minecraft:repeating_command_block", "skywars2:blood_anal_boom"),
            "mana_gem": ("minecraft:repeating_command_block", "skywars2:mana_gem"),
        }[source_table.removeprefix("trinket/")]
        assert entry["name"] == expected_item[0], f"Unexpected LBC trinket source item: {table}"
        entry["name"] = expected_item[1]
        actual = json.loads((resources / "data/lbc" / relative).read_text(encoding="utf-8"))
        assert actual == expected, f"LBC trinket source payload differs: {table}"

    # Standalone native item payloads retain their source data after the
    # schema migration; only the server-side item id changes.
    magic_source = SOURCE / "loot_table"
    for source_name, destination_name, expected_item, selector in [
        ("artifact.json", "mystical_artifact.json", "skywars2:mystical_artifact", 1),
        ("artifact.json", "mystical_artifact_colors.json", "skywars2:mystical_artifact", 7),
        ("artifact.json", "mystical_artifact_clear.json", "skywars2:mystical_artifact", 6),
        ("artifact.json", "mystical_artifact_metabolism.json", "skywars2:mystical_artifact", 8),
        ("artifact.json", "mystical_artifact_debuff.json", "skywars2:mystical_artifact", 5),
        ("artifact.json", "mystical_artifact_arrows.json", "skywars2:mystical_artifact", 4),
        ("artifact.json", "mystical_artifact_liquids.json", "skywars2:mystical_artifact", 3),
        ("artifact.json", "mystical_artifact_explosions.json", "skywars2:mystical_artifact", 2),
        ("stopper_magic.json", "magic_beacon.json", "skywars2:magic_beacon", "beacon"),
    ]:
        source_table = json.loads((magic_source / source_name).read_text(encoding="utf-8-sig"))
        migrate_loot_schema(source_table)
        matches = []
        for source_pool in source_table.get("pools", []):
            for source_entry in source_pool.get("entries", []):
                if selector == "beacon":
                    selected = source_entry.get("name") == "minecraft:panda_spawn_egg"
                else:
                    selected = source_entry.get("name") == "minecraft:repeating_command_block" and any(
                        function.get("type") == "minecraft:set_custom_data"
                        and re.search(r"(?:^|[,{}])unresolved_artifact:1(?:[,{}]|$)", function.get("tag", ""))
                        and re.search(r"(?:^|[,{}])mystical:1(?:[,{}]|$)", function.get("tag", ""))
                        and re.search(rf"(?:^|[,{{}}])type:{selector}(?:[,{{}}]|$)", function.get("tag", ""))
                        for function in (source_entry.get("modifier", [])
                                         if isinstance(source_entry.get("modifier", []), list)
                                         else [source_entry.get("modifier", {})])
                    )
                if selected:
                    matches.append((source_pool, source_entry))
        assert len(matches) == 1, f"Expected one selected magic item in {source_name}, found {len(matches)}"
        source_pool, source_entry = matches[0]
        source_entry["name"] = expected_item
        expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
        expected_pool["entries"] = [source_entry]
        expected_table = {key: value for key, value in source_table.items() if key != "pools"}
        expected_table["pools"] = [expected_pool]
        actual_table = json.loads((resources / f"data/lbc/loot_table/ported/{destination_name}").read_text(encoding="utf-8"))
        assert actual_table == expected_table, f"Ported magic payload differs from selected source entry: {source_name}"

    for relative in ("minecraft/tags/block/magic.json", "minecraft/tags/entity_type/magic_entities.json",
                     "minecraft/tags/entity_type/unstable.json"):
        expected_tag = json.loads((SOURCE.parent / relative).read_text(encoding="utf-8-sig"))
        actual_tag = json.loads((resources / "data" / relative).read_text(encoding="utf-8"))
        assert actual_tag == expected_tag, f"Native LBC tag differs from source: {relative}"

    color_tag_names = (
        "colored", "in_colored", "black", "blue", "brown", "cyan", "gray", "green",
        "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red",
        "white", "yellow",
    )
    for tag_name in color_tag_names:
        relative = f"minecraft/tags/block/{tag_name}.json"
        expected_tag_bytes = (SOURCE.parent / relative).read_bytes()
        actual_tag_bytes = (resources / "data" / relative).read_bytes()
        assert actual_tag_bytes == expected_tag_bytes, f"Native LBC color tag bytes differ from source: {relative}"
    color_dispatch = json.loads((resources / "data/minecraft/tags/block/colored.json").read_text(encoding="utf-8"))
    in_color_dispatch = json.loads((resources / "data/minecraft/tags/block/in_colored.json").read_text(encoding="utf-8"))
    assert len(color_dispatch.get("values", [])) == 128, "colored dispatch tag must retain 128 source values"
    assert len(in_color_dispatch.get("values", [])) == 64, "in_colored dispatch tag must retain 64 source values"
    for color in ("black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
                  "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow"):
        tag = json.loads((resources / f"data/minecraft/tags/block/{color}.json").read_text(encoding="utf-8"))
        assert len(tag.get("values", [])) == 13, f"Native LBC color tag must retain 13 source values: {color}"

    modifier_relative = "item_modifier/add_lore_rocket.json"
    expected_modifier = json.loads((SOURCE / modifier_relative).read_text(encoding="utf-8-sig"))
    migrate_loot_schema(expected_modifier)
    if isinstance(expected_modifier, list):
        expected_modifier = {"type": "minecraft:sequence", "functions": expected_modifier}
    actual_modifier = json.loads((resources / "data/lbc" / modifier_relative).read_text(encoding="utf-8"))
    assert actual_modifier == expected_modifier, "LBC rocket item modifier differs from migrated source"

    expected_notifications = {}
    gain_root = SOURCE / "function/swrg_kit_integration/gain"
    prefix = "lbc:swrg_kit_integration/gain/"
    for file in (SOURCE / "advancement/true_advancements").rglob("*.json"):
        data = json.loads(file.read_text(encoding="utf-8-sig"))
        function = data.get("rewards", {}).get("function", "")
        if not function.startswith(prefix):
            continue
        text = (gain_root / (function.removeprefix(prefix) + ".mcfunction")).read_text(encoding="utf-8-sig")
        notification = {}
        if function.endswith("/skill_acidman"):
            notification["requires_all"] = re.findall(r"(lbc:true_advancements/[^=,}]+)=true", text)
            assert "run function lbc:swrg_kit_integration/gain/skill" in text
            text = (gain_root / "skill.mcfunction").read_text(encoding="utf-8-sig")
        title = re.search(r'^title @s title (.+)$', text, re.MULTILINE)
        subtitle = re.search(r'^title @s subtitle (.+)$', text, re.MULTILINE)
        assert title and json.loads(title[1]) == " ", f"Unexpected source title: {function}"
        assert subtitle, f"Missing source subtitle: {function}"
        component = json.loads(subtitle[1])
        assert set(component) == {"translate"}, f"Unimported subtitle formatting: {function}"
        notification["subtitle"] = component["translate"]
        ident = "lbc:true_advancements/" + file.relative_to(SOURCE / "advancement/true_advancements").with_suffix("").as_posix()
        expected_notifications[ident] = notification
    actual = json.loads((resources / "skywars2/lbc_unlock_notifications.json").read_text(encoding="utf-8"))["notifications"]
    assert actual == expected_notifications, "Native LBC unlock notifications differ from source rewards"
    return len(catalog["kits"]), len(catalog["trinkets"]), len(actual)


def check_mana_potion_payloads(project: Path) -> int:
    """Compare mana potion payloads to both OLD LBC source loot tables."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    potion_source = json.loads((SOURCE / "loot_table/imba/potions.json").read_text(encoding="utf-8-sig"))
    migrate_loot_schema(potion_source)
    entries = [entry for pool in potion_source.get("pools", []) for entry in pool.get("entries", [])]
    regular_source = json.loads(
        (SOURCE / "loot_table/transformables_potion.json").read_text(encoding="utf-8-sig"))
    migrate_loot_schema(regular_source)
    regular_entries = [entry for pool in regular_source.get("pools", []) for entry in pool.get("entries", [])]
    families = {"add_manamax": 17, "add_manaregen": 18, "add_mana": 19,
                "remove_mana": 20, "temporary_mana": 16}
    regular_name_keys = {"add_manamax": "max_mana_potion", "add_manaregen": "regen_mana_potion",
                         "add_mana": "add_mana_potion", "remove_mana": "remove_mana_potion",
                         "temporary_mana": "mana_potion"}
    deliveries = {
        "drink": ("minecraft:potion", "skywars2:mana_potion"),
        "splash_potion": ("minecraft:splash_potion", "skywars2:mana_splash_potion"),
        "lingering_potion": ("minecraft:lingering_potion", "skywars2:mana_lingering_potion"),
    }

    def convert_score_value_check(value: Any) -> None:
        """Independent OLD-to-26.3 conversion for source score value checks."""
        if isinstance(value, list):
            for child in value:
                convert_score_value_check(child)
            return
        if not isinstance(value, dict):
            return
        for child in list(value.values()):
            convert_score_value_check(child)
        if value.get("type") != "minecraft:value_check":
            return
        provider = value.get("value")
        assert isinstance(provider, dict) and provider.get("type") == "minecraft:score", (
            f"Unexpected legacy score provider: {provider!r}")
        assert provider.pop("scale", 1) == 1, "Non-unit score scale is not source-equivalent"
        exact_point = value.pop("range", None)
        assert isinstance(exact_point, int), f"Expected exact int range point, found {exact_point!r}"
        value["type"] = "minecraft:int_value_check"
        value["test"] = exact_point

    count = 0
    for family, amplifier in families.items():
        for delivery, (source_item, polymer_item) in deliveries.items():
            expected_entries = []
            for entry in entries:
                if entry.get("name") != source_item:
                    continue
                functions = entry.get("modifier", [])
                functions = functions if isinstance(functions, list) else [functions]
                effect_components = next((function.get("components", {}) for function in functions
                                          if function.get("type") == "minecraft:set_components"), {})
                effects = effect_components.get("minecraft:potion_contents", {}).get("custom_effects", [])
                named_family = any(function.get("type") == "minecraft:set_name"
                                   and family in json.dumps(function, ensure_ascii=False)
                                   for function in functions)
                if (len(effects) == 1 and effects[0].get("id") == "minecraft:raid_omen"
                        and effects[0].get("amplifier") == amplifier and named_family):
                    copied = json.loads(json.dumps(entry))
                    copied["name"] = polymer_item
                    expected_entries.append(copied)
            durations = sorted(
                next(function["components"]["minecraft:potion_contents"]["custom_effects"][0]["duration"]
                     for function in entry.get("modifier", [])
                     if function.get("type") == "minecraft:set_components")
                for entry in expected_entries
            )
            assert durations == [3600, 9600], f"OLD LBC source variants changed for {family}/{delivery}: {durations}"
            imba_entries = list(expected_entries)
            regular_expected = []
            for entry in regular_entries:
                if entry.get("name") != source_item:
                    continue
                functions = entry.get("modifier", [])
                functions = functions if isinstance(functions, list) else [functions]
                effect_components = next((function.get("components", {}) for function in functions
                                          if function.get("type") == "minecraft:set_components"), {})
                effects = effect_components.get("minecraft:potion_contents", {}).get("custom_effects", [])
                named_family = any(function.get("type") == "minecraft:set_name"
                                   and regular_name_keys[family] in json.dumps(function, ensure_ascii=False)
                                   for function in functions)
                if (len(effects) == 1 and effects[0].get("id") == "minecraft:raid_omen"
                        and effects[0].get("amplifier") == amplifier and named_family):
                    copied = json.loads(json.dumps(entry))
                    copied["name"] = polymer_item
                    convert_score_value_check(copied)
                    regular_expected.append(copied)
            expected_regular_count = 1 if family == "remove_mana" and delivery == "lingering_potion" else (
                2 if family != "remove_mana" and delivery == "drink" else 0)
            assert len(regular_expected) == expected_regular_count, (
                f"Unexpected OLD LBC regular variants for {family}/{delivery}: {len(regular_expected)}")
            expected_entries.extend(regular_expected)
            for entry in regular_expected:
                condition = entry.get("condition")
                if condition is None:
                    continue
                assert condition == {
                    "type": "minecraft:int_value_check",
                    "value": {
                        "type": "minecraft:score",
                        "target": {"type": "minecraft:fixed", "name": "final_luck_modifier"},
                        "score": "lbc.math",
                    },
                    "test": 10,
                }, f"Unexpected 26.3 regular mana condition codec: {condition!r}"
                # Native IntValueCheck + IntRangePredicate.Point preserves exact equality.
                # This audit compares source/schema; native parsing is covered by server boot.
            target = project / "src/main/resources/data/lbc/loot_table/ported" / f"mana_{family}_{delivery}.json"
            actual = json.loads(target.read_text(encoding="utf-8"))
            assert actual == {"pools": [{"rolls": 1, "entries": expected_entries}]}, (
                f"Imported mana payload differs from OLD LBC source: {target.name}")
            count += len(imba_entries) + len(regular_expected)

    return count


def check_overload_payloads(project: Path) -> int:
    """Independently compare the five imported Overload payloads to OLD LBC tables."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    resources = project / "src/main/resources/data/lbc/loot_table/ported"
    selections = [
        ("overload_crux.json", "overload_crux.json", "minecraft:carrot_on_a_stick",
         "overload_crux", "skywars2:overload_crux"),
        ("neko_set_overload/helmet.json", "overload_helmet.json", "minecraft:leather_helmet",
         "helmet", "skywars2:overload_helmet"),
        ("neko_set_overload/chestplate.json", "overload_chestplate.json", "minecraft:leather_chestplate",
         "chestplate", "skywars2:overload_chestplate"),
        ("neko_set_overload/leggings.json", "overload_leggings.json", "minecraft:leather_leggings",
         "leggings", "skywars2:overload_leggings"),
        ("neko_set_overload/boots.json", "overload_boots.json", "minecraft:leather_boots",
         "boots", "skywars2:overload_boots"),
    ]
    for source_name, destination_name, source_item, marker, target_item in selections:
        source_table = json.loads((SOURCE / "loot_table" / source_name).read_text(encoding="utf-8-sig"))
        migrate_loot_schema(source_table)
        matches = []
        for pool in source_table.get("pools", []):
            for entry in pool.get("entries", []):
                if entry.get("name") != source_item:
                    continue
                functions = entry.get("modifier", [])
                functions = functions if isinstance(functions, list) else [functions]
                custom_tags = [function.get("tag", "") for function in functions
                               if function.get("type") == "minecraft:set_custom_data"]
                if marker == "overload_crux":
                    selected = any(re.search(r"(?:^|[,{}])overload_crux:1(?:[,{}]|$)", tag)
                                   and re.search(r"(?:^|[,{}])magicwand:1(?:[,{}]|$)", tag)
                                   for tag in custom_tags)
                else:
                    selected = any(re.search(rf"(?:^|[,{{}}]){marker}:1(?:[,{{}}]|$)", tag)
                                   and re.search(r"(?:^|[,{}])overload:1(?:[,{}]|$)", tag)
                                   for tag in custom_tags)
                if selected:
                    matches.append((pool, entry))
        assert len(matches) == 1, (
            f"Expected exactly one OLD LBC {marker} entry in {source_name}; found {len(matches)}")
        source_pool, source_entry = matches[0]
        source_entry["name"] = target_item
        expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
        expected_pool["entries"] = [source_entry]
        expected_table = {key: value for key, value in source_table.items() if key != "pools"}
        expected_table["pools"] = [expected_pool]
        actual_table = json.loads((resources / destination_name).read_text(encoding="utf-8"))
        assert actual_table == expected_table, (
            f"Overload payload differs from exact OLD LBC source: {destination_name}")
    return len(selections)


def check_first_wand_payloads(project: Path) -> int:
    """Compare the first two wand entries and their cast trinkets to OLD LBC source."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    resources = project / "src/main/resources/data/lbc/loot_table/ported"
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
        source_table = json.loads((SOURCE / "loot_table" / source_name).read_text(encoding="utf-8-sig"))
        migrate_loot_schema(source_table)
        matches = []
        for pool in source_table.get("pools", []):
            for entry in pool.get("entries", []):
                if entry.get("name") != proxy:
                    continue
                functions = entry.get("modifier", [])
                functions = functions if isinstance(functions, list) else [functions]
                if any(function.get("type") == "minecraft:set_custom_data"
                       and marker in function.get("tag", "") for function in functions):
                    matches.append((pool, entry))
        assert len(matches) == 1, (
            f"Expected exactly one OLD LBC {marker} item in {source_name}; found {len(matches)}")
        source_pool, source_entry = matches[0]
        source_entry["name"] = item_id
        expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
        expected_pool["entries"] = [source_entry]
        expected_table = {key: value for key, value in source_table.items() if key != "pools"}
        expected_table["pools"] = [expected_pool]
        actual_table = json.loads((resources / destination_name).read_text(encoding="utf-8"))
        assert actual_table == expected_table, (
            f"Wand/trinket payload differs from exact OLD LBC source: {destination_name}")
    return len(selections)


def check_second_wand_payloads(project: Path) -> int:
    """Independently compare the Air, Stone, and Teleport wand payloads to OLD LBC."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    source_name = "random_sauvojen_wand_tiny.json"
    source_table = json.loads((SOURCE / "loot_table" / source_name).read_text(encoding="utf-8-sig"))
    migrate_loot_schema(source_table)
    selections = [
        ("stair_tiny", "tiny_air_wand.json", "skywars2:tiny_air_wand"),
        ("ststone_tiny", "tiny_stone_wand.json", "skywars2:tiny_stone_wand"),
        ("stteleport_tiny", "tiny_teleport_wand.json", "skywars2:tiny_teleport_wand"),
    ]
    resources = project / "src/main/resources/data/lbc/loot_table/ported"
    for marker, destination_name, target_item in selections:
        matches = []
        for source_pool in source_table.get("pools", []):
            for source_entry in source_pool.get("entries", []):
                if source_entry.get("name") != "minecraft:carrot_on_a_stick":
                    continue
                functions = source_entry.get("modifier", [])
                functions = functions if isinstance(functions, list) else [functions]
                if any(function.get("type") == "minecraft:set_custom_data"
                       and re.search(rf"(?:^|[,{{}}]){re.escape(marker)}:1(?:[,{{}}]|$)",
                                     function.get("tag", "")) for function in functions):
                    matches.append((source_pool, source_entry))
        assert len(matches) == 1, (
            f"Expected one OLD LBC {marker} entry in {source_name}; found {len(matches)}")
        source_pool, source_entry = matches[0]
        source_entry["name"] = target_item
        expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
        expected_pool["entries"] = [source_entry]
        expected_table = {key: value for key, value in source_table.items() if key != "pools"}
        expected_table["pools"] = [expected_pool]
        actual_table = json.loads((resources / destination_name).read_text(encoding="utf-8"))
        assert actual_table == expected_table, (
            f"Tiny wand payload differs from exact OLD LBC source: {destination_name}")
    return len(selections)


def check_third_wand_payloads(project: Path) -> int:
    """Independently compare the Ruby and Terra wand payloads to OLD LBC."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    source_name = "random_sauvojen_wand_tiny.json"
    source_table = json.loads((SOURCE / "loot_table" / source_name).read_text(encoding="utf-8-sig"))
    migrate_loot_schema(source_table)
    selections = [
        ("struby_tiny", "tiny_ruby_wand.json", "skywars2:tiny_ruby_wand"),
        ("stterra_tiny", "tiny_terra_wand.json", "skywars2:tiny_terra_wand"),
    ]
    resources = project / "src/main/resources/data/lbc/loot_table/ported"
    for marker, destination_name, target_item in selections:
        matches = []
        for source_pool in source_table.get("pools", []):
            for source_entry in source_pool.get("entries", []):
                if source_entry.get("name") != "minecraft:carrot_on_a_stick":
                    continue
                functions = source_entry.get("modifier", [])
                functions = functions if isinstance(functions, list) else [functions]
                if any(function.get("type") == "minecraft:set_custom_data"
                       and re.search(rf"(?:^|[,{{}}]){re.escape(marker)}:1(?:[,{{}}]|$)",
                                     function.get("tag", "")) for function in functions):
                    matches.append((source_pool, source_entry))
        assert len(matches) == 1, (
            f"Expected one OLD LBC {marker} entry in {source_name}; found {len(matches)}")
        source_pool, source_entry = matches[0]
        source_entry["name"] = target_item
        expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
        expected_pool["entries"] = [source_entry]
        expected_table = {key: value for key, value in source_table.items() if key != "pools"}
        expected_table["pools"] = [expected_pool]
        actual_table = json.loads((resources / destination_name).read_text(encoding="utf-8"))
        assert actual_table == expected_table, (
            f"Tiny wand payload differs from exact OLD LBC source: {destination_name}")
    return len(selections)


def check_fourth_wand_payloads(project: Path) -> int:
    """Independently compare the tiny Acid wand payload to OLD LBC."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    source_name = "random_sauvojen_wand_tiny.json"
    source_table = json.loads((SOURCE / "loot_table" / source_name).read_text(encoding="utf-8-sig"))
    migrate_loot_schema(source_table)
    marker = "stacid_tiny"
    matches = []
    for source_pool in source_table.get("pools", []):
        for source_entry in source_pool.get("entries", []):
            if source_entry.get("name") != "minecraft:carrot_on_a_stick":
                continue
            functions = source_entry.get("modifier", [])
            functions = functions if isinstance(functions, list) else [functions]
            if any(function.get("type") == "minecraft:set_custom_data"
                   and re.search(r"(?:^|[,{}])stacid_tiny:1(?:[,{}]|$)", function.get("tag", ""))
                   for function in functions):
                matches.append((source_pool, source_entry))
    assert len(matches) == 1, (
        f"Expected one OLD LBC {marker} entry in {source_name}; found {len(matches)}")
    source_pool, source_entry = matches[0]
    source_entry["name"] = "skywars2:tiny_acid_wand"
    expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
    expected_pool["entries"] = [source_entry]
    expected_table = {key: value for key, value in source_table.items() if key != "pools"}
    expected_table["pools"] = [expected_pool]
    resources = project / "src/main/resources/data/lbc/loot_table/ported"
    actual_table = json.loads((resources / "tiny_acid_wand.json").read_text(encoding="utf-8"))
    assert actual_table == expected_table, (
        "Tiny Acid wand payload differs from exact OLD LBC source: tiny_acid_wand.json")
    return 1


def check_electro_wand_payloads(project: Path) -> int:
    """Independently compare the tiny Electro wand payload to OLD LBC."""
    from import_game_resources import migrate_loot_schema
    from import_lbc_loadouts import SOURCE

    source_name = "random_sauvojen_wand_tiny.json"
    source_table = json.loads((SOURCE / "loot_table" / source_name).read_text(encoding="utf-8-sig"))
    migrate_loot_schema(source_table)
    marker = "stelectro_tiny"
    matches = []
    for source_pool in source_table.get("pools", []):
        for source_entry in source_pool.get("entries", []):
            if source_entry.get("name") != "minecraft:carrot_on_a_stick":
                continue
            functions = source_entry.get("modifier", [])
            functions = functions if isinstance(functions, list) else [functions]
            if any(function.get("type") == "minecraft:set_custom_data"
                   and re.search(r"(?:^|[,{}])stelectro_tiny:1(?:[,{}]|$)", function.get("tag", ""))
                   for function in functions):
                matches.append((source_pool, source_entry))
    assert len(matches) == 1, (
        f"Expected one OLD LBC {marker} entry in {source_name}; found {len(matches)}")
    source_pool, source_entry = matches[0]
    source_entry["name"] = "skywars2:tiny_electro_wand"
    expected_pool = {key: value for key, value in source_pool.items() if key != "entries"}
    expected_pool["entries"] = [source_entry]
    expected_table = {key: value for key, value in source_table.items() if key != "pools"}
    expected_table["pools"] = [expected_pool]
    resources = project / "src/main/resources/data/lbc/loot_table/ported"
    actual_table = json.loads((resources / "tiny_electro_wand.json").read_text(encoding="utf-8"))
    assert actual_table == expected_table, (
        "Tiny Electro wand payload differs from exact OLD LBC source: tiny_electro_wand.json")
    return 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-manifest", type=Path, default=DEFAULT_SOURCE_MANIFEST)
    parser.add_argument("--design-manifest", type=Path, default=DEFAULT_DESIGN_MANIFEST)
    parser.add_argument("--project-root", type=Path, default=PROJECT_ROOT)
    parser.add_argument("--snapshot", type=Path, help="validate a GameChecks menu-design-snapshot.json export")
    parser.add_argument("--game-mode", type=int, choices=(1, 2, 3, 4),
                        help="validate the conditional hub team-choice slot against a known runtime mode")
    args = parser.parse_args()
    if not args.source_manifest.exists():
        print(f"ERROR: source manifest not found: {args.source_manifest}", file=sys.stderr)
        return 2
    if not args.design_manifest.exists():
        print(f"ERROR: GUI design import not found: {args.design_manifest}", file=sys.stderr)
        print("Expected container size/title, source-function and slot-to-sourceLootTable records.", file=sys.stderr)
        return 2
    try:
        count, errors, counters = check(args.source_manifest, args.design_manifest, args.project_root)
        kits, trinkets, notifications = check_native_lbc_sources(args.project_root)
        mana_payloads = check_mana_potion_payloads(args.project_root)
        overload_payloads = check_overload_payloads(args.project_root)
        first_wand_payloads = check_first_wand_payloads(args.project_root)
        second_wand_payloads = check_second_wand_payloads(args.project_root)
        third_wand_payloads = check_third_wand_payloads(args.project_root)
        fourth_wand_payloads = check_fourth_wand_payloads(args.project_root)
        electro_wand_payloads = check_electro_wand_payloads(args.project_root)
        counters["nativeLooperArtifactDescriptions"] = check_looper_artifact_source(
            args.project_root, args.source_manifest
        )
        counters["nativeLbcKitPayloads"] = kits
        counters["nativeLbcTrinketPayloads"] = trinkets
        counters["nativeLbcUnlockNotifications"] = notifications
        counters["nativeManaPotionPayloads"] = mana_payloads
        counters["nativeOverloadPayloads"] = overload_payloads
        counters["nativeFirstWandPayloads"] = first_wand_payloads
        counters["nativeSecondWandPayloads"] = second_wand_payloads
        counters["nativeThirdWandPayloads"] = third_wand_payloads
        counters["nativeFourthWandPayloads"] = fourth_wand_payloads
        counters["nativeElectroWandPayloads"] = electro_wand_payloads
    except (OSError, json.JSONDecodeError, TypeError, ValueError, AssertionError) as exc:
        print(f"ERROR: unable to compare manifests: {exc}", file=sys.stderr)
        return 2

    print(json.dumps(counters, ensure_ascii=False, indent=2))
    print(f"source comparisons: pages={counters['importPages']}, slots={counters['slotReferencesCompared']}, "
          f"loot JSON exact={counters['localLootJsonExactMatches']}/{counters['localLootJsonCompared']}, "
          f"model IDs={counters['sourceModelIdsPresent']}/{counters['sourceModelIdsCompared']}")
    if errors:
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        print(f"design source check failed: {len(errors)} discrepancies", file=sys.stderr)
        return 1
    if args.snapshot:
        if not args.snapshot.exists():
            print(f"ERROR: runtime snapshot not found: {args.snapshot}", file=sys.stderr)
            return 2
        try:
            snapshot_errors, snapshot_counts = check_snapshot(args.snapshot, args.design_manifest, args.source_manifest,
                                                              args.project_root, args.game_mode)
        except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
            print(f"ERROR: unable to validate runtime snapshot: {exc}", file=sys.stderr)
            return 2
        print("runtime snapshot: " + json.dumps(snapshot_counts, ensure_ascii=False))
        if snapshot_errors:
            for error in snapshot_errors:
                print(f"FAIL: {error}", file=sys.stderr)
            print(f"runtime design snapshot check failed: {len(snapshot_errors)} discrepancies", file=sys.stderr)
            return 1
    print("design source check passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
