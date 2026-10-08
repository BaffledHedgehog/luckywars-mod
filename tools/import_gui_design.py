#!/usr/bin/env python3
"""Import the supported datapack GUI source map and missing storage captions.

Reads the legacy server tree and the user resource pack, both as read-only inputs.
Writes only src/main/resources/skywars2/gui_design.json and
build/verification/gui-design-new-translations.json in this project.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


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
        "12000": "data/lbc/function/swrg_kit_integration/gui/page/12000.mcfunction",
    },
}
LOOT_REF = re.compile(
    r"enderchest\.(\d+)\s+loot\s+((?:swrg|lbc):gui/page/([0-9]+)/([a-z0-9_]+))(?=\s|$)"
)
ITEM_REF = re.compile(r"enderchest\.(\d+)\s+with\s+(minecraft:[a-z0-9_]+)(\[[^\]]*\])?")
MODIFIER_REF = re.compile(r"enderchest\.(\d+)\s+([a-z0-9_]+:[a-z0-9_./-]+)\s*$")
STORAGE_LINE = re.compile(r"data modify storage swrg:lang\s+(\S+)\s+set value\s+(.+)$")
TEXT_VALUE = re.compile(r'"text"\s*:\s*("(?:\\.|[^"\\])*")')
STORAGE_ENTRY = re.compile(r"data modify storage swrg:lang\s+(\S+)\s+set value\s+(.+)$")


def parse_snbt_component(raw: str):
    # Source GUI text uses JSON-compatible component values inside SNBT compounds.
    normalized = re.sub(r'([,{]\s*)([A-Za-z_][A-Za-z0-9_.-]*)\s*:', r'\1"\2":', raw)
    return json.loads(normalized)


def source_storage_components(old_root: Path) -> dict:
    result = {"en": {}, "ru": {}}
    for locale, filename in (("en", "en.mcfunction"), ("ru", "ru.mcfunction")):
        source = old_root / "world/datapacksdisabled/swarogskywars/data/swrg/function/core/lang" / filename
        for line_no, line in enumerate(source.read_text(encoding="utf-8-sig").splitlines(), 1):
            match = STORAGE_ENTRY.search(line)
            if not match or not match.group(1).startswith(("page0.", "page1000.", "page3000.",
                                                            "page4000.", "page5000.", "page6000.",
                                                            "page7000.", "gui.", "stat.", "kit.", "lore.")):
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
            for entry in slots.values():
                for source in entry["sources"]:
                    table_id = source.get("sourceLootTable")
                    if table_id:
                        relative_table = Path("data") / table_id.split(":", 1)[0] / "loot_table" / Path(*table_id.split(":", 1)[1].split("/"))
                        relative_table = relative_table.with_suffix(".json")
                        if not (local_resources / relative_table).is_file():
                            source["localTableMissing"] = True
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
        shared.append({"sourceLootTable": table_id, "localTableMissing": not (local_resources / "data" / namespace / "loot_table" / f"{path}.json").is_file()})
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
                    source["translationKey"] = f"swrg.gui.map.{page['pageId']}.{slot['slot']}"

    return {
        "schemaVersion": 1,
        "container": {"size": 27, "title": {"translate": "container.enderchest"}, "slots": list(range(27))},
        "requiredSourcePages": [f"{n}:{p}" for n, ps in GUI_PAGES.items() for p in ps],
        "pages": pages,
        "sharedButtons": shared,
        "storageBindings": [{"storagePath": "swrg:lang", "sourceFunction": "swrg:gui/page/1000-lang"}],
        "sourceStorageComponents": source_storage_components(old_root),
        "sourceNotes": [
            "Map page items in pages 2000/2000b/2100/2200 are literal item-replace commands, not loot tables.",
            "Page 1100 live player-head profiles are direct item-replace commands; commented loot references are ignored.",
            "Scoreboard-backed selected/glint, unlock, price, and live-stat variants are not runtime imported by this manifest.",
        ],
    }


def decode_text_components(raw: str) -> list[str]:
    result = []
    for value in TEXT_VALUE.findall(raw):
        try:
            result.append(json.loads(value))
        except json.JSONDecodeError:
            continue
    return result


def missing_storage_translations(old_root: Path) -> dict:
    wanted: set[str] = set()
    for slot, values in {
        0: range(1, 3), 1: range(1, 4), 3: range(0, 2), 4: range(0, 2),
        5: range(0, 10), 6: range(0, 8),
        24: range(0, 5),
    }.items():
        wanted.update(f"page1000.s{slot}-{value}" for value in values)

    pack_en = json.loads((RESOURCE_PACK / "assets/minecraft/lang/en_us.json").read_text(encoding="utf-8-sig"))
    pack_ru = json.loads((RESOURCE_PACK / "assets/minecraft/lang/ru_ru.json").read_text(encoding="utf-8-sig"))
    locale_values: dict[str, dict[str, str]] = {"en_us": {}, "ru_ru": {}}
    for locale, file_name in (("en_us", "en.mcfunction"), ("ru_ru", "ru.mcfunction")):
        path = old_root / "world/datapacksdisabled/swarogskywars/data/swrg/function/core/lang" / file_name
        assignments = {}
        for line in path.read_text(encoding="utf-8-sig").splitlines():
            match = STORAGE_LINE.search(line)
            if match and match.group(1) in wanted:
                texts = decode_text_components(match.group(2))
                assignments[match.group(1)] = "".join(texts)
        pack = pack_en if locale == "en_us" else pack_ru
        for storage_key in sorted(wanted):
            translation_key = "swrg." + storage_key
            if translation_key not in pack:
                value = assignments.get(storage_key)
                if value is None:
                    raise ValueError(f"Missing source language value for {storage_key} ({locale})")
                locale_values[locale][translation_key] = value
    design = page_manifest(old_root, PROJECT / "src/main/resources")
    for page in design["pages"]:
        if page["pageId"] not in {"2000", "2000b", "2100", "2200"}:
            continue
        for slot in page["slots"]:
            for source in slot["sources"]:
                if "translationKey" not in source:
                    continue
                text = source.get("sourceName", {}).get("text")
                if text is None:
                    continue
                key = source["translationKey"]
                for locale, pack in (("en_us", pack_en), ("ru_ru", pack_ru)):
                    if key not in pack:
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
    args = parser.parse_args()
    resources = args.project / "src/main/resources"
    (resources / "skywars2").mkdir(parents=True, exist_ok=True)
    design_path = resources / "skywars2/gui_design.json"
    design_path.write_text(json.dumps(page_manifest(args.old_root, resources), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    translations_path = args.project / "build/verification/gui-design-new-translations.json"
    translations_path.parent.mkdir(parents=True, exist_ok=True)
    translations_path.write_text(json.dumps(missing_storage_translations(args.old_root), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
