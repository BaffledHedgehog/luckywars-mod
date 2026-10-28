"""Nuclear mushroom twenty-second fade and shared producer regressions in two native matches."""
from pathlib import Path
import argparse
import json
import smoke_armor_sets as native

if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir",type=Path,required=True)
    parser.add_argument("--artifact",type=Path,required=True)
    args=parser.parse_args()
    native.SUITES=("hedgehog","nuclear-visual","visual-reload","exoperso","exoperso-blast","exasperato-academy","time-stop")
    try:
        native.run(args.server_dir,artifact=args.artifact,fixture_port=25577)
    finally:
        source=native.cycle.OUT/"armor-sets-gameplay-results.json"
        if source.exists():
            result=json.loads(source.read_text(encoding="utf-8"))
            result["scope"]="native-nuclear-fade-20s"
            result["measurementLimit"]="Native carrier metadata, 420tick retention, slot handoff/quarantine, freeze/Stand and shared Exoperso/Exasperato/Hedgehog producers; explicit lifecycle boundary ticks, no physical GPU/camera/pixel or natural TPS claim."
            target=native.cycle.PROJECT/"build/verification/nuclear-fade-20s/native-results.json"
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
