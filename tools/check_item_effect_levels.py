"""Check the item-grant boundary without changing loot, generated files or author overrides."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "src/main/java/me/skywars2/game"


def main():
    factories = 0
    snapshots = []
    errors = []
    for path in sorted(SOURCES.glob("Lbc*.java")):
        if path.name.endswith("Checks.java"):
            continue
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            factories += line.count("ItemEffectLevels.create(")
            if not re.search(r"new\s+MobEffectInstance\s*\(", line):
                continue
            # Stun snapshots must restore explicit administrator effects exactly, not issue a new item buff.
            if path.name == "LbcShooters.java" and ("new MobEffectInstance(before)" in line
                    or "new MobEffectInstance(entry.getKey(),remaining,previous.getAmplifier()" in line):
                snapshots.append(f"{path.name}:{number}")
            elif path.name == "LbcIndirectKills.java" and "new MobEffectInstance(effect.getEffect(),duration+1,amplifier," in line:
                snapshots.append(f"{path.name}:{number}: duration-only extension")
            else:
                errors.append(f"{path.name}:{number}: item grants must use ItemEffectLevels.create")
    policy = (SOURCES / "ItemEffectLevels.java").read_text(encoding="utf-8")
    for effect in ("REGENERATION", "POISON", "WITHER", "RESISTANCE", "BLINDNESS", "DARKNESS"):
        if f"MobEffects.{effect}" not in policy:
            errors.append(f"Missing item policy for {effect}")
    hooks = (ROOT / "src/main/java/me/skywars2/mixin/ItemEffectLevelsMixin.java").read_text(encoding="utf-8")
    for native in ("PotionContents.class", "ApplyStatusEffectsConsumeEffect.class"):
        if native not in hooks:
            errors.append(f"Missing native item path {native}")
    result = {"success": not errors, "grantFactoryCalls": factories,
              "exactStatusSnapshots": snapshots, "errors": errors,
              "scope": "Production Lbc item factories and native potion/consume boundaries; read only"}
    print(json.dumps(result, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
