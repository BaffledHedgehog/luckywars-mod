#!/usr/bin/env python3
"""Import only the static resource closure used by Reforge's category rolls."""
from __future__ import annotations

import hashlib
import json
import argparse
import re
from collections import deque

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_weapons import migrate_legacy_score_value_checks

DEST = ROOT / "src/main/resources/data/lbc"
CATEGORIES = "accessory axe bow hoe pickaxe potion shield shooter shovel staff sword totem spells summons trident arcanum helmet chestplate leggings boots".split()
ROOT_TABLES = [f"transformables_{name}" for name in CATEGORIES] + ["spells_library", "halloween/metasola"]


def migrate_score_attribute_amounts(value):
    """Bridge legacy score float amounts through the native 26.3 int codec."""
    if isinstance(value, list):
        for child in value:
            migrate_score_attribute_amounts(child)
    elif isinstance(value, dict):
        for child in list(value.values()):
            migrate_score_attribute_amounts(child)
        if value.get("type") != "minecraft:set_attributes":
            return
        for modifier in value.get("modifiers", []):
            amount = modifier.get("amount")
            if not isinstance(amount, dict) or amount.get("type") != "minecraft:score":
                continue
            # ScoreboardValue is an int provider in 26.3; FromInt preserves its
            # conversion to the float consumed by set_attributes. Refuse scales
            # this scoped migration cannot represent rather than dropping them.
            if amount.get("scale", 1) != 1:
                raise SystemExit(f"Unsupported legacy attribute score scale: {amount!r}")
            score = {key: item for key, item in amount.items() if key != "scale"}
            modifier["amount"] = {"type": "minecraft:from_int", "input": score}


def dependencies(value, kind):
    if isinstance(value, list):
        for child in value:
            yield from dependencies(child, kind)
    elif isinstance(value, dict):
        discriminator = value.get("type", value.get("function", value.get("condition")))
        target_kind = None
        reference = None
        if discriminator in ("minecraft:loot_table", "loot_table"):
            target_kind, reference = "loot_table", value.get("value", value.get("name"))
        elif discriminator == "minecraft:reference":
            target_kind = "item_modifier" if "function" in value or kind == "item_modifier" else "predicate"
            reference = value.get("name")
        elif discriminator == "minecraft:tag":
            target_kind, reference = "tags/item", value.get("name")
        if target_kind and isinstance(reference, str) and reference.startswith("lbc:"):
            yield target_kind, reference[4:]
        if kind.startswith("tags/"):
            for entry in value.get("values", []):
                ref = entry if isinstance(entry, str) else entry.get("id", "")
                if ref.startswith("#lbc:"):
                    yield kind, ref[5:]
        for child in value.values():
            yield from dependencies(child, kind)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--infbolt", action="store_true", help="import the ultra_reforge closure and write its incremental manifest")
    args = parser.parse_args()
    roots = ROOT_TABLES
    previous_paths = set()
    if args.infbolt:
        roots = sorted(set(re.findall(r"loot lbc:([a-z0-9_/]+)",
            (SOURCE / "function/ultra_reforge.mcfunction").read_text(encoding="utf-8-sig"))))
        previous_paths = set(json.loads((ROOT / "agent_notes/REFORGE_RESOURCE_MANIFEST.json").read_text(encoding="utf-8"))["paths"])
    queue = deque(("loot_table", name) for name in roots)
    seen = set()
    paths = []
    entries = 0
    while queue:
        kind, name = queue.popleft()
        if (kind, name) in seen:
            continue
        seen.add((kind, name))
        relative = f"{kind}/{name}.json"
        data = json.loads((SOURCE / relative).read_text(encoding="utf-8-sig"))
        queue.extend(dependencies(data, kind))
        if kind == "loot_table":
            entries += sum(len(pool.get("entries", [])) for pool in data.get("pools", []))
        migrate_loot_schema(data)
        migrate_legacy_score_value_checks(data)
        migrate_score_attribute_amounts(data)
        # SLR now has a native behavior owner; preserve its complete source payload.
        if kind == "loot_table" and name == "slr":
            for pool in data.get("pools", []):
                for entry in pool.get("entries", []):
                    if entry.get("name") == "minecraft:carrot_on_a_stick":
                        entry["name"] = "skywars2:slr"
        target = DEST / relative
        text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        if not target.exists() or target.read_text(encoding="utf-8-sig") != text:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding="utf-8", newline="\n")
        assert json.loads(target.read_text(encoding="utf-8")) == data
        resource_path = "data/lbc/" + relative
        if resource_path not in previous_paths:
            paths.append(resource_path)
    source = SOURCE / "structure/big_snowball.nbt"
    target = DEST / "structure/big_snowball.nbt"
    payload = source.read_bytes()
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists() or target.read_bytes() != payload:
        target.write_bytes(payload)
    assert target.read_bytes() == payload
    if "data/lbc/structure/big_snowball.nbt" not in previous_paths:
        paths.append("data/lbc/structure/big_snowball.nbt")
    report = {"paths": sorted(paths), "lootTables": sum(k == "loot_table" for k, _ in seen),
              "directEntries": entries, "bytes": sum((ROOT / "src/main/resources" / p).stat().st_size for p in paths),
              "bigSnowballSha256": hashlib.sha256(payload).hexdigest()}
    if args.infbolt:
        report["roots"] = roots
        report["reusedReforgePaths"] = len(seen) - len(paths)
    manifest = "INFBOLT_RESOURCE_MANIFEST.json" if args.infbolt else "REFORGE_RESOURCE_MANIFEST.json"
    (ROOT / "agent_notes" / manifest).write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "paths"}))


if __name__ == "__main__":
    main()
