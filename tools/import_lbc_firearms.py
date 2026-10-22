#!/usr/bin/env python3
"""Import source-faithful AK47/Magnum payloads; audit the current RP read-only."""
from __future__ import annotations

from copy import deepcopy

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_shooter_pair import translate_keys
from import_lbc_wands import build_single_entry_table, write_table
from import_lbc_woodificator import RP, read_json

RESOURCES = ROOT / 'src/main/resources'


def verify_model(identifier, item_definition=False, seen=None):
    seen = set() if seen is None else seen
    if identifier in seen:
        return
    seen.add(identifier)
    if identifier in ('minecraft:item/generated', 'minecraft:item/handheld',
                      'minecraft:item/handheld_rod'):
        return  # Vanilla client geometry, not a missing resource-pack override.
    namespace, path = identifier.split(':', 1) if ':' in identifier else ('minecraft', identifier)
    data = read_json(RP / f'assets/{namespace}/{"items" if item_definition else "models"}/{path}.json')
    if item_definition:
        assert data['model']['type'] in ('model', 'minecraft:model')
        verify_model(data['model']['model'], seen=seen)
    else:
        if 'parent' in data:
            verify_model(data['parent'], seen=seen)
        for texture in data.get('textures', {}).values():
            if texture.startswith('#'):
                continue
            ns, path = texture.split(':', 1) if ':' in texture else ('minecraft', texture)
            assert (RP / f'assets/{ns}/textures/{path}.png').is_file(), texture


def migrated(name):
    table = read_json(SOURCE / f'loot_table/{name}.json')
    migrate_loot_schema(table)
    return table


def stage_gun(source, target, marker, native):
    table = migrated(source)
    selected = [(pool, entry) for pool in table['pools'] for entry in pool['entries']
                if entry.get('name') == 'minecraft:carrot_on_a_stick'
                and any(function.get('type') == 'minecraft:set_custom_data'
                        and marker in function['tag'] for function in entry.get('modifier', []))]
    assert len(selected) == 1, (source, marker, len(selected))
    pool, original = selected[0]
    entry = deepcopy(original)
    entry['name'] = native
    assert {k: v for k, v in entry.items() if k != 'name'} == {
        k: v for k, v in original.items() if k != 'name'}
    payload = build_single_entry_table(table, pool, entry)
    destination = RESOURCES / f'data/lbc/loot_table/ported/{target}.json'
    write_table(destination, payload)
    assert read_json(destination) == payload
    return payload


def main():
    payloads = [stage_gun('stalin', 'ak47', 'ak47:1', 'skywars2:ak47'),
                stage_gun('stalin_ak_finite', 'ak47_finite', 'finite:1', 'skywars2:ak47'),
                stage_gun('transformables_shooter_all', 'magnum', 'magnum:1', 'skywars2:magnum')]
    ammo = migrated('magnum_ammo')
    entries = ammo['pools'][0]['entries']
    assert ammo['pools'][0]['rolls'] == 4 and len(entries) == 6
    assert all(entry['name'] == 'minecraft:repeating_command_block' for entry in entries)
    # Polymer rounds preserve every source component; matching also accepts legacy custom_data.
    for entry in entries:
        components=next(function['components'] for function in entry['modifier']
                        if function['type']=='minecraft:set_components')
        mode=components['minecraft:item_model'].split('round_',1)[1]
        entry['name']='skywars2:magnum_round_'+mode
    destination = RESOURCES / 'data/lbc/loot_table/ported/magnum_ammo.json'
    write_table(destination, ammo)
    write_table(RESOURCES / "data/lbc/loot_table/magnum_ammo.json", ammo)
    assert read_json(destination) == ammo
    payloads.append(ammo)

    keys = set(translate_keys(payloads)) | {
        'magnum_mode_' + mode for mode in
        ('standard', 'incendiary', 'electric', 'acid', 'void', 'explosive')}
    for language in ('ru_ru', 'en_us', 'rpr'):
        translations = read_json(RP / f'assets/minecraft/lang/{language}.json')
        assert not keys - translations.keys(), (language, keys - translations.keys())
    for model in ('ak47', 'magnum', 'round_standard', 'round_incendiary',
                  'round_electric', 'round_acid', 'round_void', 'round_explosive'):
        verify_model('lbc:' + model, item_definition=True)
    for tier in ('a', 'b', 's', 'ss'):
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/lbc/textures/gui/sprites/tooltip/tier_{tier}_{suffix}.png').is_file()
    sounds = read_json(RP / 'assets/minecraft/sounds.json')
    for entry in sounds['lbcsounds.shot']['sounds']:
        sound = entry if isinstance(entry, str) else entry['name']
        namespace, path = sound.split(':', 1) if ':' in sound else ('minecraft', sound)
        assert (RP / f'assets/{namespace}/sounds/{path}.ogg').is_file(), sound
    print('Imported AK47, finite AK47, Magnum and six Polymer rounds with unchanged source presentation.')
    print('Source payload equality and current26.3 RU/EN/RPR, model/texture, tooltip and shot sound checks passed; no RP/template writes.')


if __name__ == '__main__':
    main()
