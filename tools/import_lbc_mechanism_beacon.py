#!/usr/bin/env python3
"""Import the mechanism beacon and its exact source target tags; pack is read-only."""
from copy import deepcopy
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import write_table
from import_lbc_woodificator import RP, read_json


def main():
    table=deepcopy(read_json(SOURCE/'loot_table/stopper_techno.json'))
    migrate_loot_schema(table)
    entry=table['pools'][0]['entries'][0]
    assert entry['name']=='minecraft:panda_spawn_egg'
    entry['name']='skywars2:mechanism_beacon'
    target=ROOT/'src/main/resources/data/lbc/loot_table/ported/mechanism_beacon.json'
    write_table(target,table)
    for folder in ('block','entity_type'):
        original=read_json(SOURCE.parent/f'minecraft/tags/{folder}/mecha.json')
        path=ROOT/f'src/main/resources/data/minecraft/tags/{folder}/mecha.json'
        if path.exists(): assert read_json(path)==original,path
        else: write_table(path,original)
    for language in ('ru_ru','en_us'):
        translations=read_json(RP/f'assets/minecraft/lang/{language}.json')
        for key in ('mecha_breaker','mecha_breaker.lore.1','mecha_breaker.lore.2'): assert key in translations
    assert read_json(RP/'assets/lbc/items/stopper.json')['model']['model']=='lbc:item/stopper'
    print('Mechanism beacon imported; exact target tags and original RP translations/model verified')

if __name__=='__main__': main()
