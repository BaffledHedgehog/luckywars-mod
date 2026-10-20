#!/usr/bin/env python3
"""Import isolated KFC payloads and audit their existing resource-pack assets read-only."""
from __future__ import annotations

import copy
import json

from import_game_resources import migrate_loot_schema
from import_lbc_consuming import RP
from import_lbc_fireblaze import select_entry
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, write_table

RESOURCES = ROOT / 'src/main/resources'


def main():
    shooter = json.loads((SOURCE / 'loot_table/transformables_shooter.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(shooter)
    pool, entry = select_entry(shooter, 'kfc_gun', 'kfcgun:1')
    gun = build_single_entry_table(shooter, pool, copy.deepcopy(entry))
    gun['pools'][0]['entries'][0]['name'] = 'skywars2:kfc_gun'

    chicken = json.loads((SOURCE / 'loot_table/kfc_yumyumyummy.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(chicken)
    entries = [entry for pool in chicken['pools'] for entry in pool['entries']]
    assert len(entries) == 1 and entries[0]['name'] == 'minecraft:cooked_chicken'
    entries[0]['name'] = 'skywars2:kfc_chick'

    payloads = {'ported/kfc_gun': gun, 'ported/kfc_chick': chicken, 'kfc_yumyumyummy': chicken}
    # These food outcomes are emitted directly by the native service; legacy functions remain disabled.
    for name in ('govno', 'govno_eaten', 'govno_lot', 'govno_lot_2', 'win_button', 'penis_cultus'):
        payload = json.loads((SOURCE / f'loot_table/{name}.json').read_text(encoding='utf-8-sig'))
        migrate_loot_schema(payload)
        if name in ('govno_eaten', 'win_button', 'penis_cultus'):
            payload['pools'][0]['entries'][0]['name'] = f'skywars2:{name}'
        payloads[name] = payload
    for name, payload in payloads.items():
        path = RESOURCES / f'data/lbc/loot_table/{name}.json'
        write_table(path, payload)
        assert json.loads(path.read_text(encoding='utf-8')) == payload

    keys, models, styles = set(), set(), set()
    def visit(value):
        if isinstance(value, list):
            for child in value:
                visit(child)
        elif isinstance(value, dict):
            if 'translate' in value:
                keys.add(value['translate'])
            if 'minecraft:item_model' in value:
                models.add(value['minecraft:item_model'])
            if 'minecraft:tooltip_style' in value:
                styles.add(value['minecraft:tooltip_style'])
            for child in value.values():
                visit(child)
    visit([gun, chicken])
    for lang in ('ru_ru', 'en_us'):
        data = json.loads((RP / f'assets/minecraft/lang/{lang}.json').read_text(encoding='utf-8-sig'))
        assert not keys - data.keys(), (lang, sorted(keys - data.keys()))
    for identifier in models:
        namespace, name = identifier.split(':', 1)
        assert (RP / f'assets/{namespace}/items/{name}.json').is_file(), identifier
        model = json.loads((RP / f'assets/{namespace}/models/item/{name}.json').read_text(encoding='utf-8-sig'))
        for texture in model.get('textures', {}).values():
            if not texture.startswith('#'):
                texture_namespace, texture_name = texture.split(':', 1)
                assert (RP / f'assets/{texture_namespace}/textures/{texture_name}.png').is_file(), texture
    for identifier in styles:
        namespace, name = identifier.split(':', 1)
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/{namespace}/textures/gui/sprites/tooltip/{name}_{suffix}.png').is_file()
    print('KFC native payload equality and existing RU/EN keys, item models, tooltip sprites verified; RP untouched')
    print('Imported isolated tables only; transformables_shooter random pool unchanged')


if __name__ == '__main__':
    main()
