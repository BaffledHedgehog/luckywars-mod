"""Build finite native TMT use inputs; Java native-owner admission decides executable profiles.

Reads only the current project's resources. Never reads/writes the original server or author RP.
No gameplay functions are interpreted. Native defaults are merged later, after registry binding.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TABLES = ROOT / "src/main/resources/data/lbc/loot_table"
OUTPUT = ROOT / "src/main/resources/skywars2/tmt_use_profiles.json"
# Historical gaps can close only after Java admits the corresponding real native
# input with this exact flag. Removing a factory makes the gap visible again.
NATIVE_LEGACY_OWNERS = {"gayradar": "skywars2:gayradar",
                        "steampunk_rocket": "skywars2:steampunk_rocket",
                        "trollge": "skywars2:troll_box"}
PAYLOAD_COMPONENTS = {"minecraft:" + name for name in (
    "entity_data", "bucket_entity_data", "block_entity_data", "block_state",
    "potion_contents", "potion_duration_scale", "fireworks", "container", "container_loot", "blocks_attacks")}


def build():
    cache = {}
    invalid = []

    def read(path):
        if path not in cache:
            try:
                cache[path] = json.loads(path.read_text(encoding="utf-8-sig"))
            except ValueError as error:
                invalid.append({"source": str(path.relative_to(ROOT)).replace("\\", "/"),
                                "error": str(error)})
                cache[path] = {}
        return cache[path]

    def leaves(node, outer=(), references=()):
        if isinstance(node, list):
            for child in node:
                yield from leaves(child, outer, references)
        elif isinstance(node, dict):
            local = []
            for key in ("functions", "modifier"):
                value = node.get(key, [])
                local.extend(value if isinstance(value, list) else [value])
            # Inner referenced loot functions apply BEFORE their caller's modifiers.
            modifiers = tuple(row for row in local if isinstance(row, dict)) + outer
            kind = node.get("type", "")
            if kind in ("minecraft:item", "item"):
                item = node.get("name", "")
                if item.startswith(("skywars2:", "minecraft:")):
                    payloads = []
                    components = {}
                    for row in modifiers:
                        operation = row.get("type", row.get("function", ""))
                        if operation.endswith("set_custom_data"):
                            payloads.append(row["tag"])
                        elif operation.endswith("set_components"):
                            source_components = row.get("components", {})
                            data = source_components.get("minecraft:custom_data")
                            if data is not None:
                                payloads.append(data)
                            for key, value in source_components.items():
                                if key.removeprefix("!") in PAYLOAD_COMPONENTS:
                                    components.pop(key[1:] if key.startswith("!") else "!" + key, None)
                                    components[key] = value
                    # Several finished owners still use custom data on a vanilla
                    # carrier (Big Bob is one). Plain vanilla loot is not an ability.
                    if item.startswith("skywars2:") or payloads:
                        yield item, payloads, components, references
            elif kind in ("minecraft:loot_table", "loot_table"):
                reference = node.get("value", node.get("name"))
                if isinstance(reference, str) and reference.startswith("lbc:") and reference not in references:
                    path = TABLES / (reference[4:] + ".json")
                    if not path.is_file():
                        raise ValueError("Missing canonical reference " + reference)
                    yield from leaves(read(path), modifiers, references + (reference,))
            else:
                for key in ("pools", "entries", "children"):
                    if key in node:
                        yield from leaves(node[key], modifiers, references)

    prototypes = {}
    paths = sorted(TABLES.rglob("*.json"))
    for path in paths:
        relative = str(path.relative_to(TABLES)).replace("\\", "/")
        for item, data, components, references in leaves(read(path)):
            signature = json.dumps([item, data, components], sort_keys=True, ensure_ascii=False)
            row = prototypes.setdefault(signature, {"item": item, "data": data, "components": components, "sources": [], "aliases": []})
            if relative not in row["sources"]:
                row["sources"].append(relative)
            for reference in references:
                if reference not in row["aliases"]:
                    row["aliases"].append(reference)
    audit = json.loads((ROOT / "agent_notes/TMT_CATALOG_2026_11_06.json").read_text(encoding="utf-8"))
    result = {"schema": 1,
              "source": "All current native loot leaves/reference overlays; finalized registered native defaults augment these at bootstrap. Owner predicate admits executable direct-use profiles.",
              "prototypes": list(prototypes.values()),
              "knownLegacyOwnerGaps": [{"key": row["key"], "status": row["status"], "reason": row["reason"],
                                        **({"nativeItem": NATIVE_LEGACY_OWNERS[row["key"]]} if row["key"] in NATIVE_LEGACY_OWNERS else {})}
                                       for row in audit["legacyReconciliation"]
                                       if row["status"] in ("live_legacy_native_owner_gap", "live_legacy_family_no_item_marker_route")],
              "unparsedSourceResources": invalid}
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"resources": len(paths), "canonicalPrototypes": len(prototypes),
                      "unparsedSourceResources": invalid}))


if __name__ == "__main__":
    build()
