"""Negative-glow invisibility and no-break mining prediction in two native matches."""
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
        "negative-mining", "signed-visuals", "full-invisibility", "tps-compensation",
        "attribute-policy", "signed-effects-math", "effect-layers", "phantom-armor",
        "rifle-zoom", "speed-fov-transitions", "rifle-main-hand", "time-stop",
    )
    native.PROBES = ("rifle-zoom-stop-probe",)
    native.CLEANUP = ("rifle-zoom-cleanup", "signed-effects-cleanup")
    try:
        native.run(args.server_dir, artifact=args.artifact)
    finally:
        source = native.cycle.OUT / "armor-sets-gameplay-results.json"
        if source.exists():
            result = json.loads(source.read_text(encoding="utf-8"))
            result["scope"] = "native-mode1-signed-glow-mining"
            result["measurementLimit"] = (
                "Native signed-glow visibility/proxy/metadata and mining packet/raycast/clone paths; "
                "effect layers, attributes, TPS compensation, scope/FOV and Time Stop regressions. "
                "No physical-client rendering, real network latency or peakTPS measurement."
            )
            target = native.cycle.PROJECT / "build/verification/signed-glow-mining/native-results.json"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
