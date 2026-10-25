#!/usr/bin/env python3
"""Import only direct Danmaku grimoire159, preserving authored presentation."""
from copy import deepcopy
from import_lbc_firearms import migrated, verify_model
from import_lbc_loadouts import ROOT
from import_lbc_shooter_pair import translate_keys
from import_lbc_wands import build_single_entry_table, write_table
from import_lbc_woodificator import RP, read_json

def main():
    source=migrated("spells_library")
    candidates=[(pool,entry) for pool in source["pools"] for entry in pool["entries"]
                if any(fn.get("type")=="minecraft:set_custom_data" and "spellid:159," in fn.get("tag","")
                       for fn in entry.get("modifier",[]))]
    assert len(candidates)==1,len(candidates)
    pool,original=candidates[0]
    entry=deepcopy(original)
    assert entry["name"]=="minecraft:snowball"
    entry["name"]="skywars2:danmaku_grimoire"
    result=build_single_entry_table(source,pool,entry)
    restored=deepcopy(result);restored["pools"][0]["entries"][0]["name"]="minecraft:snowball"
    assert restored==build_single_entry_table(source,pool,original)
    for language in ("ru_ru","en_us","rpr"):
        translations=read_json(RP/f"assets/minecraft/lang/{language}.json")
        assert not set(translate_keys([result]))-translations.keys()
    verify_model("lbc:spelllib/danmaku",item_definition=True)
    destination=ROOT/"src/main/resources/data/lbc/loot_table/ported/danmaku_grimoire.json"
    write_table(destination,result)
    assert read_json(destination)==result
    print("Imported direct159 only; exact source except native item ID. RP is read-only.")

if __name__=="__main__":main()
