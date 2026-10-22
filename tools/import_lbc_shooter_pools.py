#!/usr/bin/env python3
"""Close the source shooter graph over individually qualified Polymer payloads."""
from __future__ import annotations

import argparse
from copy import deepcopy
import hashlib
import json

from import_lbc_firearms import migrated, verify_model
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_shooter_pair import apply_minigun_changes, translate_keys
from import_lbc_wands import write_table
from import_lbc_weapons import migrate_legacy_score_value_checks
from import_lbc_woodificator import RP, read_json

RESOURCES = ROOT / 'src/main/resources'
TABLES = ('transformables_shooter', 'transformables_shooter_all',
          'razorpine_with_leaves', 'stalin', 'tuntijaa_terra_razor',
          'helicopter_boss', 'slr')
REFERENCES = {
    'lbc:phantomknifes': 'phantom_knives',
    'lbc:mobgun': 'mobgun',
    'lbc:kfcgun': 'kfc_gun',
    'lbc:razorpine': 'razorpine',
    'lbc:attack_helicopter': 'attack_helicopter',
    'lbc:laser_minigun/1': 'laser_minigun',
    'lbc:woodificator': 'woodificator',
    'lbc:ak47': 'ak47',
    'lbc:magnum': 'magnum',
    'lbc:terra_razorpine': 'terra_razorpine',
    'lbc:govnomet': 'govnomet',
    'lbc:helicopter_boss': 'helicopter_boss',
    'lbc:super_rifle': 'slr',
}


def model(entry):
    modifiers = entry.get('modifier', [])
    if isinstance(modifiers, dict):
        modifiers = [modifiers]
    return next((fn['components']['minecraft:item_model'] for fn in modifiers
                 if fn.get('type') == 'minecraft:set_components'
                 and 'minecraft:item_model' in fn.get('components', {})), None)


def qualified_entry(identifier):
    reference = read_json(RESOURCES / f'data/lbc/loot_table/ported/{REFERENCES[identifier]}.json')
    matches = [entry for pool in reference['pools'] for entry in pool['entries']
               if model(entry) == identifier]
    assert len(matches) == 1, (identifier, len(matches))
    return matches[0]


def allowed_source_changes(entry, identifier):
    """Selection remains original; only these previously approved item changes differ."""
    if identifier == 'lbc:mobgun':
        lore = next(fn['lore'] for fn in entry['modifier'] if fn['type'] == 'minecraft:set_lore')
        assert lore[1][0]['text'] == '3.5 '
        lore[1][0]['text'] = '2.5 '
    elif identifier == 'lbc:laser_minigun/1':
        apply_minigun_changes(entry)


def normalize(table, report):
    result = deepcopy(table)
    for pool in result['pools']:
        for entry in pool['entries']:
            if entry['type'].removeprefix('minecraft:') == 'loot_table':
                assert entry['value'] in {'lbc:' + name for name in TABLES}, entry['value']
                continue
            identifier = model(entry)
            if identifier is None:
                assert entry['name'] in ('wheat_seeds', 'minecraft:wheat_seeds'), entry
                continue
            assert identifier in REFERENCES, identifier
            qualified = qualified_entry(identifier)
            expected = deepcopy(entry)
            allowed_source_changes(expected, identifier)
            # Never silently replace item presentation with an unrelated same-category item.
            assert expected['modifier'] == qualified['modifier'], identifier
            entry['name'] = qualified['name']
            entry['modifier'] = deepcopy(qualified['modifier'])
            expected['name'] = qualified['name']
            assert entry == expected, identifier
            report.append({'model': identifier, 'nativeItem': entry['name'],
                           'reference': 'lbc:ported/' + REFERENCES[identifier],
                           'weight': entry.get('weight', 1), 'quality': entry.get('quality', 0),
                           'condition': entry.get('condition')})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publish', action='store_true',
                        help='Expose the two individually verified pools under lbc:ported/')
    args = parser.parse_args()
    leaves, tables = [], {}
    for name in TABLES:
        original = migrated(name)
        # Reforge's existing schema bridge preserves the legacy equality predicate in 26.3.
        migrate_legacy_score_value_checks(original)
        normalized = normalize(original, leaves)
        # Parent structure, weights, quality, fixed-score conditions and seeds are untouched.
        for before, after in zip(original['pools'], normalized['pools'], strict=True):
            assert {k: v for k, v in before.items() if k != 'entries'} == {
                k: v for k, v in after.items() if k != 'entries'}
            for old, new in zip(before['entries'], after['entries'], strict=True):
                assert {k: v for k, v in old.items() if k not in ('name', 'modifier')} == {
                    k: v for k, v in new.items() if k not in ('name', 'modifier')}
        tables[name] = normalized
    assert len(leaves) == 14 and len({leaf['model'] for leaf in leaves}) == 13
    keys = set(translate_keys(tables))
    for language in ('ru_ru', 'en_us', 'rpr'):
        translations = read_json(RP / f'assets/minecraft/lang/{language}.json')
        assert not keys - translations.keys(), (language, keys - translations.keys())
    for identifier in REFERENCES:
        verify_model(identifier, item_definition=True)
    for name, payload in tables.items():
        destination = RESOURCES / f'data/lbc/loot_table/{name}.json'
        write_table(destination, payload)
        assert read_json(destination) == payload
    if args.publish:
        for name in TABLES[:2]:
            write_table(RESOURCES / f'data/lbc/loot_table/ported/{name}.json', tables[name])
    report = {'sourceTables': {name: hashlib.sha256(
        (SOURCE / f'loot_table/{name}.json').read_bytes()).hexdigest() for name in TABLES},
        'nativeLeaves': leaves, 'published': args.publish,
        'presentation': 'source-equal except previously approved Mobgun/Minigun changes',
        'selection': 'original nested weights, quality, luck=7 condition and Razor seeds retained'}
    target = ROOT / 'build/verification/shooter-pools-source-validation.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('Verified 7 source tables, 14 weapon leaves / 13 native models; source selection preserved.')
    print('Read-only RU/EN/RPR and model dependencies passed; published:', args.publish)


if __name__ == '__main__':
    main()
