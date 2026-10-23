"""Extract only Blood/Confuse families, preserving source presentation and Academy modifiers."""
from pathlib import Path
import json
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family


def main():
    dest = ROOT / "src/main/resources/data/lbc/loot_table/ported"
    for name, flag in (("blood_wand", "bloodmagic"), ("confuse_wand", "foxusing")):
        import_wand_family(SOURCE / "loot_table/transformables_staff.json",
                          dest / f"{name}.json", dest / f"academy_{name}.json",
                          name, f"skywars2:{name}", f"skywars2:academy_{name}",
                          academy_modifiers=("add_upgrade_desc", "tiers/a"),
                          data_marker=flag + ":1")
    # Read-only author pack audit; the pack itself is never modified.
    rp = Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8")
    keys = {"mode_invisibility", "mode_teleport", "mode_speed", "mode_levitate",
            "player_blooding", "player_blinded", "player_stopped", "player_teleported",
            "upgrade_condition_bloodmagic", "upgrade_succesful_bloodmagic",
            "upgrade_condition_foxusing", "upgrade_succesful_foxusing"}
    def scan(value):
        if isinstance(value, list):
            for child in value: scan(child)
        elif isinstance(value, dict):
            if "translate" in value: keys.add(value["translate"])
            for child in value.values(): scan(child)
    for name in ("blood_wand", "confuse_wand", "academy_blood_wand", "academy_confuse_wand"):
        scan(json.loads((dest / f"{name}.json").read_text(encoding="utf-8")))
    for lang in ("ru_ru", "en_us"):
        data = json.loads((rp / f"assets/minecraft/lang/{lang}.json").read_text(encoding="utf-8-sig"))
        assert not keys - data.keys(), (lang, keys - data.keys())
    for model in ("killingwand", "foxuswand"):
        assert (rp / f"assets/lbc/items/{model}.json").is_file(), model
    print(f"Four complete leaves; {len(keys)} existing RU/EN keys. No RP writes.")


if __name__ == "__main__":
    main()
