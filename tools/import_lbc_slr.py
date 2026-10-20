#!/usr/bin/env python3
"""Import SLR payload and its block tag; audit the existing resource pack read-only."""
from __future__ import annotations
import json
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_consuming import RP
from import_lbc_wands import write_table

RESOURCES = ROOT / 'src/main/resources'


def main():
    table = json.loads((SOURCE / 'loot_table/slr.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(table)
    entries = [entry for pool in table['pools'] for entry in pool['entries']]
    assert len(entries) == 1 and entries[0]['name'] == 'minecraft:carrot_on_a_stick'
    entries[0]['name'] = 'skywars2:slr'
    for relative in ('data/lbc/loot_table/ported/slr.json', 'data/lbc/loot_table/slr.json'):
        write_table(RESOURCES / relative, table)
        assert json.loads((RESOURCES / relative).read_text(encoding='utf-8')) == table

    # Source-defined tags only: leave shared tags and vanilla registry tags untouched.
    source_data = SOURCE.parent
    queue = ['breakable_slr']
    seen = set()
    while queue:
        name = queue.pop()
        if name in seen:
            continue
        seen.add(name)
        relative = f'data/minecraft/tags/block/{name}.json'
        source = source_data / f'minecraft/tags/block/{name}.json'
        payload = json.loads(source.read_text(encoding='utf-8-sig'))
        target = RESOURCES / relative
        if name == 'breakable_slr' or not target.exists():
            write_table(target, payload)
        else:
            assert json.loads(target.read_text(encoding='utf-8-sig')) == payload, f'Shared tag differs: {name}'
        for entry in payload['values']:
            ref = entry if isinstance(entry, str) else entry['id']
            if ref.startswith('#minecraft:'):
                child = ref.removeprefix('#minecraft:')
                if (source_data / f'minecraft/tags/block/{child}.json').exists():
                    queue.append(child)

    keys = {'karich_mode_1', 'karich_mode_2', 'karich_mode_3', 'karich_mode_4'}
    models = set()
    def visit(value):
        if isinstance(value, list):
            for child in value: visit(child)
        elif isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            if 'minecraft:item_model' in value: models.add(value['minecraft:item_model'])
            for child in value.values(): visit(child)
    visit(table)
    for lang in ('ru_ru', 'en_us'):
        data = json.loads((RP / f'assets/minecraft/lang/{lang}.json').read_text(encoding='utf-8-sig'))
        assert not keys - data.keys(), (lang, sorted(keys - data.keys()))
    for identifier in models:
        namespace, name = identifier.split(':')
        assert (RP / f'assets/{namespace}/items/{name}.json').exists(), identifier
    sounds = json.loads((RP / 'assets/minecraft/sounds.json').read_text(encoding='utf-8-sig'))
    assert 'lbcsounds.karich' in sounds
    for entry in sounds['lbcsounds.karich']['sounds']:
        name = entry if isinstance(entry, str) else entry['name']
        namespace, name = name.split(':', 1) if ':' in name else ('minecraft', name)
        assert (RP / f'assets/{namespace}/sounds/{name}.ogg').exists(), name
    for suffix in ('background', 'frame'):
        assert (RP / f'assets/lbc/textures/gui/sprites/tooltip/tier_s_{suffix}.png').exists()
    print('SLR complete native payload equality passed; tags:', ', '.join(sorted(seen)))
    print('Existing RU/EN translations, item model, tier S tooltip and karich sound verified; RP untouched')


if __name__ == '__main__':
    main()
