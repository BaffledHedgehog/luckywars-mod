"""Bounded source tool pools; never reads or writes the live grimoire overrides."""
from copy import deepcopy
import json
from import_lbc_tools import ROOT, SOURCE, write
from import_game_resources import migrate_loot_schema


def expected():
    out = {}
    for name in ('transformables_shovel', 'transformables_shovel_all', 'transformables_hoe_all', 'transformables_axe_all',
                 'death_loot', 'halloween/transformables_shovel', 'halloween/soulhunter_mace'):
        data = json.loads((SOURCE / f'loot_table/{name}.json').read_text(encoding='utf-8-sig'))
        migrate_loot_schema(data)
        if name == 'death_loot': data['pools'][0]['entries'][0]['name'] = 'skywars2:death_scythe'
        if name == 'halloween/transformables_shovel': data['pools'][0]['entries'][0]['name'] = 'skywars2:halloween_mace'
        if name == 'halloween/soulhunter_mace': data['pools'][0]['entries'][0]['name'] = 'skywars2:soulhunter_mace'
        out[f'data/lbc/loot_table/{name}.json'] = data
    shovel = out['data/lbc/loot_table/transformables_shovel.json']
    for leaf, entry in zip(('random_enchanted_shovel', 'efficient_shovel', 'mortis'), shovel['pools'][0]['entries'], strict=True):
        out[f'data/lbc/loot_table/ported/{leaf}.json'] = {'pools': [{'rolls': 1, 'entries': [deepcopy(entry)]}]}
    out['data/lbc/loot_table/ported/death_scythe.json'] = deepcopy(out['data/lbc/loot_table/death_loot.json'])
    axe = out['data/lbc/loot_table/transformables_axe_all.json']
    axe['pools'][0]['entries'][1]['name'] = 'skywars2:banhammer'
    out['data/lbc/loot_table/ported/banhammer.json'] = {'pools': [{'rolls': 1, 'entries': [deepcopy(axe['pools'][0]['entries'][1])]}]}
    return out


if __name__ == '__main__':
    for path, value in expected().items(): write(ROOT / 'src/main/resources' / path, value)
    print('Imported native shovels, Death Scythe, Banhammer and four closed tool pools')
