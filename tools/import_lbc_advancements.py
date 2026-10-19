#!/usr/bin/env python3
"""Prepare the reviewed LBC true-advancement tree for native 26.3 loading.

This importer is deliberately fixed to one resource directory. It preserves each
advancement display, parent, criteria semantics, and non-function rewards. Legacy
player-condition arrays are converted to native 26.3 predicates; only audited
gain-function rewards and bare `impossible` trigger names are removed or normalized.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
ADVANCEMENTS = ROOT / "src/main/resources/data/lbc/advancement/true_advancements"
SOURCE_ROOTS = ROOT / "agent_notes/inventory/source_roots.json"
UNLOCK_NOTIFICATIONS = ROOT / "src/main/resources/skywars2/lbc_unlock_notifications.json"
EXPECTED_FILES = 357
GAIN_PREFIX = "lbc:swrg_kit_integration/gain/"

# These simple legacy gain functions have been checked to emit only a blank
# title plus the listed subtitle. skill_acidman is deliberately not in this
# table: its source function is gated by ten separate advancement IDs.
SIMPLE_GAIN_SUBTITLES = {
    "kit": "new_kit",
    "skill": "new_skill",
    "levelup": "new_levelup",
    "trinket": "new_trinket",
    "default_kit": "new_swrg_kit",
    "default_skill": "new_swrg_skill",
    "default_kill_effect": "new_kill_effect",
    "death_effect": "new_death_effect",
    "challenge": "new_challenge",
}
ACIDMAN_PREREQUISITES = [
    "lbc:true_advancements/staffs/acid",
    "lbc:true_advancements/legends/academy/corruption",
    "lbc:true_advancements/staffs/corruption",
    "lbc:true_advancements/legends/academy/acid",
    "lbc:true_advancements/legends/academy/acidacid",
    "lbc:true_advancements/legends/academy/acidcorrupt",
    "lbc:true_advancements/legends/academy/acid_tuntija",
    "lbc:true_advancements/legends/tuntija_wands/acid",
    "lbc:true_advancements/legends/tuntija_wands/acidacid",
    "lbc:true_advancements/legends/tuntija_wands/acidcorrupt",
]

GAIN_MEANINGS = {
    "kit": "source function only showed the `new_kit` subtitle",
    "levelup": "source function only showed the `new_levelup` subtitle",
    "skill": "source function only showed the `new_skill` subtitle",
    "trinket": "source function only showed the `new_trinket` subtitle",
    "default_kit": "source function only showed the `new_swrg_kit` subtitle",
    "death_effect": "source function only showed the `new_death_effect` subtitle",
    "default_skill": "source function only showed the `new_swrg_skill` subtitle",
    "default_kill_effect": "source function only showed the `new_kill_effect` subtitle",
    "skill_acidman": "conditional wrapper; shows `new_skill` when one of its listed acidman IDs is complete",
    "challenge": "source function only showed the `new_challenge` subtitle",
}


def checked_files() -> list[Path]:
    resolved = ADVANCEMENTS.resolve(strict=True)
    if resolved != (ROOT / "src/main/resources/data/lbc/advancement/true_advancements").resolve():
        raise RuntimeError(f"unexpected advancement output directory: {resolved}")
    files = sorted(resolved.rglob("*.json"))
    if len(files) != EXPECTED_FILES:
        raise RuntimeError(f"expected exactly {EXPECTED_FILES} advancement JSON files, found {len(files)}")
    for path in files:
        path.resolve(strict=True).relative_to(resolved)
    return files


def write_unlock_notifications() -> dict[str, int]:
    """Build the native runtime lookup from legacy source reward IDs."""
    source_roots = json.loads(SOURCE_ROOTS.read_text(encoding="utf-8-sig"))
    source_tree = Path(source_roots["datapack_root"]) / "world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements"
    source_tree = source_tree.resolve(strict=True)
    notifications: dict[str, dict[str, object]] = {}
    counts: Counter[str] = Counter()
    files = sorted(source_tree.rglob("*.json"))
    if len(files) != EXPECTED_FILES:
        raise RuntimeError(f"expected {EXPECTED_FILES} source advancements, found {len(files)}")
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        function = data.get("rewards", {}).get("function")
        if not isinstance(function, str) or not function.startswith(GAIN_PREFIX):
            continue
        gain = function.removeprefix(GAIN_PREFIX)
        advancement_id = "lbc:true_advancements/" + path.relative_to(source_tree).with_suffix("").as_posix()
        if gain in SIMPLE_GAIN_SUBTITLES:
            notifications[advancement_id] = {"subtitle": SIMPLE_GAIN_SUBTITLES[gain]}
        elif gain == "skill_acidman":
            notifications[advancement_id] = {
                "subtitle": "new_skill",
                "requires_all": ACIDMAN_PREREQUISITES,
            }
        else:
            # Keep the importer strict: every stripped reward is accounted for,
            # while only source-audited notification behavior enters runtime.
            if gain not in GAIN_MEANINGS:
                raise ValueError(f"{path}: no audited meaning for {function!r}")
            continue
        counts[gain] += 1
    UNLOCK_NOTIFICATIONS.parent.mkdir(parents=True, exist_ok=True)
    UNLOCK_NOTIFICATIONS.write_text(json.dumps({"notifications": notifications}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    return counts


def normalize_legacy_entity_predicate(predicate: object) -> int:
    """Convert old flat entity-predicate fields to the 26.3 sub-predicate map."""
    if not isinstance(predicate, dict) or any(str(key).startswith("minecraft:") for key in predicate):
        return 0
    converted = {}
    for key, value in predicate.items():
        registry_id = "minecraft:entity_type" if key == "type" else "minecraft:" + key
        converted[registry_id] = value
    predicate.clear()
    predicate.update(converted)
    return len(converted)


LEGACY_LOOT_CONDITION_TYPES = {
    "minecraft:entity_properties", "minecraft:inverted", "minecraft:all_of", "minecraft:any_of"
}


def as_context_predicate(value: object) -> object:
    """Wrap old EntityPredicate JSON where 26.3 expects a LootItemCondition."""
    if not isinstance(value, dict):
        return value
    discriminator = value.get("condition", value.get("type"))
    if isinstance(discriminator, str) and discriminator in LEGACY_LOOT_CONDITION_TYPES:
        return value
    return {"condition": "minecraft:entity_properties", "entity": "this", "predicate": value}


def normalize_loot_condition_tree(node: object) -> int:
    """Convert legacy loot-condition and nested entity-predicate codecs to 26.3."""
    changed = 0
    if isinstance(node, dict):
        if isinstance(node.get("condition"), str):
            node["type"] = node.pop("condition")
            changed += 1
        for key, value in node.items():
            # Damage predicates contain EntityPredicate values directly. These
            # used the old flat shape too, but are not wrapped in
            # entity_properties conditions (for example direct_entity.type).
            if key in ("direct_entity", "source_entity"):
                changed += normalize_legacy_entity_predicate(value)
            changed += normalize_loot_condition_tree(value)
        if node.get("type") == "minecraft:entity_properties" and isinstance(node.get("predicate"), dict):
            changed += normalize_legacy_entity_predicate(node["predicate"])
    elif isinstance(node, list):
        for value in node:
            changed += normalize_loot_condition_tree(value)
    return changed


def iter_dicts(node: object):
    if isinstance(node, dict):
        yield node
        for value in node.values():
            yield from iter_dicts(value)
    elif isinstance(node, list):
        for value in node:
            yield from iter_dicts(value)


def transform(data: object, path: Path, gain_counts: Counter[str], gain_ids: dict[str, list[str]]) -> bool:
    if not isinstance(data, dict):
        raise ValueError(f"{path}: advancement root must be an object")
    criteria = data.get("criteria")
    if not isinstance(criteria, dict) or not criteria:
        raise ValueError(f"{path}: missing or empty criteria object")
    changed = False
    for name, criterion in criteria.items():
        if not isinstance(criterion, dict):
            raise ValueError(f"{path}: criterion {name!r} must be an object")
        if criterion.get("trigger") == "impossible":
            criterion["trigger"] = "minecraft:impossible"
            changed = True
        conditions = criterion.get("conditions")
        if conditions is not None:
            if not isinstance(conditions, dict):
                raise ValueError(f"{path}: criterion {name!r} conditions must be an object")
            # These trigger fields are ContextAwarePredicate in 26.3 (one LootItemCondition).
            # Legacy datapacks represented them as condition arrays with implicit AND.
            for predicate_field in ("player", "entity"):
                predicate = conditions.get(predicate_field)
                if isinstance(predicate, list):
                    if not predicate:
                        raise ValueError(f"{path}: criterion {name!r} has empty {predicate_field} conditions")
                    terms = [as_context_predicate(term) for term in predicate]
                    conditions[predicate_field] = terms[0] if len(terms) == 1 else {
                        "condition": "minecraft:all_of", "terms": terms
                    }
                    changed = True
                elif isinstance(predicate, dict):
                    wrapped = as_context_predicate(predicate)
                    if wrapped is not predicate:
                        conditions[predicate_field] = wrapped
                        changed = True
                elif predicate is not None:
                    raise ValueError(f"{path}: criterion {name!r} {predicate_field} condition must be object or legacy list")
            changed |= normalize_loot_condition_tree(conditions) > 0
            for node in iter_dicts(conditions):
                tags = node.get("tags")
                if isinstance(tags, list):
                    for tag in tags:
                        if isinstance(tag, dict) and isinstance(tag.get("id"), str) and not tag["id"].startswith("#"):
                            tag["id"] = "#" + tag["id"]
                            changed = True

    rewards = data.get("rewards")
    if rewards is not None:
        if not isinstance(rewards, dict):
            raise ValueError(f"{path}: rewards must be an object")
        function = rewards.get("function")
        if function is not None:
            if not isinstance(function, str) or not function.startswith(GAIN_PREFIX):
                raise ValueError(f"{path}: refusing to strip non-legacy reward function {function!r}")
            gain = function.removeprefix(GAIN_PREFIX)
            if gain not in GAIN_MEANINGS:
                raise ValueError(f"{path}: no audited meaning recorded for {function!r}")
            advancement_id = "lbc:true_advancements/" + path.relative_to(ADVANCEMENTS).with_suffix("").as_posix()
            gain_counts[gain] += 1
            gain_ids[gain].append(advancement_id)
            del rewards["function"]
            changed = True
        if not rewards:
            del data["rewards"]

    return changed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="report needed changes without writing")
    parser.add_argument("--print-id-map", action="store_true", help="print each stripped advancement ID and legacy gain")
    parser.add_argument("--write-unlock-notifications", action="store_true", help="write native subtitle lookup from read-only source advancement rewards")
    args = parser.parse_args()

    if args.write_unlock_notifications:
        counts = write_unlock_notifications()
        print(f"Wrote {sum(counts.values())} notification mappings to {UNLOCK_NOTIFICATIONS.relative_to(ROOT)}: "
              + ", ".join(f"{key}={counts[key]}" for key in sorted(counts)))
        return 0

    files = checked_files()
    changed_files = 0
    normalized_triggers = 0
    normalized_player_conditions = 0
    normalized_entity_conditions = 0
    normalized_loot_discriminators = 0
    stripped_rewards = 0
    gain_counts: Counter[str] = Counter()
    gain_ids: dict[str, list[str]] = defaultdict(list)

    for path in files:
        original = path.read_text(encoding="utf-8-sig")
        data = json.loads(original)
        bare_impossible = sum(
            criterion.get("trigger") == "impossible" for criterion in data.get("criteria", {}).values()
        )
        before_player_lists = sum(
            isinstance(criterion.get("conditions"), dict)
            and isinstance(criterion["conditions"].get("player"), list)
            for criterion in data.get("criteria", {}).values()
        )
        before_entity_lists = sum(
            isinstance(criterion.get("conditions"), dict)
            and isinstance(criterion["conditions"].get("entity"), list)
            for criterion in data.get("criteria", {}).values()
        )
        before_condition_discriminators = sum(
            count for key, count in Counter(
                str(node.get("condition")) for criterion in data.get("criteria", {}).values()
                for node in iter_dicts(criterion.get("conditions", {})) if isinstance(node.get("condition"), str)
            ).items()
        )
        before_gains = sum(gain_counts.values())
        changed = transform(data, path, gain_counts, gain_ids)
        added_gains = sum(gain_counts.values()) - before_gains
        stripped_rewards += added_gains
        normalized_triggers += bare_impossible
        normalized_player_conditions += before_player_lists
        normalized_entity_conditions += before_entity_lists
        normalized_loot_discriminators += before_condition_discriminators
        if changed:
            changed_files += 1
            if not args.check:
                path.write_text(json.dumps(data, ensure_ascii=False, indent=4) + "\n", encoding="utf-8", newline="\n")

    print(f"Advancements: {len(files)}; files needing normalization: {changed_files}; bare impossible triggers normalized: {normalized_triggers}; legacy player-condition arrays normalized: {normalized_player_conditions}; legacy entity-condition arrays normalized: {normalized_entity_conditions}; legacy loot-condition discriminators normalized: {normalized_loot_discriminators}; legacy function rewards removed: {stripped_rewards}.")
    print("Legacy gain reward summary (see GAIN_MEANINGS for conditional/non-notification behavior):")
    for gain in sorted(gain_counts):
        print(f"  {GAIN_PREFIX}{gain}: {gain_counts[gain]} — {GAIN_MEANINGS[gain]}")
        if args.print_id_map:
            for advancement_id in sorted(gain_ids[gain]):
                print(f"    {advancement_id} -> {gain}")
    if args.check and changed_files:
        print("Run without --check to write only the normalized JSON files.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
