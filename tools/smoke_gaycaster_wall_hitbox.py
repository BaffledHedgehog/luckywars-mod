"""Contact geometry, Radiant Metasola, direct melee and native Ultraponos in two matches."""
from pathlib import Path
import argparse
import json
import smoke_armor_sets as native

if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir",type=Path,required=True)
    parser.add_argument("--artifact",type=Path,required=True)
    args=parser.parse_args()
    native.SUITES=("attribute-policy","babylon","gaycaster","void-arcanum","projectile-body-collision","projectile-self-hits","soulhunter","radiant-metasola","effect-layers","metal-pipe","ultraponos-drops","creeper-drops")
    native.PROBES=("ultraponos-drops-stop-probe",)
    native.CLEANUP=("ultraponos-drops-cleanup",)
    try:
        native.run(args.server_dir,artifact=args.artifact,fixture_port=25577)
    finally:
        source=native.cycle.OUT/"armor-sets-gameplay-results.json"
        if source.exists():
            result=json.loads(source.read_text(encoding="utf-8"))
            result["scope"]="native-contact-radiant-melee-and-ultraponos"
            result["measurementLimit"]="Two normal native matches: exact walls/contact pushes, full strict Radiant pattern/persistence, direct versus attributed melee, native Ultraponos290tick expiry/Time Stop/WILD/TNT lineage and actual lightning charging; no physical-client rendering or natural TPS claim."
            target=native.cycle.PROJECT/"build/verification/gaycaster-wall-hitbox/native-results.json"
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
