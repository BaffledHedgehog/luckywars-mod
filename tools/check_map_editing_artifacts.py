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
REQUIRED = {
    "me/skywars2/game/MapAuthoring.class",
    "me/skywars2/game/MapDeletion.class",
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


def verify(production: Path, test_server: Path) -> dict[str, object]:
    require(production.resolve() != test_server.resolve(), "Two distinct artifacts are required")
    normal = entries(production)
    testing = entries(test_server)
    forbidden = sorted(name for name in normal if AUTHORING.fullmatch(name))
    require(not forbidden, f"Production contains map editing classes: {forbidden}")
    authoring = {name for name in testing if AUTHORING.fullmatch(name)}
    require(REQUIRED <= authoring, f"Test server is missing authoring implementations: {sorted(REQUIRED - authoring)}")
    require(set(normal) <= set(testing), f"Test server dropped production entries: {sorted(set(normal) - set(testing))}")
    extras = set(testing) - set(normal)
    require(extras == authoring, f"Unexpected test-only entries: {sorted(extras - authoring)}")
    changed = [name for name, value in normal.items() if testing[name] != value]
    require(not changed, f"Shared production entries changed bytes: {sorted(changed)}")
    require("fabric.mod.json" in normal and "META-INF/MANIFEST.MF" in normal, "Missing Fabric metadata/manifest")
    metadata = json.loads(normal["fabric.mod.json"])
    nested = [entry["file"] for entry in metadata.get("jars", [])]
    require(bool(nested), "Production Fabric metadata has no bundled Jar-in-Jar dependencies")
    for name in nested:
        require(name in normal and name in testing, f"Missing declared bundled dependency: {name}")
        require(name.endswith(".jar"), f"Unexpected bundled dependency path: {name}")
    return {
        "production": str(production.resolve()),
        "productionSha256": hashlib.sha256(production.read_bytes()).hexdigest(),
        "testServer": str(test_server.resolve()),
        "testServerSha256": hashlib.sha256(test_server.read_bytes()).hexdigest(),
        "sharedEntriesByteIdentical": len(normal),
        "testOnlyAuthoringClasses": sorted(authoring),
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
