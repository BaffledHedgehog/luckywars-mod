"""Extend infinity from an actual vanilla registry report without altering existing entries."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TABLES = ROOT / "src/main/resources/data/lbc/loot_table"
PARENT = TABLES / "infinity_no_shulker.json"


def packed(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":"))


def collect_items(value, seen):
    found = set()
    if isinstance(value, dict):
        if value.get("type") == "minecraft:item":
            found.add(value["name"])
        if value.get("type") == "minecraft:loot_table":
            ref = value.get("value")
            if isinstance(ref, str) and ref.startswith("lbc:") and ref not in seen:
                seen.add(ref)
                found |= collect_items(json.loads((TABLES / (ref[4:] + ".json")).read_text(encoding="utf-8")), seen)
        for child in value.values():
            found |= collect_items(child, seen)
    elif isinstance(value, list):
        for child in value:
            found |= collect_items(child, seen)
    return found


def refresh(report, apply=False):
    native = set(json.loads(report.read_text(encoding="utf-8"))["minecraft:item"]["entries"])
    native = {item for item in native if item.startswith("minecraft:") and item != "minecraft:air"}
    parent = json.loads(PARENT.read_text(encoding="utf-8"))
    covered = collect_items(parent, set())
    missing = sorted(native - covered)
    if not apply:
        print(json.dumps({"nativeUsableItems": len(native), "coveredItems": len(covered), "missing": missing}, indent=2))
        return
    assert len(parent["pools"]) == 1 and parent["pools"][0]["rolls"] == 26
    entries = parent["pools"][0]["entries"]
    original = json.loads(json.dumps(entries))
    added = []
    for item in missing:
        entries.append({"type": "minecraft:item", "name": item, "weight": 1})
        added.append({"item": item})
    assert entries[:len(original)] == original
    if added:
        PARENT.write_text(json.dumps(parent, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    assert collect_items(parent, set()) == native
    result = {"minecraftVersion": "26.3", "expectedVanillaItems": sorted(native),
              "originalPoolEntryCount": len(original), "addedItems": added,
              "originalEntriesSha256": hashlib.sha256(packed(original).encode()).hexdigest(),
              "bioAdded": [], "entryFormat": "direct vanilla item entries; old canonical references retained"}
    metadata = ROOT / "src/main/resources/skywars2/content_catalog_refresh.json"
    if added:
        metadata.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("Added", len(added), "direct items; complete vanilla coverage", len(native))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("registry_report", type=Path)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    refresh(args.registry_report.resolve(strict=True), args.apply)
