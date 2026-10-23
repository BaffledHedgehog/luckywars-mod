"""Extract complete Neutral/Academy/Feather leaves and exact native projectile payloads."""
from pathlib import Path
import json
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family, write_table
from import_game_resources import migrate_loot_schema

def main():
    dest=ROOT/"src/main/resources/data/lbc/loot_table/ported"
    import_wand_family(SOURCE/"loot_table/transformables_staff.json",dest/"neutral_wand.json",dest/"academy_neutral_wand.json",
                      "neutral_wand","skywars2:neutral_wand","skywars2:academy_neutral_wand",
                      academy_modifiers=("add_upgrade_desc","tiers/a"),data_marker="neutralmage:1")
    payload=json.loads((SOURCE/"loot_table/feather_greed.json").read_text(encoding="utf-8-sig"))
    migrate_loot_schema(payload)
    payload["pools"][0]["entries"][0]["name"]="skywars2:feather_greed"
    write_table(dest/"feather_greed.json",payload)
    functions=SOURCE/"function/workingitems/neutralmage"
    rockets=[line.split(" run summon minecraft:firework_rocket ~ ~ ~ ",1)[1] for line in (functions/"4.mcfunction").read_text(encoding="utf-8-sig").splitlines() if " run summon minecraft:firework_rocket ~ ~ ~ " in line]
    potions=[line.split(" run summon minecraft:splash_potion ~ ~ ~ ",1)[1] for line in (functions/"5.mcfunction").read_text(encoding="utf-8-sig").splitlines() if " run summon minecraft:splash_potion ~ ~ ~ " in line]
    assert len(rockets)==3 and len(potions)==3 and potions[0]==potions[1]==potions[2]
    out=ROOT/"src/main/resources/skywars2/neutral_wand_presets.json"
    out.write_text(json.dumps({"rockets":rockets,"potion":potions[0]},indent=2)+"\n",encoding="utf-8")
    rp=Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8")
    keys={"cast_wither_skulls","cast_small_fireballs","cast_big_fireball","cast_fireworks","cast_potions","upgrade_condition_neutralmage","upgrade_succesful_neutralmage"}
    def scan(v):
        if isinstance(v,list):
            for c in v:scan(c)
        elif isinstance(v,dict):
            if "translate" in v:keys.add(v["translate"])
            for c in v.values():scan(c)
    for name in ["neutral_wand","academy_neutral_wand","feather_greed"]:scan(json.loads((dest/(name+".json")).read_text(encoding="utf-8")))
    for lang in ["ru_ru","en_us"]:
        data=json.loads((rp/f"assets/minecraft/lang/{lang}.json").read_text(encoding="utf-8-sig"));assert not keys-data.keys(),(lang,keys-data.keys())
    assert (rp/"assets/lbc/items/neutralwand.json").is_file()
    print(f"Three source-complete leaves; three rocket/one potion presets; {len(keys)} existing RU/EN keys. No RP writes.")

if __name__=="__main__":main()
