"""Vanilla scale bounds and adjacent signed-attribute regressions in two native matches."""
from pathlib import Path
import argparse
import json
import smoke_armor_sets as native

if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir",type=Path,required=True)
    parser.add_argument("--artifact",type=Path,required=True)
    args=parser.parse_args()
    native.SUITES=("attribute-policy","negative-attributes","signed-effects-math","effect-layers")
    try:
        native.run(args.server_dir,artifact=args.artifact,fixture_port=25577)
    finally:
        source=native.cycle.OUT/"armor-sets-gameplay-results.json"
        if source.exists():
            result=json.loads(source.read_text(encoding="utf-8"))
            result["scope"]="native-scale-vanilla-bounds"
            result["measurementLimit"]="Live registry, native attribute instances/dimensions and modifier limits; no physical-client rendering claim."
            target=native.cycle.PROJECT/"build/verification/scale-vanilla/native-results.json"
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
