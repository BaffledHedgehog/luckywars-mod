"""Check native damage-type parity for the locally modified yellow katana damage aliases."""
from pathlib import Path
import argparse
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ALIASES = {"yellow_bolt": "arrow", "yellow_spin": "generic", "yellow_conduction": "cactus"}


def read_tags(archive):
    prefix = "data/minecraft/tags/damage_type/"
    return {Path(name).stem: json.loads(archive.read(name)) for name in archive.namelist()
            if name.startswith(prefix) and name.endswith(".json")}


def contains(tags, name, damage, visited=frozenset()):
    if name in visited:
        raise AssertionError(f"Cyclic damage tag: {name}")
    for value in tags.get(name, {}).get("values", []):
        value = value if isinstance(value, str) else value["id"]
        if value == damage or (value.startswith("#minecraft:") and
                               contains(tags, value.split(":", 1)[1], damage, visited | {name})):
            return True
    return False


def check(artifact):
    with zipfile.ZipFile(ROOT.parent / "versions/26.3/server-26.3.jar") as vanilla, \
            zipfile.ZipFile(artifact) as mod:
        tags = read_tags(vanilla)
        merged = {key: {"values": list(value["values"])} for key, value in tags.items()}
        for key, value in read_tags(mod).items():
            old = [] if value.get("replace", False) else merged.get(key, {}).get("values", [])
            merged[key] = {"values": old + value["values"]}
        report = {}
        for alias, base in ALIASES.items():
            native = json.loads(vanilla.read(f"data/minecraft/damage_type/{base}.json"))
            custom = json.loads(mod.read(f"data/skywars2/damage_type/{alias}.json"))
            assert native == custom, f"Damage fields differ: {alias}: {custom} != {native}"
            expected = {key for key in tags if contains(tags, key, "minecraft:" + base)}
            if base in {"arrow", "generic"}:
                expected.add("bypasses_cooldown")  # Legacy LBC changed these two vanilla types globally.
            actual = {key for key in merged if contains(merged, key, "skywars2:" + alias)}
            if base == "cactus":
                expected.add("cactus_only")
            assert actual == expected, f"Damage tags differ: {alias}: extra={actual-expected}, missing={expected-actual}"
            report[alias] = sorted(actual)
        print("PASS damage aliases: vanilla fields, armor/shield/knockback, projectile and cooldown tags")
        return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, default=ROOT / "build/libs/skywars2-0.1.0.jar")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    result = check(args.jar)
    if args.report:
        args.report.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
