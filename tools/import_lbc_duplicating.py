#!/usr/bin/env python3
"""Import complete Duplicating and independent Divider source payloads; audit read-only RP."""
from __future__ import annotations
import hashlib
import json
from import_lbc_consuming import RP, migrated
from import_lbc_fireblaze import PORTED, import_item, select_entry
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family, build_single_entry_table, write_table

NAMES = ('duplicating_wand', 'academy_duplicating_wand', 'divider')

def main():
    source = SOURCE / 'loot_table/tuntijaa_greed_avarice.json'
    import_wand_family(source, PORTED / 'duplicating_wand.json', PORTED / 'academy_duplicating_wand.json',
        'dublicating_wand', 'skywars2:duplicating_wand', 'skywars2:academy_duplicating_wand',
        academy_modifiers=('add_upgrade_desc', 'tiers/ss'), data_marker='dublicating_wand:1')
    import_item(SOURCE / 'loot_table/greed_greed.json', PORTED / 'divider.json', 'divider', 'greed_greed:1', 'skywars2:divider')
    # Compare whole migrated source payloads; only outer native item identity changes.
    for name, path, key, marker in (
        ('duplicating_wand', source, 'dublicating_wand', 'dublicating_wand:1'),
        ('divider', SOURCE / 'loot_table/greed_greed.json', 'divider', 'greed_greed:1')):
        table = migrated(path)
        pool, entry = select_entry(table, key, marker)
        entry = json.loads(json.dumps(entry)); entry['name'] = 'skywars2:' + name
        assert json.loads((PORTED / f'{name}.json').read_text(encoding='utf-8')) == build_single_entry_table(table, pool, entry)
    expected = json.loads((PORTED / 'duplicating_wand.json').read_text(encoding='utf-8'))
    entry = expected['pools'][0]['entries'][0]; entry['name'] = 'skywars2:academy_duplicating_wand'
    functions = entry['modifier']
    custom = next(fn for fn in functions if fn.get('type') == 'minecraft:set_custom_data')
    custom['tag'] = custom['tag'].rstrip('}') + ',upgradable:0,upgrade_available:1,upgraded:1,mode:1}'
    for name in ('add_upgrade_desc', 'tiers/ss'):
        modifier = migrated(SOURCE / f'item_modifier/{name}.json')
        functions.extend(modifier if isinstance(modifier, list) else [modifier])
    assert json.loads((PORTED / 'academy_duplicating_wand.json').read_text(encoding='utf-8')) == expected
    keys = {'upgrade_condition_dublicating', 'upgrade_succesful_dublicating', 'mode_default', 'mode_expand', 'mode_divide', 'mode_black_hole'}
    models = set(); tooltip_styles = set()
    def visit(value):
        if isinstance(value, list):
            for child in value: visit(child)
        elif isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            if 'minecraft:item_model' in value: models.add(value['minecraft:item_model'])
            if 'minecraft:tooltip_style' in value: tooltip_styles.add(value['minecraft:tooltip_style'])
            for child in value.values(): visit(child)
    paths = [PORTED / f'{name}.json' for name in NAMES]
    for path in paths: visit(json.loads(path.read_text(encoding='utf-8')))
    languages = {}
    for language in ('ru_ru', 'en_us'):
        path = RP / f'assets/minecraft/lang/{language}.json'
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        missing = sorted(keys - data.keys())
        if missing: raise SystemExit(f'Missing {language} keys: {missing}')
        languages[language] = {'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'values': {key: data[key] for key in sorted(keys)}}
    for identifier in models:
        namespace, name = identifier.split(':')
        assert (RP / f'assets/{namespace}/items/{name}.json').exists(), identifier
    for identifier in tooltip_styles:
        namespace, name = identifier.split(':')
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/{namespace}/textures/gui/sprites/tooltip/{name}_{suffix}.png').exists(), (identifier, suffix)
    report = {'nativeIds': ['skywars2:' + name for name in NAMES],
        'sourceComparison': 'complete payload equality passed',
        'paths': [path.relative_to(ROOT).as_posix() for path in paths],
        'sha256': {path.relative_to(ROOT).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest() for path in paths},
        'design': {'keys': sorted(keys), 'models': sorted(models), 'tooltipStyles': sorted(tooltip_styles), 'languages': languages},
        'resourcePackModified': False, 'templatesNeeded': []}
    write_table(ROOT / 'agent_notes/DUPLICATING_RESOURCE_MANIFEST.json', report)
    print('Verified three complete item payloads and existing RU/EN keys, models and tooltip sprites; no RP/template changes')

if __name__ == '__main__':
    main()

