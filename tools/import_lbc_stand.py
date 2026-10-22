#!/usr/bin/env python3
"""Import exact Stand payloads; retain the unported time-stopper donor's original identity."""
from __future__ import annotations

import copy
import json
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_consuming import RP
from import_lbc_wands import write_table


def main():
    resources = ROOT / 'src/main/resources/data/lbc/loot_table'
    keys, models = set(), set()

    def visit(value):
        if isinstance(value, list):
            for child in value:
                visit(child)
        elif isinstance(value, dict):
            if 'translate' in value:
                keys.add(value['translate'])
            if 'minecraft:item_model' in value:
                models.add(value['minecraft:item_model'])
            for child in value.values():
                visit(child)

    for name, base in [('stand_arrow', 'minecraft:tipped_arrow'),
                       ('stand_oraora', 'minecraft:carrot_on_a_stick'),
                       ('stand_clocks', 'minecraft:carrot_on_a_stick'),
                       ('homing_lightning', 'minecraft:carrot_on_a_stick'),
                       ('time_stopper', 'minecraft:snowball')]:
        source = json.loads((SOURCE / f'loot_table/{name}.json').read_text(encoding='utf-8-sig'))
        expected = copy.deepcopy(source)
        migrate_loot_schema(expected)
        assert len(expected['pools']) == 1 and len(expected['pools'][0]['entries']) == 1
        entry = expected['pools'][0]['entries'][0]
        assert entry['name'] == base
        payload = copy.deepcopy(expected)
        native = name != 'time_stopper'
        if native:
            payload['pools'][0]['entries'][0]['name'] = f'skywars2:{name}'
        # Its independent snowball ability is not ported: recipes consume its source
        # custom_data donor, and source loot references must keep lbc:time_stopper.
        target = resources / 'ported' / f'{name}.json' if native else resources / f'{name}.json'
        write_table(target, payload)
        actual = json.loads(target.read_text(encoding='utf-8'))
        assert actual == payload
        actual['pools'][0]['entries'][0]['name'] = base
        assert actual == expected, f'Payload drift outside native registry identity: {name}'
        visit(source)

    for language in ('ru_ru', 'en_us'):
        translations = json.loads((RP / f'assets/minecraft/lang/{language}.json').read_text(encoding='utf-8-sig'))
        assert not keys - translations.keys(), (language, sorted(keys - translations.keys()))
    for model in models | {'lbc:ora', 'lbc:overheaven'}:
        namespace, path = model.split(':')
        assert (RP / f'assets/{namespace}/items/{path}.json').exists(), model
    sounds = json.loads((RP / 'assets/minecraft/sounds.json').read_text(encoding='utf-8-sig'))
    assert 'lbcsounds.oraora' in sounds
    for sound in sounds['lbcsounds.oraora']['sounds']:
        sound = sound if isinstance(sound, str) else sound['name']
        namespace, path = sound.split(':', 1) if ':' in sound else ('minecraft', sound)
        assert (RP / f'assets/{namespace}/sounds/{path}.ogg').exists(), sound
    for tier in ('q', 's', 'x', 'a'):
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/lbc/textures/gui/sprites/tooltip/tier_{tier}_{suffix}.png').exists()
    print('Stand arrow/ORAORA/clocks and original time-stopper donor payload equality, RU/EN, models, tooltips and ORAORA sound verified; RP untouched')


if __name__ == '__main__':
    main()
