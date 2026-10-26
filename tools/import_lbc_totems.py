"""Import the complete two-outcome native totem pool without touching grimoire authoring."""
from copy import deepcopy
import json
from import_lbc_tools import ROOT, SOURCE, write
from import_game_resources import migrate_loot_schema


def expected():
    result = {}
    for name in ('transformables_totem', 'infinite_totem'):
        data = json.loads((SOURCE / f'loot_table/{name}.json').read_text(encoding='utf-8-sig'))
        migrate_loot_schema(data)
        result[f'data/lbc/loot_table/{name}.json'] = data
    devil = result['data/lbc/loot_table/transformables_totem.json']['pools'][0]['entries'][1]
    result['data/lbc/loot_table/ported/devil_totem.json'] = {'pools': [{'rolls': 1, 'entries': [deepcopy(devil)]}]}
    result['data/lbc/loot_table/ported/infinite_totem.json'] = deepcopy(result['data/lbc/loot_table/infinite_totem.json'])
    return result


if __name__ == '__main__':
    for path, value in expected().items(): write(ROOT / 'src/main/resources' / path, value)
    print('Imported exact Infinite/Devil Totem leaves and complete weighted totem pool')
