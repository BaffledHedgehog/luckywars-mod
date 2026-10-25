#!/usr/bin/env python3
"""Import28 direct ordinary and variant elemental library spells, preserving source components."""
from copy import deepcopy
from pathlib import Path
import re
import import_lbc_firearms as firearms
from import_lbc_loadouts import ROOT
from import_lbc_shooter_pair import translate_keys
from import_lbc_wands import build_single_entry_table, write_table
from import_lbc_woodificator import read_json

RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
SPELLS = {160:'ruby',163:'terra',165:'electro',167:'air',171:'acid',173:'teleport',176:'stone',181:'fire',184:'blind',
          161:'ruby_ray', 162:'ruby_hole', 164:'terra_fast', 166:'electro_ray', 168:'air_burst', 169:'air_fast', 170:'air_homing', 172:'acid_burning', 174:'teleport_inverted', 175:'teleport_cloud', 177:'stone_ray', 178:'stone_spikes', 179:'stone_bridge', 180:'stone_shield', 182:'fire_big', 183:'fire_ray', 185:'blind_gatling', 186:'blind_phantom', 187:'blind_rain'}

def main():
    firearms.RP = RP  # Audit the current author pack; never modify it.
    source = firearms.migrated('spells_library')
    for spell_id, name in SPELLS.items():
        candidates = [(pool,entry) for pool in source['pools'] for entry in pool['entries']
                      if any(fn.get('type')=='minecraft:set_custom_data'
                             and re.search(rf'\bspellid\s*:\s*{spell_id}\b',fn.get('tag',''))
                             for fn in entry.get('modifier',[]))]
        assert len(candidates)==1,(spell_id,len(candidates))
        pool,original = candidates[0]
        entry = deepcopy(original)
        assert entry['name']=='minecraft:snowball'
        entry['name']=f'skywars2:{name}_grimoire'
        result=build_single_entry_table(source,pool,entry)
        restored=deepcopy(result);restored['pools'][0]['entries'][0]['name']='minecraft:snowball'
        assert restored==build_single_entry_table(source,pool,original)
        for language in ('ru_ru','en_us','rpr'):
            translations=read_json(RP/f'assets/minecraft/lang/{language}.json')
            assert not set(translate_keys([result]))-translations.keys(),(spell_id,language)
        components=next(fn['components'] for fn in entry['modifier'] if fn.get('type')=='minecraft:set_components')
        firearms.verify_model(components['minecraft:item_model'],item_definition=True)
        destination=ROOT/f'src/main/resources/data/lbc/loot_table/ported/{name}_grimoire.json'
        write_table(destination,result)
        assert read_json(destination)==result
        print(f'{spell_id}: {name}_grimoire, exact source except native item ID; RP read-only')

if __name__=='__main__':main()
