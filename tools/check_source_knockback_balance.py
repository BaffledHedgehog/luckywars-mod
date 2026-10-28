#!/usr/bin/env python3
"""Verify source budget edge cases and ordinary defaults in processed build resources."""
from copy import deepcopy
import argparse
import json
from pathlib import Path
from normalize_source_knockback import normalize_source_knockback


def sample(attribute, enchant, imba=False):
    components = {'minecraft:enchantments': {'minecraft:knockback': enchant},
                  'minecraft:attribute_modifiers': [{'type': 'minecraft:attack_knockback',
                    'slot': 'mainhand', 'id': 'probe', 'operation': 'add_value', 'amount': attribute}]}
    return {'type': 'minecraft:item', 'name': 'minecraft:stick', 'modifier': [
        {'type': 'minecraft:set_components', 'components': components},
        {'type': 'minecraft:set_custom_data', 'tag': '{imba:1}' if imba else '{source:1}'}]}


def regression_checks():
    from normalize_source_knockback import MAX_SOURCE_PUNCH
    assert MAX_SOURCE_PUNCH == 6
    assert .4 + .6 * (MAX_SOURCE_PUNCH - 1) < 3.9 <= .4 + .6 * MAX_SOURCE_PUNCH
    for level, imba, wanted in [(2, False, 2), (255, False, 6), (255, True, 255)]:
        value = sample(0, 1, imba)
        value['name'] = 'minecraft:bow'
        value['modifier'][0]['components']['minecraft:enchantments']['minecraft:punch'] = level
        normalize_source_knockback(value)
        assert value['modifier'][0]['components']['minecraft:enchantments']['minecraft:punch'] == wanted
    for attribute, enchant, wanted_attribute, wanted_enchant in [
            (3, 4, 1, 4), (3, 9, 0, 5), (-2, 9, -2, 7), (-10, 9, -10, 9), (0, 255, 0, 5)]:
        value = sample(attribute, enchant)
        normalize_source_knockback(value)
        components = value['modifier'][0]['components']
        assert components['minecraft:attribute_modifiers'][0]['amount'] == wanted_attribute
        assert components['minecraft:enchantments']['minecraft:knockback'] == wanted_enchant
        before = deepcopy(value); normalize_source_knockback(value)
        assert before == value, 'migration must be idempotent'
    value = sample(100, 255, True); before = deepcopy(value)
    normalize_source_knockback(value); assert before == value, 'imba values must remain exact'
    value = sample(3, 9)
    value['modifier'][0]['components']['minecraft:entity_data'] = {'equipment': {
        'mainhand': {'components': {'minecraft:custom_data': {'imba': 1}}}}}
    normalize_source_knockback(value)
    assert value['modifier'][0]['components']['minecraft:enchantments']['minecraft:knockback'] == 5, 'nested imba cannot exempt parent'
    from import_game_resources import migrate_loot_schema
    value = sample(3, 255); before = deepcopy(value)
    migrate_loot_schema(value, preserve_imba=True)
    assert value == before, 'explicit imbadrops source provenance survives without an item marker'
    value = sample(3, 9)
    value['modifier'][0]['components']['minecraft:attribute_modifiers'][0]['operation'] = 'add_multiplied_total'
    try:
        normalize_source_knockback(value)
    except ValueError:
        pass
    else:
        raise AssertionError('non-additive authored attributes need explicit migration review')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--resources', type=Path, default=Path(__file__).resolve().parents[1] / 'build/resources/main')
    parser.add_argument('--self-test-only', action='store_true')
    args = parser.parse_args()
    regression_checks()
    if args.self_test_only:
        print('PASS source knockback migration edge cases')
        return
    assert args.resources.is_dir(), f'processed resources missing: {args.resources}'
    count = 0
    errors = []
    for path in sorted((args.resources / 'data').rglob('loot_table/**/*.json')):
        value = json.loads(path.read_text(encoding='utf-8-sig'))
        normalized = deepcopy(value)
        relative = path.relative_to(args.resources).as_posix()
        if '/loot_table/imba/' not in relative and '/loot_table/imbadrops/' not in relative:
            normalize_source_knockback(normalized)
        count += 1
        if normalized != value:
            errors.append(path.relative_to(args.resources).as_posix())
    assert not errors, 'unbalanced ordinary authored processed loot: ' + ', '.join(errors)
    print(f'PASS source knockback migration edge cases and {count} processed loot tables')


if __name__ == '__main__':
    main()
