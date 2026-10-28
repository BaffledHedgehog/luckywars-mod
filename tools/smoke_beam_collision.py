"""Exact laser collision and neighboring ray services in two isolated native matches."""
from pathlib import Path
import argparse
import json

import smoke_armor_sets as native


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    args = parser.parse_args()
    native.SUITES = (
        "gaycaster", "god-hedgehog", "prism", "minigun-visual", "rifle-main-hand",
        "sauvojen-meditation", "caustic-omega", "phantom-armor", "time-stop", "projectile-self-hits",
    )
    native.PROBES = ("gaycaster-stop-probe", "god-hedgehog-stop-probe")
    native.CLEANUP = ("gaycaster-cleanup", "god-hedgehog-cleanup")
    try:
        native.run(args.server_dir, artifact=args.artifact)
    finally:
        source = native.cycle.OUT / "armor-sets-gameplay-results.json"
        if source.exists():
            result = json.loads(source.read_text(encoding="utf-8"))
            result["scope"] = "native-mode1-beam-collision"
            result["measurementLimit"] = (
                "Exact native player hitboxes against the Gaycaster meditation prism; God Hedgehog swept ray; "
                "shared resident rays, warning lifecycle, Time Stop and neighboring combat regressions. "
                "No physical-client rendering or peakTPS measurement."
            )
            target = native.cycle.PROJECT / "build/verification/beam-collision/native-results.json"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
