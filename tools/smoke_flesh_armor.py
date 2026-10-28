"""Flesh armor and adjacent native hurt/armor regressions in two native matches."""
from pathlib import Path
import argparse
import json
import smoke_armor_sets as native

if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir",type=Path,required=True)
    parser.add_argument("--artifact",type=Path,required=True)
    args=parser.parse_args()
    native.SUITES=("flesh-armor","berserk-armor","glass-armor","nanobot-armor","armor-sets","shulker-armor","incoming-artifacts","attribute-policy","signed-effects-math","effect-layers","shadow-absorption","time-stop")
    try:
        native.run(args.server_dir,artifact=args.artifact,fixture_port=25577)
    finally:
        source=native.cycle.OUT/"armor-sets-gameplay-results.json"
        if source.exists():
            result=json.loads(source.read_text(encoding="utf-8"))
            result["scope"]="native-flesh-armor"
            result["measurementLimit"]="Actual Flesh source acquisition, native equipment max-health, atomic x2 migration, native save/load HP preservation, independent attribute/shadow-effect coexistence and physical jammer policy and adjacent shared hurt/armor/effect regressions; no physical-client or natural TPS claim."
            target=native.cycle.PROJECT/"build/verification/flesh-armor/native-results.json"
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
