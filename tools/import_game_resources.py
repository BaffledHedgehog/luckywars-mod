#!/usr/bin/env python3
"""Import static SkyWars map data and loot tables from the legacy server snapshot.

The script deliberately copies only NBT structures and loot-table JSON. It does not
copy legacy functions, tags, advancements, predicates, or other executable datapack
resources into the Fabric mod.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LEGACY = Path(r"E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled")
MAPS = LEGACY / "maps/data/swrg/structure"
FUNCTIONS = LEGACY / "swarogskywars/data/swrg/function/maploader/map"
LOOT = LEGACY / "swarogskywars/data/swrg/loot_table"
DEST_RESOURCES = ROOT / "src/main/resources"
DEST_STRUCTURES = DEST_RESOURCES / "data/swrg/structure"
DEST_LOOT = DEST_RESOURCES / "data/swrg/loot_table"
DEST_MANIFESTS = DEST_RESOURCES / "skywars2/maps"
INVENTORY = ROOT / "agent_notes/inventory/maps_structures.json"
ORDINARY_CHEST_WRAPPERS = {
    "chests/default.json": "default",
    "chests/midle.json": "midle",
    "chests/center.json": "center",
}
LOOT_TEXT_KEYS = {
    "Lucky Block": "skywars2.item.lucky_block.name",
    "Made by: FaffledHedgehog, Scuferis,": "skywars2.item.lucky_block.credits.1",
    "Swagrumm9690_, JastSh2sh": "skywars2.item.lucky_block.credits.2",
}
LOOT_COMPATIBILITY_NOTES = [
    "Replaced legacy swrg:chests/{default,midle,center} gamemode score dispatchers with direct *_0 references. The Minecraft 26.3 loot-condition registry has no minecraft:value_check, and the Fabric game cycle does not provide the legacy #gamemode swrg.math scoreboard; these wrappers now select ordinary SkyWars loot. LBC *_lb tables remain imported but are not selected by this game cycle."
]

PLACEMENT = re.compile(
    r"^place template (?P<id>[a-z0-9_.-]+:[a-z0-9_./-]+) "
    r"(?P<x>-?\d+) (?P<y>-?\d+) (?P<z>-?\d+) "
    r"(?P<rotation>\S+) (?P<mirror>\S+) (?P<integrity>[0-9.]+) "
    r"(?P<seed>-?\d+)(?: (?P<mode>\S+))?$"
)


def copy_tree_files(source: Path, destination: Path, suffix: str,
                    loot_schema_stats: dict[str, int] | None = None) -> int:
    count = 0
    for file in source.rglob(f"*{suffix}"):
        relative = file.relative_to(source)
        target = destination / relative
        # The legacy pack contains this exact zero-byte placeholder. It is not a
        # loot table and Minecraft reports it as malformed JSON during reload.
        if suffix == ".json" and relative.as_posix() == "locked_suffix_tournament.json" and file.stat().st_size == 0:
            print(f"WARN: skipped empty legacy loot table {file}")
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        if suffix == ".json":
            try:
                data = json.loads(file.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError) as exc:
                raise SystemExit(f"Invalid loot JSON {file}: {exc}") from exc
            relative_key = relative.as_posix()
            if relative_key in ORDINARY_CHEST_WRAPPERS:
                category = ORDINARY_CHEST_WRAPPERS[relative_key]
                data = {
                    "pools": [{
                        "rolls": 1,
                        "entries": [{
                            "type": "minecraft:loot_table",
                            "value": f"swrg:chests/{category}_0",
                        }],
                    }]
                }
                if loot_schema_stats is not None:
                    loot_schema_stats["gamemode wrappers simplified"] = (
                        loot_schema_stats.get("gamemode wrappers simplified", 0) + 1
                    )
                print(f"WARN: simplified swrg:chests/{category} to ordinary {category}_0; "
                      "legacy #gamemode score dispatch is unavailable in this Fabric game cycle")
            else:
                migrate_loot_schema(data, loot_schema_stats)
            serialized = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
            if not target.is_file() or target.read_text(encoding="utf-8") != serialized:
                target.write_text(serialized, encoding="utf-8", newline="\n")
        elif not target.is_file() or target.stat().st_size != file.stat().st_size:
            shutil.copy2(file, target)
        count += 1
    return count


def migrate_loot_schema(value: object, stats: dict[str, int] | None = None, *, preserve_imba: bool = False) -> None:
    """Migrate a complete source tree, then apply ordinary authored balance once."""
    from normalize_source_knockback import normalize_source_knockback
    _migrate_loot_schema(value, stats)
    if not preserve_imba:
        normalize_source_knockback(value)


def _migrate_loot_schema(value: object, stats: dict[str, int] | None = None) -> None:
    """Adapt legacy loot JSON discriminators and arrays to Minecraft 26.3 codecs."""
    if isinstance(value, list):
        for element in value:
            _migrate_loot_schema(element, stats)
        return
    if not isinstance(value, dict):
        return

    if value.get("text") in LOOT_TEXT_KEYS:
        value["translate"] = LOOT_TEXT_KEYS[value.pop("text")]
        # A literal component's explicit discriminator cannot accompany translate.
        value.pop("type", None)

    # Recurse through the original tree first. A generated sequence's own
    # `functions` list is the current 26.3 codec shape and must remain intact.
    for child in list(value.values()):
        _migrate_loot_schema(child, stats)

    if "function" in value:
        value["type"] = value.pop("function")
        if stats is not None:
            stats["function discriminators"] = stats.get("function discriminators", 0) + 1
    # Current containers already use condition for inline predicates/references.
    # Rename only a legacy predicate discriminator, retaining current containers.
    condition = value.get("condition")
    legacy_condition = isinstance(condition, str) and (":" not in condition or condition.startswith("minecraft:"))
    if legacy_condition and not any(key in value for key in ("type", "entries", "rolls", "modifier")):
        value["type"] = value.pop("condition")
        if stats is not None:
            stats["condition discriminators"] = stats.get("condition discriminators", 0) + 1

    functions = None if value.get("type") == "minecraft:sequence" else value.pop("functions", None)
    if functions is not None:
        if not isinstance(functions, list) or not functions:
            raise ValueError("loot `functions` must be a non-empty list")
        if "modifier" in value:
            raise ValueError("loot object has both legacy `functions` and current `modifier`")
        value["modifier"] = functions[0] if len(functions) == 1 else functions
        if stats is not None:
            stats["function arrays"] = stats.get("function arrays", 0) + 1

    conditions = value.pop("conditions", None)
    if conditions is not None:
        if not isinstance(conditions, list) or not conditions:
            raise ValueError("loot `conditions` must be a non-empty list")
        if "condition" in value:
            raise ValueError("loot object has both legacy `conditions` and current `condition`")
        value["condition"] = conditions[0] if len(conditions) == 1 else {
            "type": "minecraft:all_of",
            "terms": conditions,
        }
        if stats is not None:
            stats["condition arrays"] = stats.get("condition arrays", 0) + 1


def spawn_counts() -> dict[str, int]:
    if not INVENTORY.is_file():
        return {}
    try:
        inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"WARN: cannot read NBT inventory ({exc}); map player capacities will be 0")
        return {}
    counts: dict[str, int] = {}
    for structure in inventory.get("structures", []):
        if structure.get("group") != "maps":
            continue
        filename = Path(structure.get("source", "")).name
        spawns = 0
        for entity in structure.get("entities", []):
            tags = entity.get("nbt", {}).get("Tags", entity.get("tags", []))
            if "swrg.spawn" in tags:
                spawns += 1
        counts[filename.removesuffix(".nbt")] = spawns
    return counts


def map_tokens() -> list[str]:
    tokens = [str(i) for i in range(1, 65)]
    tokens.extend(f"{i}b" for i in range(1, 26))
    return tokens


def read_manifest(token: str, capacities: dict[str, int]) -> dict:
    placements, omitted, parse_errors = read_placements(token, DEST_STRUCTURES)
    capacity = sum(capacities.get(p["template"].split(":", 1)[1], 0) for p in placements)
    broken_placements, broken_omitted, broken_errors = read_placements(
        f"{token}_broken", DEST_STRUCTURES
    )
    broken_capacity = sum(capacities.get(p["template"].split(":", 1)[1], 0) for p in broken_placements)
    available = bool(placements) and capacity >= 2 and not parse_errors
    map_info = {
        "id": token,
        "name": f"Map {token}",
        "playerCapacity": capacity,
        "available": available,
        "reason": "" if available else "; ".join(
            (["unsupported manifest: " + "; ".join(parse_errors)] if parse_errors else [])
            + (["no existing template placements"] if not placements else [])
            + ([f"fewer than two spawn markers: {capacity}"] if capacity < 2 else [])
        ),
        "placements": placements,
        "omittedPlacements": omitted,
        "parseErrors": parse_errors,
        "brokenPlacements": broken_placements,
        "brokenOmittedPlacements": broken_omitted,
        "brokenParseErrors": broken_errors,
        "brokenPlayerCapacity": broken_capacity,
        "brokenAvailable": available and bool(broken_placements) and not broken_errors
                           and broken_capacity == capacity,
    }
    return map_info


def read_placements(token: str, structures: Path) -> tuple[list[dict], list[str], list[str]]:
    source = FUNCTIONS / f"{token}.mcfunction"
    placements: list[dict] = []
    missing: list[str] = []
    errors: list[str] = []
    if not source.is_file():
        return placements, missing, [f"missing manifest: {source}"]
    for line_number, raw in enumerate(source.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#") or line.startswith("scoreboard "):
            continue
        match = PLACEMENT.match(line)
        if not match:
            errors.append(f"{line_number}: {line}")
            continue
        row = match.groupdict()
        namespace, template_id = row["id"].split(":", 1)
        if namespace != "swrg":
            errors.append(f"{line_number}: unsupported template namespace: {row['id']}")
            continue
        # Author-confirmed unused Glekocode lines: a failed source /place is a
        # no-op and subsequent existing template commands still execute.
        if not (structures / f"{template_id}.nbt").is_file():
            missing.append(row["id"])
            continue
        placements.append({
            "template": row["id"],
            "origin": [int(row["x"]), int(row["y"]), int(row["z"])],
            "rotation": row["rotation"],
            "mirror": row["mirror"],
            "integrity": float(row["integrity"]),
            "seed": int(row["seed"]),
            "mode": row["mode"] or "strict",
        })
    return placements, missing, errors


def diagnose_loot() -> list[str]:
    issues = []
    ref = re.compile(r'"(?:value|name)"\s*:\s*"swrg:([a-z0-9_./-]+)"')
    for path in DEST_LOOT.rglob("*.json"):
        try:
            body = path.read_text(encoding="utf-8")
        except OSError as exc:
            issues.append(f"unreadable {path.relative_to(DEST_RESOURCES)}: {exc}")
            continue
        for match in ref.finditer(body):
            target = DEST_LOOT / f"{match.group(1)}.json"
            if not target.is_file():
                issues.append(
                    f"{path.relative_to(DEST_RESOURCES)} references missing swrg:{match.group(1)}"
                )
    return issues


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maps-only", action="store_true",
                        help="refresh map manifests/catalog/diagnostics without copying NBT or loot")
    args = parser.parse_args()
    if not FUNCTIONS.is_dir() or (not args.maps_only and (not MAPS.is_dir() or not LOOT.is_dir())):
        raise SystemExit(f"Legacy datapack source directories not found under {LEGACY}")
    if args.maps_only and not DEST_STRUCTURES.is_dir():
        raise SystemExit(f"Imported structures required for --maps-only: {DEST_STRUCTURES}")
    DEST_MANIFESTS.mkdir(parents=True, exist_ok=True)

    nbt_count = loot_count = 0
    loot_schema_stats: dict[str, int] = {}
    if not args.maps_only:
        DEST_STRUCTURES.mkdir(parents=True, exist_ok=True)
        DEST_LOOT.mkdir(parents=True, exist_ok=True)
        nbt_count = copy_tree_files(MAPS, DEST_STRUCTURES, ".nbt")
        loot_count = copy_tree_files(LOOT, DEST_LOOT, ".json", loot_schema_stats)
        empty_legacy_loot = LOOT / "locked_suffix_tournament.json"
        skipped_target = DEST_LOOT / "locked_suffix_tournament.json"
        if empty_legacy_loot.is_file() and empty_legacy_loot.stat().st_size == 0 and skipped_target.is_file() and skipped_target.stat().st_size == 0:
            skipped_target.unlink()
            print(f"Removed stale empty imported loot table {skipped_target}")

    capacities = spawn_counts()
    manifests = [read_manifest(token, capacities) for token in map_tokens()]
    for manifest in manifests:
        (DEST_MANIFESTS / f"{manifest['id']}.json").write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n"
        )
    (DEST_MANIFESTS / "catalog.json").write_text(
        json.dumps({"maps": manifests}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n"
    )

    diagnostic_path = DEST_MANIFESTS / "resource_diagnostics.json"
    diagnostics = json.loads(diagnostic_path.read_text(encoding="utf-8")) if args.maps_only and diagnostic_path.is_file() else {}
    if not args.maps_only:
        diagnostics.update(unresolvedLootReferences=diagnose_loot(), lootCompatibility=LOOT_COMPATIBILITY_NOTES)
    map_diagnostics = [
        f"map {entry['id']} unavailable: {entry['reason']}"
        for entry in manifests if not entry["available"]
    ]
    diagnostics.update(unavailableMaps=map_diagnostics,
                       omittedMapPlacements={entry["id"]: {
                           "normal": entry["omittedPlacements"], "broken": entry["brokenOmittedPlacements"]
                       } for entry in manifests if entry["omittedPlacements"] or entry["brokenOmittedPlacements"]})
    diagnostic_path.write_text(
        json.dumps(diagnostics, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8", newline="\n",
    )
    available = [m["id"] for m in manifests if m["available"]]
    if not args.maps_only:
        print(f"Imported {nbt_count} NBT structures, {loot_count} swrg loot tables")
        print("Migrated loot schema fields: " + ", ".join(
            f"{name}={amount}" for name, amount in sorted(loot_schema_stats.items())
        ))
    print(f"Available maps ({len(available)}): {', '.join(available)}")
    print(f"Unresolved loot references: {len(diagnostics.get('unresolvedLootReferences', []))}; unavailable maps: {len(map_diagnostics)}")
    print(f"Omitted unused source references: {sum(len(m['omittedPlacements']) + len(m['brokenOmittedPlacements']) for m in manifests)}")


if __name__ == "__main__":
    main()
