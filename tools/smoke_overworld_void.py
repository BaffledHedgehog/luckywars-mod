"""Actual Overworld lobby/matches/respawn plus dimension-sensitive gameplay regressions."""
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
        "map-world", "map-lighting", "dimension-items", "hedgehog-dimensions",
        "duplicating-academy", "duplicating-expand", "nuclear-visual", "visual-reload",
        "time-stop-schedules", "time-stop", "negative-mining", "signed-visuals",
    )
    native.PROBES = ()
    native.CLEANUP = ("map-world",)
    native.LOBBY_SUITES = ("map-world", "map-streaming", "lobby-rules")
    native.LIVES_SCENARIO = True
    native.FIXTURE_LIVES = 2
    try:
        native.run(args.server_dir, artifact=args.artifact, fixture_port=25577)
    finally:
        source = native.cycle.OUT / "armor-sets-gameplay-results.json"
        if source.exists():
            result = json.loads(source.read_text(encoding="utf-8"))
            result["scope"] = "native-overworld-void-generation"
            result["measurementLimit"] = (
                "Two actual native Overworld qualification matches and one real remaining-life respawn/elimination scenario; "
                "native full chunk packets during actual60->61->1 map changes, lighting, dimension-sensitive effects, visuals and cleanup. No imported live legacy-world write or physical-client claim."
            )
            target = native.cycle.PROJECT / "build/verification/overworld-void/native-results.json"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
