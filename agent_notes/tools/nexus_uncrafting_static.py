"""Generate/audit the fixed Nexus uncraft lookup; never used by the game runtime.

Schema: items maps vanilla item ID -> loot ID (659 direct branches);
suspicious_stew_effects maps effect ID -> loot ID (9 branches).
The special suspicious_stew selector is handled by the runtime, not encoded as a
fake loot ID. XP, custom-item and stew fallthrough policy are runtime decisions.
--write writes ONLY the static lookup. --modernize upgrades only the 594 selected
uncrafted resources using the existing 26.3 schema importer, after full validation.
"""
import argparse
from copy import deepcopy
import json
from pathlib import Path
import re
import sys

PROJECT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = Path("E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc")
sys.path.insert(0, str(PROJECT / "tools"))
from import_game_resources import migrate_loot_schema


def modernized(value):
    result = deepcopy(value)
    # This closure contains only item entries and set_count; retain authored values.
    migrate_loot_schema(result, preserve_imba=True)
    return result


def modifiers(entry):
    value = entry.get("modifier", entry.get("functions", []))
    return value if isinstance(value, list) else [value]


def collect(source, modernize=False):
    functions = source / "function/other/nexus/click/orb_gun"
    selector = (functions / "uncraft_f_loot_select.mcfunction").read_text(encoding="utf-8")
    stews = (functions / "susp_stews.mcfunction").read_text(encoding="utf-8")
    item_pairs = re.findall(r'\{item:"([^"]+)"\} run return run loot spawn ~ ~ ~ loot (\S+)', selector)
    stew_pairs = re.findall(r'\[\{id:"([^"]+)"\}.*? run loot spawn ~ ~ ~ loot (\S+)', stews)
    all_inputs = re.findall(r'\{item:"([^"]+)"\}', selector)
    assert len(all_inputs) == len(set(all_inputs)) == 660
    assert len(item_pairs) == len(dict(item_pairs)) == 659
    assert set(all_inputs) - dict(item_pairs).keys() == {"minecraft:suspicious_stew"}
    assert len(stew_pairs) == len(dict(stew_pairs)) == 9
    expected = {"items": dict(item_pairs), "suspicious_stew_effects": dict(stew_pairs)}
    references = sorted(set(expected["items"].values()) | set(expected["suspicious_stew_effects"].values()))
    assert len(references) == 594
    entries = counts = 0
    included = set()
    staged = {}
    # Stage only selector roots, never shared item_defs or unrelated resources.
    if modernize:
        for loot_id in references:
            relative = "data/lbc/loot_table/" + loot_id.split(":", 1)[1] + ".json"
            path = PROJECT / "src/main/resources" / relative
            before = json.loads(path.read_text(encoding="utf-8"))
            after = modernized(before)
            if before != after:
                staged[relative] = after

    def native_table(loot_id, trail=()):
        assert loot_id not in trail, (loot_id, trail)
        namespace, name = loot_id.split(":", 1)
        assert namespace == "lbc"
        relative = "data/lbc/loot_table/" + name + ".json"
        included.add(relative)
        table = deepcopy(staged[relative]) if relative in staged else json.loads((PROJECT / "src/main/resources" / relative).read_text(encoding="utf-8"))
        for pool in table["pools"]:
            expanded = []
            for entry in pool["entries"]:
                if entry["type"] == "minecraft:loot_table":
                    child = native_table(entry["value"], (*trail, loot_id))
                    assert set(child) == {"pools"} and len(child["pools"]) == 1, entry
                    child_pool = child["pools"][0]
                    assert set(child_pool) == {"rolls", "entries"} and child_pool["rolls"] == 1, entry
                    assert len(child_pool["entries"]) == 1, entry
                    flattened = child_pool["entries"][0].copy()
                    for key, value in entry.items():
                        if key not in {"type", "value"}:
                            assert key not in flattened, (entry, flattened)
                            flattened[key] = value
                    entry = flattened
                expanded.append(entry)
            pool["entries"] = expanded
        return table

    for loot_id in references:
        namespace, name = loot_id.split(":", 1)
        assert namespace == "lbc" and name.startswith("uncrafted/")
        relative = Path("loot_table") / (name + ".json")
        original = source / relative
        original_json = json.loads(original.read_text(encoding="utf-8"))
        # Exact tree equality after codec-only normalization and singleton expansion.
        assert modernized(original_json) == native_table(loot_id), loot_id
        for pool in original_json["pools"]:
            for entry in pool["entries"]:
                assert entry["type"] == "minecraft:item", (loot_id, entry)
                entries += 1
                for function in modifiers(entry):
                    assert function.get("type", function.get("function")) == "minecraft:set_count", (loot_id, function)
                    counts += 1
    assert entries == 1027 and counts == 396, (entries, counts)
    # Validate the complete closure before committing any resource changes.
    for relative, document in staged.items():
        (PROJECT / "src/main/resources" / relative).write_text(
            json.dumps(document, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if modernize:
        print("Modernized " + str(len(staged)) + " of 594 selected uncrafted resources; shared item_defs unchanged")
    return expected, sorted(included)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--modernize", action="store_true", help="Upgrade only the 594 selected uncrafted resources after validating their complete closure")
    parser.add_argument("--print-includes", action="store_true", help="Print all 594 exact Gradle resource inclusion paths")
    args = parser.parse_args()
    expected, includes = collect(args.source, args.modernize)
    target = PROJECT / "src/main/resources/skywars2/nexus_uncrafting.json"
    if args.write:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(expected, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    assert json.loads(target.read_text(encoding="utf-8")) == expected
    print("PASS: 660 source inputs; 659 direct mappings + special stew; 9 stew effects; 594 source-equivalent flat loot tables; 1027 entries; 396 set_count modifiers; " + str(len(includes)) + " native resources in closure")
    if args.print_includes:
        for relative in includes:
            print(relative)


if __name__ == "__main__":
    main()
