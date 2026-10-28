"""Complete Nanobots armor and neighboring equipment in two actual native matches."""
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
        "nanobot-armor", "armor-sets", "assassin-armor", "phantom-armor",
        "shulker-armor", "effect-layers", "shared-status-hud", "kfc-food",
        "player-rules", "attack-knockback", "time-stop",
    )
    native.EXTRA_FIXTURE_PLAYERS = ("TestKnockback",)
    native.PROBES = ("nanobot-armor-stop-probe",)
    native.CLEANUP = ("nanobot-armor-cleanup",)
    try:
        native.run(args.server_dir, artifact=args.artifact)
    finally:
        source = native.cycle.OUT / "armor-sets-gameplay-results.json"
        if source.exists():
            result = json.loads(source.read_text(encoding="utf-8"))
            result["scope"] = "native-mode1-complete-nanobot-armor"
            result["measurementLimit"] = (
                "Original loot/reforge and imported PvP criterion dispatch, Nanobots charging/damage/HUD, "
                "TECH-only suppression and real 200-tick scheduling with native player connections. "
                "No new fatal PvP kill, physical-client rendering or peakTPS claim."
            )
            target = native.cycle.PROJECT / "build/verification/nanobot-armor/native-results.json"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
