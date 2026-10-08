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
            if pkey not in source_pages_with_slots:
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


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-manifest", type=Path, default=DEFAULT_SOURCE_MANIFEST)
    parser.add_argument("--design-manifest", type=Path, default=DEFAULT_DESIGN_MANIFEST)
    parser.add_argument("--project-root", type=Path, default=PROJECT_ROOT)
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
    except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
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
    print("design source check passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
