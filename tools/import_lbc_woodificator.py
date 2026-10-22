#!/usr/bin/env python3
"""Import only the qualified Woodificator item; verify existing resource-pack assets."""
from __future__ import annotations

import json
from pathlib import Path

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, select_ordinary_wand_entry, write_table

RESOURCES = ROOT / 'src/main/resources'
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')



def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def main() -> None:
    source = SOURCE / 'loot_table/transformables_shooter.json'
    table = read_json(source)
    migrate_loot_schema(table)
    pool, original = select_ordinary_wand_entry(table, 'woodificator', source, 'woodificator:1')
    entry = json.loads(json.dumps(original))
    entry['name'] = 'skywars2:woodificator'
    payload = build_single_entry_table(table, pool, entry)

    keys = {'woodificator', 'woodificator.lore', 'type', 'shooter'}
    for language in ('ru_ru', 'en_us'):
        translations = read_json(RP / f'assets/minecraft/lang/{language}.json')
        assert not keys - translations.keys(), (language, keys - translations.keys())
    model = read_json(RP / 'assets/lbc/items/woodificator.json')
    assert model['model']['model'] == 'lbc:item/woodificator'
    geometry = read_json(RP / 'assets/lbc/models/item/woodificator.json')
    assert geometry['parent'] == 'minecraft:item/handheld'
    assert geometry['textures']['layer0'] == 'minecraft:item/woodificator'
    assert (RP / 'assets/minecraft/textures/item/woodificator.png').is_file()
    for suffix in ('background', 'frame'):
        assert (RP / f'assets/lbc/textures/gui/sprites/tooltip/tier_d_{suffix}.png').is_file()

    target = RESOURCES / 'data/lbc/loot_table/ported/woodificator.json'
    write_table(target, payload)
    assert read_json(target) == payload
    print('Written qualified item:', target.relative_to(RESOURCES).as_posix())
    print('RU/EN keys, item/model/texture and tier D tooltip verified read-only; no pack templates needed')


if __name__ == '__main__':
    main()
