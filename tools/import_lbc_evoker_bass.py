"""Extract only Evoker/Bass families, preserving source presentation and Academy modifiers."""
from pathlib import Path
import json
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family


def main():
    dest = ROOT / "src/main/resources/data/lbc/loot_table/ported"
    for name, flag in (("evoker_wand", "evocator"), ("bass_wand", "bassboost")):
        import_wand_family(SOURCE / "loot_table/transformables_staff.json",
                          dest / f"{name}.json", dest / f"academy_{name}.json",
                          name, f"skywars2:{name}", f"skywars2:academy_{name}",
                          academy_modifiers=("add_upgrade_desc", "tiers/b"),
                          data_marker=flag + ":1")
    # Read-only author pack audit; the pack itself is never modified.
    rp = Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8")
    keys = {"formation", "player_stunned", "effect_weakness", "effect_slowness",
            "effect_blindness", "effect_mining_fatigue", "upgrade_condition_evocator",
            "upgrade_succesful_evocator", "upgrade_condition_bassboost", "upgrade_succesful_bassboost"}
    def scan(value):
        if isinstance(value, list):
            for child in value: scan(child)
        elif isinstance(value, dict):
            if "translate" in value: keys.add(value["translate"])
            for child in value.values(): scan(child)
    for name in ("evoker_wand", "bass_wand", "academy_evoker_wand", "academy_bass_wand"):
        scan(json.loads((dest / f"{name}.json").read_text(encoding="utf-8")))
    for lang in ("ru_ru", "en_us"):
        data = json.loads((rp / f"assets/minecraft/lang/{lang}.json").read_text(encoding="utf-8-sig"))
        assert not keys - data.keys(), (lang, keys - data.keys())
    for model in ("evokerwand", "basswand"):
        assert (rp / f"assets/lbc/items/{model}.json").is_file(), model
    print(f"Four complete leaves; {len(keys)} existing RU/EN keys. No RP writes.")


if __name__ == "__main__":
    main()
