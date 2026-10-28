"""Reactive, Techno and MK2 armor and adjacent native regressions in two native matches."""
from pathlib import Path
import argparse
import json
import smoke_armor_sets as native

if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir",type=Path,required=True)
    parser.add_argument("--artifact",type=Path,required=True)
    args=parser.parse_args()
    native.SUITES=("techno-armor","mage-nyauwu","flesh-armor","berserk-armor","glass-armor","nanobot-armor","armor-sets","shulker-armor","incoming-artifacts","attribute-policy","signed-effects-math","effect-layers","shadow-absorption","time-stop")
    try:
        native.run(args.server_dir,artifact=args.artifact,fixture_port=25577)
    finally:
        source=native.cycle.OUT/"armor-sets-gameplay-results.json"
        if source.exists():
            result=json.loads(source.read_text(encoding="utf-8"))
            result["scope"]="native-techno-armor"
            result["measurementLimit"]="Actual three-family acquisition and armor fusion, native technical abilities, fuel/gravity/owned effects, shield and shared incoming/electric arc pipeline, source HUD and adjacent armor/effect regressions. No physical-client or natural TPS claim."
            target=native.cycle.PROJECT/"build/verification/techno-armor/native-results.json"
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
