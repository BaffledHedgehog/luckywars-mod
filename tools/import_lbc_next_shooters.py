#!/usr/bin/env python3
"""Import qualified Govnomet/Boss Helicopter payloads with original presentation."""
from __future__ import annotations

from copy import deepcopy

from import_lbc_firearms import migrated, verify_model
from import_lbc_loadouts import ROOT
from import_lbc_shooter_pair import translate_keys
from import_lbc_wands import build_single_entry_table, write_table
from import_lbc_woodificator import RP, read_json

DESTINATION = ROOT / 'src/main/resources/data/lbc/loot_table/ported'


def payload(source, marker, native):
    table = migrated(source)
    selected = [(pool, entry) for pool in table['pools'] for entry in pool['entries']
                if entry.get('name') == 'minecraft:carrot_on_a_stick'
                and any(function.get('type') == 'minecraft:set_custom_data'
                        and marker in function['tag'] for function in entry.get('modifier', []))]
    assert len(selected) == 1, (source, marker, len(selected))
    pool, original = selected[0]
    entry = deepcopy(original)
    entry['name'] = native
    expected = build_single_entry_table(table, pool, deepcopy(original))
    result = build_single_entry_table(table, pool, entry)
    restored = deepcopy(result)
    restored['pools'][0]['entries'][0]['name'] = original['name']
    assert restored == expected, source
    return result


def verify_resources(payloads):
    keys = set(translate_keys(payloads)) | {'helicopter_boss'}
    for language in ('ru_ru', 'en_us', 'rpr'):
        translations = read_json(RP / f'assets/minecraft/lang/{language}.json')
        assert not keys - translations.keys(), (language, keys - translations.keys())
    for model in ('govnomet', 'helicopter_boss', *(f'helicopter_{i}' for i in range(1, 5))):
        verify_model('lbc:' + model, item_definition=True)
    for tier in ('d', 's'):
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/lbc/textures/gui/sprites/tooltip/tier_{tier}_{suffix}.png').is_file()
    sounds = read_json(RP / 'assets/minecraft/sounds.json')
    for key in ('lbcsounds.helicopter', 'lbcsounds.kazahstan', 'lbcsounds.drop_bomb'):
        for entry in sounds[key]['sounds']:
            sound = entry if isinstance(entry, str) else entry['name']
            namespace, path = sound.split(':', 1) if ':' in sound else ('minecraft', sound)
            assert (RP / f'assets/{namespace}/sounds/{path}.ogg').is_file(), sound


def main():
    payloads = {
        'govnomet': payload('transformables_shooter_all', 'govnomet:1', 'skywars2:govnomet'),
        'helicopter_boss': payload('helicopter_boss', 'boss_helicopter:1', 'skywars2:boss_helicopter'),
    }
    verify_resources(list(payloads.values()))
    for name, table in payloads.items():
        destination = DESTINATION / f'{name}.json'
        write_table(destination, table)
        assert read_json(destination) == table
        print('Imported:', destination.relative_to(ROOT).as_posix())
    print('Source equality except native IDs; RU/EN/RPR, models/textures, tooltips and sounds passed.')
    write_table(ROOT / 'src/main/resources/data/lbc/loot_table/helicopter_boss.json', payloads['helicopter_boss'])
    print('Original Boss weapon alias preserved for existing internal references; no source RP/template writes.')


if __name__ == '__main__':
    main()
