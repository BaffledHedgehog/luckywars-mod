"""Repair stale vanilla carriers using exact identities from the existing ready catalog.

Only item-entry names change. Counts, selection and every authored modifier remain
byte-preserved. No new references, profiles, tables or catalog entries are added.
"""
from __future__ import annotations
import argparse
from collections import defaultdict
from copy import deepcopy
import json
from pathlib import Path
import re
from canonicalize_lbc_loot import identity, item_entries, packed

ROOT = Path(__file__).resolve().parents[1]
TABLES = ROOT / "src/main/resources/data/lbc/loot_table"
CATALOG = ROOT / "src/main/resources/skywars2/loot_test_catalog.json"
# Explicit carrier registrations in LbcKaraSlabs distinguish these otherwise
# identical authored identity modifiers. Never choose an ambiguous target by order.
KARA_CARRIERS = {
    "minecraft:waxed_exposed_cut_copper_slab": "skywars2:kara_exposed_slab",
    "minecraft:waxed_weathered_cut_copper_slab": "skywars2:kara_weathered_slab",
}


def signature(entry):
    result = identity(entry)
    result.pop("carrier")
    return packed(result) if result and "tmt_item" not in packed(result) else None


def references(value):
    if isinstance(value, list):
        for child in value:
            yield from references(child)
    elif isinstance(value, dict):
        if value.get("type") == "minecraft:loot_table" and isinstance(value.get("value"), str):
            yield value["value"]
        for key in ("pools", "entries", "children", "value"):
            if key in value:
                yield from references(value[key])


def load(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def closure(seeds):
    pending = list(seeds)
    seen = set()
    while pending:
        table = pending.pop()
        if table in seen or not table.startswith("lbc:"):
            continue
        seen.add(table)
        path = TABLES / (table[4:] + ".json")
        if path.is_file():
            try:
                pending.extend(references(load(path)))
            except json.JSONDecodeError:
                pass
    return seen


def reconcile(apply=False):
    seeds = load(CATALOG)["tables"] + ["lbc:" + p.relative_to(TABLES).with_suffix("").as_posix()
                                                for p in (TABLES / "ported").rglob("*.json")]
    ready = closure(seeds)
    tree = closure(["lbc:infinity_upgraded_luck_noadv"])
    owners = defaultdict(set)
    authority = defaultdict(set)
    for table in sorted(ready):
        path = TABLES / (table[4:] + ".json")
        if not path.is_file():
            continue
        try:
            document = load(path)
        except json.JSONDecodeError:
            continue
        for entry, _ in item_entries(document):
            key = signature(entry)
            if entry["name"].startswith("skywars2:") and key:
                owners[key].add(entry["name"])
                authority[(key, entry["name"])].add(table)
    changes, invalid, ambiguous, staged = [], [], [], []
    for path in sorted(TABLES.rglob("*.json")):
        try:
            document = load(path)
        except json.JSONDecodeError:
            invalid.append(path.relative_to(ROOT).as_posix())
            continue
        before = deepcopy(document)
        replacements = []
        table = "lbc:" + path.relative_to(TABLES).with_suffix("").as_posix()
        for entry, where in item_entries(document):
            old = entry["name"]
            if not old.startswith("minecraft:"):
                continue
            key = signature(entry)
            targets = owners.get(key, set())
            if len(targets) == 1:
                target = next(iter(targets))
            elif len(targets) > 1 and KARA_CARRIERS.get(old) in targets:
                target = KARA_CARRIERS[old]
            else:
                if targets:
                    ambiguous.append({"table": table, "path": where, "targets": sorted(targets)})
                continue
            entry["name"] = target
            replacements.append((old, target))
            changes.append({"table": table, "path": where, "before": old, "after": target,
                            "readyAuthority": sorted(authority[(key, target)]), "treeReachable": table in tree})
        if not replacements:
            continue
        # Prove the parsed change contains exactly the approved identity substitutions.
        restored = deepcopy(document)
        for original, repaired in zip(item_entries(before), item_entries(restored)):
            repaired[0]["name"] = original[0]["name"]
        assert restored == before, path
        raw = path.read_bytes().decode("utf-8-sig")
        for old, target in replacements:
            raw, count = re.subn(r'("name"\s*:\s*)' + re.escape(json.dumps(old)),
                                  lambda match: match[1] + json.dumps(target), raw, count=1)
            assert count == 1, path
        assert json.loads(raw) == document, path
        # Save original newline style and all original bytes except the identifiers.
        original = path.read_bytes()
        encoded = raw.encode("utf-8")
        if original.startswith(b"\xef\xbb\xbf"):
            encoded = b"\xef\xbb\xbf" + encoded
        staged.append((path, encoded))
    if apply:
        for path, encoded in staged:
            path.write_bytes(encoded)
    return {"applied": apply, "readyTables": len(ready), "readyNativeSignatures": len(owners),
            "changedFiles": len(staged), "changedEntries": len(changes),
            "treeReachableChanges": sum(row["treeReachable"] for row in changes),
            "ambiguous": ambiguous, "existingInvalidTablesUntouched": invalid, "changes": changes}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    report = reconcile(args.apply)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "changes"}, indent=2))
