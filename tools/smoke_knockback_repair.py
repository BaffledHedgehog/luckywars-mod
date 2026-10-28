"""Signed look-directed melee and migrated knockback balance in two native matches."""
from pathlib import Path
import argparse
import json

import smoke_armor_sets as native


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--artifact", type=Path, required=True)
    args = parser.parse_args()
    native.EXTRA_FIXTURE_PLAYERS = ("TestKnockback",)
    native.SUITES = (
        "attack-knockback", "source-knockback-balance", "attribute-policy",
        "negative-attributes", "enchantment-rules", "creature-drops",
        "encounter-eggs", "special-eggs", "native-gear", "classic-melee", "player-rules",
        "friendly-projectiles", "time-stop",
    )
    # A retained probe freezes the world until match stop; run only the changed
    # creature source's probe here, so independent retained fixtures cannot collide.
    native.PROBES = ("creature-drops-stop-probe",)
    native.CLEANUP = ("creature-drops-cleanup",)
    try:
        native.run(args.server_dir, artifact=args.artifact)
    finally:
        source = native.cycle.OUT / "armor-sets-gameplay-results.json"
        if source.exists():
            result = json.loads(source.read_text(encoding="utf-8"))
            result["scope"] = "native-mode1-directional-knockback-and-source-balance"
            result["measurementLimit"] = (
                "Native melee, actual Carpet players, outgoing player motion and source loot/mob balancing; "
                "attributes, unrestricted enchantments, role/immunity and Time Stop regressions. "
                "No physical-client observation or peakTPS measurement."
            )
            target = native.cycle.PROJECT / "build/verification/knockback-repair/native-results.json"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
