#!/usr/bin/env python3
"""Import the two qualified ordinary shooters without changing their source presentation."""
from __future__ import annotations

from copy import deepcopy

from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, select_ordinary_wand_entry, write_table
from import_lbc_woodificator import RP, read_json

RESOURCES = ROOT / 'src/main/resources'


def apply_minigun_changes(entry):
    """The explicit user changes; every other source component remains intact."""
    functions = entry['modifier']
    components = next(function['components'] for function in functions
                      if function['type'] == 'minecraft:set_components')
    attributes = components['minecraft:attribute_modifiers']
    assert len(attributes) == 2 and all(
        modifier['type'] == 'minecraft:attack_speed'
        and modifier['amount'] == -1
        and modifier['operation'] == 'add_multiplied_total' for modifier in attributes)
    for attribute, amount, key in (('movement_speed', -.5, 'movement'),
                                    ('jump_strength', -1, 'jump')):
        for slot in ('mainhand', 'offhand'):
            attributes.append(dict(type='minecraft:' + attribute, slot=slot,
                                   id=f'skywars2:laser_minigun_{key}_{slot}', amount=amount,
                                   operation='add_multiplied_total'))
    lore = next(function['lore'] for function in functions
                if function['type'] == 'minecraft:set_lore')
    damage_lines = [line for line in lore if isinstance(line, list)
                    and any(part.get('translate') == 'projectile_damage' for part in line)]
    assert len(damage_lines) == 1
    literal = next(part for part in damage_lines[0] if 'text' in part)
    assert literal['text'] == '4 '
    literal['text'] = '2.5 '


def translate_keys(value):
    if isinstance(value, dict):
        if 'translate' in value:
            yield value['translate']
        for child in value.values():
            yield from translate_keys(child)
    elif isinstance(value, list):
        for child in value:
            yield from translate_keys(child)


def verify_geometry(identifier, visited=None):
    """Resolve non-builtin model parents and every explicit texture read-only."""
    visited = set() if visited is None else visited
    if identifier in visited:
        return
    visited.add(identifier)
    namespace, path = identifier.split(':', 1) if ':' in identifier else ('minecraft', identifier)
    geometry_path = RP / f'assets/{namespace}/models/{path}.json'
    if identifier in ('minecraft:item/generated', 'minecraft:item/handheld'):
        return
    geometry = read_json(geometry_path)
    if 'parent' in geometry:
        verify_geometry(geometry['parent'], visited)
    for texture in geometry.get('textures', {}).values():
        if texture.startswith('#'):
            continue
        ns, texture_path = texture.split(':', 1) if ':' in texture else ('minecraft', texture)
        assert (RP / f'assets/{ns}/textures/{texture_path}.png').is_file(), texture


def verify_item_model(identifier):
    namespace, path = identifier.split(':', 1)
    definition = read_json(RP / f'assets/{namespace}/items/{path}.json')
    assert definition['model']['type'] in ('model', 'minecraft:model'), identifier
    verify_geometry(definition['model']['model'])


def main():
    source = SOURCE / 'loot_table/transformables_shooter.json'
    table = read_json(source)
    migrate_loot_schema(table)
    for item in ('attack_helicopter', 'laser_minigun'):
        pool, original = select_ordinary_wand_entry(table, item, source, item + ':1')
        entry = deepcopy(original)
        entry['name'] = 'skywars2:' + item
        if item == 'laser_minigun':
            apply_minigun_changes(entry)
        expected = deepcopy(original)
        if item == 'laser_minigun':
            apply_minigun_changes(expected)
        assert {k: v for k, v in entry.items() if k != 'name'} == {
            k: v for k, v in expected.items() if k != 'name'}
        keys = set(translate_keys(entry))
        for language in ('ru_ru', 'en_us'):
            translations = read_json(RP / f'assets/minecraft/lang/{language}.json')
            assert not keys - translations.keys(), (item, language, keys - translations.keys())
        target = RESOURCES / f'data/lbc/loot_table/ported/{item}.json'
        payload = build_single_entry_table(table, pool, entry)
        write_table(target, payload)
        assert read_json(target) == payload
        print('Imported:', target.relative_to(RESOURCES).as_posix(), 'keys:', ', '.join(sorted(keys)))
    for model in ('lbc:attack_helicopter', 'lbc:laser_minigun_ray',
                  *(f'lbc:helicopter_{index}' for index in range(1, 5)),
                  *(f'lbc:laser_minigun/{index}' for index in range(1, 9))):
        verify_item_model(model)
    for tier in ('a', 'b'):
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/lbc/textures/gui/sprites/tooltip/tier_{tier}_{suffix}.png').is_file()
    print('RU/EN, both tooltip styles, helicopter icon, all eight spin variants and ray verified read-only; no template needed')


if __name__ == '__main__':
    main()
