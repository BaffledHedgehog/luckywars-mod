"""Import the bounded ordinary axe/hoe/pickaxe pools without touching grimoires."""
from copy import deepcopy
import json
from import_lbc_loadouts import ROOT, SOURCE
from import_game_resources import migrate_loot_schema


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))


def expected():
    out = {}
    names = {
        'axe': ['random_enchanted_axe', 'tomahawk', 'poleaxe', 'hammer'],
        'hoe': ['op_hoe', 'sickle'],
        'pickaxe': ['random_enchanted_pickaxe', 'silk_pickaxe'],
    }
    for family, leaves in names.items():
        table = json.loads((SOURCE / f'loot_table/transformables_{family}.json').read_text(encoding='utf-8-sig'))
        migrate_loot_schema(table)
        for name, entry in zip(leaves, table['pools'][0]['entries'], strict=True):
            out[f'data/lbc/loot_table/ported/{name}.json'] = {'pools': [{'rolls': 1, 'entries': [deepcopy(entry)]}]}
        out[f'data/lbc/loot_table/transformables_{family}.json'] = table
    hoe = json.loads((SOURCE / 'loot_table/op_hoe.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(hoe)
    out['data/lbc/loot_table/op_hoe.json'] = hoe
    # Source recipe payload has no inferred mana cost, durability, type row or extra rarity line.
    entry = {'type': 'minecraft:item', 'name': 'skywars2:communism', 'modifier': [
        {'type': 'minecraft:set_components', 'components': {
            'minecraft:custom_name': {'italic': False, 'translate': 'socializm'},
            'minecraft:lore': [{'translate': 'socializm.lore', 'color': 'white', 'italic': False}],
            'minecraft:item_model': 'lbc:communism', 'minecraft:tooltip_style': 'lbc:tier_s'}},
        {'type': 'minecraft:set_custom_data', 'tag': '{communism:1,raycastspell:1}'}]}
    out['data/lbc/loot_table/ported/communism.json'] = {'pools': [{'rolls': 1, 'entries': [entry]}]}
    return out


if __name__ == '__main__':
    for name, data in expected().items():
        write(ROOT / 'src/main/resources' / name, data)
    print('Imported eight native tool outcomes, three complete ordinary pools and Communism recipe result')
