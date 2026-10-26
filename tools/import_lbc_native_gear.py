"""Import exact native shields/tridents, their complete pools and the native Electro alias."""
from copy import deepcopy
import json
from import_lbc_tools import ROOT, SOURCE, write
from import_game_resources import migrate_loot_schema


def expected():
    out = {}
    for family, names in {
        'trident': ['sea_legend', 'harpoon'],
        'shield': ['hylian_shield', None, 'holy_shield'],
    }.items():
        table = json.loads((SOURCE / f'loot_table/transformables_{family}.json').read_text(encoding='utf-8-sig'))
        migrate_loot_schema(table)
        out[f'data/lbc/loot_table/transformables_{family}.json'] = table
        for name, entry in zip(names, table['pools'][0]['entries'], strict=True):
            if name is not None:
                out[f'data/lbc/loot_table/ported/{name}.json'] = {'pools': [{'rolls': 1, 'entries': [deepcopy(entry)]}]}
    electro = json.loads((SOURCE / 'loot_table/electro_shield.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(electro)
    assert electro['pools'][0]['entries'][0]['name'] == 'minecraft:shield'
    electro['pools'][0]['entries'][0]['name'] = 'skywars2:electro_shield'
    out['data/lbc/loot_table/electro_shield.json'] = electro
    return out


if __name__ == '__main__':
    for name, table in expected().items():
        write(ROOT / 'src/main/resources' / name, table)
    print('Imported four exact native leaves, both complete pools and native Electro shield alias')
