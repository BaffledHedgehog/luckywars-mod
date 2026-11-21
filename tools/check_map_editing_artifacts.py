#!/usr/bin/env python3
"""Verify the physical production/test-server split, including bundled Fabric dependencies.

Usage: python tools/check_map_editing_artifacts.py PRODUCTION.jar TEST-SERVER.jar
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile


AUTHORING = re.compile(r"me/skywars2/game/(?:MapAuthoring|MapDeletion)[^/]*\.class\Z")
CHECKS = re.compile(
    r"me/skywars2/(?:[^/]+/)*(?:[^/]*Checks|Vanity[^/]*Scene|NativeCheckBackend)"
    r"(?:\$[^/]*)?\.class\Z"
)
VALIDATION_CONFIG = "skywars2.validation.mixins.json"
# This fixture helper intentionally has no command/scenario of its own.
FIXTURE_HELPERS = {"me/skywars2/game/items/phone/TikTokCheckFields.class"}
TEST_ENTRYPOINTS = {
    "skywars2:map-editing": ["me.skywars2.game.MapAuthoringBackend"],
    "skywars2:test-checks": ["me.skywars2.game.NativeCheckBackend"],
}
REQUIRED_CLASSES = {
    "me/skywars2/game/MapAuthoring.class",
    "me/skywars2/game/MapDeletion.class",
    "me/skywars2/game/GameChecks.class",
    *(entry.replace(".", "/") + ".class" for entries in TEST_ENTRYPOINTS.values() for entry in entries),
}


def require(value: bool, message: str) -> None:
    if not value:
        raise ValueError(message)


def entries(path: Path) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as archive:
        rows = [row for row in archive.infolist() if not row.is_dir()]
        names = [row.filename for row in rows]
        require(len(names) == len(set(names)), f"Duplicate archive entries: {path}")
        require(archive.testzip() is None, f"Corrupt archive entry: {path}")
        return {row.filename: archive.read(row) for row in rows}


def test_only(name: str) -> bool:
    return (name == VALIDATION_CONFIG or name in FIXTURE_HELPERS
            or (name.startswith("me/skywars2/validation/") and name.endswith(".class"))
            or AUTHORING.fullmatch(name) is not None
            or CHECKS.fullmatch(name) is not None)


def verify(production: Path, test_server: Path) -> dict[str, object]:
    require(production.resolve() != test_server.resolve(), "Two distinct artifacts are required")
    normal = entries(production)
    testing = entries(test_server)
    forbidden = sorted(name for name in normal if test_only(name))
    require(not forbidden, f"Production contains test-only entries: {forbidden}")
    authoring = {name for name in testing if AUTHORING.fullmatch(name)}
    required = REQUIRED_CLASSES | {VALIDATION_CONFIG}
    require(required <= set(testing), f"Missing test-server entries: {sorted(required - set(testing))}")
    require(set(normal) <= set(testing), f"Test server dropped production entries: {sorted(set(normal) - set(testing))}")
    extras = set(testing) - set(normal)
    unexpected = sorted(name for name in extras if not test_only(name))
    require(not unexpected, f"Unexpected test-only entries: {unexpected}")
    changed = [name for name, value in normal.items() if name != "fabric.mod.json" and testing[name] != value]
    require(not changed, f"Shared production entries changed bytes: {sorted(changed)}")
    require("fabric.mod.json" in normal and "META-INF/MANIFEST.MF" in normal, "Missing Fabric metadata/manifest")
    metadata = json.loads(normal["fabric.mod.json"])
    entrypoints = metadata.setdefault("entrypoints", {})
    require(not set(TEST_ENTRYPOINTS) & entrypoints.keys(), "Production exposes test entrypoints")
    mixins = metadata.setdefault("mixins", [])
    require(VALIDATION_CONFIG not in mixins, "Production registers the validation mixin config")
    nested = [entry["file"] for entry in metadata.get("jars", [])]
    require(bool(nested), "Production Fabric metadata has no bundled Jar-in-Jar dependencies")
    for name in nested:
        require(name in normal and name in testing, f"Missing declared bundled dependency: {name}")
        require(name.endswith(".jar"), f"Unexpected bundled dependency path: {name}")
    entrypoints.update(TEST_ENTRYPOINTS)
    mixins.append(VALIDATION_CONFIG)
    require(json.loads(testing["fabric.mod.json"]) == metadata,
            "Test Fabric metadata must add only the check entrypoints and validation mixins")
    validation = json.loads(testing[VALIDATION_CONFIG])
    prefix = validation["package"].replace(".", "/") + "/"
    for environment in ("mixins", "server", "client"):
        for mixin in validation.get(environment, []):
            require(prefix + mixin.replace(".", "/") + ".class" in testing,
                    f"Missing validation mixin class: {mixin}")
    return {
        "production": str(production.resolve()),
        "productionSha256": hashlib.sha256(production.read_bytes()).hexdigest(),
        "testServer": str(test_server.resolve()),
        "testServerSha256": hashlib.sha256(test_server.read_bytes()).hexdigest(),
        "sharedEntriesByteIdentical": len(normal) - 1,
        "testOnlyAuthoringClasses": sorted(authoring),
        "testOnlyEntries": len(extras),
        "bundledDependenciesRetained": nested,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("production", type=Path)
    parser.add_argument("test_server", type=Path)
    args = parser.parse_args()
    try:
        result = verify(args.production, args.test_server)
    except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
        parser.exit(1, f"Artifact split check failed: {error}\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
