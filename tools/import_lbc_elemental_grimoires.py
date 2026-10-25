#!/usr/bin/env python3
"""Import72 direct ordinary and variant elemental library spells, preserving source components."""
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
          161:'ruby_ray', 162:'ruby_hole', 164:'terra_fast', 166:'electro_ray', 168:'air_burst', 169:'air_fast', 170:'air_homing', 172:'acid_burning', 174:'teleport_inverted', 175:'teleport_cloud', 177:'stone_ray', 178:'stone_spikes', 179:'stone_bridge', 180:'stone_shield', 182:'fire_big', 183:'fire_ray', 185:'blind_gatling', 186:'blind_phantom', 187:'blind_rain',
          204:'exahoming', 205:'exahoming_curse', 206:'exahoming_destiny', 207:'exahoming_sisyphus', 208:'psychoblind', 209:'psychoblind_invisible', 210:'psychoblind_ray', 211:'psychoblind_spray', 212:'boltbolt', 213:'boltbolt_meteor', 214:'boltbolt_watering', 215:'boltbolt_racer',
          216:'erecto', 217:'erecto_ray', 218:'erecto_sniper', 219:'erecto_sphere', 220:'fireblaze', 221:'fireblaze_kiuas', 222:'fireblaze_mega', 223:'fireblaze_pillars', 224:'fireblaze_world', 233:'stonekivi', 234:'stonekivi_earth', 235:'stonekivi_sun', 236:'stonekivi_comet',
          188:'awakened_acid', 190:'awakened_acid_caust', 191:'awakened_acid_caust_burning', 192:'awakened_acid_gatling', 193:'awakened_acid_gatling_burning', 194:'awakened_acid_rifle', 199:'exasperato', 200:'exasperato_triple', 201:'exasperato_chaos', 202:'exasperato_ray', 203:'exasperato_nuke',
          225:'consuming', 226:'consuming_avarice', 227:'consuming_silence', 228:'consuming_end', 229:'duplicating', 230:'duplicating_hole', 231:'duplicating_divide', 232:'duplicating_expand'}

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
